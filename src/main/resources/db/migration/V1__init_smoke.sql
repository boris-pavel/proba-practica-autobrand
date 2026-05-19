-- Smoke test migration: va fi eliminat în Phase 2 când scriem tabela product
CREATE TABLE smoke_test (id BIGSERIAL PRIMARY KEY, message VARCHAR(50));
INSERT INTO smoke_test (message) VALUES ('hello flyway');