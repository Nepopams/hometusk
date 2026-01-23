import { getFieldLabel } from '../../lib/fieldLabels';
import { FieldSuggestions } from './FieldSuggestions';

interface RequiredFieldsListProps {
  requiredFields: string[];
  suggestions?: Record<string, unknown>;
}

export function RequiredFieldsList({ requiredFields, suggestions }: RequiredFieldsListProps) {
  if (requiredFields.length === 0) return null;

  return (
    <div className="needs-input-fields">
      <span className="needs-input-fields__title">Missing information:</span>
      <ul className="needs-input-fields__list">
        {requiredFields.map((field) => {
          const suggestion = suggestions?.[field];

          return (
            <li key={field} className="needs-input-field">
              <span className="needs-input-field__label">
                {getFieldLabel(field)}{' '}
                <span className="needs-input-field__required">(required)</span>
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
