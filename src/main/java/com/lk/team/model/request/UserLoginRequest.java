package com.lk.team.model.request;

import lombok.Data;

import java.io.Serializable;

/**
 * 用户登陆请求体
 * @Author : lk
 * @create 2023/5/5 18:54
 */
@Data
public class UserLoginRequest implements Serializable {
    private String userAccount;
    private String userPassword;
}
