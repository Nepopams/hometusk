import { describe, expect, it } from 'vitest';
import { mapAsrErrorType } from './useAsrTranscription';

describe('mapAsrErrorType', () => {
  it('maps controlled voice ASR statuses to recovery states', () => {
    expect(mapAsrErrorType(415, 'unsupported_media')).toBe('unsupported_media');
    expect(mapAsrErrorType(429, 'local_rate_limit')).toBe('rate_limited');
    expect(mapAsrErrorType(504, 'timeout')).toBe('timeout');
    expect(mapAsrErrorType(502, 'upstream_unavailable')).toBe('transcription_failed');
    expect(mapAsrErrorType(401)).toBe('not_authenticated');
  });
});
