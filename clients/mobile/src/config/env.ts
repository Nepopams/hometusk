declare const process: {
  env?: Record<string, string | undefined>;
};

const DEFAULT_API_BASE_URL = 'http://localhost:8080/api/v1';

function trimTrailingSlash(value: string): string {
  return value.endsWith('/') ? value.slice(0, -1) : value;
}

export const appConfig = {
  apiBaseUrl: trimTrailingSlash(
    process.env?.EXPO_PUBLIC_API_BASE_URL?.trim() || DEFAULT_API_BASE_URL
  ),
};
