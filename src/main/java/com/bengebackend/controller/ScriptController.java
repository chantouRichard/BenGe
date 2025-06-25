package com.bengebackend.controller;

import com.bengebackend.dto.ScriptDetailDto;
import com.bengebackend.dto.ScriptFrameworkDto;
import com.bengebackend.entity.ScriptReplyRequestEntity;
import com.bengebackend.entity.ScriptUpdateRequestEntity;
import com.bengebackend.entity.ScriptVisualRequestEntity;
import com.bengebackend.model.Script;
import com.bengebackend.model.ScriptAnalysis;
import com.bengebackend.service.ScriptService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 剧本控制器
 */
@RestController
@RequestMapping("/api/script")
@CrossOrigin(origins = "*")
public class ScriptController {

    @Autowired
    private ScriptService scriptService;

    /**
     * 根据剧本ID获取剧本详情
     */
    @GetMapping("/{id}")
    public ResponseEntity<ScriptDetailDto> getScriptById(@PathVariable Integer id) {
        ScriptDetailDto scriptDetail = scriptService.getScriptByIdAsync(id);
        if (scriptDetail == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(scriptDetail);
    }

    /**
     * 获取用户的剧本列表
     */
    @GetMapping("/user")
    public ResponseEntity<List<Script>> getUserScripts() {
        Integer userId = getCurrentUserId();
        List<Script> scripts = scriptService.getUserScriptsAsync(userId);
        return ResponseEntity.ok(scripts);
    }

    /**
     * 删除剧本
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteScript(@PathVariable Integer id) {
        scriptService.deleteScriptAsync(id);
        return ResponseEntity.noContent().build();
    }

    /**
     * 创建新剧本
     */
    @PostMapping("/create")
    public ResponseEntity<ScriptDetailDto> createNewScript() {
        Integer userId = getCurrentUserId();
        ScriptDetailDto result = scriptService.initializeScriptAsync(userId);
        return ResponseEntity.ok(result);
    }

    /**
     * 第二阶段对话 - 生成剧本框架
     */
    @PostMapping("/reply2nd")
    public ResponseEntity<ScriptFrameworkDto> handleUserMessage2nd(@RequestBody ScriptReplyRequestEntity request) {
        if (request.getMessage() == null || request.getMessage().trim().isEmpty() || request.getScriptId() <= 0) {
            return ResponseEntity.badRequest().build();
        }

        ScriptDetailDto scriptDetail = scriptService.getScriptByIdAsync(request.getScriptId());
        if (scriptDetail == null) {
            return ResponseEntity.badRequest().build();
        }

        ScriptFrameworkDto frameworkDto = scriptService.genFrame(request, scriptDetail.getHistory(), scriptDetail.getScript().getContent());
        if (frameworkDto == null) {
            return ResponseEntity.badRequest().build();
        }

        return ResponseEntity.ok(frameworkDto);
    }

    /**
     * 第二阶段对话 - 流式生成
     */
    @PutMapping("/reply2nd/stream")
    public ResponseEntity<String> streamHandleUserMessage2nd(@RequestBody ScriptReplyRequestEntity request) {
        if (request.getMessage() == null || request.getMessage().trim().isEmpty() || request.getScriptId() <= 0) {
            return ResponseEntity.badRequest().body("修改的剧本或者用户输入的消息不能为空");
        }

        try {
            String result = scriptService.genFrameStreamAsync(request);
            return ResponseEntity.ok(result);
        } catch (Exception ex) {
            return ResponseEntity.badRequest().body("生成失败：" + ex.getMessage());
        }
    }

    /**
     * 第三阶段 - 获取完整剧本
     */
    @PostMapping("/reply3rd")
    public ResponseEntity<ScriptDetailDto> handleUserMessage3rd(@RequestBody ScriptReplyRequestEntity request) {
        if (request.getScriptId() <= 0) {
            return ResponseEntity.badRequest().build();
        }

        ScriptDetailDto scriptDto = scriptService.getScriptByIdAsync(request.getScriptId());
        if (scriptDto == null) {
            return ResponseEntity.notFound().build();
        }

        ScriptDetailDto result = scriptService.getCompScriptAndDesc(scriptDto.getScript());
        return ResponseEntity.ok(result);
    }

    /**
     * 剧本分析
     */
    @PutMapping("/analyze")
    public ResponseEntity<ScriptAnalysis> analyzeScriptContent(@RequestBody ScriptReplyRequestEntity request) {
        if (request.getMessage() == null || request.getMessage().trim().isEmpty() || request.getScriptId() <= 0) {
            return ResponseEntity.badRequest().build();
        }

        ScriptAnalysis analysis = scriptService.analyzeScriptContent(request.getMessage(), request.getScriptId());
        if (analysis == null) {
            return ResponseEntity.badRequest().build();
        }

        return ResponseEntity.ok(analysis);
    }

    /**
     * 更新剧本 - 从第一阶段进入第二阶段
     */
    @PutMapping("/update")
    public ResponseEntity<ScriptDetailDto> updateScript(@RequestBody ScriptUpdateRequestEntity request) {
        if (request.getContent() == null || request.getContent().trim().isEmpty() || 
            request.getScriptId() <= 0 || request.getStage() <= 0) {
            return ResponseEntity.badRequest().build();
        }

        scriptService.updateScriptAsync(request.getScriptId(), "新剧本", request.getContent(), request.getStage());
        ScriptDetailDto result = scriptService.getScriptByIdAsync(request.getScriptId());
        return ResponseEntity.ok(result);
    }

    /**
     * 可视化选定元素
     */
    @PutMapping("/visualize")
    public ResponseEntity<String> visualSelectedElement(@RequestBody ScriptVisualRequestEntity request) {
        if (request.getElementId() <= 0 || request.getScriptId() <= 0) {
            return ResponseEntity.badRequest().build();
        }

        String urlPath = scriptService.visualizeScriptAsync(request.getScriptId(), request.getElementId());
        return ResponseEntity.ok(urlPath);
    }

    /**
     * 获取当前用户ID
     */
    private Integer getCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof String) {
            return Integer.parseInt((String) authentication.getPrincipal());
        }
        return 1;
    }
}
