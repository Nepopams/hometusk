import type { AuthErrorCode } from '../types/api';

export class AuthError extends Error {
  constructor(message: string, public code?: AuthErrorCode) {
    super(message);
    this.name = 'AuthError';
  }
}

export class ApiError extends Error {
  constructor(public status: number, public body: unknown, message?: string) {
    super(message || `API Error: ${status}`);
    this.name = 'ApiError';
  }
}
