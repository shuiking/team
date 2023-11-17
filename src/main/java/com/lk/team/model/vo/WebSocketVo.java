package com.lk.team.model.vo;

import lombok.Data;

import java.io.Serializable;

/**
 * 聊天信息请求类
 * @Author : lk
 * @create 2023/5/10 21:38
 */
@Data
public class WebSocketVo implements Serializable {
    private long id;

    /**
     * 用户昵称
     */
    private String username;

    /**
     * 账号
     */
    private String userAccount;

    /**
     * 用户头像
     */
    private String userAvatarUrl;
}
