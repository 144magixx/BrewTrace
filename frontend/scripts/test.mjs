import fs from "node:fs";
import path from "node:path";

export function runFrontendChecks() {
  const root = process.cwd();
  const app = fs.readFileSync(path.join(root, "src/app/App.tsx"), "utf8");
  const composer = fs.readFileSync(path.join(root, "src/features/conversation/ConversationComposer.tsx"), "utf8");
  assert(composer.includes("今天喝了什么咖啡？"), "首页必须展示对话创作入口");
  assert(app.includes("显式工作流") && app.includes("模型自主工具调用"), "必须展示双 Agent 模式");
  const layout = fs.readFileSync(path.join(root, "src/components/layout/WorkbenchLayout.tsx"), "utf8");
  assert(layout.includes("leftNav") && layout.includes("agentTrace"), "必须具备三栏工作台布局");
  const styles = fs.readFileSync(path.join(root, "src/app/styles.css"), "utf8");
  assert(styles.includes("--trace-model") && styles.includes("--trace-tool"), "必须定义轨迹类型配色");
  const flow = fs.readFileSync(path.join(root, "src/features/conversation/ConversationFlow.test.tsx"), "utf8");
  assert(flow.includes("还需要确认豆子信息") && flow.includes("未确认风味只作为联想"), "对话流程测试必须覆盖追问和事实边界");
  const form = fs.readFileSync(path.join(root, "src/features/tasting-form/TastingForm.test.tsx"), "utf8");
  const formComponent = fs.readFileSync(path.join(root, "src/features/tasting-form/TastingForm.tsx"), "utf8");
  assert(form.includes("甜橙") && form.includes("REJECTED") && formComponent.includes("0-10"), "模板表单测试必须覆盖风味候选状态和评分范围");
  const memory = fs.readFileSync(path.join(root, "src/features/memory/MemoryPanel.test.tsx"), "utf8");
  const memoryPanel = fs.readFileSync(path.join(root, "src/features/memory/MemoryRecallPanel.tsx"), "utf8");
  assert(memoryPanel.includes("可能重复") && memory.includes("CANDIDATE") && memory.includes("相同风味关键词"), "记忆面板测试必须覆盖相似原因、重复提示和偏好候选");
  const trace = fs.readFileSync(path.join(root, "src/features/agent-trace/AgentTracePanel.test.tsx"), "utf8");
  assert(trace.includes("USER_INPUT") && trace.includes("MODEL_CALL") && trace.includes("MEMORY_RECALL") && trace.includes("REVIEW") && trace.includes("promptSnapshot"), "轨迹侧边栏测试必须覆盖至少 5 类轨迹和详情快照");
  const publishing = fs.readFileSync(path.join(root, "src/features/publishing/PublishingFlow.test.tsx"), "utf8");
  const publishDialog = fs.readFileSync(path.join(root, "src/features/publishing/PublishConfirmDialog.tsx"), "utf8");
  const imagePanel = fs.readFileSync(path.join(root, "src/features/publishing/ImageGenerationPanel.tsx"), "utf8");
  const referencePanel = fs.readFileSync(path.join(root, "src/features/publishing/ExternalReferencePanel.tsx"), "utf8");
  assert(publishDialog.includes("二次确认") && imagePanel.includes("用户主动请求生图") && referencePanel.includes("来源") && publishing.includes("renderPublishingPackageReview"), "发布流程测试必须覆盖外部参考、二次确认和主动生图");
}

function assert(condition, message) {
  if (!condition) {
    throw new Error(message);
  }
}

if (import.meta.url === `file://${process.argv[1]}`) {
  runFrontendChecks();
  console.log("frontend tests passed");
}
