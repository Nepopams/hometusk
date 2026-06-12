import { getFieldLabel } from '../../lib/fieldLabels';
import { useI18n } from '../../i18n';
import { FieldSuggestions } from './FieldSuggestions';

interface RequiredFieldsListProps {
  requiredFields: string[];
  suggestions?: Record<string, unknown>;
}

export function RequiredFieldsList({ requiredFields, suggestions }: RequiredFieldsListProps) {
  const { t } = useI18n();

  if (requiredFields.length === 0) return null;

  return (
    <div className="needs-input-fields">
      <span className="needs-input-fields__title">{t('commands.missingInfo')}</span>
      <ul className="needs-input-fields__list">
        {requiredFields.map((field) => {
          const suggestion = suggestions?.[field];

          return (
            <li key={field} className="needs-input-field">
              <span className="needs-input-field__label">
                {getFieldLabel(field, t)}{' '}
                <span className="needs-input-field__required">{t('commands.required')}</span>
              </span>
              {suggestion !== undefined && suggestion !== null && (
                <FieldSuggestions field={field} values={suggestion} />
              )}
            </li>
          );
        })}
      </ul>
    </div>
  );
}
