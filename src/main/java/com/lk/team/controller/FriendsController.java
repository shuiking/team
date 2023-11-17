package com.lk.team.controller;

import com.lk.team.common.BaseResponse;
import com.lk.team.common.ErrorCode;
import com.lk.team.common.ResultUtil;
import com.lk.team.exception.ApiException;
import com.lk.team.model.request.FriendAddRequest;
import com.lk.team.model.vo.FriendsRecordVO;
import com.lk.team.model.vo.UserVo;
import com.lk.team.service.FriendsService;
import com.lk.team.service.UserService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.CollectionUtils;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Set;

/**
 * @Author : lk
 * @create 2023/5/5 18:29
 */
@Api(tags = "好友管理")
@RestController
@RequestMapping("/friends")
@Slf4j
public class FriendsController {
    @Autowired
    private FriendsService friendsService;
    @Autowired
    private UserService userService;

    @PostMapping("add")
    @ApiOperation("添加好友")
    public BaseResponse<Boolean> addFriendRecords(@RequestBody FriendAddRequest friendAddRequest, HttpServletRequest request) {
        if (friendAddRequest == null) {
            throw new ApiException(ErrorCode.PARAMS_ERROR, "请求有误");
        }
        UserVo loginUser = userService.getLoginUser(request);
        boolean addStatus = friendsService.addFriendRecords(loginUser, friendAddRequest);
        return ResultUtil.success(addStatus, "申请成功");
    }

    @GetMapping("getRecords")
    @ApiOperation("查询申请或同意的记录")
    public BaseResponse<List<FriendsRecordVO>> getRecords(HttpServletRequest request) {
        UserVo loginUser = userService.getLoginUser(request);
        List<FriendsRecordVO> friendsList = friendsService.obtainFriendApplicationRecords(loginUser);
        return ResultUtil.success(friendsList);
    }

    @PostMapping("agree/{fromId}")
    @ApiOperation("同意申请")
    public BaseResponse<Boolean> agreeToApply(@PathVariable("fromId") Long fromId,HttpServletRequest request) {
        if(fromId==null){
            throw new ApiException(ErrorCode.PARAMS_ERROR);
        }
        UserVo loginUser = userService.getLoginUser(request);
        boolean agreeToApplyStatus = friendsService.agreeToApply(loginUser, fromId);
        return ResultUtil.success(agreeToApplyStatus);
    }

    @PostMapping("canceledApply/{id}")
    @ApiOperation("拒绝申请")
    public BaseResponse<Boolean> canceledApply(@PathVariable("id") Long id,HttpServletRequest request) {
        if (id == null) {
            throw new ApiException(ErrorCode.PARAMS_ERROR, "请求有误");
        }
        UserVo loginUser = userService.getLoginUser(request);
        boolean canceledApplyStatus = friendsService.canceledApply(id, loginUser);
        return ResultUtil.success(canceledApplyStatus);
    }

    @GetMapping("getMyRecords")
    @ApiOperation("获取我申请的记录")
    public BaseResponse<List<FriendsRecordVO>> getMyRecords(HttpServletRequest request) {
        UserVo loginUser = userService.getLoginUser(request);
        List<FriendsRecordVO> myFriendsList = friendsService.getMyRecords(loginUser);
        return ResultUtil.success(myFriendsList);
    }

    @GetMapping("getRecordCount")
    @ApiOperation("获取未读记录条数")
    public BaseResponse<Integer> getRecordCount(HttpServletRequest request) {
        UserVo loginUser = userService.getLoginUser(request);
        int recordCount = friendsService.getRecordCount(loginUser);
        return ResultUtil.success(recordCount);
    }

    @GetMapping("/read")
    @ApiOperation("读取纪录")
    public BaseResponse<Boolean> toRead(@RequestParam(required = false) Set<Long> ids,HttpServletRequest request) {
        if (CollectionUtils.isEmpty(ids)) {
            return ResultUtil.success(false);
        }
        UserVo loginUser = userService.getLoginUser(request);
        boolean isRead = friendsService.toRead(loginUser, ids);
        return ResultUtil.success(isRead);
    }
}
