/**
 * Token Provider — centralized token access for api.ts
 * Allows AuthContext to register token getter and auth error handler
 */

let tokenGetter: () => string | null = () => null;
let authErrorHandler: (reason?: string) => void = () => {};

export function setTokenGetter(getter: () => string | null): void {
  tokenGetter = getter;
}

export function getAuthToken(): string | null {
  return tokenGetter();
}

export function setAuthErrorHandler(handler: (reason?: string) => void): void {
  authErrorHandler = handler;
}

export function handleAuthError(reason?: string): void {
  authErrorHandler(reason);
}
