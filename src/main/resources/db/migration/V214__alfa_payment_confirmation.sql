ALTER TABLE public.ducks_alfa_order_transfers
    ADD COLUMN payment_status integer NOT NULL DEFAULT 0 CHECK (payment_status BETWEEN 0 AND 3),
    ADD COLUMN cheque_id varchar(200),
    ADD COLUMN paid_at bigint,
    ADD COLUMN paid_amount numeric(15,2),
    ADD CONSTRAINT alfa_paid_receipt_required CHECK (
        payment_status <> 1 OR (cheque_id IS NOT NULL AND paid_at IS NOT NULL AND paid_amount IS NOT NULL)
    );

CREATE UNIQUE INDEX ducks_alfa_order_transfers_shop_id_cheque_id
    ON public.ducks_alfa_order_transfers(shop_id, cheque_id);

-- Ранее завершённые отправки не считаются подтверждённой оплатой.
-- Исторические finished_time сохраняются; результат нужно сверить с кассой.
UPDATE public.ducks_alfa_order_transfers SET payment_status = 3 WHERE state IN (1, 2, 3);
UPDATE public.ducks_alfa_order_transfers SET payment_status = 2 WHERE state = 4;
