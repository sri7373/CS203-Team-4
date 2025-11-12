-- Insert test users
INSERT INTO "user" (id) VALUES (1);

-- Insert test query logs
INSERT INTO query_log (user_id, type, params, result, origin_country, destination_country, created_at)
VALUES (1, 'CALCULATE', '{}', 'success', 'SGP', 'USA', CURRENT_TIMESTAMP);
