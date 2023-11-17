package com.lk.team.model.entity;

import com.baomidou.mybatisplus.annotation.*;

import java.io.Serializable;
import java.util.Date;
import lombok.Data;

/**
 * 队伍
 * @TableName team
 */
@TableName(value ="team")
@Data
public class Team implements Serializable {
    /**
     * id
     */
    @TableId(type = IdType.AUTO)
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
    private Date expireTime;

    /**
     * 创建人id
     */
    private Long userId;

    /**
     * 加入队伍的用户id
     */
    private String usersId;

    /**
     * 创建时间
     */
    private Date createTime;

    /**
     * 0 - 公开，1 - 私有，2 - 加密
     */
    private Integer teamStatus;

    /**
     * 是否删除
     */
    @TableLogic
    private Integer isDelete;

    /**
     * 队伍公告
     */
    private String announce;

    /**
     * 更新时间
     */
    private Date updateTime;

    @TableField(exist = false)
    private static final long serialVersionUID = 1L;
}