-- ASR Idempotency Records (EP-011, Decision F)
CREATE TABLE asr_idempotency_records (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    household_id UUID NOT NULL REFERENCES households(id),
    user_id UUID NOT NULL REFERENCES users(id),
    idempotency_key VARCHAR(64) NOT NULL,
    payload_digest VARCHAR(64) NOT NULL,
    transcription_id UUID NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    expires_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT uk_asr_idempotency_key UNIQUE (household_id, user_id, idempotency_key)
);

CREATE INDEX idx_asr_idempotency_expires ON asr_idempotency_records(expires_at);
