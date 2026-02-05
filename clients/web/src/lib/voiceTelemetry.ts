const STORAGE_KEY = 'hometusk:voiceTelemetry';
const MAX_EVENTS = 100;

export type VoiceEventType =
  | 'voice_start'
  | 'voice_cancel'
  | 'voice_upload_ok'
  | 'voice_upload_fail'
  | 'voice_asr_ok'
  | 'voice_asr_fail'
  | 'voice_transcript_edited'
  | 'voice_command_submitted';

export interface VoiceEvent {
  type: VoiceEventType;
  timestamp: number;
  correlationId?: string;
  durationMs?: number;
  errorType?: string;
}

export function logVoiceEvent(event: Omit<VoiceEvent, 'timestamp'>): void {
  const fullEvent: VoiceEvent = {
    ...event,
    timestamp: Date.now(),
  };

  console.log('[VoiceTelemetry]', fullEvent.type, fullEvent);

  try {
    const existing = getVoiceEvents();
    const updated = [...existing, fullEvent].slice(-MAX_EVENTS);
    localStorage.setItem(STORAGE_KEY, JSON.stringify(updated));
  } catch {
    // Ignore storage errors
  }
}

export function getVoiceEvents(): VoiceEvent[] {
  try {
    const raw = localStorage.getItem(STORAGE_KEY);
    if (!raw) return [];
    return JSON.parse(raw) as VoiceEvent[];
  } catch {
    return [];
  }
}

export function clearVoiceEvents(): void {
  try {
    localStorage.removeItem(STORAGE_KEY);
  } catch {
    // Ignore
  }
}
