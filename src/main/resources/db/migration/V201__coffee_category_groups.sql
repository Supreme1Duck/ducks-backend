-- Верхний уровень над категориями кофейных товаров: Напитки / Еда / Дополнительно.

CREATE TABLE public.ducks_coffee_category_group_table (
    id bigserial PRIMARY KEY,
    name text NOT NULL,
    sort_order integer NOT NULL
);

INSERT INTO public.ducks_coffee_category_group_table (name, sort_order) VALUES
    ('Напитки', 0),
    ('Еда', 1),
    ('Дополнительно', 2);

ALTER TABLE public.ducks_coffee_product_category_table ADD COLUMN group_id bigint;

-- Раскладка существующих категорий. Всё, что не напиток и не еда, уезжает в «Дополнительно»:
-- «Леденцы», «Другое» и «Акции» (последняя — скидочная витрина, а не тип товара).
UPDATE public.ducks_coffee_product_category_table c
SET group_id = g.id
FROM public.ducks_coffee_category_group_table g
WHERE g.name = CASE
    WHEN c.name IN ('Горячие напитки', 'Холодные напитки') THEN 'Напитки'
    WHEN c.name IN ('Кухня', 'Сендвичи', 'Салаты', 'Выпечка', 'Десерты') THEN 'Еда'
    ELSE 'Дополнительно'
END;

ALTER TABLE public.ducks_coffee_product_category_table ALTER COLUMN group_id SET NOT NULL;

ALTER TABLE ONLY public.ducks_coffee_product_category_table
    ADD CONSTRAINT fk_ducks_coffee_product_category_table_group_id__id
    FOREIGN KEY (group_id) REFERENCES public.ducks_coffee_category_group_table(id)
    ON UPDATE RESTRICT ON DELETE RESTRICT;
