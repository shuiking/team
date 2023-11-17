package com.lk.team.service;

import com.lk.team.model.entity.User;
import com.baomidou.mybatisplus.extension.service.IService;
import com.lk.team.model.request.UpdateTagRequest;
import com.lk.team.model.request.UserQueryRequest;
import com.lk.team.model.request.UserUpdatePassword;
import com.lk.team.model.vo.UserVo;

import javax.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Set;

/**
* @author k
* @description 针对表【user(用户表)】的数据库操作Service
* @createDate 2023-05-05 19:22:34
*/
public interface UserService extends IService<User> {
    /**
     * 用户注册
     * @param username      用户名
     * @param userAccount   用户账号
     * @param userPassword  用户密码
     * @param checkPassword 确认密码
     * @return 新注册用户的id
     */
    long userRegistration(String username, String userAccount, String userPassword, String checkPassword);

    /**
     * 用户登录
     * @param userAccount  用户账号
     * @param userPassword 用户密码
     * @param request      记录用户的登录态
     * @return 登陆成功的用户信息（脱敏之后）
     */
    UserVo userLogin(String userAccount, String userPassword, HttpServletRequest request);

    /**
     * 退出登录
     * @param request
     * @return
     */
    Integer loginOut(HttpServletRequest request);

    /**
     * 根据标签搜索用户
     * @param tagNameSet
     * @return
     */
    List<UserVo> searchUserByTags(Set<String> tagNameSet);


    /**
     * 获取当前用户是否为管理员
     * @param user
     * @return
     */
    boolean isAdmin(UserVo user);


    /**
     * 获取当前登录信息
     *
     * @param request
     * @return
     */
    UserVo getLoginUser(HttpServletRequest request);

    /**
     * 修改用户
     * @param user
     * @param loginUser
     * @return
     */
    int updateUser(User user, UserVo loginUser);

    /**
     * 修改标签
     * @param updateTag   修改标签dto
     * @param loginUser 当前用户
     * @return
     */
    int updateTagById(UpdateTagRequest updateTag, UserVo loginUser);

    /**
     * 修改密码
     * @param updatePassword
     * @param loginUser
     * @return
     */
    int updatePasswordById(UserUpdatePassword updatePassword, UserVo loginUser);

    /**
     * 删除好友
     * @param currentUser
     * @param id
     * @return
     */
    boolean deleteFriend(UserVo currentUser, Long id);

    /**
     * 根据id获取好友列表
     * @param loginUser
     * @return
     */
    List<UserVo> getFriendsById(UserVo loginUser);

    /**
     * 搜索好友
     * @param userQueryRequest
     * @param loginUser
     * @return
     */
    List<UserVo> searchFriend(UserQueryRequest userQueryRequest, UserVo loginUser);

    /**
     * 获取用户列表
     * @return
     */
    List<UserVo> getUserList(UserVo loginUser);
}
