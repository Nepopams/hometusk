import { useState } from 'react';
import { useI18n } from '../../i18n';

interface CopyButtonProps {
  text: string;
  label?: string;
  successLabel?: string;
  className?: string;
}

export function CopyButton({
  text,
  label,
  successLabel,
  className = '',
}: CopyButtonProps) {
  const [copied, setCopied] = useState(false);
  const { t } = useI18n();
  const resolvedLabel = label ?? t('common.copy');
  const resolvedSuccessLabel = successLabel ?? t('common.copied');

  const handleCopy = async () => {
    if (copied) return;

    try {
      await navigator.clipboard.writeText(text);
      setCopied(true);
      setTimeout(() => setCopied(false), 2000);
    } catch {
      // Ignore copy failures.
    }
  };

  return (
    <button
      type="button"
      className={`copy-button ${className}`.trim()}
      onClick={handleCopy}
    >
      {copied ? resolvedSuccessLabel : resolvedLabel}
    </button>
  );
}
