-- Cleanup script to delete test data after each test
-- Option A: disable referential integrity briefly, truncate tables, then re-enable
-- Safe for in-memory test DB (H2) used during tests.
SET REFERENTIAL_INTEGRITY FALSE;
TRUNCATE TABLE query_log;
TRUNCATE TABLE tariff_rate;
TRUNCATE TABLE product_category;
TRUNCATE TABLE country;
SET REFERENTIAL_INTEGRITY TRUE;
