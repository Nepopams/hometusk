import type { AnchorHTMLAttributes, ButtonHTMLAttributes, ReactNode } from 'react';
import { Link, type LinkProps } from 'react-router-dom';
import './TextLink.css';

type TextLinkVariant = 'default' | 'muted';

interface TextLinkBaseProps {
  /** Visual style */
  variant?: TextLinkVariant;
  /** Whether the link is centered */
  centered?: boolean;
  children: ReactNode;
}

type TextLinkAsAnchor = TextLinkBaseProps &
  Omit<AnchorHTMLAttributes<HTMLAnchorElement>, 'children'> & {
    href: string;
    to?: never;
    onClick?: never;
  };

type TextLinkAsRouterLink = TextLinkBaseProps &
  Omit<LinkProps, 'children'> & {
    to: string;
    href?: never;
    onClick?: never;
  };

type TextLinkAsButton = TextLinkBaseProps &
  Omit<ButtonHTMLAttributes<HTMLButtonElement>, 'children'> & {
    onClick: () => void;
    to?: never;
    href?: never;
  };

type TextLinkProps = TextLinkAsAnchor | TextLinkAsRouterLink | TextLinkAsButton;

/**
 * Styled text link for inline links in forms.
 *
 * @example
 * // As React Router Link
 * <TextLink to="/register">Create account</TextLink>
 *
 * // As anchor
 * <TextLink href="/forgot-password">Forgot password?</TextLink>
 *
 * // Muted variant
 * <TextLink to="/register" variant="muted">Sign up</TextLink>
 *
 * // Centered
 * <TextLink to="/register" centered>Create account</TextLink>
 */
export default function TextLink({
  variant = 'default',
  centered = false,
  children,
  className = '',
  ...props
}: TextLinkProps) {
  const classNames = [
    'text-link',
    `text-link--${variant}`,
    centered && 'text-link--centered',
    className,
  ]
    .filter(Boolean)
    .join(' ');

  // Router Link
  if ('to' in props && props.to) {
    const { to, ...rest } = props;
    return (
      <Link to={to} className={classNames} {...rest}>
        {children}
      </Link>
    );
  }

  // Button (onClick without navigation)
  if ('onClick' in props && props.onClick && !('href' in props)) {
    const { onClick, type = 'button', ...rest } = props as TextLinkAsButton;
    return (
      <button type={type} onClick={onClick} className={classNames} {...rest}>
        {children}
      </button>
    );
  }

  // Anchor link
  const { href, ...rest } = props as TextLinkAsAnchor;
  return (
    <a href={href} className={classNames} {...rest}>
      {children}
    </a>
  );
}
