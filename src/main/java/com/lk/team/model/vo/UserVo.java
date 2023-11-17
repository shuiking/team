package com.lk.team.model.vo;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 用户信息脱敏
 * @Author : lk
 * @create 2023/5/5 18:53
 */
@Data
public class UserVo implements Serializable {
    private Long id;
    private String username;
    private String userAccount;
    private String userAvatarUrl;
    private Integer gender;
    private String contactInfo;
    private String userDesc;
    private Integer userStatus;
    private Integer userRole;
    private String tags;
    private String teamIds;
    private Date createTime;
    private Date updateTime;
    private String email;
}
