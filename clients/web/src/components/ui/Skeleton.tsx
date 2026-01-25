import type { CSSProperties } from 'react';
import './Skeleton.css';

interface SkeletonProps {
  /** Width of the skeleton (CSS value) */
  width?: string | number;
  /** Height of the skeleton (CSS value) */
  height?: string | number;
  /** Variant shape */
  variant?: 'text' | 'rectangular' | 'circular';
  /** Custom class name */
  className?: string;
  /** Number of text lines to render */
  lines?: number;
  /** Accessible label */
  'aria-label'?: string;
}

/**
 * Skeleton loader component for placeholder content during loading.
 *
 * @example
 * // Text placeholder
 * <Skeleton variant="text" width="80%" />
 *
 * // Avatar placeholder
 * <Skeleton variant="circular" width={48} height={48} />
 *
 * // Card placeholder
 * <Skeleton variant="rectangular" height={120} />
 *
 * // Multiple text lines
 * <Skeleton variant="text" lines={3} />
 */
export default function Skeleton({
  width,
  height,
  variant = 'text',
  className = '',
  lines = 1,
  'aria-label': ariaLabel = 'Loading...',
}: SkeletonProps) {
  const style: CSSProperties = {
    width: typeof width === 'number' ? `${width}px` : width,
    height: typeof height === 'number' ? `${height}px` : height,
  };

  const classNames = ['skeleton', `skeleton--${variant}`, className]
    .filter(Boolean)
    .join(' ');

  if (variant === 'text' && lines > 1) {
    return (
      <div className="skeleton-lines" role="status" aria-label={ariaLabel}>
        {Array.from({ length: lines }).map((_, i) => (
          <span
            key={i}
            className={classNames}
            style={{
              ...style,
              width: i === lines - 1 ? '60%' : style.width || '100%',
            }}
          />
        ))}
      </div>
    );
  }

  return (
    <span
      className={classNames}
      style={style}
      role="status"
      aria-label={ariaLabel}
    />
  );
}
