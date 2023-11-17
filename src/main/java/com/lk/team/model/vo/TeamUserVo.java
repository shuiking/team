package com.lk.team.model.vo;

import lombok.Data;

import java.io.Serializable;
import java.util.Set;

/**
 * 用户和队伍信息脱敏
 * @Author : lk
 * @create 2023/5/6 20:06
 */
@Data
public class TeamUserVo implements Serializable {
    private Set<TeamVo> teamSet;
}
