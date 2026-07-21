ALTER TABLE ducks_shop_table ADD COLUMN name_citext CITEXT;
UPDATE ducks_shop_table SET name_citext = name;
ALTER TABLE ducks_shop_table DROP COLUMN name;
ALTER TABLE ducks_shop_table RENAME COLUMN name_citext TO name;

ALTER TABLE ducks_shop_table ADD COLUMN address_citext CITEXT;
UPDATE ducks_shop_table SET address_citext = address;
ALTER TABLE ducks_shop_table DROP COLUMN address;
ALTER TABLE ducks_shop_table RENAME COLUMN address_citext TO address;