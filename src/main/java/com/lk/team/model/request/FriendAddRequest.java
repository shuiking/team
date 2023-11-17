package com.lk.team.model.request;

import lombok.Data;

import java.io.Serializable;

/**
 * 添加好友请求体
 * @Author : lk
 * @create 2023/5/9 22:11
 */
@Data
public class FriendAddRequest implements Serializable {
    private Long id;
    /**
     * 接收申请的用户id
     */
    private Long receiveId;

    /**
     * 好友申请备注信息
     */
    private String remark;
}
