CREATE TABLE product (
                         id BIGSERIAL PRIMARY KEY,
                         name VARCHAR(255) NOT NULL UNIQUE,
                         description TEXT,
                         price NUMERIC(12, 2) NOT NULL,
                         currency VARCHAR(3) NOT NULL,
                         price_ron NUMERIC(12, 2),
                         image_url VARCHAR(1000),
                         source_url VARCHAR(1000),
                         manually_edited BOOLEAN NOT NULL DEFAULT FALSE,
                         first_seen TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                         last_scraped TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                         updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_product_currency ON product(currency);
CREATE INDEX idx_product_price ON product(price);