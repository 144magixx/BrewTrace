import type { ReactNode } from "react";
import { CoffeeBeanLogo } from "../branding/CoffeeBeanLogo";

export type WorkbenchLayoutSlots = {
  main: ReactNode;
  recordPanel: ReactNode;
  agentTrace: ReactNode;
};

export function WorkbenchLayout(slots: WorkbenchLayoutSlots) {
  return (
    <div className="workbench-shell">
      <aside className="left-nav" aria-label="主导航">
        <CoffeeBeanLogo />
        <nav>
          <span className="nav-active">当前记录</span>
          <span>历史记录</span>
          <span>风味词库</span>
          <span>用户偏好</span>
          <span>发布记录</span>
          <span>设置</span>
        </nav>
      </aside>
      <main className="main-workspace">{slots.main}</main>
      <aside className="right-inspector" aria-label="当前记录与 Agent 状态">
        <div className="record-panel" aria-label="当前记录摘要">{slots.recordPanel}</div>
        <div className="agent-trace" aria-label="Agent 状态">{slots.agentTrace}</div>
      </aside>
    </div>
  );
}
