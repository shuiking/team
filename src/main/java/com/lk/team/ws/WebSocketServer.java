package com.lk.team.ws;

import cn.hutool.core.date.DateUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.TypeReference;
import com.google.gson.Gson;
import com.lk.team.config.HttpSessionConfigurator;
import com.lk.team.constant.CharConstant;
import com.lk.team.constant.RedisConstant;
import com.lk.team.constant.UserConstant;
import com.lk.team.model.entity.Chat;
import com.lk.team.model.entity.Team;
import com.lk.team.model.entity.User;
import com.lk.team.model.request.MessageRequest;
import com.lk.team.model.vo.MessageVo;
import com.lk.team.model.vo.UserVo;
import com.lk.team.model.vo.WebSocketVo;
import com.lk.team.service.ChatService;
import com.lk.team.service.TeamService;
import com.lk.team.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import javax.servlet.http.HttpSession;
import javax.websocket.*;
import javax.websocket.server.PathParam;
import javax.websocket.server.ServerEndpoint;
import java.io.IOException;
import java.io.Serializable;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;

/**
 * websocket的服务端处理
 * @Author : lk
 * @create 2023/5/11 15:28
 */
@ServerEndpoint(value = "/websocket/{userId}/{teamId}",configurator = HttpSessionConfigurator.class)
@Component
@Slf4j
public class WebSocketServer {
    /**
     * 保存队伍的连接信息
     */
    private static final Map<String, ConcurrentHashMap<String, WebSocketServer>> ROOMS = new HashMap<>();
    /**
     * 线程安全的无序的集合
     */
    private static final CopyOnWriteArraySet<Session> SESSIONS = new CopyOnWriteArraySet<>();
    /**
     * 存储在线连接数
     */
    private static final Map<String, Session> SESSION_POOL = new HashMap<>(0);
    /**
     * 房间在线人数
     */
    private static int onlineCount = 0;
    /**
     * 当前信息
     */
    private Session session;
    private HttpSession httpSession;

    private static UserService userService;
    private static ChatService chatService;
    private static TeamService teamService;

    /**
     * 获取在线人数
     * @return 在线人数
     */
    public static synchronized int getOnlineCount() {
        return onlineCount;
    }

    /**
     * 在线人数加一
     */
    public static synchronized void addOnlineCount() {
        WebSocketServer.onlineCount++;
    }

    /**
     * 在线人数减一
     */
    public static synchronized void subOnlineCount() {
        WebSocketServer.onlineCount--;
    }

    @Resource
    public void setHeatMapService(UserService userService) {
        WebSocketServer.userService = userService;
    }

    @Resource
    public void setHeatMapService(ChatService chatService) {
        WebSocketServer.chatService = chatService;
    }

    @Resource
    public void setHeatMapService(TeamService teamService) {
        WebSocketServer.teamService = teamService;
    }

    /**
     * 队伍内群发消息
     * @param teamId
     * @param msg
     * @throws Exception
     */
    public static void broadcast(String teamId, String msg) {
        ConcurrentHashMap<String, WebSocketServer> map = ROOMS.get(teamId);
        // keySet获取map集合key的集合  然后在遍历key即可
        for (String key : map.keySet()) {
            try {
                WebSocketServer webSocket = map.get(key);
                webSocket.sendMessage(msg);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * 发送消息
     * @param message
     * @throws IOException
     */
    public void sendMessage(String message) throws IOException {
        //同步发送信息
        this.session.getBasicRemote().sendText(message);
    }


    /**
     * 链接成功调用的方法
     * @param session websocket的session
     * @param userId 用户id
     * @param teamId 队伍id
     * @param config
     */
    @OnOpen
    public void onOpen(Session session, @PathParam(value = "userId") String userId, @PathParam(value = "teamId") String teamId, EndpointConfig config) {
        try {
            //参数校验
            if (StringUtils.isBlank(userId) || "undefined".equals(userId)) {
                sendError(userId, "参数有误");
                return;
            }
            //获取websocket的session
            HttpSession httpSession = (HttpSession) config.getUserProperties().get(HttpSession.class.getName());

            //当前的用户
            UserVo user = (UserVo) httpSession.getAttribute(UserConstant.LOGIN_USER_STATUS);
            if (user != null) {
                this.session = session;
                this.httpSession = httpSession;
            }
            if (!"NaN".equals(teamId)) {
                if (!ROOMS.containsKey(teamId)) {
                    ConcurrentHashMap<String,WebSocketServer> room = new ConcurrentHashMap<>(0);
                    room.put(userId, this);
                    ROOMS.put(String.valueOf(teamId), room);
                    // 在线数加1
                    addOnlineCount();
                } else {
                    if (!ROOMS.get(teamId).containsKey(userId)) {
                        ROOMS.get(teamId).put(userId, this);
                        // 在线数加1
                        addOnlineCount();
                    }
                }
                log.info("有新连接加入！当前在线人数为" + getOnlineCount());
            } else {
                SESSIONS.add(session);
                SESSION_POOL.put(userId, session);
                log.info("有新用户加入，userId={}, 当前在线人数为：{}", userId, SESSION_POOL.size());
                sendAllUsers();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 链接关闭调用的方法
     * @param userId
     * @param teamId
     * @param session
     */
    @OnClose
    public void onClose(@PathParam("userId") String userId, @PathParam(value = "teamId") String teamId, Session session) {
        try {
            if (!"NaN".equals(teamId)) {
                ROOMS.get(teamId).remove(userId);
                if (getOnlineCount() > 0) {
                    subOnlineCount();
                }
                log.info("用户退出:当前在线人数为:" + getOnlineCount());
            } else {
                if (!SESSION_POOL.isEmpty()) {
                    SESSION_POOL.remove(userId);
                    SESSIONS.remove(session);
                }
                log.info("【WebSocket消息】连接断开，总数为：" + SESSION_POOL.size());
                sendAllUsers();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 收到客户端消息后调用的方法
     * @param message
     * @param userId
     */
    @OnMessage
    public void onMessage(String message, @PathParam("userId") String userId) {
        if ("PING".equals(message)) {
            sendOneMessage(userId, "pong");
            log.error("心跳包，发送给={},在线:{}人", userId, getOnlineCount());
            return;
        }
        log.info("服务端收到用户username={}的消息:{}", userId, message);
        MessageRequest messageRequest = new Gson().fromJson(message, MessageRequest.class);
        Long toId = messageRequest.getToId();
        Long teamId = messageRequest.getTeamId();
        String text = messageRequest.getText();
        Integer chatType = messageRequest.getChatType();
        User fromUser = userService.getById(userId);
        Team team = teamService.getById(teamId);
        if (chatType == CharConstant.PRIVATE_CHAT) {
            // 私聊
            privateChat(fromUser, toId, text, chatType);
        } else if (chatType == CharConstant.TEAM_CHAT) {
            // 队伍内聊天
            teamChat(fromUser, text, team, chatType);
        } else {
            // 群聊
            hallChat(fromUser, text, chatType);
        }
    }


    /**
     * 队伍聊天
     *
     * @param user
     * @param text
     * @param team
     * @param chatType
     */
    private void teamChat(User user, String text, Team team, Integer chatType) {
        //保存信息到数据库
        savaChat(user.getId(), null, text, team.getId(), chatType);

        //处理返回信息
        MessageVo messageVo = new MessageVo();
        WebSocketVo fromWebSocketVo = new WebSocketVo();
        BeanUtils.copyProperties(user, fromWebSocketVo);
        messageVo.setFormUser(fromWebSocketVo);
        messageVo.setText(text);
        messageVo.setTeamId(team.getId());
        messageVo.setChatType(chatType);
        messageVo.setCreateTime(DateUtil.format(new Date(), "yyyy年MM月dd日 HH:mm:ss"));
        if (user.getId() == team.getUserId() || user.getUserRole() == UserConstant.ADMIN_ROLE) {
            messageVo.setIsAdmin(true);
        }

        //获取当前用户的信息
        UserVo loginUser = (UserVo) this.httpSession.getAttribute(UserConstant.LOGIN_USER_STATUS);
        if (loginUser.getId() == user.getId()) {
            messageVo.setIsMy(true);
        }
        String toJson =JSON.toJSONString(messageVo);
        try {
            //队伍群发信息
            broadcast(String.valueOf(team.getId()), toJson);
            chatService.deleteKey(RedisConstant.CHAR_TEAM,String.valueOf(team.getId()));
            log.error("队伍聊天，发送给={},队伍={},在线:{}人", user.getId(), team.getId(), getOnlineCount());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * 大厅聊天
     *
     * @param user
     * @param text
     */
    private void hallChat(User user, String text, Integer chatType) {
        //保存信息到数据库
        savaChat(user.getId(), null, text, null, chatType);

        MessageVo messageVo = new MessageVo();
        WebSocketVo fromWebSocketVo = new WebSocketVo();
        BeanUtils.copyProperties(user, fromWebSocketVo);
        messageVo.setFormUser(fromWebSocketVo);
        messageVo.setText(text);
        messageVo.setChatType(chatType);
        messageVo.setCreateTime(DateUtil.format(new Date(), "yyyy年MM月dd日 HH:mm:ss"));
        if (user.getUserRole() == UserConstant.ADMIN_ROLE) {
            messageVo.setIsAdmin(true);
        }
        UserVo loginUser = (UserVo) this.httpSession.getAttribute(UserConstant.LOGIN_USER_STATUS);
        if (loginUser.getId() == user.getId()) {
            messageVo.setIsMy(true);
        }
        String toJson = JSON.toJSONString(messageVo);
        sendAllMessage(toJson);
        chatService.deleteKey(RedisConstant.CHAR_HALL, String.valueOf(user.getId()));

    }

    /**
     * 私聊
     *
     * @param user
     * @param text
     */
    private void privateChat(User user, Long toId, String text, Integer chatType) {
        savaChat(user.getId(), toId, text, null, chatType);
        Session toSession = SESSION_POOL.get(toId.toString());
        if (toSession != null) {
            MessageVo messageVo = chatService.chatResult(user.getId(), toId, text, chatType, DateUtil.date(System.currentTimeMillis()));
            UserVo loginUser = (UserVo) this.httpSession.getAttribute(UserConstant.LOGIN_USER_STATUS);
            if (loginUser.getId() == user.getId()) {
                messageVo.setIsMy(true);
            }
            String toJson = new Gson().toJson(messageVo);
            sendOneMessage(toId.toString(), toJson);
            chatService.deleteKey(RedisConstant.CHAR_PRIVATE,user.getId()+":"+toId);
            chatService.deleteKey(RedisConstant.CHAR_PRIVATE,toId+":"+user.getId());
            log.info("发送给用户username={}，消息：{}", messageVo.getToUser(), toJson);
        } else {
            log.info("发送失败，未找到用户username={}的session", toId);
        }
    }

    /**
     * 保存聊天
     *
     * @param userId
     * @param toId
     * @param text
     */
    private void savaChat(Long userId, Long toId, String text, Long teamId, Integer chatType) {
        if (chatType == CharConstant.PRIVATE_CHAT) {
            User user = userService.getById(userId);
            Set<Long> userIds = JSON.parseObject(user.getUserIds(),new TypeReference<Set<Long>>(){});
            if (!userIds.contains(toId)) {
                sendError(String.valueOf(userId), "该用户不是你的好友");
                return;
            }
        }
        Chat chat = new Chat();
        chat.setFromId(userId);
        chat.setText(String.valueOf(text));
        chat.setChatType(chatType);
        chat.setCreateTime(new Date());
        if (toId != null && toId > 0) {
            chat.setToId(toId);
        }
        if (teamId != null && teamId > 0) {
            chat.setTeamId(teamId);
        }
        chatService.save(chat);
    }
    /**
     * 发送失败
     *
     * @param userId
     * @param errorMessage
     */
    private void sendError(String userId, String errorMessage) {
        JSONObject obj = new JSONObject();
        obj.set("error", errorMessage);
        sendOneMessage(userId, obj.toString());
    }


    /**
     * 此为单点消息
     *
     * @param userId  用户编号
     * @param message 消息
     */
    public void sendOneMessage(String userId, String message) {
        Session session = SESSION_POOL.get(userId);
        if (session != null && session.isOpen()) {
            try {
                synchronized (session) {
                    log.info("【WebSocket消息】单点消息：" + message);
                    //异步发送信息
                    session.getAsyncRemote().sendText(message);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * 发送所有在线用户信息
     */
    public void sendAllUsers() {
        log.info("【WebSocket消息】发送所有在线用户信息");
        HashMap<String, List<WebSocketVo>> stringListHashMap = new HashMap<>(0);
        List<WebSocketVo> webSocketVos = new ArrayList<>();
        stringListHashMap.put("users", webSocketVos);
        for (Serializable key : SESSION_POOL.keySet()) {
            User user = userService.getById(key);
            WebSocketVo webSocketVo = new WebSocketVo();
            BeanUtils.copyProperties(user, webSocketVo);
            webSocketVos.add(webSocketVo);
        }
        sendAllMessage(JSONUtil.toJsonStr(stringListHashMap));
    }

    /**
     * 此为广播消息
     *
     * @param message 消息
     */
    public void sendAllMessage(String message) {
        log.info("【WebSocket消息】广播消息：" + message);
        for (Session session : SESSIONS) {
            try {
                if (session.isOpen()) {
                    synchronized (session) {
                        session.getBasicRemote().sendText(message);
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}
