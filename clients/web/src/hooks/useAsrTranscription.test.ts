import { describe, expect, it } from 'vitest';
import { getAudioFileName, getVoiceTranscriptionUrl, mapAsrErrorType } from './useAsrTranscription';

describe('mapAsrErrorType', () => {
  it('maps controlled voice ASR statuses to recovery states', () => {
    expect(mapAsrErrorType(415, 'unsupported_media')).toBe('unsupported_media');
    expect(mapAsrErrorType(429, 'local_rate_limit')).toBe('rate_limited');
    expect(mapAsrErrorType(504, 'timeout')).toBe('timeout');
    expect(mapAsrErrorType(502, 'upstream_unavailable')).toBe('transcription_failed');
    expect(mapAsrErrorType(401)).toBe('not_authenticated');
  });
});

describe('getVoiceTranscriptionUrl', () => {
  it('uses the configured API base without duplicating /api/v1', () => {
    expect(getVoiceTranscriptionUrl('/api/v1')).toBe('/api/v1/voice/transcriptions');
    expect(getVoiceTranscriptionUrl('https://example.test/api/v1/')).toBe(
      'https://example.test/api/v1/voice/transcriptions'
    );
  });
});

describe('getAudioFileName', () => {
  it('matches common browser recording MIME types', () => {
    expect(getAudioFileName('audio/webm;codecs=opus')).toBe('recording.webm');
    expect(getAudioFileName('audio/mp4')).toBe('recording.m4a');
    expect(getAudioFileName('audio/ogg')).toBe('recording.ogg');
  });
});
