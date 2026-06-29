export function renderPublishingPackageReview(status: string, risks: string[]): string {
  return `发布包:${status}:${risks.join(",")}`;
}
