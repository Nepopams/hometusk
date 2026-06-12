import { useI18n } from '../../i18n';

interface FieldSuggestionsProps {
  field: string;
  values: unknown;
}

export function FieldSuggestions({ field, values }: FieldSuggestionsProps) {
  const { t } = useI18n();

  if (!values) return null;

  const displayValue = Array.isArray(values) ? values.join(', ') : String(values);

  if (!displayValue) return null;

  return (
    <div className="needs-input-suggestions" data-field={field}>
      <span className="needs-input-suggestions__label">{t('commands.suggestions')}</span>
      <span className="needs-input-suggestions__values">{displayValue}</span>
    </div>
  );
}
