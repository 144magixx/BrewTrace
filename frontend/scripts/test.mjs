import fs from "node:fs";
import path from "node:path";

export function runFrontendChecks() {
  const root = process.cwd();
  const app = fs.readFileSync(path.join(root, "src/app/App.tsx"), "utf8");
  const composer = fs.readFileSync(path.join(root, "src/features/conversation/ConversationComposer.tsx"), "utf8");
  assert(composer.includes("今天喝了什么咖啡？"), "首页必须展示对话创作入口");
  assert(app.includes("显式工作流") && app.includes("模型自主工具调用"), "必须展示双 Agent 模式");
  const layout = fs.readFileSync(path.join(root, "src/components/layout/WorkbenchLayout.tsx"), "utf8");
  assert(layout.includes("left-nav") && layout.includes("right-inspector") && layout.includes("agentTrace"), "必须具备左导航、中间对话和右侧当前记录/Agent 状态三栏工作台布局");
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
  const agentState = fs.readFileSync(path.join(root, "src/features/agent-trace/AgentStateCards.tsx"), "utf8");
  const modelOutput = fs.readFileSync(path.join(root, "src/features/agent-trace/ModelOutputPanel.tsx"), "utf8");
  const capabilityBoundary = fs.readFileSync(path.join(root, "src/features/agent-trace/CapabilityBoundaryPanel.tsx"), "utf8");
  assert(app.includes("<AgentStateCards state={snapshot?.agentState} />") && !app.includes("<main") && layout.includes("recordPanel") && layout.includes("agentTrace"), "AgentStateCards 必须只由右侧当前记录区域引用");
  assert(agentState.includes("当前没有可发送上下文") && agentState.includes("暂无候选记忆"), "Agent 状态卡片必须展示空上下文和空候选记忆状态");
  assert(modelOutput.includes("输入后将调用 GPT-5.5") && capabilityBoundary.includes("已配置，等待返回") && capabilityBoundary.includes("未执行小红书动作"), "前端必须包含 GPT-5.5、长期数据库和小红书边界文案");
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
