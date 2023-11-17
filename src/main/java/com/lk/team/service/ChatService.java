package com.lk.team.service;

import com.lk.team.model.entity.Chat;
import com.baomidou.mybatisplus.extension.service.IService;
import com.lk.team.model.entity.User;
import com.lk.team.model.request.ChatRequest;
import com.lk.team.model.vo.MessageVo;
import com.lk.team.model.vo.UserVo;

import java.util.Date;
import java.util.List;

/**
* @author k
* @description 针对表【chat(聊天消息表)】的数据库操作Service
* @createDate 2023-05-05 19:22:15
*/
public interface ChatService extends IService<Chat> {
    /**
     * 获取私聊聊天内容
     * @param chatRequest
     * @param chatType
     * @param loginUser
     * @return
     */
    List<MessageVo> getPrivateChat(ChatRequest chatRequest, int chatType, UserVo loginUser);

    /**
     * 获取大厅聊天纪录
     * @param chatType
     * @param loginUser
     * @return
     */
    List<MessageVo> getHallChat(int chatType, UserVo loginUser);


    /**
     * 队伍聊天室
     * @param chatRequest
     * @param chatType
     * @param loginUser
     * @return
     */
    List<MessageVo> getTeamChat(ChatRequest chatRequest, int chatType, UserVo loginUser);

    /**
     * 聊天记录映射
     * @param fromId
     * @param toId
     * @param text
     * @param chatType
     * @param createTime
     * @return
     */
    MessageVo chatResult(Long fromId, Long toId, String text, Integer chatType, Date createTime);

    /**
     * 删除缓存信息
     * @param key
     * @param id
     */
    void deleteKey(String key, String id);

}
