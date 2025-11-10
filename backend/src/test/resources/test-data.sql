-- Insert test countries
INSERT INTO country (code, name) VALUES ('SGP', 'Singapore');
INSERT INTO country (code, name) VALUES ('USA', 'United States');
INSERT INTO country (code, name) VALUES ('CHN', 'China');
INSERT INTO country (code, name) VALUES ('JPN', 'Japan');

-- Insert test product categories
INSERT INTO product_category (code, description) VALUES ('ELEC', 'Electronics');
INSERT INTO product_category (code, description) VALUES ('ALCOHOL', 'Alcoholic Beverages');
INSERT INTO product_category (code, description) VALUES ('TEXTILE', 'Textiles');
INSERT INTO product_category (code, description) VALUES ('AUTO', 'Automobiles');

-- Insert test tariff rates
INSERT INTO tariff_rate (origin_country, destination_country, product_category, rate, unit, weight_value) 
VALUES ('SGP', 'USA', 'ELEC', 5.0, 'PERCENT', 1.0);

INSERT INTO tariff_rate (origin_country, destination_country, product_category, rate, unit, weight_value) 
VALUES ('CHN', 'USA', 'ELEC', 15.0, 'PERCENT', 1.0);

INSERT INTO tariff_rate (origin_country, destination_country, product_category, rate, unit, weight_value) 
VALUES ('JPN', 'USA', 'AUTO', 2.5, 'PERCENT', 1.0);

INSERT INTO tariff_rate (origin_country, destination_country, product_category, rate, unit, weight_value) 
VALUES ('SGP', 'CHN', 'ALCOHOL', 20.0, 'PERCENT', 1.0);
