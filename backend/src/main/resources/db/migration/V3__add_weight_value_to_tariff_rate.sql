-- Add weight_value column to tariff_rate table
ALTER TABLE tariff_rate
ADD COLUMN IF NOT EXISTS weight_value REAL NOT NULL DEFAULT 0.0;

COMMENT ON COLUMN tariff_rate.weight_value IS 'Weight value for tariff calculation, default 0.0';
