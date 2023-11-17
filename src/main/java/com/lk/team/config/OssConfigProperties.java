package com.lk.team.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 自定义阿里云oss文件上传属性
 * @Author : lk
 * @create 2023/5/11 22:45
 */
@ConfigurationProperties(prefix="lk.oss")
@Component
@Data
public class OssConfigProperties {
    private String accessKey;
    private String secretKey;
    private String endpoint;
    private String bucket;

}
