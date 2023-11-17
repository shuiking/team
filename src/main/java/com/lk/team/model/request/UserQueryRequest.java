package com.lk.team.model.request;

import lombok.Data;

import java.io.Serializable;

/**
 * 查找朋友请求类
 * @Author : lk
 * @create 2023/5/6 19:07
 */
@Data
public class UserQueryRequest implements Serializable {
    private String searchText;
}
