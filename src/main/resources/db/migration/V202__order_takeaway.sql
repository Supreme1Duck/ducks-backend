-- Заказ с собой или на месте. false — на месте, как и было у всех существующих заказов.

ALTER TABLE public.ducks_coffee_orders_table
    ADD COLUMN is_takeaway boolean DEFAULT false NOT NULL;
