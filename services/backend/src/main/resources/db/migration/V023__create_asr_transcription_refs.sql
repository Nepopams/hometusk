-- ASR Transcription Reference for IDOR prevention (EP-011, Decision G)
CREATE TABLE asr_transcription_refs (
    transcription_id UUID PRIMARY KEY,
    household_id UUID NOT NULL REFERENCES households(id),
    created_by_user_id UUID NOT NULL REFERENCES users(id),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    expires_at TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE INDEX idx_asr_transcription_refs_household ON asr_transcription_refs(household_id);
CREATE INDEX idx_asr_transcription_refs_expires ON asr_transcription_refs(expires_at);

COMMENT ON TABLE asr_transcription_refs IS 'Maps transcription IDs to households for IDOR prevention';
