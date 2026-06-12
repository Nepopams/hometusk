import {
  createContext,
  useCallback,
  useContext,
  useEffect,
  useMemo,
  useState,
  type ReactNode,
} from 'react';
import {
  LANGUAGE_OPTIONS,
  LANGUAGES,
  translations,
  type LanguageCode,
  type TranslationKey,
} from './translations';

const STORAGE_KEY = 'hometusk.language';

type TranslationParams = Record<string, string | number | boolean | null | undefined>;

interface I18nContextValue {
  language: LanguageCode;
  locale: string;
  setLanguage: (language: LanguageCode) => void;
  t: (key: TranslationKey, params?: TranslationParams) => string;
  formatDate: (value: string | Date, options?: Intl.DateTimeFormatOptions) => string;
  formatDateTime: (value: string | Date) => string;
  formatRelativeTime: (value: string | Date, compact?: boolean) => string;
}

const I18nContext = createContext<I18nContextValue | null>(null);

function isLanguageCode(value: string | null): value is LanguageCode {
  return Boolean(value && (LANGUAGES as readonly string[]).includes(value));
}

function getInitialLanguage(): LanguageCode {
  const stored = localStorage.getItem(STORAGE_KEY);
  if (isLanguageCode(stored)) return stored;

  const browserLanguage = navigator.language.toLowerCase();
  if (browserLanguage.startsWith('ru')) return 'ru';
  if (browserLanguage.startsWith('fr')) return 'fr';
  if (browserLanguage.startsWith('es')) return 'esp';
  if (browserLanguage.startsWith('zh')) return 'chn';
  return 'en';
}

function interpolate(template: string, params?: TranslationParams): string {
  if (!params) return template;

  return template.replace(/\{\{(\w+)\}\}/g, (_, rawKey: string) => {
    const value = params[rawKey];
    return value === null || value === undefined ? '' : String(value);
  });
}

export function I18nProvider({ children }: { children: ReactNode }) {
  const [language, setLanguageState] = useState<LanguageCode>(() => getInitialLanguage());

  const languageOption = LANGUAGE_OPTIONS.find((option) => option.code === language) ?? LANGUAGE_OPTIONS[0];
  const locale = languageOption.locale;

  const setLanguage = useCallback((nextLanguage: LanguageCode) => {
    setLanguageState(nextLanguage);
    localStorage.setItem(STORAGE_KEY, nextLanguage);
  }, []);

  useEffect(() => {
    document.documentElement.lang = locale;
  }, [locale]);

  const t = useCallback(
    (key: TranslationKey, params?: TranslationParams) => {
      const template = translations[language][key] ?? translations.en[key] ?? key;
      return interpolate(template, params);
    },
    [language]
  );

  const formatDate = useCallback(
    (value: string | Date, options?: Intl.DateTimeFormatOptions) => {
      const date = typeof value === 'string' ? new Date(value) : value;
      if (Number.isNaN(date.getTime())) return String(value);
      return date.toLocaleDateString(locale, options);
    },
    [locale]
  );

  const formatDateTime = useCallback(
    (value: string | Date) => {
      const date = typeof value === 'string' ? new Date(value) : value;
      if (Number.isNaN(date.getTime())) return t('common.unknown');
      return date.toLocaleDateString(locale, {
        month: 'short',
        day: 'numeric',
        year: 'numeric',
        hour: 'numeric',
        minute: '2-digit',
      });
    },
    [locale, t]
  );

  const formatRelativeTime = useCallback(
    (value: string | Date, compact = true) => {
      const date = typeof value === 'string' ? new Date(value) : value;
      const then = date.getTime();
      if (Number.isNaN(then)) return String(value);

      const diffMs = then - Date.now();
      const absMs = Math.abs(diffMs);
      const units: Array<[Intl.RelativeTimeFormatUnit, number]> = [
        ['day', 86_400_000],
        ['hour', 3_600_000],
        ['minute', 60_000],
      ];

      for (const [unit, unitMs] of units) {
        if (absMs >= unitMs || unit === 'minute') {
          const valueInUnit = Math.round(diffMs / unitMs);
          if (unit === 'minute' && valueInUnit === 0) {
            return compact ? t('tasks.justNow') : new Intl.RelativeTimeFormat(locale, { numeric: 'auto' }).format(0, 'minute');
          }
          if (compact && diffMs < 0) {
            const count = Math.abs(valueInUnit);
            if (unit === 'minute') return t('tasks.minutesAgo', { count });
            if (unit === 'hour') return t('tasks.hoursAgo', { count });
            return t('tasks.daysAgo', { count });
          }
          return new Intl.RelativeTimeFormat(locale, { numeric: 'auto' }).format(valueInUnit, unit);
        }
      }

      return t('tasks.justNow');
    },
    [locale, t]
  );

  const value = useMemo<I18nContextValue>(
    () => ({
      language,
      locale,
      setLanguage,
      t,
      formatDate,
      formatDateTime,
      formatRelativeTime,
    }),
    [formatDate, formatDateTime, formatRelativeTime, language, locale, setLanguage, t]
  );

  return <I18nContext.Provider value={value}>{children}</I18nContext.Provider>;
}

export function useI18n() {
  const context = useContext(I18nContext);
  if (!context) {
    throw new Error('useI18n must be used inside I18nProvider');
  }
  return context;
}
