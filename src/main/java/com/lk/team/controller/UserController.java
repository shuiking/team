package com.lk.team.controller;
import com.lk.team.common.BaseResponse;
import com.lk.team.common.ErrorCode;
import com.lk.team.common.ResultUtil;
import com.lk.team.constant.RedisConstant;
import com.lk.team.exception.ApiException;
import com.lk.team.model.entity.User;
import com.lk.team.model.request.*;
import com.lk.team.model.vo.UserVo;
import com.lk.team.service.UserService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.util.CollectionUtils;
import org.springframework.web.bind.annotation.*;
import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Set;;

/**
 * @Author : lk
 * @create 2023/5/5 17:57
 */

@Api(tags = "用户管理")
@RestController
@RequestMapping("/user")
@Slf4j
public class UserController {
    @Autowired
    private UserService userService;
    @Autowired
    private StringRedisTemplate redisTemplate;

    @PostMapping("/login")
    @ApiOperation("用户登陆")
    public BaseResponse<UserVo> login(@RequestBody UserLoginRequest loginRequest, HttpServletRequest request){

        if(loginRequest==null){
            throw new ApiException(ErrorCode.PARAMS_ERROR);
        }
        String userAccount = loginRequest.getUserAccount();
        String userPassword = loginRequest.getUserPassword();
        UserVo user = userService.userLogin(userAccount, userPassword, request);
        return ResultUtil.success(user, "登陆成功");
    }

    @GetMapping("/current")
    @ApiOperation("获取当前的用户")
    public BaseResponse<UserVo> getCurrentUser(HttpServletRequest request) {
        UserVo loginUser = userService.getLoginUser(request);
        Long userId = loginUser.getId();
        User user = userService.getById(userId);
        UserVo userVo=new UserVo();
        BeanUtils.copyProperties(user,userVo);
        return ResultUtil.success(userVo);
    }

    @GetMapping("/search")
    @ApiOperation("获取用户列表")
    public BaseResponse<List<UserVo>> searchList(HttpServletRequest request) {
        UserVo loginUser = userService.getLoginUser(request);
        List<UserVo> userList = userService.getUserList(loginUser);
        return ResultUtil.success(userList);
    }

    @PostMapping("/register")
    @ApiOperation("用户注册")
    public BaseResponse<Long> register(@RequestBody UserRegisterRequest registerRequest) {
        if (registerRequest == null) {
            throw new ApiException(ErrorCode.PARAMS_ERROR);
        }

        String username = registerRequest.getUsername();
        String userAccount = registerRequest.getUserAccount();
        String userPassword = registerRequest.getUserPassword();
        String checkPassword = registerRequest.getCheckPassword();
        if (StringUtils.isAnyBlank(userAccount, userPassword, checkPassword)) {
            throw new ApiException(ErrorCode.PARAMS_ERROR);
        }
        long result = userService.userRegistration(username, userAccount, userPassword, checkPassword);
        return ResultUtil.success(result, "注册成功");
    }

    @PostMapping("/loginOut")
    @ApiOperation("用户退出")
    public BaseResponse<Integer> loginOut(HttpServletRequest request) {
        if (request == null) {
            throw new ApiException(ErrorCode.PARAMS_ERROR);
        }
        return ResultUtil.success(userService.loginOut(request));
    }

    @GetMapping("/{id}")
    @ApiOperation("查询用户信息")
    public BaseResponse<UserVo> getUserById(@PathVariable("id") Integer id) {
        if (id == null) {
            throw new ApiException(ErrorCode.PARAMS_ERROR);
        }
        User user = userService.getById(id);
        //用户脱敏
        UserVo safeUser=new UserVo();
        BeanUtils.copyProperties(user,safeUser);
        return ResultUtil.success(safeUser);
    }

    @GetMapping("/search/tags")
    @ApiOperation("根据标签搜索用户")
    public BaseResponse<List<UserVo>> searchUsersByTags(@RequestParam(required = false) Set<String> tagNameList) {
        if (CollectionUtils.isEmpty(tagNameList)) {
            throw new ApiException(ErrorCode.PARAMS_ERROR);
        }
        List<UserVo> userList = userService.searchUserByTags(tagNameList);
        return ResultUtil.success(userList);
    }

    @PostMapping("/update")
    @ApiOperation("更新用户信息")
    public BaseResponse<Integer> getUpdateUserById(@RequestBody User user,HttpServletRequest request) {
        if (user == null) {
            throw new ApiException(ErrorCode.PARAMS_ERROR);
        }
        UserVo loginUser = userService.getLoginUser(request);
        int updateId = userService.updateUser(user, loginUser);
        redisTemplate.delete(RedisConstant.USER_SEARCH+ loginUser.getId());
        return ResultUtil.success(updateId);
    }

    @PostMapping("/update/tags")
    @ApiOperation("更新用户标签")
    public BaseResponse<Integer> updateTagById(@RequestBody UpdateTagRequest tagRequest,HttpServletRequest request) {
        if (tagRequest == null) {
            throw new ApiException(ErrorCode.PARAMS_ERROR);
        }
        UserVo loginUser = userService.getLoginUser(request);
        int updateTag = userService.updateTagById(tagRequest, loginUser);
        redisTemplate.delete(RedisConstant.USER_SEARCH+ loginUser.getId());
        return ResultUtil.success(updateTag);
    }

    @PostMapping("/update/password")
    @ApiOperation("用户密码修改")
    public BaseResponse<Integer> updatePassword(@RequestBody UserUpdatePassword updatePassword,HttpServletRequest request) {
        if (updatePassword == null) {
            throw new ApiException(ErrorCode.PARAMS_ERROR);
        }
        UserVo loginUser = userService.getLoginUser(request);
        int updateTag = userService.updatePasswordById(updatePassword, loginUser);
        return ResultUtil.success(updateTag);
    }

    @GetMapping("/friends")
    @ApiOperation("朋友列表")
    public BaseResponse<List<UserVo>> getFriends(HttpServletRequest request) {
        UserVo loginUser = userService.getLoginUser(request);
        List<UserVo> getUser = userService.getFriendsById(loginUser);
        return ResultUtil.success(getUser);
    }

    @PostMapping("/deleteFriend/{id}")
    @ApiOperation("删除朋友")
    public BaseResponse<Boolean> deleteFriend(@PathVariable("id") Long id,HttpServletRequest request) {
        if (id == null) {
            throw new ApiException(ErrorCode.PARAMS_ERROR);
        }
        UserVo loginUser = userService.getLoginUser(request);
        boolean deleteFriend = userService.deleteFriend(loginUser, id);
        redisTemplate.delete(RedisConstant.USER_FRIEND+loginUser.getId());
        return ResultUtil.success(deleteFriend);
    }

    @PostMapping("/searchFriend")
    @ApiOperation("查找朋友")
    public BaseResponse<List<UserVo>> searchFriend(@RequestBody UserQueryRequest userQueryRequest,HttpServletRequest request) {
        if (userQueryRequest == null) {
            throw new ApiException(ErrorCode.PARAMS_ERROR);
        }
        UserVo loginUser = userService.getLoginUser(request);
        List<UserVo> searchFriend = userService.searchFriend(userQueryRequest, loginUser);
        return ResultUtil.success(searchFriend);
    }
}
