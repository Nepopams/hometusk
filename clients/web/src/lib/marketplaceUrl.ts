const MAX_QUERY_LENGTH = 200;
const QUERY_PLACEHOLDER = '{query}';

/**
 * Build a marketplace search URL by substituting the item name
 * into the URL template with proper encoding.
 *
 * Per ADR-015: Client is responsible for URL encoding before substitution.
 *
 * @param template - URL template with {query} placeholder
 * @param itemName - Item name to search for
 * @returns Fully constructed URL with encoded query
 * @throws Error if template doesn't contain {query} placeholder
 */
export function buildMarketplaceUrl(template: string, itemName: string): string {
  if (!template.includes(QUERY_PLACEHOLDER)) {
    throw new Error('Template must contain {query} placeholder');
  }

  if (!itemName || itemName.trim() === '') {
    return template.replace(QUERY_PLACEHOLDER, '');
  }

  const truncated = itemName.slice(0, MAX_QUERY_LENGTH);
  const encoded = encodeRFC3986(truncated);

  return template.replace(QUERY_PLACEHOLDER, encoded);
}

function encodeRFC3986(value: string): string {
  return encodeURIComponent(value).replace(/[!'()*]/g, (char) =>
    `%${char.charCodeAt(0).toString(16).toUpperCase()}`
  );
}

export const MARKETPLACE_URL_CONFIG = {
  maxQueryLength: MAX_QUERY_LENGTH,
  placeholder: QUERY_PLACEHOLDER,
} as const;
