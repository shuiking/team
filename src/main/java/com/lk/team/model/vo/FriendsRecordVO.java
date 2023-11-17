package com.lk.team.model.vo;

import com.lk.team.model.entity.User;
import lombok.Data;

import java.io.Serializable;

/**
 * 用户申请好友或同意的返回结果类
 * @Author : lk
 * @create 2023/5/9 22:49
 */
@Data
public class FriendsRecordVO implements Serializable {
    private Long id;

    /**
     * 申请状态 默认0 （0-未通过 1-已同意 2-已过期）
     */
    private Integer status;

    /**
     * 好友申请备注信息
     */
    private String remark;

    /**
     * 申请用户
     */
    private UserVo applyUser;
}
