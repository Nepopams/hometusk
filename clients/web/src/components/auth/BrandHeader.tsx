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
        {/* Logo mark - terracotta square */}
        <div className="brand-header__mark" aria-hidden="true" />
        <span className="brand-header__name">HOMETUSK</span>
      </div>
      {tagline && <p className="brand-header__tagline">{tagline}</p>}
    </div>
  );
}
