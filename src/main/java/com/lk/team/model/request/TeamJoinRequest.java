package com.lk.team.model.request;

import lombok.Data;

import java.io.Serializable;

/**
 * 加入队伍请求类
 * @Author : lk
 * @create 2023/5/6 20:10
 */
@Data
public class TeamJoinRequest implements Serializable {
    /**
     * 队伍id
     */
    private Long teamId;
    /**
     * 队伍密码
     */
    private String password;
}
