import fs from "node:fs";
import path from "node:path";

const root = process.cwd();
const requiredSpecs = [
  "e2e/workbench-main-flow.spec.ts",
  "e2e/workbench-error-recovery.spec.ts",
  "src/app/App.test.tsx"
];

for (const spec of requiredSpecs) {
  const fullPath = path.join(root, spec);
  if (!fs.existsSync(fullPath)) {
    throw new Error(`missing e2e artifact: ${spec}`);
  }
}

console.log("frontend e2e local artifact check passed");
