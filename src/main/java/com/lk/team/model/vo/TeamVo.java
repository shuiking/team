package com.lk.team.model.vo;

import com.lk.team.model.entity.User;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;
import java.util.Set;

/**
 * 队伍信息返回类
 * @Author : lk
 * @create 2023/5/6 20:04
 */
@Data
public class TeamVo implements Serializable {
    private Long id;

    private String teamName;

    private String teamAvatarUrl;

    private String teamPassword;

    private String teamDesc;

    private Integer maxNum;

    private Date expireTime;

    private Integer teamStatus;

    private Date createTime;

    private String announce;

    private UserVo userVo;

    private Set<UserVo> userSet;
}
