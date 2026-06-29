export type WorkbenchLayoutSlots = {
  leftNav: string;
  main: string;
  recordPanel: string;
  agentTrace: string;
};

export function WorkbenchLayout(slots: WorkbenchLayoutSlots): string {
  return [
    `<aside class="left-nav">${slots.leftNav}</aside>`,
    `<main class="main-workspace">${slots.main}<section class="record-panel">${slots.recordPanel}</section></main>`,
    `<aside class="agent-trace">${slots.agentTrace}</aside>`
  ].join("");
}
