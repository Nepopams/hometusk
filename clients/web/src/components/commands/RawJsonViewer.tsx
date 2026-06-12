import { useState } from 'react';
import { useI18n } from '../../i18n';
import { CopyButton } from '../ui/CopyButton';

interface RawJsonViewerProps {
  data: unknown;
  label?: string;
}

export function RawJsonViewer({ data, label = 'Raw Response' }: RawJsonViewerProps) {
  const [isOpen, setIsOpen] = useState(false);
  const { t } = useI18n();
  const resolvedLabel = label === 'Raw Response' ? t('commands.rawResponse') : label;
  const jsonString = JSON.stringify(data, null, 2);

  return (
    <div className="raw-json-viewer">
      <div className="raw-json-viewer__header">
        <button
          type="button"
          className="raw-json-viewer__toggle"
          onClick={() => setIsOpen(!isOpen)}
        >
          {isOpen ? t('common.hide') : t('common.show')} {resolvedLabel}
        </button>
        {isOpen && <CopyButton text={jsonString} label={t('common.copyJson')} />}
      </div>
      {isOpen && <pre className="raw-json-viewer__code">{jsonString}</pre>}
    </div>
  );
}
