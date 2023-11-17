package com.lk.team.constant;

/**
 * 用户相关的常量
 * @Author : lk
 * @create 2023/5/5 19:45
 */
public interface UserConstant {
    /**
     * 用户登录态键值
     */
    String LOGIN_USER_STATUS = "loginUserStatus";

    /**
     * 用户缓存键名称
     */
    String USER_REDIS_KEY = String.format("lk:user:search:%s", "qimu");

    /**
     * 标签缓存键名称
     */
    String TAGS_REDIS_KEY = String.format("lk:tags:%s", "tags");

    /**
     * 默认权限
     */
    int DEFAULT_ROLE = 0;

    /**
     * 管理员权限
     */
    int ADMIN_ROLE = 1;

    /**
     * 未登录最大可以看多少条
     */
    int NOT_LONGIN_LOOK_MAX = 10;
}
