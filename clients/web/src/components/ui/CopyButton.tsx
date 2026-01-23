import { useState } from 'react';

interface CopyButtonProps {
  text: string;
  label?: string;
  successLabel?: string;
  className?: string;
}

export function CopyButton({
  text,
  label = 'Copy',
  successLabel = 'Copied',
  className = '',
}: CopyButtonProps) {
  const [copied, setCopied] = useState(false);

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
      {copied ? successLabel : label}
    </button>
  );
}
