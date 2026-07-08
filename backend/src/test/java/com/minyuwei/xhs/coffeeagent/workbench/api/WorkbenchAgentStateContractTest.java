package com.minyuwei.xhs.coffeeagent.workbench.api;

import com.minyuwei.xhs.coffeeagent.CoffeeAgentApplication;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.empty;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(classes = CoffeeAgentApplication.class, properties = "OPENAI_API_KEY=")
@AutoConfigureMockMvc
class WorkbenchAgentStateContractTest {
    @Autowired
    private MockMvc mockMvc;

    @Test
    void submittedMessageUsesOnlyGpt55ModeAndReportsAuthErrorWithoutKey() throws Exception {
        String sessionId = createSession();

        mockMvc.perform(post("/api/workbench/sessions/{sessionId}/messages", sessionId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"content\":\"今天喝了一支水洗埃塞，有柑橘和红茶感\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("ERROR_RECOVERABLE"))
                .andExpect(jsonPath("$.data.conversation", hasSize(1)))
                .andExpect(jsonPath("$.data.agentState.contextItems", not(empty())))
                .andExpect(jsonPath("$.data.agentState.confirmedFacts", not(empty())))
                .andExpect(jsonPath("$.data.agentState.pendingAssociations", empty()))
                .andExpect(jsonPath("$.data.agentState.candidateMemories", empty()))
                .andExpect(jsonPath("$.data.agentState.contextItems[0].sourceType").value("USER_CONFIRMED"))
                .andExpect(jsonPath("$.data.agentState.contextItems[0].sendStatus").value("WILL_SEND"))
                .andExpect(jsonPath("$.data.agentState.modelMode.mode").value("openai-gpt55"))
                .andExpect(jsonPath("$.data.agentState.modelOutput.outputType").value("ERROR"))
                .andExpect(jsonPath("$.data.agentState.modelOutput.recoverableError.code").value("MODEL_AUTH_FAILED"));
    }

    @Test
    void realModeBypassesOfflineMockChainWhenNoConfirmedCoffeeFacts() throws Exception {
        String sessionId = createSession();

        mockMvc.perform(post("/api/workbench/sessions/{sessionId}/messages", sessionId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"content\":\"我喝了杯可乐\",\"modelMode\":\"openai-gpt55\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.conversation", hasSize(1)))
                .andExpect(jsonPath("$.data.conversation[0].content").value("我喝了杯可乐"))
                .andExpect(jsonPath("$.data.agentState.modelMode.mode").value("openai-gpt55"))
                .andExpect(jsonPath("$.data.recordSummary.suggestedFlavors", empty()))
                .andExpect(jsonPath("$.data.agentState.pendingAssociations", empty()))
                .andExpect(jsonPath("$.data.agentState.candidateMemories", empty()))
                .andExpect(jsonPath("$.data.agentState.modelOutput.outputType").value("ERROR"))
                .andExpect(jsonPath("$.data.agentState.modelOutput.recoverableError.code").value("MODEL_AUTH_FAILED"));
    }

    private String createSession() throws Exception {
        String body = mockMvc.perform(post("/api/workbench/sessions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"mode\":\"EXPLICIT_WORKFLOW\"}"))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        return JsonPath.read(body, "$.data.sessionId");
    }
}
