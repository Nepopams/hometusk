import {
  useState,
  useRef,
  useEffect,
  type ReactNode,
  type KeyboardEvent,
} from 'react';
import './Menu.css';

interface MenuProps {
  /** Trigger element (e.g., button) */
  trigger: ReactNode;
  /** Menu items */
  children: ReactNode;
  /** Alignment of menu relative to trigger */
  align?: 'start' | 'end';
  /** Accessible label */
  'aria-label'?: string;
}

/**
 * Dropdown menu component with keyboard navigation.
 *
 * @example
 * <Menu
 *   trigger={<Button variant="ghost">Options</Button>}
 *   aria-label="User options"
 * >
 *   <MenuItem onClick={handleProfile}>Profile</MenuItem>
 *   <MenuDivider />
 *   <MenuItem onClick={handleLogout} destructive>Sign out</MenuItem>
 * </Menu>
 */
export default function Menu({
  trigger,
  children,
  align = 'end',
  'aria-label': ariaLabel,
}: MenuProps) {
  const [open, setOpen] = useState(false);
  const containerRef = useRef<HTMLDivElement>(null);
  const menuRef = useRef<HTMLDivElement>(null);
  const triggerRef = useRef<HTMLButtonElement>(null);

  // Close on outside click
  useEffect(() => {
    if (!open) return;

    const handleClickOutside = (e: MouseEvent) => {
      if (containerRef.current && !containerRef.current.contains(e.target as Node)) {
        setOpen(false);
      }
    };

    document.addEventListener('mousedown', handleClickOutside);
    return () => document.removeEventListener('mousedown', handleClickOutside);
  }, [open]);

  // Close on Escape
  useEffect(() => {
    if (!open) return;

    const handleKeyDown = (e: globalThis.KeyboardEvent) => {
      if (e.key === 'Escape') {
        setOpen(false);
        triggerRef.current?.focus();
      }
    };

    document.addEventListener('keydown', handleKeyDown);
    return () => document.removeEventListener('keydown', handleKeyDown);
  }, [open]);

  // Focus first item when opened
  useEffect(() => {
    if (open) {
      const firstItem = menuRef.current?.querySelector('[role="menuitem"]:not([disabled])');
      (firstItem as HTMLElement)?.focus();
    }
  }, [open]);

  const handleTriggerKeyDown = (e: KeyboardEvent) => {
    if (e.key === 'ArrowDown' || e.key === 'Enter' || e.key === ' ') {
      e.preventDefault();
      setOpen(true);
    }
  };

  const handleMenuKeyDown = (e: KeyboardEvent) => {
    const items = menuRef.current?.querySelectorAll('[role="menuitem"]:not([disabled])');
    if (!items?.length) return;

    const currentIndex = Array.from(items).findIndex(
      (item) => item === document.activeElement
    );

    switch (e.key) {
      case 'ArrowDown': {
        e.preventDefault();
        const nextIndex = currentIndex < items.length - 1 ? currentIndex + 1 : 0;
        (items[nextIndex] as HTMLElement).focus();
        break;
      }
      case 'ArrowUp': {
        e.preventDefault();
        const prevIndex = currentIndex > 0 ? currentIndex - 1 : items.length - 1;
        (items[prevIndex] as HTMLElement).focus();
        break;
      }
      case 'Home':
        e.preventDefault();
        (items[0] as HTMLElement).focus();
        break;
      case 'End':
        e.preventDefault();
        (items[items.length - 1] as HTMLElement).focus();
        break;
      case 'Tab':
        setOpen(false);
        break;
    }
  };

  const handleItemClick = () => {
    setOpen(false);
    triggerRef.current?.focus();
  };

  return (
    <div ref={containerRef} className="menu">
      <button
        ref={triggerRef}
        type="button"
        className="menu__trigger"
        onClick={() => setOpen(!open)}
        onKeyDown={handleTriggerKeyDown}
        aria-haspopup="menu"
        aria-expanded={open}
      >
        {trigger}
      </button>
      {open && (
        <div
          ref={menuRef}
          className={`menu__dropdown menu__dropdown--${align}`}
          role="menu"
          aria-label={ariaLabel}
          onKeyDown={handleMenuKeyDown}
          onClick={handleItemClick}
        >
          {children}
        </div>
      )}
    </div>
  );
}

interface MenuItemProps {
  /** Click handler */
  onClick?: () => void;
  /** Item content */
  children: ReactNode;
  /** Whether item is destructive action */
  destructive?: boolean;
  /** Disable the item */
  disabled?: boolean;
  /** Icon to show before label */
  icon?: ReactNode;
}

/**
 * Menu item component.
 */
export function MenuItem({
  onClick,
  children,
  destructive = false,
  disabled = false,
  icon,
}: MenuItemProps) {
  const classNames = [
    'menu__item',
    destructive && 'menu__item--destructive',
    disabled && 'menu__item--disabled',
  ]
    .filter(Boolean)
    .join(' ');

  const handleClick = () => {
    if (!disabled && onClick) {
      onClick();
    }
  };

  const handleKeyDown = (e: KeyboardEvent) => {
    if ((e.key === 'Enter' || e.key === ' ') && !disabled) {
      e.preventDefault();
      onClick?.();
    }
  };

  return (
    <div
      className={classNames}
      role="menuitem"
      tabIndex={disabled ? -1 : 0}
      onClick={handleClick}
      onKeyDown={handleKeyDown}
      aria-disabled={disabled}
    >
      {icon && <span className="menu__item-icon">{icon}</span>}
      <span className="menu__item-label">{children}</span>
    </div>
  );
}

/**
 * Divider for separating menu sections.
 */
export function MenuDivider() {
  return <div className="menu__divider" role="separator" />;
}
