package com.lk.team.service.impl;

import cn.hutool.core.util.RandomUtil;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.TypeReference;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.lk.team.common.ErrorCode;
import com.lk.team.constant.Md5Constant;
import com.lk.team.constant.RedisConstant;
import com.lk.team.constant.TeamConstant;
import com.lk.team.exception.ApiException;
import com.lk.team.mapper.TeamMapper;
import com.lk.team.model.entity.Team;
import com.lk.team.model.entity.User;
import com.lk.team.model.request.*;
import com.lk.team.model.vo.TeamUserVo;
import com.lk.team.model.vo.TeamVo;
import com.lk.team.model.vo.UserVo;
import com.lk.team.service.TeamService;
import com.lk.team.service.UserService;
import org.apache.commons.lang3.StringUtils;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.util.DigestUtils;

import javax.annotation.Resource;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
* @author k
* @description 针对表【team(队伍)】的数据库操作Service实现
* @createDate 2023-05-05 19:22:29
*/
@Service
public class TeamServiceImpl extends ServiceImpl<TeamMapper,Team> implements TeamService {
    @Autowired
    private UserService userService;
    @Autowired
    private RedissonClient redissonClient;
    @Autowired
    private RedisTemplate<String,Object> redisTemplate;

    @Override
    public TeamUserVo getTeams() {
        //从缓存中获取全部队伍
        ValueOperations<String, Object> ops = redisTemplate.opsForValue();
        TeamUserVo teamList =(TeamUserVo)ops.get(RedisConstant.TEAM_LIST);
        if(teamList!=null)
        {
            return teamList;
        }
        List<Team> teams = this.list();
        TeamUserVo teamUserVo = teamSet(teams);
        //把数据存到redis中
        ops.set(RedisConstant.TEAM_LIST,teamUserVo,1 + RandomUtil.randomInt(1, 2) / 10, TimeUnit.SECONDS);
        return teamUserVo;
    }


    @Override
    public TeamVo getUsersByTeamId(Long teamId,UserVo loginUser) {
        // 当前用户是否登录
        User user = userService.getById(loginUser.getId());
        // 当前用户加入的队伍id
        String userTeamIds = user.getTeamIds();
        Team team = this.getById(teamId);

        // 创建队伍者id
        Long userId = team.getUserId();

        // 当前用户加入的队伍的id
        Set<Long> userTeamIdSet=JSON.parseObject(userTeamIds,new TypeReference<Set<Long>>() {
        });

        // 当前用户不是管理员
        // 当前用户加入的队伍的ids中不包含传过来的队伍id
        // 当前用户的id不等于队伍的创建者id 说明没权限
        boolean noPermissions = !userService.isAdmin(loginUser) && !userTeamIdSet.contains(teamId) && loginUser.getId() != userId;
        if (noPermissions) {
            throw new ApiException(ErrorCode.NO_AUTH, "暂无权限查看");
        }

        TeamVo teamVo = new TeamVo();

        setTeamVo(team,userId,teamVo);

        return teamVo;
    }


    @Override
    public TeamUserVo getTeamListByTeamIds(Set<Long> teamId,UserVo loginUser) {
        if (CollectionUtils.isEmpty(teamId)) {
            throw new ApiException(ErrorCode.NULL_ERROR, "信息有误");
        }
        //查缓存
        ValueOperations<String, Object> ops = redisTemplate.opsForValue();
        TeamUserVo teamUserVo = (TeamUserVo)ops.get(RedisConstant.TEAM_USER + loginUser.getId());
        if(teamUserVo!=null)
        {
            return teamUserVo;
        }
        // 获取所有队伍
        List<Team> teams = this.list();
        // 过滤后的队伍列表
        List<Team> teamList = teams.stream().filter(team -> {
            for (Long tid : teamId) {
                // 保留当前没有过期的队伍和搜索的队伍
                if (!new Date().after(team.getExpireTime()) && tid.equals(team.getId())) {
                    return true;
                }
            }
            return false;
        }).collect(Collectors.toList());
        TeamUserVo vo = teamSet(teamList);
        //设置缓存
        ops.set(RedisConstant.TEAM_USER + loginUser.getId(),vo,1 + RandomUtil.randomInt(1, 2) / 10, TimeUnit.SECONDS);
        return vo;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean createTeam(TeamCreateRequest teamCreateRequest, UserVo loginUser) {
        //添加队伍信息
        Team team = new Team();

        //参数校验
        teamCheck(team, teamCreateRequest.getTeamName(), teamCreateRequest.getAnnounce(), teamCreateRequest.getTeamDesc(), teamCreateRequest.getExpireTime(), teamCreateRequest.getMaxNum(), teamCreateRequest.getTeamPassword(),team.getTeamPassword(), teamCreateRequest.getTeamStatus());

        //获取当前用户的id
        long id = loginUser.getId();
        //获取队长的信息
        User user = userService.getById(id);
        //队长创建或加入的队伍
        String teamIds = user.getTeamIds();
        //redisson锁
        RLock lock = redissonClient.getLock("lk:create_team");

        try {
            // 抢到锁并执行
            while (true) {
                if (lock.tryLock(0, -1, TimeUnit.MILLISECONDS)) {
                    Set<Long> teamIdList=JSON.parseObject(teamIds, new TypeReference<Set<Long>>() {
                    });
                    //普通成员
                    if (!userService.isAdmin(loginUser) && teamIdList.size() >= 5) {
                        throw new ApiException(ErrorCode.PARAMS_ERROR, "最多只能拥有5个队伍");
                    }

                    //赋值
                    team.setTeamName(teamCreateRequest.getTeamName());
                    team.setTeamDesc(teamCreateRequest.getTeamDesc());
                    team.setMaxNum(teamCreateRequest.getMaxNum());
                    team.setExpireTime(teamCreateRequest.getExpireTime());
                    team.setUserId(loginUser.getId());
                    team.setUsersId("[]");
                    team.setTeamStatus(teamCreateRequest.getTeamStatus());
                    team.setCreateTime(new Date());
                    team.setUpdateTime(new Date());
                    team.setAnnounce(teamCreateRequest.getAnnounce());
                    team.setTeamAvatarUrl(teamCreateRequest.getTeamAvatarUrl());

                    //添加队伍
                    boolean createTeam = this.save(team);
                    if (!createTeam) {
                        throw new ApiException(ErrorCode.PARAMS_ERROR, "创建队伍失败");
                    }

                    //获取新队伍的信息
                    Team newTeam = this.getById(team);
                    String usersId = newTeam.getUsersId();

                    Set<Long> usersIdList = JSON.parseObject(usersId, new TypeReference<Set<Long>>() {
                    });
                    //创建人的信息加入到队伍中
                    usersIdList.add(loginUser.getId());

                    //新队伍队员列表
                    String users=JSON.toJSONString(usersIdList);
                    newTeam.setUsersId(users);
                    teamIdList.add(newTeam.getId());

                    //用户新队伍json数组
                    String teams=JSON.toJSONString(teamIdList);
                    user.setTeamIds(teams);

                    return userService.updateById(user) && this.updateById(newTeam);
                }
            }
        } catch (InterruptedException e) {
            log.error("joinTeam error", e);
            return false;
        } finally {
            // 只能释放自己的锁
            if (lock.isHeldByCurrentThread()) {
                System.out.println("unLock: " + Thread.currentThread().getId());
                lock.unlock();
            }
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean dissolutionTeam(Long teamId, UserVo loginUser) {

        //获取队伍
        Team team = this.getById(teamId);

        //判断是否是管理员或者队长
        if (!userService.isAdmin(loginUser) && loginUser.getId() != team.getUserId()) {
            throw new ApiException(ErrorCode.NOT_LOGIN, "暂无权限");
        }

        //TODO 删除优化
        List<User> users = userService.list();
        //清除所有加入该队伍的成员
        users.forEach(user -> {
            Set<Long> teamIds = JSON.parseObject(user.getTeamIds(), new TypeReference<Set<Long>>() {
            });
            teamIds.remove(team.getId());
            user.setTeamIds(JSON.toJSONString(teamIds));
            //更新用户的信息
            userService.updateById(user);

        });

        //删除队伍
        return this.removeById(team);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean quitTeam(Long teamId, UserVo loginUser) {
        //获取以前的队伍和当前的用户对象
        Team team = this.getById(teamId);

        //反序列化后移除用户加入的队伍和队伍中的成员信息
        String userIdsStr = loginUser.getTeamIds();
        String teamIdsStr = team.getUsersId();
        Set<Long> usersIds = JSON.parseObject(userIdsStr, new TypeReference<Set<Long>>() {
        });
        Set<Long> teamIds = JSON.parseObject(teamIdsStr, new TypeReference<Set<Long>>() {
        });
        usersIds.remove(teamId);
        teamIds.remove(loginUser.getId());

        //序列化
        String userStr = JSON.toJSONString(usersIds);
        String teamStr = JSON.toJSONString(teamIds);

        //更新对象信息
        loginUser.setTeamIds(userStr);
        team.setUsersId(teamStr);
        User user=new User();
        BeanUtils.copyProperties(loginUser,user);
        return this.updateById(team)&&userService.updateById(user);
    }

    @Override
    @Transactional
    public Boolean updateTeam(TeamUpdateRequest teamUpdateRequest, UserVo loginUser) {
        Long id = teamUpdateRequest.getId();
        Team oldTeam = this.getById(id);

        // 只有管理员或者队伍的创建者可以修改
        if (oldTeam.getUserId() != loginUser.getId() && !userService.isAdmin(loginUser)) {
            throw new ApiException(ErrorCode.NO_AUTH, "暂无权限");
        }

        if (oldTeam == null) {
            throw new ApiException(ErrorCode.NULL_ERROR, "队伍不存在");
        }

        if (id == null || id <= 0) {
            throw new ApiException(ErrorCode.PARAMS_ERROR, "当前队伍不存在");
        }

        //队伍参数校验
        teamCheck(oldTeam, teamUpdateRequest.getTeamName(),teamUpdateRequest.getAnnounce(), teamUpdateRequest.getTeamDesc(), teamUpdateRequest.getExpireTime(),teamUpdateRequest.getMaxNum(),teamUpdateRequest.getTeamPassword(), oldTeam.getTeamPassword(), teamUpdateRequest.getTeamStatus());

        oldTeam.setTeamName(teamUpdateRequest.getTeamName());
        oldTeam.setTeamAvatarUrl(teamUpdateRequest.getTeamAvatarUrl());
        oldTeam.setTeamDesc(teamUpdateRequest.getTeamDesc());
        oldTeam.setMaxNum(teamUpdateRequest.getMaxNum());
        oldTeam.setExpireTime(teamUpdateRequest.getExpireTime());
        oldTeam.setTeamStatus(teamUpdateRequest.getTeamStatus());
        oldTeam.setUpdateTime(new Date());
        oldTeam.setAnnounce(teamUpdateRequest.getAnnounce());
        return this.updateById(oldTeam);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public UserVo joinTeam(TeamJoinRequest joinTeam, UserVo loginUser) {
        if (joinTeam == null) {
            throw new ApiException(ErrorCode.PARAMS_ERROR, "加入队伍有误");
        }
        //获取当前用户的信息
        Team team = this.getById(joinTeam.getTeamId());
        Date expireTime = team.getExpireTime();

        // 当前队伍有没有过期
        if (expireTime != null && expireTime.before(new Date())) {
            throw new ApiException(ErrorCode.PARAMS_ERROR, "当前队伍已过期");
        }

        // 当前队伍有没有私密
        if (team.getTeamStatus() == TeamConstant.PRIVATE_TEAM_STATUS) {
            throw new ApiException(ErrorCode.PARAMS_ERROR, "当前队伍私有,不可加入");
        }
        // 队伍密码加密
        String encryptPassword = DigestUtils.md5DigestAsHex((Md5Constant.TEAM_SALT + joinTeam.getPassword()).getBytes(StandardCharsets.UTF_8));
        // 当前队伍是加密队伍
        // 不是管理员需要密码
        if (!userService.isAdmin(loginUser) && team.getTeamStatus() == TeamConstant.ENCRYPTION_TEAM_STATUS) {
            if (StringUtils.isBlank(joinTeam.getPassword()) || !encryptPassword.equals(team.getTeamPassword())) {
                throw new ApiException(ErrorCode.PARAMS_ERROR, "密码错误");
            }
        }
        RLock lock = redissonClient.getLock("lk:join_team");
        try {
            // 抢到锁并执行
            while (true) {
                if (lock.tryLock(0, -1, TimeUnit.MILLISECONDS)) {
                    // 当前队伍加入的队员id
                    String usersId = team.getUsersId();
                    Set<Long> userIdList=JSON.parseObject(usersId,new TypeReference<Set<Long>>(){});

                    // 当前队伍是不是已经满人了
                    // 可以补位两个人
                    if (userIdList.size() >= team.getMaxNum() +TeamConstant.NUMBER_OF_PLACES_TO_BE_FILLED) {
                        throw new ApiException(ErrorCode.PARAMS_ERROR, "当前队伍人数已满");
                    }


                    // 当前用户已经加入的队伍
                    User user = userService.getById(loginUser);
                    String teamIds = user.getTeamIds();

                    Set<Long> loginUserTeamIdList=JSON.parseObject(teamIds,new TypeReference<Set<Long>>(){});
                    // 最多加入5个队伍
                    if (!userService.isAdmin(loginUser) && loginUserTeamIdList.size() >= 5) {
                        throw new ApiException(ErrorCode.PARAMS_ERROR, "最多加入5个队伍");
                    }
                    // 是否已经加入该队伍
                    if (userIdList.contains(loginUser.getId()) || loginUserTeamIdList.contains(joinTeam.getTeamId())) {
                        throw new ApiException(ErrorCode.PARAMS_ERROR, "已经加入过当前队伍");
                    }

                    userIdList.add(loginUser.getId());
                    String newUserid=JSON.toJSONString(userIdList);

                    //更新队伍的信息
                    team.setUsersId(newUserid);

                    loginUserTeamIdList.add(joinTeam.getTeamId());
                    String loginTeamsId=JSON.toJSONString(loginUserTeamIdList);

                    //更新用户的信息
                    user.setTeamIds(loginTeamsId);

                    boolean joinTeamStatus = this.updateById(team) && userService.updateById(user);

                    if (!joinTeamStatus) {
                        throw new ApiException(ErrorCode.PARAMS_ERROR, "加入失败");
                    }
                    UserVo vo=new UserVo();
                    BeanUtils.copyProperties(user,vo);

                    return vo;
                }
            }
        } catch (InterruptedException e) {
            log.error("joinTeam error", e);
            return null;
        } finally {
            // 只能释放自己的锁
            if (lock.isHeldByCurrentThread()) {
                System.out.println("unLock: " + Thread.currentThread().getId());
                lock.unlock();
            }
        }
    }

    @Override
    public Boolean kickOutTeamByUserId(KickOutUserRequest kickOutUserRequest, UserVo loginUser) {
        //获取当前队伍id和踢出用户的id
        Long teamId = kickOutUserRequest.getTeamId();
        Long userId = kickOutUserRequest.getUserId();

        if (teamId == null || teamId <= 0) {
            throw new ApiException(ErrorCode.PARAMS_ERROR, "该队伍不存在");
        }
        if (userId == null || userId <= 0) {
            throw new ApiException(ErrorCode.PARAMS_ERROR, "该队员不存在");
        }
        //当前队伍的创建者
        Team team = this.getById(teamId);

        //被踢出者
        User user = userService.getById(userId);

        // 当前用户不是管理员、踢出的用户是队伍的创建者、当前用户不是队伍的创建者无法踢出队员
        if (!userService.isAdmin(loginUser) || team.getUserId().equals(userId) || loginUser.getId() != team.getUserId()) {
            throw new ApiException(ErrorCode.KICK_OUT_USER, "权限不足");
        }

        //队伍的成员
        String usersId = team.getUsersId();

        //被踢出者加入的队伍
        String userTeamIds = user.getTeamIds();

        //反序列化为集合
        Set<Long> teamIds = JSON.parseObject(usersId, new TypeReference<Set<Long>>() {
        });
        Set<Long> userIds = JSON.parseObject(userTeamIds, new TypeReference<Set<Long>>() {
        });

        //// 用户和队伍都删除各自的id
        teamIds.remove(userId);
        userIds.remove(teamId);

        //序列化成字符串
        String teamStr = JSON.toJSONString(teamIds);
        String userStr = JSON.toJSONString(userIds);

        //保存最新的信息到对象中
        team.setUsersId(teamStr);
        user.setTeamIds(userStr);

        //更新信息
        return this.updateById(team)&&userService.updateById(user);
    }

    @Override
    public TeamUserVo teamQuery(TeamQueryRequest teamQueryRequest) {

        //获取搜索文本信息
        String searchText = teamQueryRequest.getSearchText();

        LambdaQueryWrapper<Team> queryWrapper=new LambdaQueryWrapper<>();
        queryWrapper.like(Team::getTeamDesc,searchText.trim()).or()
                .like(Team::getTeamName,searchText.trim());

        List<Team> list = this.list(queryWrapper);

        //过滤队伍列表
        return teamSet(list);
    }

    @Override
    public Boolean transferTeam(TransferTeamRequest transferTeamRequest, UserVo loginUser) {
        if (transferTeamRequest.getTeamId() == null || transferTeamRequest.getTeamId() <= 0) {
            throw new ApiException(ErrorCode.PARAMS_ERROR, "该队伍不存在");
        }
        if (StringUtils.isBlank(transferTeamRequest.getUserAccount())) {
            throw new ApiException(ErrorCode.PARAMS_ERROR, "账号不能为空");
        }

        //获取当前队伍
        Team team = this.getById(transferTeamRequest.getTeamId());

        //获取队伍的id和转交者的账户
        String userAccount = transferTeamRequest.getUserAccount();
        Long teamId = transferTeamRequest.getTeamId();

        //查询用户是否存在
        LambdaQueryWrapper<User> queryWrapper=new LambdaQueryWrapper<>();
        queryWrapper.eq(User::getUserAccount,userAccount);
        User user = userService.getOne(queryWrapper);

        //判断用户是否存在
        if(user==null)
        {
            throw new ApiException(ErrorCode.NULL_ERROR, "该用户不存在");
        }

        Set<Long> userIds = JSON.parseObject(team.getUsersId(),new TypeReference<Set<Long>>(){});
        Set<Long> teamIds = JSON.parseObject(user.getTeamIds(),new TypeReference<Set<Long>>(){});

        // 新队长不在队伍中的
        if (!userIds.contains(user.getId()) || !teamIds.contains(teamId)) {
            throw new ApiException(ErrorCode.KICK_OUT_USER, "输入用户不在队伍中");
        }
        // 当前用户不是管理员、当前用户也不是队伍的创建者无法转移
        if (!userService.isAdmin(loginUser) && loginUser.getId() != team.getUserId()) {
            throw new ApiException(ErrorCode.KICK_OUT_USER, "权限不足");
        }
        // 队伍的创建者修改为新用户
        team.setUserId(user.getId());

        return this.updateById(team);
    }

    /**
     * 队伍参数校验
     * @param team
     * @param name
     * @param announce
     * @param teamDesc
     * @param expireTime
     * @param maxNum
     * @param newPassword
     * @param oldPassword
     * @param teamStatus
     */
    private void teamCheck(Team team, String name, String announce, String teamDesc, Date expireTime, Integer maxNum, String newPassword, String oldPassword, Integer teamStatus) {
        if (StringUtils.isAnyBlank(name, announce, teamDesc)) {
            throw new ApiException(ErrorCode.PARAMS_ERROR, "输入不能为空");
        }
        if (name.length() > 16) {
            throw new ApiException(ErrorCode.PARAMS_ERROR, "队伍名称不能超过16个字符");
        }
        if (teamDesc.length() > 50) {
            throw new ApiException(ErrorCode.PARAMS_ERROR, "队伍描述不能超过50个字符");
        }
        if (announce.length() > 50) {
            throw new ApiException(ErrorCode.PARAMS_ERROR, "队伍公告不能超过50个字符");
        }
        if (expireTime == null) {
            throw new ApiException(ErrorCode.PARAMS_ERROR, "过期时间不能为空");
        }
        // 过期时间在当前日期之前
        if (new Date().after(expireTime)) {
            throw new ApiException(ErrorCode.PARAMS_ERROR, "过期时间不能在当前时间之前");
        }
        if (maxNum == null || maxNum > 10) {
            throw new ApiException(ErrorCode.PARAMS_ERROR, "队伍最多只能容纳10人");
        }
        if (maxNum < 5) {
            throw new ApiException(ErrorCode.PARAMS_ERROR, "队伍最少要有5人");
        }

        int status = Optional.ofNullable(teamStatus).orElse(0);


        if (status == TeamConstant.ENCRYPTION_TEAM_STATUS) {
            if (!StringUtils.isBlank(newPassword)) {
                // 加密队伍校验
                encryptTeamCheck(newPassword, team);
            }
            if (StringUtils.isBlank(oldPassword)) {
                throw new ApiException(ErrorCode.PARAMS_ERROR, "加密状态,必须设置密码");
            }
        }
    }


    /**
     * 队伍加密和校验
     * @param teamCreateRequest
     * @param team
     */
    private void encryptTeamCheck(String teamCreateRequest, Team team) {
        if (teamCreateRequest.length() < 6) {
            throw new ApiException(ErrorCode.PARAMS_ERROR, "队伍密码长度低于6位");
        }
        if (teamCreateRequest.length() > 16) {
            throw new ApiException(ErrorCode.PARAMS_ERROR, "密码最长只能设置16位");
        }
        // 加密
        String encryptPassword = DigestUtils.md5DigestAsHex((Md5Constant.TEAM_SALT + teamCreateRequest).getBytes(StandardCharsets.UTF_8));
        team.setTeamPassword(encryptPassword);
    }

    /**
     * 处理返回信息Vo
     * @param teamList
     * @return
     */
    private TeamUserVo teamSet(List<Team> teamList) {
        // 过滤过期的队伍
        List<Team> listTeam = teamList.stream()
                .filter(team -> !new Date().after(team.getExpireTime()))
                .collect(Collectors.toList());

        //打乱顺序
        Collections.shuffle(listTeam);

        //返回的vo
        TeamUserVo teamUserVo = new TeamUserVo();


        Set<TeamVo> users = new HashSet<>();
        listTeam.forEach(team -> {
            Long userId=team.getUserId();

            TeamVo teamVo = new TeamVo();

            setTeamVo(team, userId, teamVo);

            users.add(teamVo);
        });
        teamUserVo.setTeamSet(users);
        return teamUserVo;
    }

    /**
     * 队伍返回类的封装
     * @param team
     * @param userId
     * @param teamVo
     */
    private void setTeamVo(Team team, Long userId, TeamVo teamVo) {
        //用户返回类的集合
        Set<UserVo> userList = new HashSet<>();

        //当前队伍普通成员的id
        String usersId = team.getUsersId();

        //string转化为集合
        Set<Long> userSet = JSON.parseObject(usersId, new TypeReference<Set<Long>>() {
        });

        //当前队伍普通成员的信息脱敏
        for (Long id : userSet) {
            User byId = userService.getById(id);
            UserVo vo = new UserVo();
            BeanUtils.copyProperties(byId,vo);
            userList.add(vo);
        }

        //当前队伍队长信息的脱敏
        User createUser = userService.getById(userId);
        UserVo safetyUser=new UserVo();
        BeanUtils.copyProperties(createUser,safetyUser);

        //返回信息的封装
        teamVo.setId(team.getId());
        teamVo.setTeamName(team.getTeamName());
        teamVo.setTeamAvatarUrl(team.getTeamAvatarUrl());
        teamVo.setTeamDesc(team.getTeamDesc());
        teamVo.setMaxNum(team.getMaxNum());
        teamVo.setExpireTime(team.getExpireTime());
        teamVo.setTeamStatus(team.getTeamStatus());
        teamVo.setCreateTime(team.getCreateTime());
        teamVo.setAnnounce(team.getAnnounce());
        teamVo.setUserSet(userList);
        teamVo.setUserVo(safetyUser);
    }
}




