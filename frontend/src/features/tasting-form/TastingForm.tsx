export type TastingFormValue = {
  beanName: string;
  process: string;
  waterTemperatureCelsius: number;
  ratio: string;
  scores: Record<string, number>;
};

export function validateTastingForm(value: TastingFormValue): string[] {
  const messages: string[] = [];
  if (!value.beanName) messages.push("需要咖啡豆名称");
  if (!value.ratio) messages.push("需要粉水比");
  for (const score of Object.values(value.scores)) {
    if (score < 0 || score > 10) messages.push("感官评分必须在 0-10");
  }
  return messages;
}
