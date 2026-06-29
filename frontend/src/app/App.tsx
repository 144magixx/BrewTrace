import { CoffeeBeanLogo } from "../components/branding/CoffeeBeanLogo";
import { WorkbenchLayout } from "../components/layout/WorkbenchLayout";
import { ConversationComposer } from "../features/conversation/ConversationComposer";
import { renderConversationThread } from "../features/conversation/ConversationThread";
import { renderDraftTabs } from "../features/conversation/DraftTabs";
import { CurrentRecordPanel } from "../features/tasting-form/CurrentRecordPanel";

export function App(): string {
  const composer = ConversationComposer();
  const thread = renderConversationThread([
    { role: "ASSISTANT", content: "告诉我今天这杯咖啡的豆子、冲煮和风味，我会先追问缺失事实。" }
  ]);
  const drafts = renderDraftTabs([
    { style: "RESTRAINED", title: "克制版", body: "" },
    { style: "EXAGGERATED", title: "夸张版", body: "" },
    { style: "SHARP_REVIEW", title: "锐评版", body: "" }
  ]);
  const recordPanel = CurrentRecordPanel({
    beanFields: ["豆名", "烘焙商", "处理法"],
    brewFields: ["水温", "粉水比", "冲煮时间"],
    flavorFields: ["高温", "中温", "低温"],
    sensorySummary: "酸质/甜感/余韵 0-10",
    pendingConfirmations: ["甜橙、青柠、葡萄柚仅为待确认联想"]
  });
  return WorkbenchLayout({
    leftNav: `${CoffeeBeanLogo()} 当前记录 历史记录 风味词库 用户偏好 发布记录 设置`,
    main: `${composer.placeholder} 显式工作流 模型自主工具调用 ${thread} ${drafts}`,
    recordPanel,
    agentTrace: "Agent 轨迹卡片栏"
  });
}

export default App;
