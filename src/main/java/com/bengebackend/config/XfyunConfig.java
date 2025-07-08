package com.bengebackend.config;

import com.bengebackend.util.ContextDataProcessor;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.*;

@Data
@Slf4j
@Component
@ConfigurationProperties(prefix = "xfyun")
public class XfyunConfig {
    private String appid;
    private String apiPassword;
    private String apiUrl;

    @Autowired
    private ContextDataProcessor contextDataProcessor;

    @Autowired
    private ObjectMapper objectMapper;

    public static final String SYSTEM_MESSAGE = """
            你是一位热爱剧本创作的小伙伴，正在和其他创作者一起在群聊里讨论项目。你很懂剧本写作，经验丰富，总能给出实用的建议。

            聊天风格：

            用日常口语，就像平时微信群聊一样自然

            语气轻松友好，偶尔用点表情符号😊

            回复简洁有用，不要太正式或冗长

            把自己当成群里的一员，而不是AI助手

            专业能力：

            擅长剧本结构、人物塑造、对话写作

            能快速给出创意建议和修改意见

            了解各种类型剧本的创作技巧

            会分享实用的写作方法和工具

            记住：你就是个爱写剧本的朋友，在群里和大家一起头脑风暴呢！
            """;

    /**
     * 根据第二阶段工作成果生成完整内容的提示词
     * @param contextSummary：原始创作内容
     * @return 完整提示词
     */
    public static String buildCollaborationPrompt(String contextSummary) {
        return """
                你是一名逻辑严谨且追求内容完整的剧本杀作家，专门负责将团队协作设计的内容整合为完整剧本。必须严格遵循以下规则：
                
                                规则1. 请严格按指定MarkDown格式输出以下字段，七部分内容缺一不可！不要包含额外文本！:
                                    标题（不少于8字）
                                    背景（一段话，不少于200字，包含时间、地点、氛围细节）
                                    人物剧本（数组，每个角色剧本不少于250字，包含人物性格、动机、秘密、目标、与其他角色的关系）
                                    线索（数组，每条线索不少于50字，共不少于8条，包含发现方式、位置、指向哪个角色或事件）
                                    真相（不少于150字，需完整交代凶手身份、作案动机、作案手法与时间）
                                    组织者手册（不少于150字，包含主持流程、提示时机、隐藏线索操作、氛围营造建议）
                
                                输出需符合如下结构：
                                    AI回复：
                                    ...（这部分是给用户的礼貌性回答，体现协作成果的整合）
                                    》》》
                                    # ...（这部分是标题）
                                    ---
                                    ## 背景
                                    ...（不少于200字）
                                    ---
                                    ## 人物剧本:
                                    -CHR ...（角色1名称）
                                    ...（角色1剧本，不少于300字）
                                    -CHR ...（角色2名称）
                                    ...（角色2剧本，不少于300字）
                                    ...（其它角色的格式以此类推）
                                    ---
                                    ## 线索
                                    -C> ...（线索1，不少于50字）
                                    -C> ...（线索2，不少于50字）
                                    ...
                                    -C> ...（至少8条线索）
                                    ---
                                    ## 真相
                                    ...（不少于200字）
                                    ---
                                    ## 组织者手册
                                    ...（不少于200字）
                                    ---
                                    （到此结束，不要再输出文本）
                
                                规则2. 协作整合要求：
                                    - 必须充分利用团队设计的所有元素：场景设计师的场景、角色设计师的角色、线索设计师的线索、氛围设计师的氛围
                                    - 将团队讨论中的创意想法融入剧本，体现集体智慧
                                    - 确保各设计师的贡献在最终剧本中都有体现和价值
                                    - 利用节点间的连接关系构建合理的剧情逻辑链
                
                                规则3. 质量要求：
                                    - 若有玩家人物是凶手，则需要在其剧本中写明身份；若未写明身份，需在真相处表明原因（如失忆、人格扭曲...）
                                    - 线索必须与剧情紧密结合，形成完整的推理链条
                                    - 每个角色都应有充分的参与度和独特的视角
                                    - 氛围描述要与场景和剧情发展相呼应
                                    - 整体剧本字数不少于3000字
                
                                ## 团队协作设计成果：
                                %s
                
                                ## 创作指导：
                                1. **背景整合**：基于场景设计师的场景设定和氛围设计师的环境描述，构建完整的故事背景，加入具体时间、天气、灯光、音效等要素。
                                2. **角色发展**：以角色设计师设计的角色为基础，结合团队讨论，丰富角色的动机、关系、秘密和性格细节，保证每个剧本段落不少于200字。
                                3. **线索布局**：将线索设计师的线索与场景、角色、事件有机结合，说明每条线索的发现方式、线索内容和指向性，确保至少8条线索。
                                4. **氛围营造**：利用氛围设计师的环境要素（灯光、音乐、天气、声效等），在背景与剧本中嵌入对应提示，增强沉浸感。
                                5. **逻辑链条**：根据节点间的连接关系，构建清晰的因果关系和推理路径，并在真相处完整说明凶手动机与过程。
                                6. **团队智慧**：将聊天讨论中的创意想法和建议融入剧本，体现协作价值与创新点。
                
                                ## 特别注意：
                                - 这是团队协作的成果，请在AI回复中体现对团队合作的认可
                                - 确保每个设计师的贡献都在最终剧本中有所体现
                                - 利用协作过程中产生的创意火花，让剧本更加精彩
                                - 保持剧本的逻辑性和可玩性，确保团队的努力转化为优质作品
                
                                请基于以上团队协作成果，创作一个完整、精彩的剧本杀剧本：
                
                """.formatted(contextSummary);
    }

    /**
     * 检测是否为冲突检测请求
     */
    private boolean isConflictDetectionRequest(String userMessage) {
        String lowerMessage = userMessage.toLowerCase();
        return lowerMessage.contains("检测冲突") ||
                lowerMessage.contains("冲突检查") ||
                lowerMessage.contains("冲突分析") ||
                lowerMessage.contains("conflict") ||
                lowerMessage.contains("检查冲突") ||
                lowerMessage.contains("分析冲突") ||
                lowerMessage.contains("有没有冲突") ||
                lowerMessage.contains("存在冲突") ||
                lowerMessage.contains("冲突问题");
    }

    /**
     * 构建冲突检测专用提示词
     */
    private String buildConflictDetectionPrompt(String contextData, String username, String userMessage) {
        StringBuilder prompt = new StringBuilder();

        prompt.append("你是专业的剧本杀设计冲突检测专家。请基于当前项目的完整状态，进行全面的冲突分析。\n\n");

        // 添加上下文信息
        if (!contextData.isEmpty()) {
            try {
                String contextSummary = contextDataProcessor.generateContextSummary(contextData, "conflict");
                prompt.append(contextSummary).append("\n");
            } catch (Exception e) {
                log.error("处理冲突检测上下文数据失败", e);
                prompt.append("当前协作环境：数据处理异常，将基于可用信息进行冲突检测。\n\n");
            }
        } else {
            prompt.append("当前协作环境：新项目，暂无现有内容可供冲突检测。\n\n");
            return "目前项目还没有足够的设计内容，暂时无法进行冲突检测。建议在添加更多场景、角色、线索等内容后再进行冲突分析。";
        }

        prompt.append("【冲突检测任务】\n");
        prompt.append(username).append("请求：").append(userMessage).append("\n\n");

        prompt.append("请进行全面的冲突检测分析，重点关注以下方面：\n\n");

        prompt.append("**1. 时间线冲突检测**\n");
        prompt.append("- 检查场景时间标签是否存在重叠或逻辑错误\n");
        prompt.append("- 分析角色在同一时间段是否出现在多个场景中\n");
        prompt.append("- 验证事件发生的时间顺序是否合理\n");
        prompt.append("- 检查线索发现时间与场景时间的一致性\n\n");

        prompt.append("**2. 逻辑冲突检测**\n");
        prompt.append("- 角色关系是否存在矛盾（如A认识B，但B不认识A）\n");
        prompt.append("- 线索逻辑链是否完整且无矛盾\n");
        prompt.append("- 角色动机与行为是否一致\n");
        prompt.append("- 推理节点的逻辑推导是否合理\n\n");

        prompt.append("**3. 内容冲突检测**\n");
        prompt.append("- 角色背景描述是否存在自相矛盾\n");
        prompt.append("- 场景描述与角色、线索的关联是否合理\n");
        prompt.append("- 角色技能、物品与其背景是否匹配\n");
        prompt.append("- 氛围设计与场景、情节是否协调\n\n");

        prompt.append("**4. 跨设计师类型冲突检测**\n");
        prompt.append("- 场景设计与角色设计的匹配度\n");
        prompt.append("- 线索设计与场景、角色的关联性\n");
        prompt.append("- 氛围设计与整体故事风格的一致性\n");
        prompt.append("- 不同设计师创建内容之间的协调性\n\n");

        prompt.append("**输出要求：**\n");
        prompt.append("请按以下格式输出检测结果：\n\n");
        prompt.append("🔍 **冲突检测报告**\n\n");
        prompt.append("**检测概况：**\n");
        prompt.append("- 总体评估：[无冲突/轻微冲突/中等冲突/严重冲突]\n");
        prompt.append("- 检测范围：[具体说明检测了哪些内容]\n\n");

        prompt.append("**发现的冲突：**\n");
        prompt.append("[如果有冲突，按类型详细列出，包括具体位置和问题描述,如果没有冲突就不要说]\n\n");

        prompt.append("**修改建议：**\n");
        prompt.append("[针对每个冲突提供具体的解决方案]\n\n");

        prompt.append("**优化建议：**\n");
        prompt.append("[提供进一步完善设计的建议]\n\n");

        prompt.append("请开始进行冲突检测分析：");

        return prompt.toString();
    }

    /**
     * 构建的提示
     */
    public String buildEnhancedPrompt(String userMessage, String contextData, String username) {
        // 检测是否为冲突检测请求
        if (isConflictDetectionRequest(userMessage)) {
            return buildConflictDetectionPrompt(contextData, username, userMessage);
        }

        StringBuilder prompt = new StringBuilder();

        prompt.append("你是团队的AI创作助手，正在和大家一起协作设计剧本杀。你能看到项目的完整状态，包括所有设计内容和讨论历史。请用自然、友好的语气参与讨论，就像团队中的一员\n\n");

        // 使用ContextDataProcessor处理上下文数据，获取完整的协作状态
        if (!contextData.isEmpty()) {
            try {
                // 生成完整的上下文摘要，不区分设计师类型
                String contextSummary = contextDataProcessor.generateContextSummary(contextData, "chat");
                prompt.append(contextSummary).append("\n");
            } catch (Exception e) {
                log.error("处理聊天上下文数据失败", e);
                prompt.append("当前协作环境：数据处理异常，基于用户问题进行回复。\n\n");
            }
        } else {
            prompt.append("当前协作环境：新项目开始，团队正在进行初步讨论。\n\n");
        }

        prompt.append("【用户消息】\n");
        prompt.append(username).append("：").append(userMessage).append("\n\n");

        prompt.append("请基于完整的协作状态、所有现有设计内容和讨论历史，用自然的聊天语气回复。");
        prompt.append("回复要求：\n");
        prompt.append("1. 语气自然友好，像团队成员一样\n");
        prompt.append("2. 基于所有现有内容（场景、角色、线索、氛围等）给出建设性建议\n");
        prompt.append("3. 参考之前的讨论历史，保持对话的连贯性\n");
        prompt.append("4. 不要列举分析步骤，直接给出讨论回答\n");
        prompt.append("5. 如果涉及具体设计，要考虑与整体项目的一致性和协调性\n");
        prompt.append("6. 能够跨设计师类型提供综合性的建议和意见\n\n");
        prompt.append("开始回复：");

        return prompt.toString();
    }

    public String buildPrompt(String userInput, String designerType, Integer count, String contextData) {
        StringBuilder prompt = new StringBuilder();

        // 根据设计师类型构建不同的提示词
        switch (designerType) {
            case "narrative":
                prompt.append("你是剧本杀创作助手，需要根据用户需求智能生成合适数量的场景节点。\n\n");
                break;
            case "character":
                prompt.append("你是剧本杀创作助手，需要根据用户需求智能生成合适数量的角色节点。\n\n");
                break;
            case "clue":
                prompt.append("你是剧本杀创作助手，需要根据用户需求智能生成合适数量的线索节点。\n\n");
                break;
            case "atmosphere":
                prompt.append("你是剧本杀创作助手，需要根据用户需求智能生成合适数量的氛围节点。\n\n");
                break;
            default:
                prompt.append("你是剧本杀创作助手，需要根据用户需求智能生成合适数量的节点。\n\n");
        }

        if (contextData != null && !contextData.isEmpty()) {
            prompt.append("当前协作状态上下文：\n").append(contextData).append("\n\n");
        }

        prompt.append("用户需求：").append(userInput).append("\n\n");

        // 根据设计师类型生成不同的字段结构
        prompt.append("请根据用户需求智能决定生成合适数量的节点（通常2-6个），每个节点包含以下字段：\n");
        prompt.append("{\n");

        switch (designerType) {
            case "narrative":
                prompt.append("  \"title\": \"场景名称\",\n");
                prompt.append("  \"timeLabel\": \"时间标签(如DAY1 09:00)\",\n");
                prompt.append("  \"characters\": \"涉及角色\",\n");
                prompt.append("  \"clues\": \"相关线索\",\n");
                prompt.append("  \"sceneDescription\": \"场景详细描述\",\n");
                prompt.append("  \"nodeConnections\": \"与其他节点的关系\",\n");
                prompt.append("  \"notes\": \"备注\"\n");
                break;
            case "character":
                prompt.append("  \"name\": \"角色姓名\",\n");
                prompt.append("  \"occupation\": \"职业\",\n");
                prompt.append("  \"age\": 年龄数字,\n");
                prompt.append("  \"background\": \"背景故事\",\n");
                prompt.append("  \"personality\": [\"性格特点1\", \"性格特点2\", \"性格特点3\"],\n");
                prompt.append("  \"skills\": [\"技能1\", \"技能2\", \"技能3\"],\n");
                prompt.append("  \"items\": \"携带物品\",\n");
                prompt.append("  \"notes\": \"备注\"\n");
                break;
            case "clue":
                prompt.append("  \"title\": \"线索名称\",\n");
                prompt.append("  \"type\": \"线索类型\",\n");
                prompt.append("  \"description\": \"线索描述\",\n");
                prompt.append("  \"location\": \"发现地点\",\n");
                prompt.append("  \"relatedCharacters\": \"相关角色\",\n");
                prompt.append("  \"importance\": \"重要程度\",\n");
                prompt.append("  \"hiddenInfo\": \"隐藏信息\",\n");
                prompt.append("  \"notes\": \"备注\"\n");
                break;
            case "atmosphere":
                prompt.append("  \"title\": \"氛围名称\",\n");
                prompt.append("  \"mood\": \"情绪氛围(如：紧张、平静、神秘)\",\n");
                prompt.append("  \"lighting\": \"灯光设置\",\n");
                prompt.append("  \"music\": \"背景音乐\",\n");
                prompt.append("  \"weather\": \"天气状况\",\n");
                prompt.append("  \"timeOfDay\": \"时间段\",\n");
                prompt.append("  \"sceneElements\": \"场景元素\",\n");
                prompt.append("  \"notes\": \"备注\"\n");
                break;
        }

        prompt.append("}\n\n");
        prompt.append("请直接返回JSON数组格式，不要其他解释：");

        return prompt.toString();
    }

    public List<Map<String, Object>> parseAiResponse(String aiResponse, String designerType) {
        try {
            // 查找JSON数组
            int start = aiResponse.indexOf('[');
            int end = aiResponse.lastIndexOf(']');

            if (start != -1 && end != -1 && end > start) {
                String jsonPart = aiResponse.substring(start, end + 1);
                return objectMapper.readValue(jsonPart, List.class);
            }

            return createDefaultNodes(designerType);

        } catch (Exception e) {
            log.error("解析AI回复失败: {}", aiResponse, e);
            return createDefaultNodes(designerType);
        }
    }

    public List<Map<String, Object>> createDefaultNodes(String designerType) {
        List<Map<String, Object>> nodes = new ArrayList<>();

        // 默认生成2个节点，避免固定数量
        for (int i = 1; i <= 2; i++) {
            Map<String, Object> node = new HashMap<>();

            switch (designerType) {
                case "character":
                    node.put("name", "AI生成角色" + i);
                    node.put("occupation", "待定职业");
                    node.put("age", 25 + i);
                    node.put("background", "AI自动生成的角色背景");
                    node.put("personality", Arrays.asList("待定性格1", "待定性格2", "待定性格3"));
                    node.put("skills", Arrays.asList("待定技能1", "待定技能2", "待定技能3"));
                    node.put("items", "待定物品");
                    node.put("notes", "AI生成失败时的默认角色节点");
                    node.put("relationships", new ArrayList<>());
                    break;
                case "clue":
                    node.put("title", "AI生成线索" + i);
                    node.put("type", "物理证据");
                    node.put("description", "AI自动生成的线索描述");
                    node.put("location", "待定地点");
                    node.put("relatedCharacters", Arrays.asList("待定角色"));
                    node.put("importance", "中");
                    node.put("hiddenInfo", "待定隐藏信息");
                    node.put("notes", "AI生成失败时的默认线索节点");
                    break;
                case "atmosphere":
                    node.put("title", "AI生成氛围" + i);
                    node.put("timeLabel", "DAY1 " + (8 + i) + ":00");
                    node.put("mood", "平静");
                    node.put("lighting", "自然光");
                    node.put("music", "无");
                    node.put("weather", "晴朗");
                    node.put("sceneElements", "待定场景元素");
                    node.put("notes", "AI生成失败时的默认氛围节点");
                    break;
                default: // narrative
                    node.put("title", "AI生成场景" + i);
                    node.put("timeLabel", "DAY1 " + (8 + i) + ":00");
                    node.put("characters", "待定角色");
                    node.put("clues", "待定线索");
                    node.put("sceneDescription", "AI自动生成的场景描述");
                    node.put("nodeConnections", "与其他场景相关");
                    node.put("notes", "AI生成失败时的默认节点");
                    break;
            }
            nodes.add(node);
        }
        return nodes;
    }

    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }
}
