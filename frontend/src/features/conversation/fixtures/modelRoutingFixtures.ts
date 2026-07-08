import type { ModelOutputSnapshot } from "../../../services/workbenchTypes";

export const conversationModelOutput: ModelOutputSnapshot = {
  outputType: "REAL_MODEL",
  messageType: "CONVERSATION",
  talk: "我还需要确认两点：这杯你喝到的主要风味是什么？是否知道产区或处理法？",
  mode: "openai-gpt55",
  modelName: "gpt-5.5",
  statusLabel: "真实模型输出 / GPT-5.5",
  content: "",
  sourceBoundary: "由真实模型生成，事实边界仍需检查。",
  post: null,
  conversation: {
    questions: ["这杯你喝到的主要风味是什么？"],
    answerOptions: [
      { id: "citrus", label: "柑橘感", content: "我喝到比较明显的柑橘感。" },
      { id: "black-tea", label: "红茶感", content: "我喝到一点红茶感。" },
      { id: "not-sure", label: "说不清", content: "我暂时说不太清楚，只觉得整体比较干净。" }
    ],
    pendingConfirmations: [],
    warnings: []
  },
  variants: [],
  requestPreview: null,
  responsePreview: null,
  recoverableError: null,
  generatedAt: "2026-06-30T00:00:00Z"
};
