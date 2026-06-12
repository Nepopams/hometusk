import { useState } from 'react';
import type { FairnessInfo } from '../../types/api';
import { useI18n } from '../../i18n';

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
  const { t } = useI18n();
  const { balance, interpretation, formula } = fairness;

  return (
    <div className="card balance-card">
      <h3>{t('analytics.balanceScore')}</h3>
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
        {expanded ? t('analytics.hideCalculation') : t('analytics.howCalculated')}
      </button>
      {expanded && formula && (
        <div className="balance-formula">
          <code>{formula}</code>
        </div>
      )}
    </div>
  );
}
