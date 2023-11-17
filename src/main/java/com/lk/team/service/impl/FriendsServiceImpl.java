package com.lk.team.service.impl;


import cn.hutool.core.date.DateUnit;
import cn.hutool.core.date.DateUtil;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.TypeReference;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.lk.team.common.ErrorCode;
import com.lk.team.constant.FriendConstant;
import com.lk.team.exception.ApiException;
import com.lk.team.mapper.FriendsMapper;
import com.lk.team.model.entity.Friends;
import com.lk.team.model.entity.User;
import com.lk.team.model.request.FriendAddRequest;
import com.lk.team.model.vo.FriendsRecordVO;
import com.lk.team.model.vo.UserVo;
import com.lk.team.service.FriendsService;
import com.lk.team.service.UserService;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

/**
* @author k
* @description 针对表【friends(好友申请管理表)】的数据库操作Service实现
* @createDate 2023-05-05 19:22:24
*/
@Service
public class FriendsServiceImpl extends ServiceImpl<FriendsMapper,Friends> implements FriendsService{
    @Autowired
    private RedissonClient redissonClient;
    @Autowired
    private UserService userService;

    @Override
    public boolean addFriendRecords(UserVo loginUser, FriendAddRequest friendAddRequest) {
        if (StringUtils.isNotBlank(friendAddRequest.getRemark()) && friendAddRequest.getRemark().length() > 120) {
            throw new ApiException(ErrorCode.PARAMS_ERROR, "申请备注最多120个字符");
        }
        if (ObjectUtils.anyNull(loginUser.getId(), friendAddRequest.getReceiveId())) {
            throw new ApiException(ErrorCode.PARAMS_ERROR, "添加失败");
        }
        // 1.添加的不能是自己
        if (loginUser.getId() == friendAddRequest.getReceiveId()) {
            throw new ApiException(ErrorCode.PARAMS_ERROR, "不能添加自己为好友");
        }

        RLock lock = redissonClient.getLock("lk:apply");

        try{
            //抢到锁
            if(lock.tryLock(0,-1, TimeUnit.MILLISECONDS)){
                LambdaQueryWrapper<Friends> queryWrapper=new LambdaQueryWrapper<>();
                //查看2人是否是好友关系
                queryWrapper.eq(Friends::getReceiveId,friendAddRequest.getReceiveId());
                queryWrapper.eq(Friends::getFromId,loginUser.getId());
                List<Friends> list = this.list(queryWrapper);

                //判断是否重复申请
                list.forEach(friends -> {
                    if(list.size()>1&&friends.getStatus()== FriendConstant.DEFAULT_STATUS){
                        throw new ApiException(ErrorCode.PARAMS_ERROR, "不能重复申请");
                    }
                });

                //添加好友
                Friends friends=new Friends();
                friends.setFromId(loginUser.getId());
                friends.setReceiveId(friendAddRequest.getReceiveId());

                //备注信息
                if (StringUtils.isBlank(friendAddRequest.getRemark())) {
                    //没有填备注
                    friends.setRemark("我是" + userService.getById(loginUser.getId()).getUsername());
                } else {
                    //填写了备注
                    friends.setRemark(friendAddRequest.getRemark());
                }

                //设置时间
                friends.setCreateTime(new Date());
                return this.save(friends);
            }
        }catch (Exception e){
            log.error("joinTeam error", e);
            return false;
        }finally {
            if(lock.isHeldByCurrentThread()){
                lock.unlock();
            }
        }
        return false;
    }

    @Override
    public List<FriendsRecordVO> obtainFriendApplicationRecords(UserVo loginUser) {
        LambdaQueryWrapper<Friends> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Friends::getReceiveId, loginUser.getId());
        List<Friends> list = this.list(queryWrapper);

        //返回类的封装
        return list.stream().map(friends -> {
            FriendsRecordVO friendsRecordVO = new FriendsRecordVO();
            BeanUtils.copyProperties(friends, friendsRecordVO);

            //封装当前用户的信息
            User user = userService.getById(friends.getFromId());
            UserVo userVo = new UserVo();
            BeanUtils.copyProperties(user, userVo);
            friendsRecordVO.setApplyUser(userVo);
            return friendsRecordVO;
        }).collect(Collectors.toList());

    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean agreeToApply(UserVo loginUser, Long fromId) {
        //查询申请记录
        LambdaQueryWrapper<Friends> queryWrapper=new LambdaQueryWrapper<>();
        queryWrapper.eq(Friends::getFromId,fromId);
        queryWrapper.eq(Friends::getReceiveId,loginUser.getId());
        long count = this.count(queryWrapper);
        if(count<1){
            throw new ApiException(ErrorCode.PARAMS_ERROR, "该申请不存在");
        }
        Friends one = this.getOne(queryWrapper);

        //校验时间
        if(DateUtil.between(new Date(),one.getCreateTime(), DateUnit.DAY)>=3|| one.getStatus() == FriendConstant.EXPIRED_STATUS)
        {
            throw new ApiException(ErrorCode.PARAMS_ERROR, "该申请已过期");
        }

        //校验是否已经为好友
        if(one.getStatus() == FriendConstant.AGREE_STATUS)
        {
            throw new ApiException(ErrorCode.PARAMS_ERROR, "该申请已同意");
        }

        // 分别查询receiveId和fromId的用户，更改userIds中的数据
        User receiveUser = userService.getById(loginUser.getId());
        User fromUser = userService.getById(fromId);
        Set<Long> receiveUserIds = JSON.parseObject(receiveUser.getUserIds(),new TypeReference<Set<Long>>(){});
        Set<Long> fromUserUserIds = JSON.parseObject(fromUser.getUserIds(),new TypeReference<Set<Long>>(){});

        fromUserUserIds.add(receiveUser.getId());
        receiveUserIds.add(fromUser.getId());

        String jsonFromUserUserIds = JSON.toJSONString(fromUserUserIds);
        String jsonReceiveUserIds = JSON.toJSONString(receiveUserIds);
        receiveUser.setUserIds(jsonReceiveUserIds);
        fromUser.setUserIds(jsonFromUserUserIds);

        one.setStatus(FriendConstant.AGREE_STATUS);

        return userService.updateById(fromUser) && userService.updateById(receiveUser) && this.updateById(one);

    }

    @Override
    public boolean canceledApply(Long id, UserVo loginUser) {
        Friends friend = this.getById(id);
        if (friend.getStatus() != FriendConstant.DEFAULT_STATUS) {
            throw new ApiException(ErrorCode.PARAMS_ERROR, "该申请已过期或已通过");
        }

        friend.setStatus(FriendConstant.REVOKE_STATUS);
        return this.updateById(friend);
    }

    @Override
    public List<FriendsRecordVO> getMyRecords(UserVo loginUser) {
        // 查询出当前用户所有申请记录
        LambdaQueryWrapper<Friends> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Friends::getFromId, loginUser.getId());
        List<Friends> friendsList = this.list(queryWrapper);

        //封装返回信息
        return friendsList.stream().map(friend -> {
            FriendsRecordVO friendsRecordVO = new FriendsRecordVO();
            BeanUtils.copyProperties(friend, friendsRecordVO);
            User user = userService.getById(friend.getReceiveId());
            UserVo userVo=new UserVo();
            BeanUtils.copyProperties(user,userVo);
            friendsRecordVO.setApplyUser(userVo);
            return friendsRecordVO;
        }).collect(Collectors.toList());
    }

    @Override
    public int getRecordCount(UserVo loginUser) {
        LambdaQueryWrapper<Friends> queryWrapper=new LambdaQueryWrapper<>();
        queryWrapper.eq(Friends::getReceiveId,loginUser.getId());
        List<Friends> list = this.list(queryWrapper);

        //过滤未处理的好友申请请求
        List<Friends> friendsList = list.stream().filter(friends -> {
            if (friends.getStatus() == FriendConstant.DEFAULT_STATUS && friends.getIsRead() == FriendConstant.NOT_READ) {
                return true;
            }
            return false;
        }).collect(Collectors.toList());
        return friendsList.size();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean toRead(UserVo loginUser, Set<Long> ids) {
        boolean flag = false;
        for (Long id : ids) {
            Friends friend = this.getById(id);
            if (friend.getStatus() == FriendConstant.DEFAULT_STATUS && friend.getIsRead() == FriendConstant.NOT_READ) {
                friend.setIsRead(FriendConstant.READ);
                flag = this.updateById(friend);
            }
        }
        return flag;
    }
}




