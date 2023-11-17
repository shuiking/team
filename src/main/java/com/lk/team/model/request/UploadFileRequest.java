package com.lk.team.model.request;

import lombok.Data;

import java.io.Serializable;

/**
 * 文件上传请求类
 * @Author : lk
 * @create 2023/5/11 22:16
 */
@Data
public class UploadFileRequest implements Serializable {
    /**
     * 业务
     */
    private String biz;
}
