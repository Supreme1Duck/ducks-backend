-- Координаты кофеен (WGS84) для сортировки списка по удалённости от пользователя.
-- Проставляет админ вручную, поэтому nullable: у уже заведённых кофеен их пока нет,
-- такие уезжают в конец выдачи.

ALTER TABLE public.ducks_coffee_shop_table
    ADD COLUMN latitude  double precision,
    ADD COLUMN longitude double precision;
