import { validateTastingForm } from "./TastingForm";
import { renderFlavorSuggestionChips } from "../flavor-suggestions/FlavorSuggestionChips";

export function tastingFormFixture(): string {
  const errors = validateTastingForm({
    beanName: "水洗埃塞",
    process: "水洗",
    waterTemperatureCelsius: 92,
    ratio: "1:15",
    scores: { ACIDITY: 8, SWEETNESS: 7, AFTERTASTE: 8 }
  });
  const chips = renderFlavorSuggestionChips([
    { name: "甜橙", status: "ACCEPTED" },
    { name: "葡萄柚", status: "REJECTED" }
  ]);
  return `${errors.length}:${chips}`;
}
