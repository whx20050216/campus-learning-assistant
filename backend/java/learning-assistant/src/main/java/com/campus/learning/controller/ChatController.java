package com.campus.learning.controller;

import com.campus.learning.dto.Result;
import com.campus.learning.entity.ChatMessage;
import com.campus.learning.entity.ChatSession;
import com.campus.learning.security.CurrentUserUtils;
import com.campus.learning.service.ChatService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/chat")
public class ChatController {

    @Autowired
    private ChatService chatService;

    @PostMapping("/sessions")
    public Result<ChatSession> createSession(@RequestBody Map<String, Object> body) {
        Long userId = CurrentUserUtils.getCurrentUserId();
        if (userId == null) {
            return Result.error(401, "未登录");
        }
        String title = body.get("title") != null ? body.get("title").toString() : null;
        Long materialId = body.get("materialId") != null ? Long.valueOf(body.get("materialId").toString()) : null;
        try {
            return Result.success(chatService.createSession(userId, title, materialId));
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    @GetMapping("/sessions")
    public Result<List<ChatSession>> listSessions() {
        Long userId = CurrentUserUtils.getCurrentUserId();
        if (userId == null) {
            return Result.error(401, "未登录");
        }
        return Result.success(chatService.listSessions(userId));
    }

    @GetMapping("/sessions/{id}/messages")
    public Result<List<ChatMessage>> getMessages(@PathVariable Long id) {
        Long userId = CurrentUserUtils.getCurrentUserId();
        if (userId == null) {
            return Result.error(401, "未登录");
        }
        try {
            return Result.success(chatService.getMessages(userId, id));
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    @DeleteMapping("/sessions/{id}")
    public Result<Void> deleteSession(@PathVariable Long id) {
        Long userId = CurrentUserUtils.getCurrentUserId();
        if (userId == null) {
            return Result.error(401, "未登录");
        }
        try {
            chatService.deleteSession(userId, id);
            return Result.success();
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    @PutMapping("/sessions/{id}/title")
    public Result<Void> updateTitle(@PathVariable Long id, @RequestBody Map<String, String> body) {
        Long userId = CurrentUserUtils.getCurrentUserId();
        if (userId == null) {
            return Result.error(401, "未登录");
        }
        String title = body.get("title");
        if (title == null || title.trim().isEmpty()) {
            return Result.error("标题不能为空");
        }
        try {
            chatService.updateTitle(userId, id, title.trim());
            return Result.success();
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    @GetMapping("/stream")
    public SseEmitter stream(
            @RequestParam String message,
            @RequestParam(required = false) Long sessionId,
            @RequestParam(required = false) Long materialId) {
        Long userId = CurrentUserUtils.getCurrentUserId();
        if (userId == null) {
            SseEmitter emitter = new SseEmitter(0L);
            try {
                emitter.send(SseEmitter.event().name("error").data("未登录"));
                emitter.complete();
            } catch (Exception ignored) {}
            return emitter;
        }
        try {
            return chatService.streamChat(userId, sessionId, message, materialId);
        } catch (Exception e) {
            SseEmitter emitter = new SseEmitter(0L);
            try {
                emitter.send(SseEmitter.event().name("error").data(e.getMessage()));
                emitter.complete();
            } catch (Exception ignored) {}
            return emitter;
        }
    }
}
