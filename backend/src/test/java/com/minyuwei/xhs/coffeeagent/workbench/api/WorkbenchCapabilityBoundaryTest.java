package com.minyuwei.xhs.coffeeagent.workbench.api;

import com.minyuwei.xhs.coffeeagent.CoffeeAgentApplication;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(classes = CoffeeAgentApplication.class)
@AutoConfigureMockMvc
class WorkbenchCapabilityBoundaryTest {
    @Autowired
    private MockMvc mockMvc;

    @Test
    void emptySnapshotDeclaresUnavailableExternalCapabilities() throws Exception {
        mockMvc.perform(get("/api/workbench/snapshot"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.agentState.capabilityBoundary.realModelConnected").value(false))
                .andExpect(jsonPath("$.data.agentState.capabilityBoundary.longTermMemoryConnected").value(false))
                .andExpect(jsonPath("$.data.agentState.capabilityBoundary.xiaohongshuConnected").value(false));
    }
}
