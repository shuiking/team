package com.lk.team.constant;

/**
 * 队伍信息的常量
 * @Author : lk
 * @create 2023/5/8 21:26
 */
public interface TeamConstant {
    /**
     * 队伍共开状态(默认)
     */
    int PUBLIC_TEAM_STATUS = 0;
    /**
     * 队伍私有状态
     */
    int PRIVATE_TEAM_STATUS = 1;
    /**
     * 队伍加密状态
     */
    int ENCRYPTION_TEAM_STATUS = 2;

    /**
     * 候补人数
     */
    int NUMBER_OF_PLACES_TO_BE_FILLED = 2;
}
