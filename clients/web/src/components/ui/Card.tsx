import type { HTMLAttributes, ReactNode } from 'react';
import './Card.css';

type CardPadding = 'none' | 'sm' | 'md' | 'lg';

interface CardProps extends HTMLAttributes<HTMLDivElement> {
  /** Padding size */
  padding?: CardPadding;
  /** Remove shadow */
  flat?: boolean;
  /** Content */
  children: ReactNode;
}

/**
 * Card container component with configurable padding.
 *
 * @example
 * // Default padding (lg)
 * <Card>Content</Card>
 *
 * // Medium padding (tablet)
 * <Card padding="md">Content</Card>
 *
 * // Flat (no shadow)
 * <Card flat>Content</Card>
 */
export default function Card({
  padding = 'lg',
  flat = false,
  className = '',
  children,
  ...props
}: CardProps) {
  const classNames = [
    'card-ui',
    `card-ui--padding-${padding}`,
    flat && 'card-ui--flat',
    className,
  ]
    .filter(Boolean)
    .join(' ');

  return (
    <div className={classNames} {...props}>
      {children}
    </div>
  );
}
