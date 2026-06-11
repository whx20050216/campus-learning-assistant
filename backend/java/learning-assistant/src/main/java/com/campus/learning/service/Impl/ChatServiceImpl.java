package com.campus.learning.service.Impl;

import com.campus.learning.entity.ChatMessage;
import com.campus.learning.entity.ChatSession;
import com.campus.learning.mapper.ChatMessageMapper;
import com.campus.learning.mapper.ChatSessionMapper;
import com.campus.learning.service.ChatService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Service
public class ChatServiceImpl implements ChatService {

    @Autowired
    private ChatSessionMapper chatSessionMapper;

    @Autowired
    private ChatMessageMapper chatMessageMapper;

    @Value("${python.api.url:http://localhost:8000}")
    private String pythonApiUrl;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final ExecutorService executor = Executors.newCachedThreadPool();
    private final Pattern ssePattern = Pattern.compile("^data: (.*)$");

    @Override
    public ChatSession createSession(Long userId, String title, Long materialId) {
        ChatSession session = new ChatSession();
        session.setUserId(userId);
        session.setTitle(title != null ? title : "新对话");
        session.setMaterialId(materialId);
        session.setCreatedAt(LocalDateTime.now());
        session.setUpdatedAt(LocalDateTime.now());
        chatSessionMapper.insert(session);
        return session;
    }

    @Override
    public List<ChatSession> listSessions(Long userId) {
        return chatSessionMapper.selectByUserId(userId);
    }

    @Override
    public void deleteSession(Long userId, Long sessionId) {
        ChatSession session = chatSessionMapper.selectById(sessionId);
        if (session == null || !session.getUserId().equals(userId)) {
            throw new RuntimeException("会话不存在或无权访问");
        }
        // 先删除消息
        com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<ChatMessage> wrapper =
                new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<>();
        wrapper.eq("session_id", sessionId);
        chatMessageMapper.delete(wrapper);
        chatSessionMapper.deleteById(sessionId);
    }

    @Override
    public void updateTitle(Long userId, Long sessionId, String title) {
        ChatSession session = chatSessionMapper.selectById(sessionId);
        if (session == null || !session.getUserId().equals(userId)) {
            throw new RuntimeException("会话不存在或无权访问");
        }
        session.setTitle(title);
        session.setUpdatedAt(LocalDateTime.now());
        chatSessionMapper.updateById(session);
    }

    @Override
    public List<ChatMessage> getMessages(Long userId, Long sessionId) {
        ChatSession session = chatSessionMapper.selectById(sessionId);
        if (session == null || !session.getUserId().equals(userId)) {
            throw new RuntimeException("会话不存在或无权访问");
        }
        return chatMessageMapper.selectBySessionId(sessionId);
    }

    @Override
    public SseEmitter streamChat(Long userId, Long sessionId, String message, Long materialId) {
        if (message == null || message.trim().isEmpty()) {
            throw new RuntimeException("消息不能为空");
        }

        // 1. 确保会话存在
        ChatSession session;
        if (sessionId == null) {
            String title = message.length() > 20 ? message.substring(0, 20) + "..." : message;
            session = createSession(userId, title, materialId);
            sessionId = session.getId();
        } else {
            session = chatSessionMapper.selectById(sessionId);
            if (session == null || !session.getUserId().equals(userId)) {
                throw new RuntimeException("会话不存在或无权访问");
            }
        }

        // 2. 保存用户消息
        ChatMessage userMsg = new ChatMessage();
        userMsg.setSessionId(sessionId);
        userMsg.setRole("user");
        userMsg.setContent(message.trim());
        userMsg.setCreatedAt(LocalDateTime.now());
        chatMessageMapper.insert(userMsg);

        // 更新会话时间
        session.setUpdatedAt(LocalDateTime.now());
        chatSessionMapper.updateById(session);

        // 3. 获取历史消息（最近10条）
        List<ChatMessage> history = chatMessageMapper.selectBySessionId(sessionId);
        if (history.size() > 10) {
            history = history.subList(history.size() - 10, history.size());
        }

        // 4. 组装 messages
        List<Map<String, String>> messages = new ArrayList<>();
        messages.add(Map.of("role", "system", "content",
                "你是一位专业的学习助手，擅长帮助大学生理解学习资料、解答问题。回答要简洁、专业、易懂。"));
        for (ChatMessage msg : history) {
            messages.add(Map.of("role", msg.getRole(), "content", msg.getContent()));
        }

        final Long finalSessionId = sessionId;
        SseEmitter emitter = new SseEmitter(0L); // 不超时

        executor.execute(() -> {
            StringBuilder assistantContent = new StringBuilder();
            try {
                // 5. 调用 Python SSE
                HttpURLConnection conn = (HttpURLConnection) new URL(pythonApiUrl + "/ai/stream").openConnection();
                conn.setRequestMethod("POST");
                conn.setRequestProperty("Content-Type", "application/json");
                conn.setDoOutput(true);
                conn.setDoInput(true);
                conn.setReadTimeout(60000);
                conn.setConnectTimeout(5000);

                Map<String, Object> body = new HashMap<>();
                body.put("messages", messages);
                String jsonBody = objectMapper.writeValueAsString(body);

                try (OutputStream os = conn.getOutputStream()) {
                    os.write(jsonBody.getBytes(StandardCharsets.UTF_8));
                }

                int status = conn.getResponseCode();
                if (status != 200) {
                    emitter.send(SseEmitter.event().name("error").data("AI服务调用失败: HTTP " + status));
                    emitter.complete();
                    return;
                }

                try (BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        if (line.isEmpty()) continue;
                        Matcher m = ssePattern.matcher(line);
                        if (m.matches()) {
                            String data = m.group(1).trim();
                            if ("[DONE]".equals(data)) {
                                break;
                            }
                            // 解析 JSON {"content":"xxx"}
                            try {
                                Map<String, Object> chunk = objectMapper.readValue(data, Map.class);
                                String content = (String) chunk.get("content");
                                if (content != null) {
                                    assistantContent.append(content);
                                    emitter.send(SseEmitter.event().name("message").data(data));
                                }
                            } catch (Exception e) {
                                // 忽略解析失败的行
                            }
                        }
                    }
                }

                // 6. 保存 AI 回复
                if (assistantContent.length() > 0) {
                    ChatMessage aiMsg = new ChatMessage();
                    aiMsg.setSessionId(finalSessionId);
                    aiMsg.setRole("assistant");
                    aiMsg.setContent(assistantContent.toString());
                    aiMsg.setCreatedAt(LocalDateTime.now());
                    chatMessageMapper.insert(aiMsg);
                }

                emitter.send(SseEmitter.event().name("done").data("{}"));
                emitter.complete();
            } catch (Exception e) {
                log.error("SSE 流处理异常", e);
                try {
                    emitter.send(SseEmitter.event().name("error").data("流式输出异常: " + e.getMessage()));
                } catch (Exception ignored) {}
                emitter.completeWithError(e);
            }
        });

        return emitter;
    }
}
