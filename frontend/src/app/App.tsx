import { CoffeeBeanLogo } from "../components/branding/CoffeeBeanLogo";
import { WorkbenchLayout } from "../components/layout/WorkbenchLayout";

export function App(): string {
  return WorkbenchLayout({
    leftNav: `${CoffeeBeanLogo()} 当前记录 历史记录 风味词库 用户偏好 发布记录 设置`,
    main: "今天喝了什么咖啡？ 显式工作流 模型自主工具调用",
    recordPanel: "当前记录紧凑面板：咖啡豆、冲煮、风味、待确认项、迷你感官雷达图",
    agentTrace: "Agent 轨迹卡片栏"
  });
}

export default App;
