export function renderPublishConfirmDialog(stage: "PACKAGE" | "PREVIEW"): string {
  return stage === "PACKAGE" ? "确认发布包并填写发布页" : "发布页预览后二次确认公开发布";
}
