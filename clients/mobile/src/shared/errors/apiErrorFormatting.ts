import { HomeTuskApiError } from '../../api/client';

export function formatAuthError(error: unknown): string {
  if (error instanceof HomeTuskApiError) {
    if (error.body.errorCode === 'AUTH_INVALID_CREDENTIALS') {
      return 'Email or password is incorrect.';
    }
    if (error.body.errorCode === 'AUTH_EMAIL_EXISTS') {
      return 'An account with this email already exists.';
    }
    if (error.body.errorCode === 'AUTH_PROVIDER_UNAVAILABLE') {
      return 'Authentication provider is unavailable. Try again later.';
    }
    if (error.body.message) {
      return error.body.message;
    }
    return `HomeTusk API returned ${error.status}.`;
  }
  return 'Could not open the session. Check the backend URL and try again.';
}

export function formatReadError(error: unknown): string {
  if (error instanceof HomeTuskApiError) {
    if (error.status === 401) {
      return 'Session needs a refresh. Logout and sign in again if retry fails.';
    }
    if (error.status === 403) {
      return 'This account cannot open the selected household.';
    }
    if (error.body.message) {
      return error.body.message;
    }
    return `HomeTusk API returned ${error.status}.`;
  }
  return 'Could not load household data. Check the backend URL and try again.';
}

export function formatMutationError(error: unknown): string {
  if (error instanceof HomeTuskApiError) {
    if (error.status === 403 || error.status === 404) {
      return 'Could not save inside the selected household.';
    }
    if (error.body.message) {
      return error.body.message;
    }
    return `HomeTusk API returned ${error.status}.`;
  }
  return 'Could not save the change. Check the backend URL and try again.';
}

export function formatConfirmationError(error: unknown): string {
  if (error instanceof HomeTuskApiError) {
    if (error.status === 403) {
      return 'Only the original command initiator can approve or cancel this confirmation.';
    }
    if (error.status === 404) {
      return 'This confirmation is no longer available.';
    }
    if (error.status === 409) {
      return 'This confirmation is already terminal, stale, or expired. Refresh command state and try again.';
    }
    if (error.body.message) {
      return error.body.message;
    }
    return `Confirmation action failed with API ${error.status}.`;
  }
  return 'Could not update this confirmation. Check the backend URL and try again.';
}

export function formatVoiceTranscriptionError(error: unknown): string {
  if (error instanceof HomeTuskApiError) {
    const code = error.body.code ?? error.body.errorCode;
    if (error.status === 401) {
      return 'Войди снова перед голосовым вводом.';
    }
    if (error.status === 400 || code === 'invalid_multipart' || code === 'missing_audio_file') {
      return 'Запись не удалось отправить. Попробуй еще раз или введи команду текстом.';
    }
    if (error.status === 413 || code === 'file_too_large') {
      return 'Запись слишком длинная. Скажи команду короче или введи ее текстом.';
    }
    if (error.status === 415 || code === 'unsupported_media') {
      return 'Формат записи не поддержан. Попробуй еще раз или введи команду текстом.';
    }
    if (error.status === 429 || code === 'local_rate_limit') {
      return 'Голосовой ввод временно ограничен. Введи команду текстом или попробуй позже.';
    }
    if (error.status === 504 || code === 'timeout') {
      return 'Голосовой ввод занял слишком много времени. Попробуй еще раз или введи команду текстом.';
    }
    if (error.status === 500 || error.status === 502) {
      return 'Голосовой ввод сейчас недоступен. Введи команду текстом или попробуй еще раз.';
    }
    if (error.body.message) {
      return error.body.message;
    }
    return `Голосовой ввод завершился ошибкой API ${error.status}.`;
  }
  return 'Голосовой ввод не завершился. Проверь соединение и попробуй еще раз.';
}

export function formatPushRegistrationError(error: unknown): string {
  if (error instanceof HomeTuskApiError) {
    if (error.status === 401) {
      return 'Push registration needs a fresh sign-in.';
    }
    if (error.status === 409) {
      return 'This push token is already active for another device registration.';
    }
    if (error.body.message) {
      return error.body.message;
    }
    return `Push registration failed with API ${error.status}.`;
  }
  return 'Could not register this device for push.';
}

export function formatLinkError(error: unknown): string {
  if (error instanceof HomeTuskApiError) {
    if (error.status === 401) {
      return 'Sign in again to open this HomeTusk link.';
    }
    if (error.status === 403 || error.status === 404) {
      return 'This link target is not available for this account.';
    }
    if (error.status === 410) {
      return 'This invite is no longer available.';
    }
    if (error.body.message) {
      return error.body.message;
    }
    return `HomeTusk link failed with API ${error.status}.`;
  }
  return 'Could not open this HomeTusk link.';
}
