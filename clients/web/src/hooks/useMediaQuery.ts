import { useState, useEffect } from 'react';

/**
 * Breakpoint values matching tokens.css
 */
export const BREAKPOINTS = {
  /** Mobile max-width */
  mobile: 480,
  /** Tablet max-width */
  tablet: 1024,
  /** Desktop min-width */
  desktop: 1200,
} as const;

/**
 * Hook to detect if a media query matches.
 *
 * @example
 * const isMobile = useMediaQuery('(max-width: 480px)');
 *
 * @example
 * const prefersReducedMotion = useMediaQuery('(prefers-reduced-motion: reduce)');
 */
export function useMediaQuery(query: string): boolean {
  const [matches, setMatches] = useState(() => {
    if (typeof window === 'undefined') return false;
    return window.matchMedia(query).matches;
  });

  useEffect(() => {
    if (typeof window === 'undefined') return;

    const mediaQuery = window.matchMedia(query);
    setMatches(mediaQuery.matches);

    const handleChange = (e: MediaQueryListEvent) => {
      setMatches(e.matches);
    };

    // Modern browsers
    if (mediaQuery.addEventListener) {
      mediaQuery.addEventListener('change', handleChange);
      return () => mediaQuery.removeEventListener('change', handleChange);
    }
    // Legacy browsers
    mediaQuery.addListener(handleChange);
    return () => mediaQuery.removeListener(handleChange);
  }, [query]);

  return matches;
}

/**
 * Hook to detect mobile viewport (<= 480px).
 */
export function useIsMobile(): boolean {
  return useMediaQuery(`(max-width: ${BREAKPOINTS.mobile}px)`);
}

/**
 * Hook to detect tablet viewport (<= 1024px).
 */
export function useIsTablet(): boolean {
  return useMediaQuery(`(max-width: ${BREAKPOINTS.tablet}px)`);
}

/**
 * Hook to detect desktop viewport (>= 1200px).
 */
export function useIsDesktop(): boolean {
  return useMediaQuery(`(min-width: ${BREAKPOINTS.desktop}px)`);
}

/**
 * Hook to get the current breakpoint name.
 *
 * @returns 'mobile' | 'tablet' | 'desktop'
 */
export function useBreakpoint(): 'mobile' | 'tablet' | 'desktop' {
  const isMobile = useIsMobile();
  const isTablet = useIsTablet();

  if (isMobile) return 'mobile';
  if (isTablet) return 'tablet';
  return 'desktop';
}

/**
 * Hook to detect reduced motion preference.
 */
export function usePrefersReducedMotion(): boolean {
  return useMediaQuery('(prefers-reduced-motion: reduce)');
}
