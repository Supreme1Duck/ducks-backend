-- Калории теперь вычисляемое поле (формула Атуотера: protein*4 + carbohydrates*4 + fats*9).
-- Нормализуем старые данные под новый инвариант: если задан весь БЖУ — пересчитываем,
-- иначе калорийность недостоверна и обнуляется.
UPDATE ducks_coffee_shop_product_table
SET calories = CASE
    WHEN protein IS NOT NULL AND fats IS NOT NULL AND carbohydrates IS NOT NULL
        THEN protein * 4 + carbohydrates * 4 + fats * 9
    ELSE NULL
END;
