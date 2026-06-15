import { describe, expect, it } from 'vitest';
import { encodePcm16MonoWav, shouldNormalizeAudioForAsr } from './audioWav';

describe('shouldNormalizeAudioForAsr', () => {
  it('keeps wav audio and normalizes recorder containers', () => {
    expect(shouldNormalizeAudioForAsr('audio/wav')).toBe(false);
    expect(shouldNormalizeAudioForAsr('audio/x-wav')).toBe(false);
    expect(shouldNormalizeAudioForAsr('audio/webm;codecs=opus')).toBe(true);
    expect(shouldNormalizeAudioForAsr('audio/mp4')).toBe(true);
  });
});

describe('encodePcm16MonoWav', () => {
  it('writes a mono PCM WAV header', async () => {
    const blob = encodePcm16MonoWav(new Float32Array([0, 0.5, -0.5]), 16_000);
    const buffer = await blob.arrayBuffer();
    const view = new DataView(buffer);
    const text = new TextDecoder('ascii').decode(buffer);

    expect(blob.type).toBe('audio/wav');
    expect(buffer.byteLength).toBe(44 + 3 * 2);
    expect(text.slice(0, 4)).toBe('RIFF');
    expect(text.slice(8, 12)).toBe('WAVE');
    expect(text.slice(36, 40)).toBe('data');
    expect(view.getUint16(22, true)).toBe(1);
    expect(view.getUint32(24, true)).toBe(16_000);
    expect(view.getUint16(34, true)).toBe(16);
  });
});
