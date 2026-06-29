import fs from "node:fs";
import path from "node:path";

export function runFrontendChecks() {
  const root = process.cwd();
  const app = fs.readFileSync(path.join(root, "src/app/App.tsx"), "utf8");
  assert(app.includes("今天喝了什么咖啡？"), "首页必须展示对话创作入口");
  assert(app.includes("显式工作流") && app.includes("模型自主工具调用"), "必须展示双 Agent 模式");
  const layout = fs.readFileSync(path.join(root, "src/components/layout/WorkbenchLayout.tsx"), "utf8");
  assert(layout.includes("leftNav") && layout.includes("agentTrace"), "必须具备三栏工作台布局");
  const styles = fs.readFileSync(path.join(root, "src/app/styles.css"), "utf8");
  assert(styles.includes("--trace-model") && styles.includes("--trace-tool"), "必须定义轨迹类型配色");
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
