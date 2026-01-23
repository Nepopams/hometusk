import { useState } from 'react';
import { CopyButton } from '../ui/CopyButton';

interface RawJsonViewerProps {
  data: unknown;
  label?: string;
}

export function RawJsonViewer({ data, label = 'Raw Response' }: RawJsonViewerProps) {
  const [isOpen, setIsOpen] = useState(false);
  const jsonString = JSON.stringify(data, null, 2);

  return (
    <div className="raw-json-viewer">
      <div className="raw-json-viewer__header">
        <button
          type="button"
          className="raw-json-viewer__toggle"
          onClick={() => setIsOpen(!isOpen)}
        >
          {isOpen ? 'Hide' : 'Show'} {label}
        </button>
        {isOpen && <CopyButton text={jsonString} label="Copy JSON" />}
      </div>
      {isOpen && <pre className="raw-json-viewer__code">{jsonString}</pre>}
    </div>
  );
}
