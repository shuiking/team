package com.lk.team.model.request;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import java.io.Serializable;
import java.util.Date;

/**
 * 更新队伍请求类
 * @Author : lk
 * @create 2023/5/6 20:12
 */
@Data
public class TeamUpdateRequest implements Serializable {
    private Long id;
    /**
     * 队伍名
     */
    private String teamName;

    /**
     * 队伍头像
     */
    private String teamAvatarUrl;

    /**
     * 队伍加密密码
     */
    private String teamPassword;

    /**
     * 队伍描述
     */
    private String teamDesc;

    /**
     * 最大人数
     */
    private Integer maxNum;

    /**
     * 过期时间
     */
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date expireTime;

    /**
     * 0 - 公开，1 - 私有，2 - 加密
     */
    private Integer teamStatus;

    /**
     * 公告
     */
    private String announce;
}
