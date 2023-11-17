package com.lk.team.service;

import com.lk.team.model.entity.Friends;
import com.baomidou.mybatisplus.extension.service.IService;
import com.lk.team.model.entity.User;
import com.lk.team.model.request.FriendAddRequest;
import com.lk.team.model.vo.FriendsRecordVO;
import com.lk.team.model.vo.UserVo;

import java.util.List;
import java.util.Set;

/**
* @author k
* @description 针对表【friends(好友申请管理表)】的数据库操作Service
* @createDate 2023-05-05 19:22:24
*/
public interface FriendsService extends IService<Friends> {
    /**
     * 好友申请
     * @param loginUser
     * @param friendAddRequest
     * @return
     */
    boolean addFriendRecords(UserVo loginUser, FriendAddRequest friendAddRequest);

    /**
     * 查询出所有申请、同意记录
     * @param loginUser
     * @return
     */
    List<FriendsRecordVO> obtainFriendApplicationRecords(UserVo loginUser);

    /**
     * 同意好友
     * @param loginUser
     * @param fromId
     * @return
     */
    boolean agreeToApply(UserVo loginUser, Long fromId);

    /**
     * 撤销好友申请
     * @param id        申请记录id
     * @param loginUser 登录用户
     * @return
     */
    boolean canceledApply(Long id, UserVo loginUser);

    /**
     * 获取我申请的记录
     * @param loginUser
     * @return
     */
    List<FriendsRecordVO> getMyRecords(UserVo loginUser);

    /**
     * 获取未读记录条数
     * @param loginUser
     * @return
     */
    int getRecordCount(UserVo loginUser);

    /**
     * 读取纪录
     * @param loginUser
     * @param ids
     * @return
     */
    boolean toRead(UserVo loginUser, Set<Long> ids);
}
