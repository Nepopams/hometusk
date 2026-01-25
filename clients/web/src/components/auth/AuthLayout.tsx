import type { ReactNode } from 'react';
import './AuthLayout.css';

interface AuthLayoutProps {
  /** Page content (usually a form card) */
  children: ReactNode;
  /** Optional footer content below the card */
  footer?: ReactNode;
}

/**
 * Layout wrapper for authentication pages (Login, Register, etc.)
 * Centers content vertically and horizontally with responsive behavior.
 *
 * Desktop (1200px+): Card with shadow, max-width 420px
 * Tablet (1024px): Card with shadow, max-width 400px
 * Mobile (390px): Full-width, no card background
 *
 * @example
 * <AuthLayout>
 *   <Card>
 *     <BrandHeader />
 *     <form>...</form>
 *   </Card>
 * </AuthLayout>
 */
export default function AuthLayout({ children, footer }: AuthLayoutProps) {
  return (
    <div className="auth-layout">
      <div className="auth-layout__content">{children}</div>
      {footer && <div className="auth-layout__footer">{footer}</div>}
    </div>
  );
}
