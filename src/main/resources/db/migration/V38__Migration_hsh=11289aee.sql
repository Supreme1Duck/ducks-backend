ALTER TABLE ducks_shop_product_table ADD COLUMN main_image_url TEXT;
UPDATE ducks_shop_product_table SET main_image_url = 'default.jpg';
ALTER TABLE ducks_shop_product_table ALTER COLUMN main_image_url SET NOT NULL;