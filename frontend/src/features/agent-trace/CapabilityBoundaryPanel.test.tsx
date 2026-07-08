import { render, screen } from "@testing-library/react";
import { describe, expect, it } from "vitest";
import { CapabilityBoundaryPanel } from "./CapabilityBoundaryPanel";

describe("CapabilityBoundaryPanel", () => {
  it("展示真实模型、长期数据库和小红书边界", () => {
    render(<CapabilityBoundaryPanel boundary={{
      realModelConnected: false,
      longTermMemoryConnected: false,
      xiaohongshuConnected: false,
      message: "已配置真实文本模型；等待输入或模型返回，未执行小红书动作，未接真实长期记忆数据库。"
    }} />);

    expect(screen.getByText("真实模型：已配置，等待返回")).toBeInTheDocument();
    expect(screen.getByText("长期数据库：未接真实长期数据库")).toBeInTheDocument();
    expect(screen.getByText("小红书：未执行小红书动作")).toBeInTheDocument();
  });
});
