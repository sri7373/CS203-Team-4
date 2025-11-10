-- Insert test countries
INSERT INTO country (id, code, name) VALUES (1, 'SGP', 'Singapore');
INSERT INTO country (id, code, name) VALUES (2, 'USA', 'United States');
INSERT INTO country (id, code, name) VALUES (3, 'CHN', 'China');
INSERT INTO country (id, code, name) VALUES (4, 'JPN', 'Japan');

-- Insert test product categories
INSERT INTO product_category (id, code, name, hs_code, weight_based) VALUES (1, 'ELEC', 'Electronics', '8517', false);
INSERT INTO product_category (id, code, name, hs_code, weight_based) VALUES (2, 'ALCOHOL', 'Alcoholic Beverages', '2208', false);
INSERT INTO product_category (id, code, name, hs_code, weight_based) VALUES (3, 'TEXTILE', 'Textiles', '5208', false);
INSERT INTO product_category (id, code, name, hs_code, weight_based) VALUES (4, 'AUTO', 'Automobiles', '8703', false);

-- Insert test tariff rates
INSERT INTO tariff_rate (origin_id, destination_id, product_category_id, base_rate, additional_fee, weight_value, effective_from) 
VALUES (1, 2, 1, 0.05, 0.00, 1.0, '2024-01-01');

INSERT INTO tariff_rate (origin_id, destination_id, product_category_id, base_rate, additional_fee, weight_value, effective_from) 
VALUES (3, 2, 1, 0.15, 0.00, 1.0, '2024-01-01');

INSERT INTO tariff_rate (origin_id, destination_id, product_category_id, base_rate, additional_fee, weight_value, effective_from) 
VALUES (4, 2, 4, 0.025, 0.00, 1.0, '2024-01-01');

INSERT INTO tariff_rate (origin_id, destination_id, product_category_id, base_rate, additional_fee, weight_value, effective_from) 
VALUES (1, 3, 2, 0.20, 0.00, 1.0, '2024-01-01');
