CREATE TABLE scrape_run (
                            id BIGSERIAL PRIMARY KEY,
                            started_at TIMESTAMP NOT NULL,
                            finished_at TIMESTAMP,
                            status VARCHAR(20) NOT NULL,
                            products_total INTEGER,
                            products_new INTEGER,
                            products_updated INTEGER,
                            error_message TEXT
);
CREATE INDEX idx_scrape_run_started_at ON scrape_run(started_at DESC);