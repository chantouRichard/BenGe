package com.bengebackend.controller;

import com.bengebackend.entity.SloganRequestEntity;
import com.bengebackend.service.AIService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@RestController
@RequestMapping("/api/ai")
@CrossOrigin(origins = "*")
public class AIStreamController {

    @Autowired
    private AIService aiService;

    private final ExecutorService executor = Executors.newCachedThreadPool();

    /**
     * Slogan流式生成接口
     */
    @PostMapping(value = "/slogan/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter generateSloganStream(@RequestBody SloganRequestEntity request) {
        SseEmitter emitter = new SseEmitter(Long.MAX_VALUE);

        executor.execute(() -> {
            try {
                aiService.GenerateSloganStreamAsync(request, slogan -> {
                    try {
                        emitter.send(SseEmitter.event()
                                .name("slogan")
                                .data(slogan));
                    } catch (IOException e) {
                        System.err.println("发送Slogan事件失败: " + e.getMessage());
                        emitter.completeWithError(e);
                    }
                }).get(); // 等待完成

                emitter.complete();
            } catch (Exception e) {
                System.err.println("生成Slogan流式输出失败: " + e.getMessage());
                emitter.completeWithError(e);
            }
        });

        return emitter;
    }

    /**
     * AI助手流式聊天接口
     */
    @PostMapping(value = "/chat/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter chatStream(@RequestBody Map<String, Object> request) {
        SseEmitter emitter = new SseEmitter(Long.MAX_VALUE);

        executor.execute(() -> {
            try {
                @SuppressWarnings("unchecked")
                List<Map<String, String>> messages = (List<Map<String, String>>) request.get("messages");

                if (messages == null) {
                    emitter.completeWithError(new IllegalArgumentException("messages参数不能为空"));
                    return;
                }

                aiService.ChatStreamAsync(messages, content -> {
                    try {
                        emitter.send(SseEmitter.event()
                                .name("message")
                                .data(content));
                    } catch (IOException e) {
                        System.err.println("发送聊天事件失败: " + e.getMessage());
                        emitter.completeWithError(e);
                    }
                }).get(); // 等待完成

                emitter.complete();
            } catch (Exception e) {
                System.err.println("AI助手流式聊天失败: " + e.getMessage());
                emitter.completeWithError(e);
            }
        });

        return emitter;
    }

    /**
     * 测试接口
     */
    @GetMapping("/test")
    public ResponseEntity<String> test() {
        return ResponseEntity.ok("AI流式服务运行正常");
    }
}
