package com.lk.team.model.request;

import lombok.Data;

import java.io.Serializable;

/**
 * 用户注册请求体
 * @Author : lk
 * @create 2023/5/5 19:55
 */
@Data
public class UserRegisterRequest implements Serializable {
    private String username;
    private String userAccount;
    private String userPassword;
    private String checkPassword;
}
