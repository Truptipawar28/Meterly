const money = new Intl.NumberFormat('en-US', { style: 'currency', currency: 'USD' });
const precise = new Intl.NumberFormat('en-US', { style: 'currency', currency: 'USD', maximumFractionDigits: 6 });
const count = new Intl.NumberFormat('en-US');

export const formatMoney = (value: number) => money.format(value);
/** Small line amounts (e.g. $0.0042) need more decimals than cents. */
export const formatPrecise = (value: number) => precise.format(value);
export const formatCount = (value: number) => count.format(value);

export const formatDate = (iso: string) =>
  new Date(iso.length === 10 ? `${iso}T00:00:00Z` : iso).toLocaleDateString('en-US', {
    year: 'numeric',
    month: 'short',
    day: 'numeric',
    timeZone: 'UTC',
  });

export const metricLabel: Record<string, string> = {
  API_CALLS: 'API calls',
  AI_TOKENS: 'AI tokens',
  STORAGE: 'Storage (GB-h)',
  EGRESS: 'Egress (MB)',
};
