import { useState } from 'react';

interface TraceInfoProps {
  commandId: string;
  correlationId: string;
  executionMs: number;
}

export function TraceInfo({ commandId, correlationId, executionMs }: TraceInfoProps) {
  const [copied, setCopied] = useState(false);
  const [copyError, setCopyError] = useState(false);

  const handleCopy = async () => {
    if (copied) return;

    try {
      await navigator.clipboard.writeText(correlationId);
      setCopied(true);
      setCopyError(false);
      setTimeout(() => setCopied(false), 2000);
    } catch {
      setCopyError(true);
    }
  };

  const formatDuration = (ms: number) => {
    if (ms < 1000) return `${ms}ms`;
    return `${(ms / 1000).toFixed(2)}s`;
  };

  return (
    <div className="trace-info">
      <div className="trace-info__item">
        <span className="trace-info__label">Command:</span>
        <code className="trace-info__value">{commandId}</code>
      </div>
      <div className="trace-info__item">
        <span className="trace-info__label">Trace:</span>
        <code className="trace-info__value">{correlationId}</code>
        <button type="button" className="trace-info__copy" onClick={handleCopy}>
          {copied ? 'Copied' : 'Copy'}
        </button>
        {copyError && <span className="trace-info__error">Copy failed</span>}
      </div>
      <div className="trace-info__item">
        <span className="trace-info__label">Time:</span>
        <span className="trace-info__value">{formatDuration(executionMs)}</span>
      </div>
    </div>
  );
}
