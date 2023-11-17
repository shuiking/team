package com.lk.team.model.request;

import lombok.Data;

import java.io.Serializable;

/**
 * 转交队长的请求体
 * @Author : lk
 * @create 2023/5/9 21:32
 */
@Data
public class TransferTeamRequest implements Serializable {
    private String userAccount;
    private Long teamId;
}
