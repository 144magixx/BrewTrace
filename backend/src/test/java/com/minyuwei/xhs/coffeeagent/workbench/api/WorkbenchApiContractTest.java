package com.minyuwei.xhs.coffeeagent.workbench.api;

import com.minyuwei.xhs.coffeeagent.CoffeeAgentApplication;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.empty;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(classes = CoffeeAgentApplication.class)
@AutoConfigureMockMvc
class WorkbenchApiContractTest {
    @Autowired
    private MockMvc mockMvc;

    @Test
    void createSessionAndCompleteDraftFlow() throws Exception {
        String created = mockMvc.perform(post("/api/workbench/sessions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"mode\":\"EXPLICIT_WORKFLOW\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.sessionId").isString())
                .andExpect(jsonPath("$.data.status").value("SESSION_CREATED"))
                .andReturn()
                .getResponse()
                .getContentAsString();
        String sessionId = created.replaceAll(".*\\\"sessionId\\\":\\\"([^\\\"]+)\\\".*", "$1");

        mockMvc.perform(post("/api/workbench/sessions/{sessionId}/messages", sessionId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"content\":\"今天喝了一支水洗埃塞，有柑橘和红茶感\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("WAITING_FOR_FACTS"))
                .andExpect(jsonPath("$.data.recordSummary.pendingQuestions", not(empty())))
                .andExpect(jsonPath("$.data.draftTabs", hasSize(0)));

        mockMvc.perform(post("/api/workbench/sessions/{sessionId}/messages", sessionId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"content\":\"豆子是某烘焙商的埃塞水洗豆，水温 92 度，粉水比 1:15，想看克制、夸张和锐评。\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("DRAFTS_READY"))
                .andExpect(jsonPath("$.data.draftTabs", hasSize(3)))
                .andExpect(jsonPath("$.data.recordSummary.factBoundaryNotes[0]").value("甜橙、青柠、葡萄柚仅作为待确认联想，不写成事实。"));
    }
}
