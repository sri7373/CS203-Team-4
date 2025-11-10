-- Insert test countries
INSERT INTO country (code, name) VALUES ('SGP', 'Singapore');
INSERT INTO country (code, name) VALUES ('USA', 'United States');
INSERT INTO country (code, name) VALUES ('CHN', 'China');
INSERT INTO country (code, name) VALUES ('JPN', 'Japan');

-- Insert test product categories
INSERT INTO product_category (code, name, hs_code, weight_based) VALUES ('ELEC', 'Electronics', '8517', false);
INSERT INTO product_category (code, name, hs_code, weight_based) VALUES ('ALCOHOL', 'Alcoholic Beverages', '2208', false);
INSERT INTO product_category (code, name, hs_code, weight_based) VALUES ('TEXTILE', 'Textiles', '5208', false);
INSERT INTO product_category (code, name, hs_code, weight_based) VALUES ('AUTO', 'Automobiles', '8703', false);

-- Insert test tariff rates
INSERT INTO tariff_rate (origin_country, destination_country, product_category, rate, unit, weight_value) 
VALUES ('SGP', 'USA', 'ELEC', 5.0, 'PERCENT', 1.0);

INSERT INTO tariff_rate (origin_country, destination_country, product_category, rate, unit, weight_value) 
VALUES ('CHN', 'USA', 'ELEC', 15.0, 'PERCENT', 1.0);

INSERT INTO tariff_rate (origin_country, destination_country, product_category, rate, unit, weight_value) 
VALUES ('JPN', 'USA', 'AUTO', 2.5, 'PERCENT', 1.0);

INSERT INTO tariff_rate (origin_country, destination_country, product_category, rate, unit, weight_value) 
VALUES ('SGP', 'CHN', 'ALCOHOL', 20.0, 'PERCENT', 1.0);
