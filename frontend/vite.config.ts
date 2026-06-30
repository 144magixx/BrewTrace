import react from "@vitejs/plugin-react";
import { defineConfig } from "vite";

export default defineConfig({
  plugins: [react()],
  server: {
    port: 5173,
    proxy: {
      "/api": "http://127.0.0.1:8080"
    }
  },
  test: {
    environment: "jsdom",
    globals: true,
    include: [
      "src/app/App.test.tsx",
      "src/features/conversation/ConversationWorkbenchFlow.test.tsx",
      "src/features/conversation/WorkbenchErrorRecovery.test.tsx"
    ],
    setupFiles: "./src/test/setup.ts"
  }
});
