package com.bengebackend.service.serviceImpl;

import com.bengebackend.dto.ScriptDetailDto;
import com.bengebackend.dto.ScriptFrameworkDto;
import com.bengebackend.entity.AIMsgDevide;
import com.bengebackend.entity.ScriptReplyRequestEntity;
import com.bengebackend.mapper.ScriptMapper;
import com.bengebackend.model.Script;
import com.bengebackend.model.ScriptAnalysis;
import com.bengebackend.model.ScriptHistory;
import com.bengebackend.model.VisualElement;
import com.bengebackend.service.*;

import dev.langchain4j.model.output.structured.Description;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.HashMap;

/**
 * 剧本服务实现类
 */
@Service
public class ScriptServiceImpl implements ScriptService {

    @Autowired
    private ScriptMapper scriptMapper;

    @Autowired
    private ScriptHistoryService scriptHistoryService;

    @Autowired
    private ScriptAnalysisService scriptAnalysisService;

    @Autowired
    private VisualElementService visualElementService;

    @Autowired
    private AIService aiService;

    @Override
    public ScriptDetailDto getScriptByIdAsync(Integer id) {
        Script script = scriptMapper.selectById(id);
        if (script == null || script.getIsDeleted()) {
            return null;
        }

        List<ScriptHistory> history = scriptHistoryService.getHistoryByScript(id);
        ScriptAnalysis analysis = scriptAnalysisService.getByScriptId(id);
        List<VisualElement> visualElements = visualElementService.getAllElements(id);

        ScriptDetailDto dto = new ScriptDetailDto();
        dto.setScript(script);
        dto.setHistory(history);
        dto.setAnalysis(analysis);
        dto.setVisualElements(visualElements);

        return dto;
    }

    @Override
    public List<Script> getUserScriptsAsync(Integer userId) {
        return scriptMapper.selectByUserId(userId);
    }

    @Override
    public Script createScriptAsync(Script script) {
        script.setLastUpdated(LocalDateTime.now());
        scriptMapper.insert(script);
        return script;
    }

    @Override
    public void updateScriptAsync(Integer scriptId, String title, String content, Integer stage) {
        scriptMapper.update(scriptId, title, content, stage, LocalDateTime.now());
    }

    @Override
    public void deleteScriptAsync(Integer id) {
        scriptMapper.deleteById(id);
    }

    @Override
    public ScriptDetailDto initializeScriptAsync(Integer userId) {
        Script newScript = new Script();
        newScript.setTitle("新剧本");
        newScript.setContent("");
        newScript.setUserId(userId);
        newScript.setIsDeleted(false);
        newScript.setLastUpdated(LocalDateTime.now());
        newScript.setStage(1);

        System.out.println("222222222:" + newScript);
        scriptMapper.insert(newScript);

        ScriptDetailDto dto = new ScriptDetailDto();
        dto.setScript(newScript);
        dto.setHistory(new ArrayList<>());
        dto.setAnalysis(null);
        dto.setVisualElements(new ArrayList<>());

        return dto;
    }

    @Override
    public ScriptFrameworkDto genFrame(ScriptReplyRequestEntity request, List<ScriptHistory> history,
            String scriptContent) {
        // 处理发给AI的消息
        List<Map<String, String>> messages = new ArrayList<>();
        for (ScriptHistory h : history) {
            Map<String, String> msg = new HashMap<>();
            if (h.getResponse() == "" && h.getMessage() != "") {
                msg.put("role", "user");
                msg.put("content", h.getMessage());
            } else {
                msg.put("role", "assistant");
                msg.put("content", h.getResponse());
            }
            messages.add(msg);
        }
        messages.add(new HashMap<String, String>() {
            {
                put("role", "user");
                put("content", request.getMessage());
            }
        });

        // 模拟AI生成框架的逻辑
        // AIMsgDevide aiMsgTemp = aiService.GenFramework(messages).join();
        // String mockResponse = aiMsgTemp.getStrScript();
        // String title = aiMsgTemp.getTitle();(AI生成调用)
        String mockResponse = "\"背景\": \"在太平洋航行的豪华游轮[爱神号]上，正举行珠宝大亨千金的婚礼。仪式开始前15分钟，新娘突然从化妆室消失，只留下地板上未干的血迹。游轮还有1小时即将起航，所有宾客都成为了嫌疑人......\"";
        String title = "消失的新娘";

        // 更新剧本
        updateScriptAsync(request.getScriptId(), title, mockResponse, 2);

        // 添加历史记录
        ScriptHistory userHistory = new ScriptHistory();
        userHistory.setScriptId(request.getScriptId());
        userHistory.setMessage(request.getMessage());
        userHistory.setResponse("");
        userHistory.setCreatedAt(LocalDateTime.now());
        scriptHistoryService.addHistory(userHistory);

        ScriptHistory aiHistory = new ScriptHistory();
        aiHistory.setScriptId(request.getScriptId());
        aiHistory.setMessage("");
        aiHistory.setResponse(mockResponse);
        aiHistory.setCreatedAt(LocalDateTime.now());
        scriptHistoryService.addHistory(aiHistory);

        // 重新获取更新后的剧本详情
        ScriptDetailDto updatedDto = getScriptByIdAsync(request.getScriptId());

        ScriptFrameworkDto frameworkDto = new ScriptFrameworkDto();
        frameworkDto.setScript(updatedDto.getScript());
        frameworkDto.setDialogHistory(updatedDto.getHistory());

        return frameworkDto;
    }

    @Override
    public String genFrameStreamAsync(ScriptReplyRequestEntity request) {
        // 模拟流式响应
        return "流式生成的剧本框架内容...";
    }

    @Override
    public ScriptAnalysis analyzeScriptContent(String scriptContent, Integer scriptId) {

        ScriptAnalysis analysis = new ScriptAnalysis();
        analysis.setScriptId(scriptId);
        analysis.setAnalysisResult("剧本分析结果：这是一个悬疑推理类剧本...");
        analysis.setAnalyzedAt(LocalDateTime.now());

        // String analysisResult = aiService.AnalyzeScriptContent(scriptContent).join();
        // ScriptAnalysis analysis = new ScriptAnalysis();
        // analysis.setScriptId(scriptId);
        // analysis.setAnalysisResult(analysisResult);
        // analysis.setAnalyzedAt(LocalDateTime.now());

        scriptAnalysisService.saveAnalysis(analysis);
        return analysis;
    }

    @Override
    public ScriptDetailDto getCompSctiptAndDesc(Script script) {
        // // 调用AI服务获取剧本内容和元素描述
        // String fullContent = aiService.GenDetail(script.getContent(),
        // script.getTitle()).join();
        // List<List<List<String>>> Descriptions =
        // aiService.GetThreeTypesOfDesc(fullContent).join();
        // List<VisualElement> newElements = new ArrayList<>();
        // for (int n1 = 0; n1 < 3; n1 += 1) {
        // for (int n3 = 0; n3 < Descriptions.get(n1).size(); n3 += 1) {
        // VisualElement element = new VisualElement();
        // element.setScriptId(script.getId());
        // if(n1==0)element.setType("Character");
        // if(n1==1)element.setType("Scene");
        // if(n1==2)element.setType("Prop");
        // element.setId(n3);
        // element.setName(Descriptions.get(n1).get(0).get(n3));
        // element.setDescription(Descriptions.get(n1).get(1).get(n3));
        // newElements.add(element);
        // }
        // }

        // // 更新剧本
        // updateScriptAsync(script.getId(), script.getTitle(), fullContent, 3);

        // // 更新视觉元素描述
        // visualElementService.updateVisualElements(script.getId(), newElements);

        return getScriptByIdAsync(script.getId());
    }

    @Override
    public String visualizeScriptAsync(Integer scriptId, Integer elementId) {
        // List<VisualElement> ves = getScriptByIdAsync(scriptId).getVisualElements();
        // String Image_base64 = aiService.GenImage(ves.get(elementId).getName() + "：" +
        // ves.get(elementId).getDescription(), null).join();
        // visualElementService.updateElementUrl(elementId, Image_base64);
        // return Image_base64;
        // 模拟生成图像URL
        return "https://example.com/generated-image-" + elementId + ".jpg";
    }
}
