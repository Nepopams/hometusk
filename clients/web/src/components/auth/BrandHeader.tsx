import './BrandHeader.css';

interface BrandHeaderProps {
  /** Tagline text below brand name */
  tagline?: string;
}

/**
 * Brand header for auth pages with logo and tagline.
 *
 * @example
 * <BrandHeader tagline="Welcome back" />
 * <BrandHeader tagline="Create your account" />
 */
export default function BrandHeader({ tagline = 'Welcome back' }: BrandHeaderProps) {
  return (
    <div className="brand-header">
      <div className="brand-header__logo">
        {/* Logo mark - terracotta square with "H" */}
        <div className="brand-header__mark" aria-hidden="true">H</div>
        <span className="brand-header__name">HomeTusk</span>
      </div>
      {tagline && <p className="brand-header__tagline">{tagline}</p>}
    </div>
  );
}
