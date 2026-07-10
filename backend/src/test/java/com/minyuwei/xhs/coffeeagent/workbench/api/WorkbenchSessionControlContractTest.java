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

import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(classes = CoffeeAgentApplication.class)
@AutoConfigureMockMvc
class WorkbenchSessionControlContractTest {
    @Autowired
    private MockMvc mockMvc;

    @Test
    void clearSessionRequiresConfirmationAndReturnsEmptySnapshotWhenConfirmed() throws Exception {
        String sessionId = createSession();

        mockMvc.perform(post("/api/workbench/sessions/{sessionId}/clear", sessionId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(ApiContractTestSupport.json(Map.of("confirmed", false))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.error.code").value("CLEAR_SESSION_NOT_CONFIRMED"));

        mockMvc.perform(post("/api/workbench/sessions/{sessionId}/clear", sessionId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(ApiContractTestSupport.json(Map.of("confirmed", true))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.sessionId", nullValue()))
                .andExpect(jsonPath("$.data.status").value("EMPTY"))
                .andExpect(jsonPath("$.data.conversation").isEmpty())
                .andExpect(jsonPath("$.data.agentState.contextPreview.boundaryNote").value("当前没有可发送上下文。输入后将调用 GPT-5.5。"))
                .andExpect(jsonPath("$.data.agentState.sessionControlAction.resultStatus").value("CLEARED"));
    }

    private String createSession() throws Exception {
        String body = mockMvc.perform(post("/api/workbench/sessions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(ApiContractTestSupport.json(Map.of("mode", "EXPLICIT_WORKFLOW"))))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        return JsonPath.read(body, "$.data.sessionId");
    }
}
