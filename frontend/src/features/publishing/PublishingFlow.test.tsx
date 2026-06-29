import { renderExternalReferencePanel } from "./ExternalReferencePanel";
import { renderPublishingPackageReview } from "./PublishingPackageReview";
import { renderPublishConfirmDialog } from "./PublishConfirmDialog";
import { renderImageGenerationPanel } from "./ImageGenerationPanel";
import { renderPublishingTraceCard } from "../agent-trace/AgentTracePanel";

export function publishingFlowFixture(): string {
  return [
    renderExternalReferencePanel(["参考1", "参考2", "参考3", "参考4", "参考5", "参考6"]),
    renderPublishingPackageReview("PACKAGE_CONFIRMED", ["外部参考已标明来源", "公开发布需要二次确认"]),
    renderPublishConfirmDialog("PREVIEW"),
    renderImageGenerationPanel(true),
    renderPublishingTraceCard("XHS_FILLED", "fillPublish 成功")
  ].join("\n");
}
