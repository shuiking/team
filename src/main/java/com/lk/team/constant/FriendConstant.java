package com.lk.team.constant;

/**
 * 添加好友的常量
 * @Author : lk
 * @create 2023/5/9 22:33
 */
public interface FriendConstant {
    /**
     * 默认状态 未处理
     */
    int DEFAULT_STATUS = 0;
    /**
     * 已同意
     */
    int AGREE_STATUS = 1;
    /**
     * 已过期
     */
    int EXPIRED_STATUS = 2;

    /**
     * 撤销
     */
    int REVOKE_STATUS = 3;
    /**
     * 未读
     */
    int NOT_READ = 0;

    /**
     * 已读
     */
    int READ = 1;
}
