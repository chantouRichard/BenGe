package com.bengebackend.controller;

import com.bengebackend.dto.CollaborationScriptDto;
import com.bengebackend.dto.ScriptDetailDto;
import com.bengebackend.dto.ScriptFrameworkDto;
import com.bengebackend.dto.SloganResponseDto;
import com.bengebackend.entity.*;
import com.bengebackend.model.Script;
import com.bengebackend.model.ScriptAnalysis;
import com.bengebackend.service.RoomService;
import com.bengebackend.service.ScriptService;
import com.bengebackend.util.ContextDataProcessor;
import dev.langchain4j.community.model.dashscope.QwenChatModel;
import lombok.extern.slf4j.Slf4j;
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
@Slf4j
public class ScriptController {

    @Autowired
    private ScriptService scriptService;

    @Autowired
    private RoomService roomService;

    @Autowired
    private ContextDataProcessor contextDataProcessor;

    /**
     * 生成剧本方向标语
     */

    @Autowired
    private QwenChatModel qwenChatModel;

    @PostMapping("/directions")
    public ResponseEntity<Object> generateSlogan(@RequestBody SloganRequestEntity request) {

        String response = qwenChatModel.chat("""
                请根据以下关键词生成完整的剧本杀广告,
                严格按照以下格式生成内容：
                剧本背景: ...\\n玩家目标: ...\\n核心创意: ...\\n
                每次生成的内容必须独特，可以通过改变背景设定、角色类型、目标描述或核心创意的表达方式来实现.
                关键词包括以下几点：
                """ + request.getPrompt());
        if (response == null || response.isEmpty()) {
            return ResponseEntity.badRequest().body("生成剧本方向标语失败");
        }
        // 解析生成的内容
        String[] parts = response.split("\\n");
        if (parts.length < 3) {
            return ResponseEntity.badRequest().body("生成的内容格式不正确");
        }
        String background = parts[0].replace("剧本背景: ", "").trim();
        String playerGoal = parts[1].replace("玩家目标: ", "").trim();
        String coreIdea = parts[2].replace("核心创意: ", "").trim();
        // 创建返回对象
        SloganResponseDto sloganResponse = new SloganResponseDto();
        sloganResponse.setSlogans(List.of(
                new Slogan("剧本背景", background),
                new Slogan("玩家目标", playerGoal),
                new Slogan("核心创意", coreIdea)));
        return ResponseEntity.ok(sloganResponse);
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
//        System.out.println("111111111111");
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

        ScriptFrameworkDto frameworkDto = scriptService.genFrame(request, scriptDetail.getHistory(),
                scriptDetail.getScript().getContent());
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
            String result = "当前服务已在controller-aistream中实现，请访问对应的流式接口";
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

        if (authentication != null && authentication.isAuthenticated()) {
            try {
                // 获取存储在 authentication 中的 userId（保存在 details 字段）
                Integer userId = (Integer) authentication.getDetails(); // 强制转换为 Integer 类型
                return userId; // 返回 userId
            } catch (Exception e) {
                System.out.println("Error retrieving userId: " + e.getMessage());
                return 1; // 如果出错，返回默认的 1
            }
        }

        return 1; // 如果未认证，返回默认的 1
    }


    @PostMapping("/collaboration/generate")
    public ResponseEntity<CollaborationScriptDto> generateCollaborationScript(@RequestBody CollaborationScriptRequestEntity request) {
        log.info(request.toString());
//        if (request.getRoomId() == null || request.getRoomId() <= 0) {
//            return ResponseEntity.badRequest().body(new CollaborationScriptDto(null, null, null, "房间ID无效", false));
//        }
//
//        if (request.getContextData() == null || request.getContextData().trim().isEmpty()) {
//            return ResponseEntity.badRequest().body(new CollaborationScriptDto(null, null, null, "上下文数据不能为空", false));
//        }

        try {
//            // 检查用户是否为房主（可选的权限控制）
//            if (!roomService.isOwner(request.getRoomId())) {
//                return ResponseEntity.status(403).body(new CollaborationScriptDto(null, null, null, "只有房主可以生成剧本", false));
//            }

            // 使用现有的ContextDataProcessor处理协作数据
            String contextSummary = contextDataProcessor.generateContextSummary(request.getContextData(), "collaboration");

            // 构建AI提示词
            String prompt = buildCollaborationPrompt(contextSummary, request.getRoomId());

            log.info(prompt);

            // 使用现有的qwenChatModel生成剧本
            String generatedScript = qwenChatModel.chat(prompt);

            log.info(generatedScript);

            if (generatedScript != null && !generatedScript.trim().isEmpty()) {
                String title = "房间" + request.getRoomId() + "协作剧本";
                return ResponseEntity.ok(new CollaborationScriptDto(title, generatedScript, request.getRoomId(), "剧本生成成功", true));
            } else {
                return ResponseEntity.status(500).body(new CollaborationScriptDto(null, null, null, "AI生成剧本失败", false));
            }
        } catch (Exception e) {
            System.err.println("生成协作剧本失败: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(500).body(new CollaborationScriptDto(null, null, null, "服务器错误: " + e.getMessage(), false));
        }
    }

    /**
     * 构建协作剧本生成的AI提示词
     */
    private String buildCollaborationPrompt(String contextSummary, Integer roomId) {
        StringBuilder prompt = new StringBuilder();

        // 系统角色定义 - 参考现有System_MSG的严格格式要求
        prompt.append("你是一名逻辑严谨且追求内容完整的剧本杀作家，专门负责将团队协作设计的内容整合为完整剧本。必须严格遵循以下规则：\n\n");

        // 规则1：严格的输出格式（参考现有System_MSG）
        prompt.append("规则1. 请严格按指定MarkDown格式输出以下字段，七部分内容缺一不可！不要包含额外文本！:\n");
        prompt.append("    AI回复（一句话）\n");
        prompt.append("    标题\n");
        prompt.append("    背景（一段话）\n");
        prompt.append("    人物剧本（数组）\n");
        prompt.append("    线索（数组）\n");
        prompt.append("    真相\n");
        prompt.append("    组织者手册\n\n");

        prompt.append("输出需符合如下结构：\n");
        prompt.append("    AI回复：\n");
        prompt.append("    ...（这部分是给用户的礼貌性回答，体现协作成果的整合）\n");
        prompt.append("    》》》\n");
        prompt.append("    # ...（这部分是标题）\n");
        prompt.append("    ---\n");
        prompt.append("    ## 背景\n");
        prompt.append("    ...\n");
        prompt.append("    ---\n");
        prompt.append("    ## 人物剧本:\n");
        prompt.append("    -CHR ...（角色1名称）\n");
        prompt.append("    ...（关于角色1的相关内容）\n");
        prompt.append("    -CHR ...（角色2名称）\n");
        prompt.append("    ...（关于角色2的相关内容）\n");
        prompt.append("    ...（其它角色的格式以此类推）\n");
        prompt.append("    ---\n");
        prompt.append("    ## 线索\n");
        prompt.append("    -C> ...\n");
        prompt.append("    -C> ...\n");
        prompt.append("    -C> ...\n");
        prompt.append("    ...\n");
        prompt.append("    ---\n");
        prompt.append("    ## 真相\n");
        prompt.append("    ...\n");
        prompt.append("    ---\n");
        prompt.append("    ## 组织者手册\n");
        prompt.append("    ...\n");
        prompt.append("    ---\n");
        prompt.append("    （到此结束，不要再输出文本）\n\n");

        // 规则2：协作特色要求
        prompt.append("规则2. 协作整合要求：\n");
        prompt.append("    - 必须充分利用团队设计的所有元素：场景设计师的场景、角色设计师的角色、线索设计师的线索、氛围设计师的氛围\n");
        prompt.append("    - 将团队讨论中的创意想法融入剧本，体现集体智慧\n");
        prompt.append("    - 确保各设计师的贡献在最终剧本中都有体现和价值\n");
        prompt.append("    - 利用节点间的连接关系构建合理的剧情逻辑链\n\n");

        // 规则3：质量要求（参考现有规则）
        prompt.append("规则3. 质量要求：\n");
        prompt.append("    - 若有玩家人物是凶手，则需要在其剧本中写明身份；若未写明身份，需在真相处表明原因（如失忆、人格扭曲...）\n");
        prompt.append("    - 线索必须与剧情紧密结合，形成完整的推理链条\n");
        prompt.append("    - 每个角色都应有充分的参与度和独特的视角\n");
        prompt.append("    - 氛围描述要与场景和剧情发展相呼应\n\n");

        // 协作设计内容
        prompt.append("## 团队协作设计成果：\n");
        prompt.append(contextSummary).append("\n\n");

        // 具体创作指导
        prompt.append("## 创作指导：\n");
        prompt.append("1. **背景整合**：基于场景设计师的场景设定和氛围设计师的环境描述，构建完整的故事背景\n");
        prompt.append("2. **角色发展**：以角色设计师设计的角色为基础，结合团队讨论，丰富角色的动机、关系和剧本内容\n");
        prompt.append("3. **线索布局**：将线索设计师的线索与场景、角色有机结合，确保每条线索都有明确的发现方式和推理价值\n");
        prompt.append("4. **氛围营造**：利用氛围设计师的环境要素（灯光、音乐、天气等）增强剧本的沉浸感\n");
        prompt.append("5. **逻辑链条**：根据节点间的连接关系，构建清晰的因果关系和推理路径\n");
        prompt.append("6. **团队智慧**：将聊天讨论中的创意想法和建议融入剧本，体现协作价值\n\n");

        // 特别注意事项
        prompt.append("## 特别注意：\n");
        prompt.append("- 这是团队协作的成果，请在AI回复中体现对团队合作的认可\n");
        prompt.append("- 确保每个设计师的贡献都在最终剧本中有所体现\n");
        prompt.append("- 利用协作过程中产生的创意火花，让剧本更加精彩\n");
        prompt.append("- 保持剧本的逻辑性和可玩性，确保团队的努力转化为优质作品\n\n");

        prompt.append("请基于以上团队协作成果，创作一个完整、精彩的剧本杀剧本：\n");

        return prompt.toString();
    }

}
