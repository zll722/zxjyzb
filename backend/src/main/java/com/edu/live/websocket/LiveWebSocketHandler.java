package com.edu.live.websocket;

import cn.hutool.json.JSONUtil;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class LiveWebSocketHandler extends TextWebSocketHandler {
    private final Map<Long, Set<WebSocketSession>> roomSessions = new ConcurrentHashMap<>();

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        Long roomId = getRoomId(session);
        roomSessions.computeIfAbsent(roomId, key -> ConcurrentHashMap.newKeySet()).add(session);
        broadcast(roomId, Map.of("type", "online", "roomId", roomId, "count", roomSessions.get(roomId).size()));
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        Long roomId = getRoomId(session);
        broadcast(roomId, message.getPayload());
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        Long roomId = getRoomId(session);
        Set<WebSocketSession> sessions = roomSessions.get(roomId);
        if (sessions != null) {
            sessions.remove(session);
            broadcast(roomId, Map.of("type", "online", "roomId", roomId, "count", sessions.size()));
        }
    }

    public void broadcast(Long roomId, Object payload) {
        broadcast(roomId, JSONUtil.toJsonStr(payload));
    }

    private void broadcast(Long roomId, String payload) {
        Set<WebSocketSession> sessions = roomSessions.getOrDefault(roomId, Set.of());
        TextMessage message = new TextMessage(payload);
        for (WebSocketSession session : sessions) {
            if (session.isOpen()) {
                // WebSocketSession 不支持并发写入，需对每个 session 加锁
                synchronized (session) {
                    try {
                        session.sendMessage(message);
                    } catch (IOException e) {
                        // session 已关闭或写入失败，忽略
                    }
                }
            }
        }
    }

    private Long getRoomId(WebSocketSession session) {
        Object value = session.getAttributes().get("roomId");
        if (value != null) {
            return Long.valueOf(String.valueOf(value));
        }
        String path = session.getUri() == null ? "" : session.getUri().getPath();
        String roomId = path.substring(path.lastIndexOf('/') + 1);
        session.getAttributes().put("roomId", roomId);
        return Long.valueOf(roomId);
    }
}
