import react from "@vitejs/plugin-react";
import { defineConfig } from "vite";

export default defineConfig({
  plugins: [react()],
  server: {
    port: 5173,
    proxy: {
      "/api": process.env.VITE_API_PROXY ?? "http://127.0.0.1:8080"
    }
  },
  test: {
    environment: "jsdom",
    globals: true,
    include: [
      "src/app/App.test.tsx",
      "src/features/conversation/ConversationThread.test.tsx",
      "src/features/conversation/ConversationWorkbenchFlow.test.tsx",
      "src/features/conversation/WorkbenchErrorRecovery.test.tsx",
      "src/features/agent-trace/AgentStateCards.test.tsx",
      "src/features/agent-trace/ContextPreviewPanel.test.tsx",
      "src/features/agent-trace/ModelOutputPanel.test.tsx",
      "src/features/agent-trace/CapabilityBoundaryPanel.test.tsx",
      "src/stores/localResumeStore.test.ts"
    ],
    setupFiles: "./src/test/setup.ts"
  }
});
