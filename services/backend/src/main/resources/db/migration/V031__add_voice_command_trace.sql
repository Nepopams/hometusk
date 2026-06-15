ALTER TABLE commands
    ADD COLUMN IF NOT EXISTS asr_trace_id VARCHAR(128);

ALTER TABLE commands DROP CONSTRAINT IF EXISTS commands_source_check;
ALTER TABLE commands
    ADD CONSTRAINT commands_source_check
    CHECK (source IN ('api', 'web', 'mobile', 'voice'));

CREATE INDEX IF NOT EXISTS idx_commands_asr_trace_id
    ON commands(asr_trace_id)
    WHERE asr_trace_id IS NOT NULL;

COMMENT ON COLUMN commands.asr_trace_id IS
    'Trace id returned by Voice ASR BFF for voice-originated command drafts';
