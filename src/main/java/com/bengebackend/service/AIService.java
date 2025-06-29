package com.bengebackend.service;

import com.bengebackend.entity.Slogan;
import com.bengebackend.entity.SloganRequestEntity;
import com.bengebackend.entity.AIMsgDevide;
import com.bengebackend.entity.AIAnalysisResult;
import com.bengebackend.entity.ThingDesc;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.bengebackend.entity.ImageParameters;
import com.bengebackend.entity.AIStruct.*;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import org.springframework.scheduling.config.Task;

import com.bengebackend.dto.SloganResponseDto;

/**
 * 剧本服务接口
 */
public interface AIService {

    /**
     * 根据ID获取剧本详情
     */
    CompletableFuture<AIMsgDevide> GenFramework(List<Map<String, String>> msgs);

    /**
     * 根据ID获取剧本详情
     */
    CompletableFuture<String> GenDetail(String frame, String title);

    /**
     * 根据ID获取剧本详情
     */
    CompletableFuture<String> GetAPIOutputAsync(List<Map<String, String>> msgs);

    /**
     * 根据ID获取剧本详情
     */
    CompletableFuture<String> GetV3Output(List<Message> msgs) throws IOException;

    /**
     * 根据ID获取剧本详情
     */
    CompletableFuture<String> AnalyzeScriptContent(String scriptContent);

    /**
     * 根据ID获取剧本详情
     */
    String ConvertToMarkdown(AIAnalysisResult result);

    /**
     * 根据ID获取剧本详情
     */
    CompletableFuture<ImageMsgs> GenDescription(String final_result);

    /**
     * 根据ID获取剧本详情
     */
    CompletableFuture<String> GenerateImageUrl(String type, String name, String imageDesc);

    /**
     * 根据ID获取剧本详情
     */
    CompletableFuture<String> GenerateImagesAsync(String prompt, String negativePrompt);

    /**
     * 根据ID获取剧本详情
     */
    CompletableFuture<String> SubmitImageGenerationTask(String prompt, String negativePrompt, ImageParameters p);

    /**
     * 根据ID获取剧本详情
     */
    CompletableFuture<List<String>> WaitForTaskCompletion(String taskId, int maxRetries, int delaySeconds);

}
