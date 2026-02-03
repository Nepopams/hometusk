/**
 * Custom hooks for HomeTusk
 */

export {
  useMediaQuery,
  useIsMobile,
  useIsTablet,
  useIsDesktop,
  useBreakpoint,
  usePrefersReducedMotion,
  BREAKPOINTS,
} from './useMediaQuery';

export { useGamification } from './useGamification';
export { useAudioRecorder } from './useAudioRecorder';
export type { UseAudioRecorderResult, AudioRecorderError } from './useAudioRecorder';
