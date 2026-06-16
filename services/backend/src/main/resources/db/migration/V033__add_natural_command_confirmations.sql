-- V033: Add natural command and pending confirmation contract foundation.

ALTER TABLE commands DROP CONSTRAINT IF EXISTS commands_type_check;
ALTER TABLE commands ADD CONSTRAINT commands_type_check CHECK (
    type IN ('create_task', 'complete_task', 'add_shopping_item', 'natural_command')
);

ALTER TABLE commands DROP CONSTRAINT IF EXISTS commands_status_check;
ALTER TABLE commands ADD CONSTRAINT commands_status_check CHECK (
    status IN (
        'received',
        'validating',
        'processing',
        'scheduled',
        'needs_input',
        'needs_confirmation',
        'executed',
        'failed',
        'rejected'
    )
);

CREATE TABLE IF NOT EXISTS command_confirmations (
    id                       UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    command_id               UUID NOT NULL REFERENCES commands(id) ON DELETE CASCADE,
    household_id             UUID NOT NULL REFERENCES households(id),
    initiator_id             UUID NOT NULL REFERENCES users(id),
    provider_confirmation_id VARCHAR(255),
    provider_decision_id     UUID,
    provider_trace_id        VARCHAR(255),
    schema_version           VARCHAR(50),
    decision_version         VARCHAR(100),
    status                   VARCHAR(50) NOT NULL,
    summary                  TEXT NOT NULL,
    reasons                  JSONB NOT NULL DEFAULT '[]'::jsonb,
    risk_labels              JSONB NOT NULL DEFAULT '[]'::jsonb,
    proposed_actions         JSONB NOT NULL,
    expires_at               TIMESTAMP WITH TIME ZONE NOT NULL,
    created_at               TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at               TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    CONSTRAINT command_confirmations_status_check CHECK (
        status IN (
            'pending_confirmation',
            'confirmed',
            'cancelled',
            'expired',
            'rejected',
            'executed',
            'failed'
        )
    )
);

CREATE INDEX IF NOT EXISTS idx_command_confirmations_command_id
ON command_confirmations(command_id);

CREATE INDEX IF NOT EXISTS idx_command_confirmations_household_status
ON command_confirmations(household_id, status);

CREATE INDEX IF NOT EXISTS idx_command_confirmations_expires_at
ON command_confirmations(expires_at)
WHERE status = 'pending_confirmation';

COMMENT ON TABLE command_confirmations IS 'HomeTusk-owned pending confirmation proposals created from AI provider confirm decisions';
COMMENT ON COLUMN command_confirmations.provider_confirmation_id IS 'Provider confirmation id retained for audit only; HomeTusk id is public authority';
COMMENT ON COLUMN command_confirmations.proposed_actions IS 'Schema-validated HomeTusk action proposals; not executed until a future approval slice';
