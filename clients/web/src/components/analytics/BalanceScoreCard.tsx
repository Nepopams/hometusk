import { useState } from 'react';
import type { FairnessInfo } from '../../types/api';

interface BalanceScoreCardProps {
  fairness: FairnessInfo;
}

function getBalanceClass(balance: number | null): string {
  if (balance === null) return 'balance-na';
  if (balance >= 70) return 'balance-excellent';
  if (balance >= 50) return 'balance-moderate';
  return 'balance-low';
}

export function BalanceScoreCard({ fairness }: BalanceScoreCardProps) {
  const [expanded, setExpanded] = useState(false);
  const { balance, interpretation, formula } = fairness;

  return (
    <div className="card balance-card">
      <h3>Balance Score</h3>
      <div className={`balance-value ${getBalanceClass(balance)}`}>
        {balance !== null ? balance : 'N/A'}
      </div>
      <p className="balance-interpretation">{interpretation}</p>
      <button
        className="balance-toggle"
        type="button"
        onClick={() => setExpanded((prev) => !prev)}
        aria-expanded={expanded}
      >
        {expanded ? 'Hide calculation' : 'How is this calculated?'}
      </button>
      {expanded && formula && (
        <div className="balance-formula">
          <code>{formula}</code>
        </div>
      )}
    </div>
  );
}
