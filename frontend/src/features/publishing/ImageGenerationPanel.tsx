export function renderImageGenerationPanel(userInitiated: boolean): string {
  return userInitiated ? "用户主动请求生图" : "未主动请求不生图";
}
