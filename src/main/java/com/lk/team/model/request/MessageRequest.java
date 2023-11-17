package com.lk.team.model.request;

import lombok.Data;

import java.io.Serializable;

/**
 * websocket接收信息请求类
 * @Author : lk
 * @create 2023/5/11 16:10
 */
@Data
public class MessageRequest implements Serializable {
    private Long toId;
    private Long teamId;
    private String text;
    private Integer chatType;
    private boolean isAdmin;
}
