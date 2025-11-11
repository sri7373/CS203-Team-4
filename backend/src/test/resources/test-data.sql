-- Test data for integration tests
-- This file is loaded into H2 database before each test
-- Simple INSERT statements (tests are transactional and rolled back)

-- Insert test countries
INSERT INTO country (code, name) VALUES ('SGP', 'Singapore');
INSERT INTO country (code, name) VALUES ('USA', 'United States');
INSERT INTO country (code, name) VALUES ('JPN', 'Japan');

-- Insert test product categories
INSERT INTO product_category (code, name, weight_based) VALUES ('ELEC', 'Electronics', false);
INSERT INTO product_category (code, name, weight_based) VALUES ('FOOD', 'Food & Beverages', false);
INSERT INTO product_category (code, name, weight_based) VALUES ('TEXT', 'Textiles', false);

-- Insert test tariff rates
WITH ids AS (
    SELECT
        (SELECT id FROM country WHERE code = 'SGP') AS origin_id,
        (SELECT id FROM country WHERE code = 'USA') AS destination_id,
        (SELECT id FROM product_category WHERE code = 'ELEC') AS product_category_id
)
INSERT INTO tariff_rate (origin_id, destination_id, product_category_id, base_rate, additional_fee, weight_value, effective_from, effective_to)
SELECT 
    ids.origin_id,
    ids.destination_id,
    ids.product_category_id,
    5.0,
    10.0,
    0.0,
    CURRENT_DATE,
    DATEADD('YEAR', 1, CURRENT_DATE)
FROM ids
WHERE NOT EXISTS (
    SELECT 1 FROM tariff_rate tr
    WHERE tr.origin_id = ids.origin_id
    AND tr.destination_id = ids.destination_id
    AND tr.product_category_id = ids.product_category_id
);

WITH ids AS (
    SELECT
        (SELECT id FROM country WHERE code = 'USA') AS origin_id,
        (SELECT id FROM country WHERE code = 'JPN') AS destination_id,
        (SELECT id FROM product_category WHERE code = 'FOOD') AS product_category_id
)
INSERT INTO tariff_rate (origin_id, destination_id, product_category_id, base_rate, additional_fee, weight_value, effective_from, effective_to)
SELECT 
    ids.origin_id,
    ids.destination_id,
    ids.product_category_id,
    3.0,
    5.0,
    0.0,
    CURRENT_DATE,
    DATEADD('YEAR', 1, CURRENT_DATE)
FROM ids
WHERE NOT EXISTS (
    SELECT 1 FROM tariff_rate tr
    WHERE tr.origin_id = ids.origin_id
    AND tr.destination_id = ids.destination_id
    AND tr.product_category_id = ids.product_category_id
);
