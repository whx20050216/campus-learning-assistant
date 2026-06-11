package com.campus.learning.service;

import com.campus.learning.entity.ChatMessage;
import com.campus.learning.entity.ChatSession;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;

public interface ChatService {

    ChatSession createSession(Long userId, String title, Long materialId);

    List<ChatSession> listSessions(Long userId);

    void deleteSession(Long userId, Long sessionId);

    void updateTitle(Long userId, Long sessionId, String title);

    List<ChatMessage> getMessages(Long userId, Long sessionId);

    SseEmitter streamChat(Long userId, Long sessionId, String message, Long materialId);
}
