CREATE TABLE mobile_devices (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    platform VARCHAR(20) NOT NULL,
    push_provider VARCHAR(20) NOT NULL,
    push_token VARCHAR(512) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    device_name VARCHAR(120),
    app_version VARCHAR(40),
    locale VARCHAR(35),
    timezone VARCHAR(80),
    last_seen_at TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    CONSTRAINT mobile_devices_platform_check CHECK (platform IN ('IOS', 'ANDROID')),
    CONSTRAINT mobile_devices_push_provider_check CHECK (push_provider IN ('EXPO')),
    CONSTRAINT mobile_devices_status_check CHECK (status IN ('ACTIVE', 'INACTIVE'))
);

CREATE INDEX idx_mobile_devices_user_status
ON mobile_devices(user_id, status);

CREATE INDEX idx_mobile_devices_user_updated_at
ON mobile_devices(user_id, updated_at DESC);

CREATE UNIQUE INDEX uq_mobile_devices_active_provider_token
ON mobile_devices(push_provider, push_token)
WHERE status = 'ACTIVE';

COMMENT ON TABLE mobile_devices IS 'Native mobile push device registrations scoped to authenticated HomeTusk users';
COMMENT ON COLUMN mobile_devices.push_token IS 'Provider push token. Sensitive operational value; do not log or return in API responses';
