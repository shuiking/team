package com.lk.team.model.vo;

import lombok.Data;

import java.io.Serializable;

/**
 * 聊天消息返回类
 * @Author : lk
 * @create 2023/5/10 21:37
 */
@Data
public class MessageVo implements Serializable {
    private WebSocketVo formUser;
    private WebSocketVo toUser;
    private Long teamId;
    private String text;
    private Boolean isMy = false;
    private Integer chatType;
    private Boolean isAdmin = false;
    private String createTime;
}
