-- Migration: add hs_code and weight_based columns to product_category
-- hs_code: varchar, nullable
-- weight_based: boolean, not null default false
-- Set existing rows weight_based=FALSE
-- Insert two product categories ALCOHOL and TOBACCO

BEGIN;

-- Add hs_code column (nullable)
ALTER TABLE product_category
    ADD COLUMN IF NOT EXISTS hs_code VARCHAR(255);

-- Add weight_based column (not null, default false)
ALTER TABLE product_category
    ADD COLUMN IF NOT EXISTS weight_based BOOLEAN NOT NULL DEFAULT FALSE;

-- Ensure all existing rows (except ALCOHOL and TOBACCO) have weight_based = FALSE
UPDATE product_category
SET weight_based = FALSE
WHERE code NOT IN ('ALCOHOL', 'TOBACCO');

-- Insert the two new product categories
INSERT INTO product_category (code, name, hs_code, weight_based)
VALUES
  ('ALCOHOL', 'Alcohol & Beverages', NULL, TRUE),
  ('TOBACCO', 'Tobacco Products', NULL, TRUE)
ON CONFLICT (code) DO UPDATE
  SET name = EXCLUDED.name,
      hs_code = EXCLUDED.hs_code,
      weight_based = EXCLUDED.weight_based;

COMMIT;
