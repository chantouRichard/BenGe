package com.bengebackend.controller;

import com.bengebackend.dto.ScriptDetailDto;
import com.bengebackend.entity.ScriptReplyRequestEntity;
import com.bengebackend.model.Script;
import com.bengebackend.service.ScriptService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Script Controller 测试类
 */
@WebMvcTest(ScriptController.class)
class ScriptControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ScriptService scriptService;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void testGetScriptById() throws Exception {
        // 准备测试数据
        Script script = new Script();
        script.setId(1);
        script.setTitle("测试剧本");
        script.setContent("测试内容");
        script.setUserId(1);
        script.setStage(1);
        script.setIsDeleted(false);
        script.setLastUpdated(LocalDateTime.now());

        ScriptDetailDto dto = new ScriptDetailDto();
        dto.setScript(script);

        when(scriptService.getScriptByIdAsync(1)).thenReturn(dto);

        mockMvc.perform(get("/api/script/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.script.id").value(1))
                .andExpect(jsonPath("$.script.title").value("测试剧本"));
    }

    @Test
    void testCreateNewScript() throws Exception {
        Script script = new Script();
        script.setId(1);
        script.setTitle("新剧本");
        script.setContent("");
        script.setUserId(1);
        script.setStage(1);
        script.setIsDeleted(false);
        script.setLastUpdated(LocalDateTime.now());

        ScriptDetailDto dto = new ScriptDetailDto();
        dto.setScript(script);

        when(scriptService.initializeScriptAsync(any())).thenReturn(dto);

        mockMvc.perform(post("/api/script/create"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.script.title").value("新剧本"));
    }

    @Test
    void testHandleUserMessage2nd() throws Exception {
        ScriptReplyRequestEntity request = new ScriptReplyRequestEntity();
        request.setScriptId(1);
        request.setMessage("请帮我创建一个悬疑剧本");

        Script script = new Script();
        script.setId(1);
        script.setTitle("测试剧本");
        script.setContent("测试内容");

        ScriptDetailDto scriptDetail = new ScriptDetailDto();
        scriptDetail.setScript(script);

        when(scriptService.getScriptByIdAsync(1)).thenReturn(scriptDetail);
        when(scriptService.genFrame(any(), any(), any())).thenReturn(new com.bengebackend.dto.ScriptFrameworkDto());

        mockMvc.perform(post("/api/script/reply2nd")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());
    }
}
