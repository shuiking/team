package com.lk.team.model.request;

import lombok.Data;

import java.io.Serializable;

/**
 * 查询队伍请求类
 * @Author : lk
 * @create 2023/5/6 20:11
 */
@Data
public class TeamQueryRequest implements Serializable {
    /**
     * 查询队伍
     */
    private String searchText;
}
