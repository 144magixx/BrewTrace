package com.minyuwei.xhs.coffeeagent.workbench.api;

import com.minyuwei.xhs.coffeeagent.CoffeeAgentApplication;
import com.minyuwei.xhs.coffeeagent.support.ApiContractTestSupport;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(classes = CoffeeAgentApplication.class, properties = "OPENAI_API_KEY=")
@AutoConfigureMockMvc
class WorkbenchApiContractTest {
    @Autowired
    private MockMvc mockMvc;

    @Test
    void createSessionAndSubmitMessageThroughGpt55Mode() throws Exception {
        String created = mockMvc.perform(post("/api/workbench/sessions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(ApiContractTestSupport.json(Map.of("mode", "EXPLICIT_WORKFLOW"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.sessionId").isString())
                .andExpect(jsonPath("$.data.status").value("SESSION_CREATED"))
                .andReturn()
                .getResponse()
                .getContentAsString();
        String sessionId = JsonPath.read(created, "$.data.sessionId");

        mockMvc.perform(post("/api/workbench/sessions/{sessionId}/messages", sessionId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(ApiContractTestSupport.json(Map.of("content", "今天喝了一支水洗埃塞，有柑橘和红茶感"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("ERROR_RECOVERABLE"))
                .andExpect(jsonPath("$.data.agentState.modelMode.mode").value("openai-gpt55"))
                .andExpect(jsonPath("$.data.agentState.modelOutput.outputType").value("ERROR"))
                .andExpect(jsonPath("$.data.agentState.modelOutput.recoverableError.code").value("MODEL_AUTH_FAILED"));
    }
}
