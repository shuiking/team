package com.lk.team.controller;

import com.lk.team.common.BaseResponse;
import com.lk.team.common.ErrorCode;
import com.lk.team.common.ResultUtil;
import com.lk.team.constant.CharConstant;
import com.lk.team.exception.ApiException;
import com.lk.team.model.request.ChatRequest;
import com.lk.team.model.vo.MessageVo;
import com.lk.team.model.vo.UserVo;
import com.lk.team.service.ChatService;
import com.lk.team.service.UserService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.List;

/**
 * @Author : lk
 * @create 2023/5/5 18:27
 */
@Api(tags = "用户聊天管理")
@RestController
@RequestMapping("/chat")
@Slf4j
public class ChatController {
    @Autowired
    private ChatService chatService;
    @Autowired
    private UserService userService;


    @PostMapping("/privateChat")
    @ApiOperation("私聊")
    public BaseResponse<List<MessageVo>> getPrivateChat(@RequestBody ChatRequest chatRequest, HttpServletRequest request) {
        if (chatRequest == null) {
            throw new ApiException(ErrorCode.PARAMS_ERROR, "请求有误");
        }
        UserVo loginUser = userService.getLoginUser(request);
        List<MessageVo> privateChat = chatService.getPrivateChat(chatRequest, CharConstant.PRIVATE_CHAT, loginUser);
        return ResultUtil.success(privateChat);
    }

    @GetMapping("/hallChat")
    @ApiOperation("大厅聊天")
    public BaseResponse<List<MessageVo>> getHallChat(HttpServletRequest request) {
        UserVo loginUser = userService.getLoginUser(request);
        List<MessageVo> hallChat = chatService.getHallChat(CharConstant.HALL_CHAT, loginUser);
        return ResultUtil.success(hallChat);
    }

    @PostMapping("/teamChat")
    @ApiOperation("组队聊天")
    public BaseResponse<List<MessageVo>> getTeamChat(@RequestBody ChatRequest chatRequest,HttpServletRequest request) {
        if (chatRequest == null) {
            throw new ApiException(ErrorCode.PARAMS_ERROR, "请求有误");
        }
        UserVo loginUser = userService.getLoginUser(request);
        List<MessageVo> teamChat = chatService.getTeamChat(chatRequest, CharConstant.TEAM_CHAT, loginUser);
        return ResultUtil.success(teamChat);
    }
}
