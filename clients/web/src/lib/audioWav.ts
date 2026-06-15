const WAV_HEADER_BYTES = 44;
const PCM_BYTES_PER_SAMPLE = 2;
const PCM_FORMAT = 1;
const MONO_CHANNELS = 1;

type WindowWithWebkitAudioContext = Window & {
  webkitAudioContext?: typeof AudioContext;
};

export function shouldNormalizeAudioForAsr(contentType: string): boolean {
  const normalizedType = contentType.split(';')[0]?.trim().toLowerCase();
  return normalizedType !== 'audio/wav' && normalizedType !== 'audio/x-wav';
}

export async function normalizeAudioForAsr(audioBlob: Blob): Promise<Blob> {
  if (!shouldNormalizeAudioForAsr(audioBlob.type)) {
    return audioBlob;
  }

  const AudioContextCtor =
    typeof window !== 'undefined'
      ? window.AudioContext || (window as WindowWithWebkitAudioContext).webkitAudioContext
      : undefined;
  if (!AudioContextCtor) {
    return audioBlob;
  }

  const audioContext = new AudioContextCtor();
  try {
    const encodedAudio = await audioBlob.arrayBuffer();
    const audioBuffer = await decodeAudioData(audioContext, encodedAudio);
    return encodePcm16MonoWav(downmixToMono(audioBuffer), audioBuffer.sampleRate);
  } finally {
    void audioContext.close().catch(() => undefined);
  }
}

export function encodePcm16MonoWav(samples: Float32Array, sampleRate: number): Blob {
  const dataSize = samples.length * PCM_BYTES_PER_SAMPLE;
  const buffer = new ArrayBuffer(WAV_HEADER_BYTES + dataSize);
  const view = new DataView(buffer);

  writeAscii(view, 0, 'RIFF');
  view.setUint32(4, 36 + dataSize, true);
  writeAscii(view, 8, 'WAVE');
  writeAscii(view, 12, 'fmt ');
  view.setUint32(16, 16, true);
  view.setUint16(20, PCM_FORMAT, true);
  view.setUint16(22, MONO_CHANNELS, true);
  view.setUint32(24, sampleRate, true);
  view.setUint32(28, sampleRate * MONO_CHANNELS * PCM_BYTES_PER_SAMPLE, true);
  view.setUint16(32, MONO_CHANNELS * PCM_BYTES_PER_SAMPLE, true);
  view.setUint16(34, 16, true);
  writeAscii(view, 36, 'data');
  view.setUint32(40, dataSize, true);

  let offset = WAV_HEADER_BYTES;
  for (const sample of samples) {
    const clamped = Math.max(-1, Math.min(1, sample));
    view.setInt16(offset, clamped < 0 ? clamped * 0x8000 : clamped * 0x7fff, true);
    offset += PCM_BYTES_PER_SAMPLE;
  }

  return new Blob([buffer], { type: 'audio/wav' });
}

function decodeAudioData(audioContext: AudioContext, encodedAudio: ArrayBuffer): Promise<AudioBuffer> {
  return new Promise((resolve, reject) => {
    audioContext.decodeAudioData(encodedAudio.slice(0), resolve, reject);
  });
}

function downmixToMono(audioBuffer: AudioBuffer): Float32Array {
  const samples = new Float32Array(audioBuffer.length);
  const channelCount = audioBuffer.numberOfChannels;

  for (let channel = 0; channel < channelCount; channel += 1) {
    const channelData = audioBuffer.getChannelData(channel);
    for (let index = 0; index < audioBuffer.length; index += 1) {
      samples[index] += channelData[index] / channelCount;
    }
  }

  return samples;
}

function writeAscii(view: DataView, offset: number, value: string): void {
  for (let index = 0; index < value.length; index += 1) {
    view.setUint8(offset + index, value.charCodeAt(index));
  }
}
