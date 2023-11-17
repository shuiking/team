package com.lk.team.model.request;

import lombok.Data;

import java.io.Serializable;
import java.util.Set;

/**
 * 用户更新标签请求体
 * @Author : lk
 * @create 2023/5/5 22:04
 */
@Data
public class UpdateTagRequest implements Serializable {
    private long id;
    private Set<String> tagList;
}
