export type FlavorChip = {
  name: string;
  status: "SUGGESTED" | "ACCEPTED" | "REJECTED" | "EDITED";
};

export function renderFlavorSuggestionChips(chips: FlavorChip[]): string {
  return chips.map(chip => `${chip.name}:${chip.status}`).join("|");
}
