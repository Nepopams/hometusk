import './BrandHeader.css';
import { useI18n } from '../../i18n';

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
export default function BrandHeader({ tagline }: BrandHeaderProps) {
  const { t } = useI18n();
  const resolvedTagline = tagline ?? t('auth.welcomeBack');

  return (
    <div className="brand-header">
      <div className="brand-header__logo">
        {/* Logo mark - terracotta square with "H" */}
        <div className="brand-header__mark" aria-hidden="true">H</div>
        <span className="brand-header__name">HomeTusk</span>
      </div>
      {resolvedTagline && <p className="brand-header__tagline">{resolvedTagline}</p>}
    </div>
  );
}
