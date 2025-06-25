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
     * 生成剧本方向标语
     */
    @PostMapping("/directions")
    public ResponseEntity<Object> generateSlogan(@RequestBody Object request) {
        return ResponseEntity.ok().body("");
    }

    /**
     * 流式生成剧本方向标语
     */
    @PutMapping("/directions/stream")
    public ResponseEntity<String> streamGenerateSlogan(@RequestBody Object request) {

        return ResponseEntity.ok("");
    }

    /**
     * 完成流式标语生成
     */
    @PutMapping("/directions/stream-complete")
    public ResponseEntity<Object> streamCompleteGenerateSlogan(@RequestBody Object request) {

        return ResponseEntity.ok().body("");
    }

    /**
     * 聊天流式接口
     */
    @PostMapping("/chat/stream")
    public ResponseEntity<String> chatStream(@RequestBody Object request) {

        return ResponseEntity.ok("AI服务暂未实现");
    }


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
        System.out.println("111111111111");
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

        ScriptDetailDto result = scriptService.getCompSctiptAndDesc(scriptDto.getScript());
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
     * 参考C#实现: User.FindFirst(ClaimTypes.NameIdentifier)?.Value
     */
    private Integer getCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        // 打印调试信息，查看当前用户的信息
        System.out.println("getCurrentUserId: " + authentication.getName());

        if (authentication != null && authentication.isAuthenticated()) {
            try {
                // 获取存储在 authentication 中的 userId（保存在 details 字段）
                Integer userId = (Integer) authentication.getDetails();  // 强制转换为 Integer 类型
                return userId;  // 返回 userId
            } catch (Exception e) {
                System.out.println("Error retrieving userId: " + e.getMessage());
                return 1;  // 如果出错，返回默认的 1
            }
        }

        return 1;  // 如果未认证，返回默认的 1
    }

}
