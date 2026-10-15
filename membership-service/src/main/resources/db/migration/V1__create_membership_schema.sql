CREATE EXTENSION IF NOT EXISTS pgcrypto;

CREATE TABLE membership_tiers (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    display_name VARCHAR(100) NOT NULL,
    status VARCHAR(20) NOT NULL,
    enrollment_closed BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by TEXT NOT NULL DEFAULT 'SYSTEM',
    updated_by TEXT NOT NULL DEFAULT 'SYSTEM'
);

CREATE UNIQUE INDEX uk_membership_tiers_display_name_lower
    ON membership_tiers (LOWER(display_name));

CREATE TABLE membership_perks (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    display_text JSONB NOT NULL,
    display_text_key VARCHAR(500) NOT NULL,
    applies_to VARCHAR(20) NOT NULL,
    perk_application JSONB NOT NULL,
    status VARCHAR(20) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by TEXT NOT NULL DEFAULT 'SYSTEM',
    updated_by TEXT NOT NULL DEFAULT 'SYSTEM'
);

CREATE UNIQUE INDEX uk_membership_perks_active_applies_to_display_text_key
    ON membership_perks (applies_to, display_text_key)
    WHERE status = 'ACTIVE';

CREATE TABLE membership_plans (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    membership_tier_id UUID NOT NULL REFERENCES membership_tiers(id),
    duration_days INTEGER NOT NULL,
    display_text VARCHAR(200) NOT NULL DEFAULT '',
    amount NUMERIC(12, 2) NOT NULL CHECK (amount >= 0),
    currency_code VARCHAR(3) NOT NULL,
    status VARCHAR(20) NOT NULL,
    enrollment_closed BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by TEXT NOT NULL DEFAULT 'SYSTEM',
    updated_by TEXT NOT NULL DEFAULT 'SYSTEM',
    CONSTRAINT uk_membership_plans_tier_duration_days UNIQUE (membership_tier_id, duration_days)
);

CREATE TABLE membership_tier_perks (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    membership_tier_id UUID NOT NULL REFERENCES membership_tiers(id),
    membership_perk_id UUID NOT NULL REFERENCES membership_perks(id),
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by TEXT NOT NULL DEFAULT 'SYSTEM',
    updated_by TEXT NOT NULL DEFAULT 'SYSTEM',
    CONSTRAINT uk_membership_tier_perks_tier_perk UNIQUE (membership_tier_id, membership_perk_id)
);

CREATE TABLE membership_tier_criteria (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    membership_tier_id UUID NOT NULL REFERENCES membership_tiers(id),
    priority INTEGER NOT NULL DEFAULT 100 CHECK (priority > 0),
    granted_plan_id UUID NOT NULL REFERENCES membership_plans(id),
    evaluation_window VARCHAR(30) NOT NULL,
    expression JSONB NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by TEXT NOT NULL DEFAULT 'SYSTEM',
    updated_by TEXT NOT NULL DEFAULT 'SYSTEM',
    CONSTRAINT uk_membership_tier_criteria_tier UNIQUE (membership_tier_id),
    CONSTRAINT uk_membership_tier_criteria_priority UNIQUE (priority)
);

CREATE INDEX idx_membership_tier_criteria_priority
    ON membership_tier_criteria (priority, membership_tier_id);
CREATE INDEX idx_membership_tier_criteria_tier_id
    ON membership_tier_criteria (membership_tier_id);

CREATE TABLE membership_subscriptions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL,
    membership_plan_id UUID NOT NULL REFERENCES membership_plans(id),
    activation_timestamp TIMESTAMPTZ,
    expiry_timestamp TIMESTAMPTZ,
    cancelled_at TIMESTAMPTZ,
    amount_paid NUMERIC(12, 2) NOT NULL CHECK (amount_paid >= 0),
    membership_value_amount NUMERIC(12, 2) NOT NULL CHECK (membership_value_amount >= 0),
    credit_applied_amount NUMERIC(12, 2) NOT NULL DEFAULT 0.00 CHECK (credit_applied_amount >= 0 AND credit_applied_amount <= membership_value_amount),
    currency_code VARCHAR(3) NOT NULL,
    auto_renew BOOLEAN NOT NULL DEFAULT FALSE,
    origin VARCHAR(30) NOT NULL DEFAULT 'PURCHASED',
    status VARCHAR(20) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by TEXT NOT NULL DEFAULT 'SYSTEM',
    updated_by TEXT NOT NULL DEFAULT 'SYSTEM',
    CONSTRAINT chk_subscription_expiry_after_activation CHECK (
        (activation_timestamp IS NULL AND expiry_timestamp IS NULL)
        OR (activation_timestamp IS NOT NULL AND expiry_timestamp IS NOT NULL AND expiry_timestamp > activation_timestamp)
    )
);

CREATE INDEX idx_membership_subscriptions_user_id ON membership_subscriptions (user_id);
CREATE UNIQUE INDEX uk_membership_subscriptions_active_user ON membership_subscriptions (user_id) WHERE status = 'ACTIVE';
CREATE UNIQUE INDEX uk_membership_subscriptions_one_pending_per_user ON membership_subscriptions (user_id) WHERE status = 'PENDING_PAYMENT';

CREATE TABLE payments (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    subscription_id UUID NOT NULL REFERENCES membership_subscriptions(id),
    transaction_id VARCHAR(100),
    bank_reference_id VARCHAR(100),
    amount NUMERIC(12, 2) NOT NULL CHECK (amount >= 0),
    currency_code VARCHAR(3) NOT NULL,
    status VARCHAR(20) NOT NULL,
    payment_expires_at TIMESTAMPTZ,
    payment_gateway VARCHAR(50),
    payment_method_reference VARCHAR(255),
    payment_type VARCHAR(30) NOT NULL,
    source_subscription_id UUID REFERENCES membership_subscriptions(id),
    source_payment_id UUID REFERENCES payments(id),
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by TEXT NOT NULL DEFAULT 'SYSTEM',
    updated_by TEXT NOT NULL DEFAULT 'SYSTEM',
    CONSTRAINT uk_payments_transaction_id UNIQUE (transaction_id),
    CONSTRAINT uk_payments_bank_reference_id UNIQUE (bank_reference_id),
    CONSTRAINT chk_payment_transaction_id_by_type CHECK (
        (payment_type = 'DIRECT_PAYMENT' AND transaction_id IS NOT NULL)
        OR (payment_type = 'MEMBERSHIP_CREDIT' AND transaction_id IS NULL)
    ),
    CONSTRAINT chk_payment_source_by_type CHECK (
        (payment_type = 'DIRECT_PAYMENT' AND source_subscription_id IS NULL AND source_payment_id IS NULL)
        OR (payment_type = 'MEMBERSHIP_CREDIT' AND source_subscription_id IS NOT NULL AND source_payment_id IS NOT NULL)
    )
);

CREATE INDEX idx_payments_subscription_id ON payments (subscription_id);
CREATE UNIQUE INDEX uk_payments_one_pending_per_subscription ON payments (subscription_id) WHERE status = 'PENDING';

CREATE TABLE refunds (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    subscription_id UUID NOT NULL REFERENCES membership_subscriptions(id),
    amount NUMERIC(12, 2) NOT NULL CHECK (amount >= 0),
    currency_code VARCHAR(3) NOT NULL,
    reason VARCHAR(200) NOT NULL,
    status VARCHAR(20) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by TEXT NOT NULL DEFAULT 'SYSTEM',
    updated_by TEXT NOT NULL DEFAULT 'SYSTEM',
    CONSTRAINT uk_refunds_subscription_id UNIQUE (subscription_id)
);

CREATE TABLE refund_allocations (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    refund_id UUID NOT NULL REFERENCES refunds(id),
    source_payment_id UUID NOT NULL REFERENCES payments(id),
    amount NUMERIC(12, 2) NOT NULL CHECK (amount > 0),
    currency_code VARCHAR(3) NOT NULL,
    status VARCHAR(30) NOT NULL,
    external_reference VARCHAR(255),
    attempt_count INTEGER NOT NULL DEFAULT 0,
    last_attempted_at TIMESTAMPTZ,
    failure_reason VARCHAR(500),
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by TEXT NOT NULL DEFAULT 'SYSTEM',
    updated_by TEXT NOT NULL DEFAULT 'SYSTEM',
    CONSTRAINT uk_refund_allocations_refund_source_payment UNIQUE (refund_id, source_payment_id)
);

CREATE INDEX idx_refund_allocations_refund_id ON refund_allocations (refund_id);
CREATE INDEX idx_refund_allocations_source_payment_id ON refund_allocations (source_payment_id);

CREATE INDEX idx_payments_subscription_created_at
    ON payments (subscription_id, created_at DESC);

CREATE INDEX idx_payments_subscription_status
    ON payments (subscription_id, status);

CREATE INDEX idx_refund_allocations_source_payment_status
    ON refund_allocations (source_payment_id, status);

CREATE INDEX idx_membership_subscriptions_auto_renew_expiry
    ON membership_subscriptions (status, auto_renew, expiry_timestamp)
    WHERE auto_renew = TRUE;

CREATE INDEX idx_membership_subscriptions_pending_payment
    ON membership_subscriptions (status)
    WHERE status = 'PENDING_PAYMENT';
