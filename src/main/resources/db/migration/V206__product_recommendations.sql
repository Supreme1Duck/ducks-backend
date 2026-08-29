-- Статистика совместных покупок: какие товары кофейни попадают в один заказ.
-- Считается ночью по истории заказов (ProductRecommendationsService) и полностью
-- перезаписывается для кофейни целиком, поэтому строки тут — кэш, а не данные:
-- таблицу можно очистить, следующий пересчёт восстановит её из заказов.

CREATE TABLE public.ducks_coffee_product_recommendation_table (
    id bigserial PRIMARY KEY,
    shop_id bigint NOT NULL,
    source_product_id bigint NOT NULL,
    target_product_id bigint NOT NULL,
    -- Во сколько раз товар чаще встречается в заказах с source, чем в заказах вообще (lift).
    score double precision NOT NULL,
    -- Сколько заказов дало эту пару: по одному-двум совпадениям рекомендовать нельзя.
    pair_count integer NOT NULL,
    calculated_at bigint NOT NULL
);

ALTER TABLE ONLY public.ducks_coffee_product_recommendation_table
    ADD CONSTRAINT fk_coffee_product_recommendation_shop_id
    FOREIGN KEY (shop_id) REFERENCES public.ducks_coffee_shop_table(id)
    ON UPDATE RESTRICT ON DELETE CASCADE;

-- Удалили товар из меню — уходят и пары с ним: рекомендовать его больше нечем и незачем.
ALTER TABLE ONLY public.ducks_coffee_product_recommendation_table
    ADD CONSTRAINT fk_coffee_product_recommendation_source_product_id
    FOREIGN KEY (source_product_id) REFERENCES public.ducks_coffee_shop_product_table(id)
    ON UPDATE RESTRICT ON DELETE CASCADE;

ALTER TABLE ONLY public.ducks_coffee_product_recommendation_table
    ADD CONSTRAINT fk_coffee_product_recommendation_target_product_id
    FOREIGN KEY (target_product_id) REFERENCES public.ducks_coffee_shop_product_table(id)
    ON UPDATE RESTRICT ON DELETE CASCADE;

CREATE UNIQUE INDEX coffee_product_recommendation_pair_index
    ON public.ducks_coffee_product_recommendation_table (shop_id, source_product_id, target_product_id);

-- Основной запрос: пары для товаров, лежащих в корзине.
CREATE INDEX coffee_product_recommendation_source_index
    ON public.ducks_coffee_product_recommendation_table (shop_id, source_product_id);
