package com.lk.team.model.request;

import lombok.Data;

import java.io.Serializable;

/**
 * 聊天功能请求类
 * @Author : lk
 * @create 2023/5/10 21:33
 */
@Data
public class ChatRequest implements Serializable {
    /**
     * 队伍聊天室id
     */
    private Long teamId;

    /**
     * 接收消息id
     */
    private Long toId;

}
