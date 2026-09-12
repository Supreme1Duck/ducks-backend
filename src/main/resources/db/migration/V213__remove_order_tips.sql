-- Сохранённые итоговые суммы старых заказов не пересчитываем.
ALTER TABLE public.ducks_coffee_orders_table DROP COLUMN tips;

-- Сохранённый запрос в кассу должен читаться моделью без поля tips.
UPDATE public.ducks_alfa_order_transfers
SET payload = (payload::jsonb - 'tips')::text
WHERE payload IS NOT NULL AND jsonb_exists(payload::jsonb, 'tips');
