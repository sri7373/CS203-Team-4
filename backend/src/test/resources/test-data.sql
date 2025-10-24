-- Test data for integration tests
-- This file is loaded into H2 database before each test
-- Using simple INSERT with subqueries to avoid duplicates

-- Insert test countries (only if not exists)
INSERT INTO countries (code, name) 
SELECT 'SGP', 'Singapore' WHERE NOT EXISTS (SELECT 1 FROM countries WHERE code = 'SGP');

INSERT INTO countries (code, name) 
SELECT 'USA', 'United States' WHERE NOT EXISTS (SELECT 1 FROM countries WHERE code = 'USA');

INSERT INTO countries (code, name) 
SELECT 'JPN', 'Japan' WHERE NOT EXISTS (SELECT 1 FROM countries WHERE code = 'JPN');

-- Insert test product categories (only if not exists)
INSERT INTO product_categories (code, name, description) 
SELECT 'ELEC', 'Electronics', 'Electronic goods' WHERE NOT EXISTS (SELECT 1 FROM product_categories WHERE code = 'ELEC');

INSERT INTO product_categories (code, name, description) 
SELECT 'FOOD', 'Food & Beverages', 'Food products' WHERE NOT EXISTS (SELECT 1 FROM product_categories WHERE code = 'FOOD');

INSERT INTO product_categories (code, name, description) 
SELECT 'TEXT', 'Textiles', 'Textile products' WHERE NOT EXISTS (SELECT 1 FROM product_categories WHERE code = 'TEXT');

-- Insert test tariff rates (only if not exists)
INSERT INTO tariff_rates (origin_country_id, destination_country_id, product_category_id, base_rate, additional_fee, effective_date, expiry_date)
SELECT 
    (SELECT id FROM countries WHERE code = 'SGP'),
    (SELECT id FROM countries WHERE code = 'USA'),
    (SELECT id FROM product_categories WHERE code = 'ELEC'),
    5.0,
    10.0,
    CURRENT_DATE,
    DATEADD('YEAR', 1, CURRENT_DATE)
WHERE NOT EXISTS (
    SELECT 1 FROM tariff_rates tr
    WHERE tr.origin_country_id = (SELECT id FROM countries WHERE code = 'SGP')
    AND tr.destination_country_id = (SELECT id FROM countries WHERE code = 'USA')
    AND tr.product_category_id = (SELECT id FROM product_categories WHERE code = 'ELEC')
);

INSERT INTO tariff_rates (origin_country_id, destination_country_id, product_category_id, base_rate, additional_fee, effective_date, expiry_date)
SELECT 
    (SELECT id FROM countries WHERE code = 'USA'),
    (SELECT id FROM countries WHERE code = 'JPN'),
    (SELECT id FROM product_categories WHERE code = 'FOOD'),
    3.0,
    5.0,
    CURRENT_DATE,
    DATEADD('YEAR', 1, CURRENT_DATE)
WHERE NOT EXISTS (
    SELECT 1 FROM tariff_rates tr
    WHERE tr.origin_country_id = (SELECT id FROM countries WHERE code = 'USA')
    AND tr.destination_country_id = (SELECT id FROM countries WHERE code = 'JPN')
    AND tr.product_category_id = (SELECT id FROM product_categories WHERE code = 'FOOD')
);
