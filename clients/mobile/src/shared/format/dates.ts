export function formatShortDate(value: string): string {
  const parsed = new Date(value);
  if (Number.isNaN(parsed.getTime())) {
    return value.slice(0, 10);
  }
  return parsed.toLocaleDateString(undefined, { month: 'short', day: 'numeric' });
}
