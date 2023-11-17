package com.lk.team.service.impl;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.RandomUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.lk.team.common.ErrorCode;
import com.lk.team.constant.RedisConstant;
import com.lk.team.constant.UserConstant;
import com.lk.team.exception.ApiException;
import com.lk.team.mapper.ChatMapper;
import com.lk.team.model.entity.Chat;
import com.lk.team.model.entity.Team;
import com.lk.team.model.entity.User;
import com.lk.team.model.request.ChatRequest;
import com.lk.team.model.vo.MessageVo;
import com.lk.team.model.vo.UserVo;
import com.lk.team.model.vo.WebSocketVo;
import com.lk.team.service.ChatService;
import com.lk.team.service.TeamService;
import com.lk.team.service.UserService;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
* @author k
* @description 针对表【chat(聊天消息表)】的数据库操作Service实现
* @createDate 2023-05-05 19:22:15
*/
@Service
public class ChatServiceImpl extends ServiceImpl<ChatMapper,Chat> implements ChatService{
    @Autowired
    private UserService userService;
    @Autowired
    private TeamService teamService;
    @Autowired
    private RedisTemplate<String,Object> redisTemplate;

    @Override
    public List<MessageVo> getPrivateChat(ChatRequest chatRequest, int chatType, UserVo loginUser) {
        Long toId = chatRequest.getToId();
        //参数校验
        if (toId == null) {
            throw new ApiException(ErrorCode.PARAMS_ERROR, "状态异常请重试");
        }

        ValueOperations<String, Object> ops = redisTemplate.opsForValue();
        List<MessageVo> list= (List<MessageVo>)ops.get(RedisConstant.CHAR_PRIVATE + loginUser.getId()+":"+toId);

        if(list!=null){
            return list;
        }
        //双方的聊天记录
        LambdaQueryWrapper<Chat> queryWrapper=new LambdaQueryWrapper<>();
        queryWrapper.and(privateChat -> privateChat.eq(Chat::getFromId, loginUser.getId()).eq(Chat::getToId, toId)
                .or().
                eq(Chat::getToId, loginUser.getId()).eq(Chat::getFromId, toId))
                .eq(Chat::getChatType,chatType);

        //所有聊天记录
        List<Chat> charList = this.list(queryWrapper);

        //返回类的封装
        List<MessageVo> collect = charList.stream().map(chat -> {
            MessageVo messageVo = chatResult(loginUser.getId(), toId, chat.getText(), chatType, chat.getCreateTime());
            if (chat.getFromId().equals(loginUser.getId())) {
                messageVo.setIsMy(true);
            }
            return messageVo;
        }).collect(Collectors.toList());

        ops.set(RedisConstant.CHAR_PRIVATE + loginUser.getId()+":"+toId,collect,2 + RandomUtil.randomInt(2, 3) / 10, TimeUnit.MINUTES);
        return collect;


    }

    @Override
    public List<MessageVo> getHallChat(int chatType, UserVo loginUser) {
        ValueOperations<String, Object> ops = redisTemplate.opsForValue();
        List<MessageVo> list= (List<MessageVo>)ops.get(RedisConstant.CHAR_HALL);
        if(list!=null){
            return list;
        }
        //获取大厅所有聊天记录
        LambdaQueryWrapper<Chat> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Chat::getChatType, chatType);
        List<Chat> chatList = this.list(queryWrapper);
        List<MessageVo> vos = returnMessage(loginUser, null, chatList);
        ops.set(RedisConstant.CHAR_HALL,vos,2 + RandomUtil.randomInt(2, 3) / 10, TimeUnit.MINUTES);
        return vos;
    }

    @Override
    public List<MessageVo> getTeamChat(ChatRequest chatRequest, int chatType, UserVo loginUser) {
        Long teamId = chatRequest.getTeamId();
        //参数校验
        if (teamId == null) {
            throw new ApiException(ErrorCode.PARAMS_ERROR, "请求有误");
        }
        ValueOperations<String, Object> ops = redisTemplate.opsForValue();
        List<MessageVo> charList= (List<MessageVo>)ops.get(RedisConstant.CHAR_PRIVATE + teamId);
        if(charList!=null){
            return charList;
        }

        //获取队伍的聊天信息
        LambdaQueryWrapper<Chat> queryWrapper=new LambdaQueryWrapper<>();
        queryWrapper.eq(Chat::getChatType,chatType).eq(Chat::getTeamId,teamId);
        List<Chat> chatList = this.list(queryWrapper);

        Team team = teamService.getById(teamId);
        List<MessageVo> vos = returnMessage(loginUser, team.getUserId(), chatList);
        ops.set(RedisConstant.CHAR_PRIVATE + teamId,vos,2 + RandomUtil.randomInt(2, 3) / 10, TimeUnit.MINUTES);
        return vos;

    }

    /**
     * 返回类的封装
     * @param userId
     * @param toId
     * @param text
     * @param chatType
     * @param createTime
     * @return
     */
    @Override
    public MessageVo chatResult(Long userId, Long toId, String text, Integer chatType, Date createTime){
        MessageVo messageVo = new MessageVo();
        User fromUser = userService.getById(userId);
        User toUser = userService.getById(toId);
        WebSocketVo fromWebSocketVo = new WebSocketVo();
        WebSocketVo toWebSocketVo = new WebSocketVo();
        BeanUtils.copyProperties(fromUser, fromWebSocketVo);
        BeanUtils.copyProperties(toUser, toWebSocketVo);
        messageVo.setFormUser(fromWebSocketVo);
        messageVo.setToUser(toWebSocketVo);
        messageVo.setChatType(chatType);
        messageVo.setText(text);
        messageVo.setCreateTime(DateUtil.format(createTime, "yyyy年MM月dd日 HH:mm:ss"));
        return messageVo;
    }

    @Override
    public void deleteKey(String key, String id) {
        if (key.equals(RedisConstant.CHAR_HALL)) {
            redisTemplate.delete(key);
        } else {
            redisTemplate.delete(key + id);
        }
    }

    /**
     * 返回类的封装
     * @param userId
     * @param text
     * @return
     */
    private MessageVo chatResult(Long userId, String text) {
        MessageVo messageVo = new MessageVo();
        User fromUser = userService.getById(userId);
        WebSocketVo fromWebSocketVo = new WebSocketVo();
        BeanUtils.copyProperties(fromUser, fromWebSocketVo);
        messageVo.setFormUser(fromWebSocketVo);
        messageVo.setText(text);
        return messageVo;
    }

    /**
     * 返回类的封装
     * @param loginUser
     * @param userId
     * @param chatList
     * @return
     */
    private List<MessageVo> returnMessage(UserVo loginUser, Long userId, List<Chat> chatList) {
        return chatList.stream().map(chat -> {
            MessageVo messageVo = chatResult(chat.getFromId(), chat.getText());
            boolean isCaptain = userId != null && userId.equals(chat.getFromId());
            if (userService.getById(chat.getFromId()).getUserRole() == UserConstant.ADMIN_ROLE || isCaptain) {
                messageVo.setIsAdmin(true);
            }
            if (chat.getFromId().equals(loginUser.getId())) {
                messageVo.setIsMy(true);
            }
            messageVo.setCreateTime(DateUtil.format(chat.getCreateTime(), "yyyy年MM月dd日 HH:mm:ss"));
            return messageVo;
        }).collect(Collectors.toList());
    }
}




