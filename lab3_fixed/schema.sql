
CREATE TABLE IF NOT EXISTS users (
    id            SERIAL PRIMARY KEY,
    login         TEXT NOT NULL UNIQUE,
    password_hash TEXT NOT NULL         -- SHA-256 hex
);

-- ─── Отчёты ───────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS reports (
    id            BIGSERIAL PRIMARY KEY,
    name          TEXT        NOT NULL,
    sample_id     BIGINT      NOT NULL DEFAULT 0,
    experiment_id BIGINT      NOT NULL DEFAULT 0,
    status        TEXT        NOT NULL DEFAULT 'DRAFT'
                  CHECK (status IN ('DRAFT','FINAL','SIGNED')),
    owner_login   TEXT        REFERENCES users(login) ON UPDATE CASCADE ON DELETE SET NULL,
    signed_by     TEXT,
    created_at    TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at    TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_reports_owner   ON reports(owner_login);
CREATE INDEX IF NOT EXISTS idx_reports_status  ON reports(status);

-- ─── Строки отчётов ───────────────────────────────────────────
CREATE TABLE IF NOT EXISTS report_lines (
    id         BIGSERIAL PRIMARY KEY,
    report_id  BIGINT      NOT NULL
               REFERENCES reports(id) ON DELETE CASCADE,
    param      TEXT        NOT NULL
               CHECK (param IN ('PH','CONDUCTIVITY','TURBIDITY','NITRATE')),
    value      DOUBLE PRECISION NOT NULL,
    unit       TEXT        NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_lines_report ON report_lines(report_id);
