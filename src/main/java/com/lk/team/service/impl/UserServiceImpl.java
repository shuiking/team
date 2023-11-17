package com.lk.team.service.impl;
import cn.hutool.core.util.RandomUtil;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.TypeReference;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.lk.team.common.ErrorCode;
import com.lk.team.constant.Md5Constant;
import com.lk.team.constant.RedisConstant;
import com.lk.team.constant.UserConstant;
import com.lk.team.exception.ApiException;
import com.lk.team.mapper.UserMapper;
import com.lk.team.model.entity.User;
import com.lk.team.model.request.UpdateTagRequest;
import com.lk.team.model.request.UserQueryRequest;
import com.lk.team.model.request.UserUpdatePassword;
import com.lk.team.model.vo.UserVo;
import com.lk.team.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.util.DigestUtils;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;


/**
* @author lk
* @description 针对表【user(用户表)】的数据库操作Service实现
* @createDate 2023-05-05 19:22:34
*/
@Service
@Slf4j
public class UserServiceImpl extends ServiceImpl<UserMapper,User> implements UserService {
    @Autowired
    private RedisTemplate<String,Object> redisTemplate;

    @Override
    public long userRegistration(String username, String userAccount, String userPassword, String checkPassword) {
        // 1. 非空
        if (StringUtils.isAnyBlank(userAccount, userPassword, checkPassword)) {
            throw new ApiException(ErrorCode.PARAMS_ERROR, "输入不能为空");
        }
        // 2. 账户长度不小于4位
        if (userAccount.length() < 4) {
            throw new ApiException(ErrorCode.PARAMS_ERROR, "账号不能小于4位");
        }
        if (!StringUtils.isAnyBlank(username) && username.length() > 20) {
            throw new ApiException(ErrorCode.PARAMS_ERROR, "昵称不能超过20个字符");
        }
        // 2. 账户长度不大于16位
        if (userAccount.length() > 16) {
            throw new ApiException(ErrorCode.PARAMS_ERROR, "账号不能能大于16位");
        }
        // 3. 密码就不小于8位吧
        if (userPassword.length() < 8 || checkPassword.length() < 8) {
            throw new ApiException(ErrorCode.PARAMS_ERROR, "密码小于8位");
        }
        //  5. 账户不包含特殊字符
        // 匹配由数字、小写字母、大写字母组成的字符串,且字符串的长度至少为1个字符
        String pattern = "[0-9a-zA-Z]+";
        if (!userAccount.matches(pattern)) {
            throw new ApiException(ErrorCode.PARAMS_ERROR, "账号包含特殊字符");
        }
        // 6. 密码和校验密码相同
        if (!userPassword.equals(checkPassword)) {
            throw new ApiException(ErrorCode.PARAMS_ERROR, "输入密码不一致");
        }

        // 4. 账户不能重复
        QueryWrapper<User> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("userAccount", userAccount);
        long count = this.count(queryWrapper);
        if (count > 0) {
            throw new ApiException(ErrorCode.PARAMS_ERROR, "账号已存在");
        }

        String encryptPassword = DigestUtils.md5DigestAsHex((Md5Constant.USER_SALT+ userPassword).getBytes(StandardCharsets.UTF_8));
        User user = new User();
        user.setUserAccount(userAccount);
        user.setUserPassword(encryptPassword);
        user.setUsername(username);
        user.setTags("[]");
        user.setTeamIds("[]");
        user.setUserIds("[]");

        boolean saveResult = this.save(user);
        if (!saveResult) {
            throw new ApiException(ErrorCode.PARAMS_ERROR, "注册失败 ");
        }
        return user.getId();
    }

    @Override
    public UserVo userLogin(String userAccount, String userPassword, HttpServletRequest request) {
        // 1. 非空
        if (StringUtils.isAnyBlank(userAccount, userPassword)) {
            throw new ApiException(ErrorCode.PARAMS_ERROR, "输入不能为空");
        }
        // 2. 账户长度不小于4位
        if (userAccount.length() < 4) {
            throw new ApiException(ErrorCode.PARAMS_ERROR, "账号不能小于4位");
        }
        // 2. 账户长度不大于16位
        if (userAccount.length() > 16) {
            throw new ApiException(ErrorCode.PARAMS_ERROR, "账号不能大于16位");
        }
        // 3. 密码就不小于8位吧
        if (userPassword.length() < 8) {
            throw new ApiException(ErrorCode.PARAMS_ERROR, "密码小于8位 ");
        }
        //  5. 账户不包含特殊字符
        String pattern = "[0-9a-zA-Z]+";
        if (!userAccount.matches(pattern)) {
            throw new ApiException(ErrorCode.PARAMS_ERROR, "账号不能包含特殊字符");
        }

        //md5非对称加密
        String encryptPassword = DigestUtils.md5DigestAsHex((Md5Constant.USER_SALT + userPassword).getBytes(StandardCharsets.UTF_8));

        //查询用户是否存在
        QueryWrapper<User> userQueryWrapper = new QueryWrapper<>();
        userQueryWrapper.eq("userAccount", userAccount);
        userQueryWrapper.eq("userPassword", encryptPassword);
        User user = this.getOne(userQueryWrapper);

        // 用户不存在
        if (user == null) {
            log.info("user login failed,userAccount cannot match userPassword");
            throw new ApiException(ErrorCode.PARAMS_ERROR, "用户名或密码错误 ");
        }

        // 用户脱敏
        UserVo safeUser=new UserVo();
        BeanUtils.copyProperties(user,safeUser);

        // 记录用户的登录态
        request.getSession().setAttribute(UserConstant.LOGIN_USER_STATUS, safeUser);

        return safeUser;
    }

    @Override
    public Integer loginOut(HttpServletRequest request) {
        request.getSession().removeAttribute(UserConstant.LOGIN_USER_STATUS);
        return 1;
    }

    @Override
    public List<UserVo> searchUserByTags(Set<String> tagNameSet) {
        if(CollectionUtils.isEmpty(tagNameSet)){
            throw new ApiException(ErrorCode.PARAMS_ERROR);
        }

        //查询所有的用户
        QueryWrapper<User> userQueryWrapper=new QueryWrapper<>();
        List<User> userList = this.baseMapper.selectList(userQueryWrapper);

        // 在内存中查询符合要求的标签
        return userList.stream().filter(user -> {
            String tagsStr = user.getTags();
            Set<String> tempTagNameStr = JSON.parseObject(tagsStr, new TypeReference<Set<String>>() {
            });
            // 是否为空，为空返回HashSet的默认值，否则返回数值
            tempTagNameStr = Optional.ofNullable(tempTagNameStr).orElse(new HashSet<>());
            // tempTagNameStr集合中每一个元素首字母转换为大写
            tempTagNameStr = tempTagNameStr.stream().map(StringUtils::capitalize).collect(Collectors.toSet());
            // 返回false会过滤掉
            for (String tagName : tagNameSet) {
                tagName = StringUtils.capitalize(tagName);
                if (!tempTagNameStr.contains(tagName)) {
                    return false;
                }
            }
            return true;
        }).map(user -> {
            UserVo userVo = new UserVo();
            BeanUtils.copyProperties(user, userVo);
            return userVo;
        }).collect(Collectors.toList());

    }


    @Override
    public boolean isAdmin(UserVo user) {
        return user != null && user.getUserRole() == UserConstant.ADMIN_ROLE;
    }

    @Override
    public UserVo getLoginUser(HttpServletRequest request) {
        if (request == null) {
            return null;
        }
        UserVo loginUser = (UserVo) request.getSession().getAttribute(UserConstant.LOGIN_USER_STATUS);
        if (loginUser == null) {
            throw new ApiException(ErrorCode.NOT_LOGIN, "请先登录");
        }
        return loginUser;
    }


    @Override
    @Transactional(rollbackFor = Exception.class)
    public int updateUser(User user, UserVo currentUser) {
        long userId = user.getId();
        if (userId <= 0) {
            throw new ApiException(ErrorCode.PARAMS_ERROR);
        }
        if (!StringUtils.isAnyBlank(user.getUserDesc()) && user.getUserDesc().length() > 30) {
            throw new ApiException(ErrorCode.PARAMS_ERROR, "简介不能超过30个字符");
        }
        if (!StringUtils.isAnyBlank(user.getUsername()) && user.getUsername().length() > 20) {
            throw new ApiException(ErrorCode.PARAMS_ERROR, "昵称不能超过20个字符");
        }
        if (!StringUtils.isAnyBlank(user.getContactInfo()) && user.getContactInfo().length() > 18) {
            throw new ApiException(ErrorCode.PARAMS_ERROR, "联系方式不能超过18个字符");
        }
        // 如果是管理员，允许更新任意用户
        // 如果不是管理员，只允许更新当前（自己的）信息
        if (!isAdmin(currentUser) && userId != currentUser.getId()) {
            throw new ApiException(ErrorCode.NO_AUTH, "无权限");
        }
        QueryWrapper<User> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("userAccount", user.getUserAccount());
        long count = this.count(queryWrapper);
        if (count > 0) {
            throw new ApiException(ErrorCode.PARAMS_ERROR, "账号已存在  请重新输入");
        }
        User oldUser = this.baseMapper.selectById(userId);
        if (oldUser == null) {
            throw new ApiException(ErrorCode.NULL_ERROR);
        }
        return this.baseMapper.updateById(user);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int updateTagById(UpdateTagRequest updateTag, UserVo currentUser) {
        long id = updateTag.getId();
        if (id <= 0) {
            throw new ApiException(ErrorCode.PARAMS_ERROR, "该用户不存在");
        }
        Set<String> newTags = updateTag.getTagList();
        if (newTags.size() > 12) {
            throw new ApiException(ErrorCode.PARAMS_ERROR, "最多设置12个标签");
        }

        if (!isAdmin(currentUser) && id != currentUser.getId()) {
            throw new ApiException(ErrorCode.NO_AUTH, "无权限");
        }
        User user = this.baseMapper.selectById(id);
        Set<String> oldTags = JSON.parseObject(user.getTags(), new TypeReference<Set<String>>() {
        });
        Set<String> oldTagsCapitalize = toCapitalize(oldTags);
        Set<String> newTagsCapitalize = toCapitalize(newTags);

        // 添加 newTagsCapitalize 中 oldTagsCapitalize 中不存在的元素
        oldTagsCapitalize.addAll(newTagsCapitalize.stream().filter(tag -> !oldTagsCapitalize.contains(tag)).collect(Collectors.toSet()));

        // 移除 oldTagsCapitalize 中 newTagsCapitalize 中不存在的元素
        oldTagsCapitalize.removeAll(oldTagsCapitalize.stream().filter(tag -> !newTagsCapitalize.contains(tag)).collect(Collectors.toSet()));
        String tagsJson = JSON.toJSONString(oldTagsCapitalize);
        user.setTags(tagsJson);
        return this.baseMapper.updateById(user);
    }

    /**
     * String类型集合首字母大写
     *
     * @param oldSet 原集合
     * @return 首字母大写的集合
     */
    private Set<String> toCapitalize(Set<String> oldSet) {
        return oldSet.stream().map(StringUtils::capitalize).collect(Collectors.toSet());
    }

    @Override
    public int updatePasswordById(UserUpdatePassword updatePassword, UserVo currentUser) {
        long id = updatePassword.getId();
        String oldPassword = updatePassword.getOldPassword();
        String newPassword = updatePassword.getNewPassword();
        String checkPassword = updatePassword.getCheckPassword();
        if (StringUtils.isAnyBlank(oldPassword, newPassword, checkPassword)) {
            throw new ApiException(ErrorCode.PARAMS_ERROR, "输入有误");
        }
        // 密码就不小于8位吧
        if (oldPassword.length() < 8 || checkPassword.length() < 8 || newPassword.length() < 8) {
            throw new ApiException(ErrorCode.PARAMS_ERROR, "密码小于8位");
        }
        // 密码和校验密码相同
        if (!newPassword.equals(checkPassword)) {
            throw new ApiException(ErrorCode.PARAMS_ERROR, "输入密码不一致");
        }
        if (!isAdmin(currentUser) && currentUser.getId() != id) {
            throw new ApiException(ErrorCode.NO_AUTH, "权限不足");
        }

        String encryptPassword = DigestUtils.md5DigestAsHex((Md5Constant.USER_SALT+ oldPassword).getBytes(StandardCharsets.UTF_8));
        QueryWrapper<User> userQueryWrapper = new QueryWrapper<>();
        userQueryWrapper.eq("userAccount", currentUser.getUserAccount());
        userQueryWrapper.eq("userPassword", encryptPassword);
        User user = this.getOne(userQueryWrapper);
        // 用户不存在
        if (user == null) {
            log.info("user login failed,userAccount cannot match userPassword");
            throw new ApiException(ErrorCode.PARAMS_ERROR, "密码错误");
        }

        String newEncryptPassword = DigestUtils.md5DigestAsHex((Md5Constant.USER_SALT + newPassword).getBytes(StandardCharsets.UTF_8));

        user.setUserPassword(newEncryptPassword);

        return this.baseMapper.updateById(user);
    }

    @Override
    public boolean deleteFriend(UserVo currentUser, Long id) {
        User loginUser = this.getById(currentUser.getId());
        User friendUser = this.getById(id);
        //当前用户的朋友列表
        Set<Long> user = JSON.parseObject(loginUser.getUserIds(), new TypeReference<Set<Long>>() {
        });
        //当前朋友的朋友列表
        Set<Long> friend = JSON.parseObject(friendUser.getUserIds(), new TypeReference<Set<Long>>() {
        });
        user.remove(id);
        friend.remove(loginUser.getId());
        String userStr = JSON.toJSONString(user);
        String friendStr = JSON.toJSONString(friend);
        //更新当前用户
        loginUser.setUserIds(userStr);
        //更新当前好友
        friendUser.setUserIds(friendStr);
        return this.updateById(loginUser) && this.updateById(friendUser);
    }

    @Override
    public List<UserVo> getFriendsById(UserVo currentUser) {
        //查缓存
        ValueOperations<String, Object> ops = redisTemplate.opsForValue();
        List<UserVo> userVoList= (List<UserVo>)ops.get(RedisConstant.USER_FRIEND + currentUser.getId());
        if(userVoList!=null)
        {
            return userVoList;
        }
        //查数据库
        User loginUser = this.getById(currentUser.getId());
        Set<Long> friendsId = JSON.parseObject(loginUser.getUserIds(), new TypeReference<Set<Long>>() {
        });
        List<UserVo> vos = friendsId.stream().map(friendId -> {
            User user = this.getById(friendId);
            UserVo userVo = new UserVo();
            BeanUtils.copyProperties(user, userVo);
            return userVo;
        }).collect(Collectors.toList());

        //把数据放进redis
        ops.set(RedisConstant.USER_FRIEND+currentUser.getId(),vos,1 + RandomUtil.randomInt(1, 2) / 10, TimeUnit.SECONDS);
        return vos;
    }

    @Override
    public List<UserVo> searchFriend(UserQueryRequest userQueryRequest, UserVo currentUser) {
        String searchText = userQueryRequest.getSearchText();
        User user = this.getById(currentUser.getId());
        Set<Long> friendsId = JSON.parseObject(user.getUserIds(), new TypeReference<Set<Long>>() {
        });
        //TODO 待优化
        List<UserVo> users = new ArrayList<>();

        friendsId.forEach(id -> {
            User u = this.getById(id);
            if (u.getUsername().contains(searchText)) {
                UserVo userVo=new UserVo();
                BeanUtils.copyProperties(u,userVo);
                users.add(userVo);
            }
        });
        return users;
    }

    @Override
    public List<UserVo> getUserList(UserVo loginUser) {
        ValueOperations<String, Object> ops = redisTemplate.opsForValue();

        //获取缓存
        List<UserVo> list = (List<UserVo>) ops.get(RedisConstant.USER_SEARCH + loginUser.getId());

        //判断是否有缓存
        if(list!=null)
        {
            return list;
        }

        //获取除了自己外全部的用户
        QueryWrapper<User> queryWrapper=new QueryWrapper<>();
        queryWrapper.ne("id",loginUser.getId());
        List<User> userList = this.list(queryWrapper);

        //敏感信息过滤
        List<UserVo> vos = userList.stream().map(user -> {
            UserVo userVo = new UserVo();
            BeanUtils.copyProperties(user, userVo);
            return userVo;
        }).collect(Collectors.toList());

        ops.set(RedisConstant.USER_SEARCH + loginUser.getId(),vos,1 + RandomUtil.randomInt(1, 2) / 10, TimeUnit.SECONDS);
        return vos;
    }

}




