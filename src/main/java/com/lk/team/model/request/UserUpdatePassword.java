package com.lk.team.model.request;

import lombok.Data;

import java.io.Serializable;

/**
 * 用户更新密码请求体
 * @Author : lk
 * @create 2023/5/5 22:40
 */
@Data
public class UserUpdatePassword implements Serializable {
    long id;
    private String oldPassword;
    private String newPassword;
    private String checkPassword;
}
