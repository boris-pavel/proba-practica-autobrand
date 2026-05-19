CREATE TABLE exchange_rate (
                               id BIGSERIAL PRIMARY KEY,
                               rate_date DATE NOT NULL,
                               currency VARCHAR(3) NOT NULL,
                               rate_to_ron NUMERIC(12, 6) NOT NULL,
                               multiplier INTEGER NOT NULL DEFAULT 1,
                               fetched_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                               UNIQUE (rate_date, currency)
);