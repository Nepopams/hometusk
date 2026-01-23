type StatusVariant = 'success' | 'warning' | 'error' | 'info';

interface StatusBadgeProps {
  variant: StatusVariant;
  title: string;
  icon?: string;
}

const DEFAULT_ICONS: Record<StatusVariant, string> = {
  success: 'OK',
  warning: '!',
  error: 'X',
  info: '?',
};

export function StatusBadge({ variant, title, icon }: StatusBadgeProps) {
  const resolvedIcon = icon ?? DEFAULT_ICONS[variant];

  return (
    <div className={`status-badge status-badge--${variant}`}>
      <span className="status-badge__icon">{resolvedIcon}</span>
      <span className="status-badge__title">{title}</span>
    </div>
  );
}
