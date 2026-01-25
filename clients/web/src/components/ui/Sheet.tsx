import { useEffect, useRef, type ReactNode } from 'react';
import './Sheet.css';

interface SheetProps {
  /** Whether the sheet is visible */
  open: boolean;
  /** Called when the sheet should close */
  onClose: () => void;
  /** Sheet content */
  children: ReactNode;
  /** Sheet title for accessibility */
  'aria-label'?: string;
  /** Title displayed in sheet header */
  title?: string;
  /** Whether clicking backdrop closes the sheet */
  closeOnBackdrop?: boolean;
}

/**
 * Bottom sheet component for mobile-friendly overlays.
 * Slides up from the bottom of the screen.
 *
 * Use this as an alternative to dropdowns on mobile viewports.
 *
 * @example
 * <Sheet
 *   open={isOpen}
 *   onClose={() => setIsOpen(false)}
 *   title="Select an option"
 * >
 *   <SheetItem onClick={handleOption1}>Option 1</SheetItem>
 *   <SheetItem onClick={handleOption2}>Option 2</SheetItem>
 * </Sheet>
 */
export default function Sheet({
  open,
  onClose,
  children,
  'aria-label': ariaLabel,
  title,
  closeOnBackdrop = true,
}: SheetProps) {
  const sheetRef = useRef<HTMLDivElement>(null);

  // Handle Escape key
  useEffect(() => {
    if (!open) return;

    const handleKeyDown = (e: globalThis.KeyboardEvent) => {
      if (e.key === 'Escape') {
        onClose();
      }
    };

    document.addEventListener('keydown', handleKeyDown);
    return () => document.removeEventListener('keydown', handleKeyDown);
  }, [open, onClose]);

  // Prevent body scroll
  useEffect(() => {
    if (open) {
      document.body.style.overflow = 'hidden';
    } else {
      document.body.style.overflow = '';
    }
    return () => {
      document.body.style.overflow = '';
    };
  }, [open]);

  // Focus sheet when opened
  useEffect(() => {
    if (open) {
      sheetRef.current?.focus();
    }
  }, [open]);

  if (!open) return null;

  const handleBackdropClick = () => {
    if (closeOnBackdrop) {
      onClose();
    }
  };

  return (
    <div
      className="sheet__backdrop"
      onClick={handleBackdropClick}
      role="presentation"
    >
      <div
        ref={sheetRef}
        className="sheet__panel"
        onClick={(e) => e.stopPropagation()}
        role="dialog"
        aria-modal="true"
        aria-label={ariaLabel || title}
        tabIndex={-1}
      >
        <div className="sheet__handle" aria-hidden="true" />
        {title && (
          <div className="sheet__header">
            <h2 className="sheet__title">{title}</h2>
          </div>
        )}
        <div className="sheet__content">{children}</div>
      </div>
    </div>
  );
}

interface SheetItemProps {
  /** Click handler */
  onClick?: () => void;
  /** Item content */
  children: ReactNode;
  /** Whether item is currently selected */
  selected?: boolean;
  /** Whether item is destructive action */
  destructive?: boolean;
  /** Disable the item */
  disabled?: boolean;
}

/**
 * Item component for use inside Sheet.
 */
export function SheetItem({
  onClick,
  children,
  selected = false,
  destructive = false,
  disabled = false,
}: SheetItemProps) {
  const classNames = [
    'sheet__item',
    selected && 'sheet__item--selected',
    destructive && 'sheet__item--destructive',
    disabled && 'sheet__item--disabled',
  ]
    .filter(Boolean)
    .join(' ');

  return (
    <button
      type="button"
      className={classNames}
      onClick={onClick}
      disabled={disabled}
    >
      {children}
      {selected && (
        <svg
          className="sheet__item-check"
          width="20"
          height="20"
          viewBox="0 0 24 24"
          fill="none"
          stroke="currentColor"
          strokeWidth="2"
          strokeLinecap="round"
        >
          <polyline points="20 6 9 17 4 12" />
        </svg>
      )}
    </button>
  );
}
