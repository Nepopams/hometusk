import { LANGUAGE_OPTIONS, useI18n, type LanguageCode } from '../i18n';

export default function LanguageSwitcher() {
  const { language, setLanguage, t } = useI18n();

  return (
    <div className="language-switcher" aria-label={t('language.label')}>
      {LANGUAGE_OPTIONS.map((option) => (
        <button
          key={option.code}
          type="button"
          className={`language-switcher__button ${language === option.code ? 'is-active' : ''}`}
          onClick={() => setLanguage(option.code as LanguageCode)}
          aria-pressed={language === option.code}
          title={`${t('language.switch')}: ${option.nativeName}`}
        >
          {option.label}
        </button>
      ))}
    </div>
  );
}
