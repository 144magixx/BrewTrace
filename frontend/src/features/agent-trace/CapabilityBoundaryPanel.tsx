import type { CapabilityBoundary } from "../../services/workbenchTypes";

export function CapabilityBoundaryPanel({ boundary }: { boundary: CapabilityBoundary }) {
  return (
    <section className="capability-boundary" aria-label="能力边界">
      <h3>能力边界</h3>
      <ul>
        <li>真实模型：{boundary.realModelConnected ? "已返回" : "已配置，等待返回"}</li>
        <li>长期数据库：{boundary.longTermMemoryConnected ? "已接入" : "未接真实长期数据库"}</li>
        <li>小红书：{boundary.xiaohongshuConnected ? "已接入" : "未执行小红书动作"}</li>
      </ul>
      <p>{boundary.message}</p>
    </section>
  );
}
