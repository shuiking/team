package com.lk.team.model.request;

import lombok.Data;

import java.io.Serializable;

/**
 * 踢出用户的请求类
 * @Author : lk
 * @create 2023/5/9 20:51
 */
@Data
public class KickOutUserRequest implements Serializable {
    Long teamId;
    Long userId;
}
