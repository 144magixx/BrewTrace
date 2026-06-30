package com.minyuwei.xhs.coffeeagent.workbench.api;

import com.minyuwei.xhs.coffeeagent.CoffeeAgentApplication;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(classes = CoffeeAgentApplication.class)
@AutoConfigureMockMvc
class WorkbenchErrorContractTest {
    @Autowired
    private MockMvc mockMvc;

    @Test
    void emptyInputReturnsRecoverableErrorWithPreservedInput() throws Exception {
        String sessionId = createSession();

        mockMvc.perform(post("/api/workbench/sessions/{sessionId}/messages", sessionId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"content\":\"\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").doesNotExist())
                .andExpect(jsonPath("$.error.code").value("EMPTY_MESSAGE"))
                .andExpect(jsonPath("$.error.recoverable").value(true))
                .andExpect(jsonPath("$.error.details.preservedInput").value(""));
    }

    @Test
    void unknownSessionReturnsUserFixableError() throws Exception {
        mockMvc.perform(post("/api/workbench/sessions/{sessionId}/messages", "missing")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"content\":\"今天喝了一支水洗埃塞\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.error.code").value("SESSION_NOT_FOUND"))
                .andExpect(jsonPath("$.error.recoverable").value(true))
                .andExpect(jsonPath("$.error.nextActions[0]").value("CREATE_SESSION"));
    }

    private String createSession() throws Exception {
        String body = mockMvc.perform(post("/api/workbench/sessions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"mode\":\"EXPLICIT_WORKFLOW\"}"))
                .andReturn()
                .getResponse()
                .getContentAsString();
        return body.replaceAll(".*\\\"sessionId\\\":\\\"([^\\\"]+)\\\".*", "$1");
    }
}
