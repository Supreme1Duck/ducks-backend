-- Карта запрашивает кофейни прямоугольником видимой области, то есть диапазоном по
-- широте и долготе. Индекс по паре координат позволяет базе взять только нужные строки,
-- не перебирая таблицу целиком на каждое движение карты.
--
-- Частичный: у кофеен без координат меток не бывает, и в индексе им делать нечего.

CREATE INDEX coffee_shop_coordinates_index
    ON public.ducks_coffee_shop_table (latitude, longitude)
    WHERE latitude IS NOT NULL AND longitude IS NOT NULL;
