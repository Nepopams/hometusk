import type { CSSProperties, ReactNode } from 'react';
import { useEffect, useRef, useState } from 'react';
import { useMediaQuery } from '../../hooks/useMediaQuery';
import { useI18n } from '../../i18n';
import Header from './Header';
import Sidebar from './Sidebar';

type LayoutProps = {
  children: ReactNode;
};

export default function Layout({ children }: LayoutProps) {
  const { t } = useI18n();
  const isCompactNavigation = useMediaQuery('(max-width: 900px)');
  const [isMobileNavOpen, setIsMobileNavOpen] = useState(false);
  const [headerHeight, setHeaderHeight] = useState(0);
  const headerRef = useRef<HTMLElement | null>(null);

  const closeMobileNavigation = () => setIsMobileNavOpen(false);
  const toggleMobileNavigation = () => setIsMobileNavOpen((isOpen) => !isOpen);

  useEffect(() => {
    if (!isCompactNavigation) {
      setIsMobileNavOpen(false);
    }
  }, [isCompactNavigation]);

  useEffect(() => {
    if (!isMobileNavOpen) return;

    const handleKeyDown = (event: KeyboardEvent) => {
      if (event.key === 'Escape') {
        setIsMobileNavOpen(false);
      }
    };

    window.addEventListener('keydown', handleKeyDown);
    return () => window.removeEventListener('keydown', handleKeyDown);
  }, [isMobileNavOpen]);

  useEffect(() => {
    const header = headerRef.current;
    if (!header) return;

    const updateHeaderHeight = () => {
      setHeaderHeight(header.getBoundingClientRect().height);
    };

    updateHeaderHeight();
    window.addEventListener('resize', updateHeaderHeight);

    const observer =
      typeof ResizeObserver === 'undefined' ? null : new ResizeObserver(updateHeaderHeight);
    observer?.observe(header);

    return () => {
      window.removeEventListener('resize', updateHeaderHeight);
      observer?.disconnect();
    };
  }, []);

  return (
    <div
      className="app-shell"
      style={{ '--app-header-height': `${headerHeight}px` } as CSSProperties}
    >
      <Header
        ref={headerRef}
        isNavigationOpen={isMobileNavOpen}
        onNavigationToggle={toggleMobileNavigation}
      />
      <div className={`app-shell__body${isMobileNavOpen ? ' app-shell__body--nav-open' : ''}`}>
        {isMobileNavOpen && (
          <button
            type="button"
            className="app-sidebar-backdrop"
            aria-label={`${t('common.close')} ${t('nav.navigation')}`}
            onClick={closeMobileNavigation}
          />
        )}
        <Sidebar
          isCompactHidden={isCompactNavigation && !isMobileNavOpen}
          onNavigate={closeMobileNavigation}
        />
        <main className="app-content">{children}</main>
      </div>
    </div>
  );
}
