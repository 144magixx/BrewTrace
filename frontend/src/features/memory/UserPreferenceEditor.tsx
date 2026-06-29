export type PreferenceCandidate = {
  value: string;
  evidence: string;
  status: "CANDIDATE" | "ACCEPTED" | "DELETED";
};

export function renderUserPreferenceEditor(preferences: PreferenceCandidate[]): string {
  return preferences.map(preference => `${preference.value}:${preference.status}:${preference.evidence}`).join("\n");
}
