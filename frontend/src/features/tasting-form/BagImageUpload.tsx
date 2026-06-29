export type BagImageUploadState = {
  filePath: string;
  confirmationStatus: "PENDING_CONFIRMATION" | "CONFIRMED";
};

export function BagImageUpload(filePath: string): BagImageUploadState {
  return {
    filePath,
    confirmationStatus: "PENDING_CONFIRMATION"
  };
}
