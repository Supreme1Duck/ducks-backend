-- Opt-in: существующие кофейни не подключаются к кассе автоматически.
CREATE TABLE public.ducks_alfa_cash_register_settings (
    shop_id bigint PRIMARY KEY REFERENCES public.ducks_coffee_shop_table(id),
    enabled boolean NOT NULL DEFAULT false,
    cafe_table varchar(100) NOT NULL
);

CREATE TABLE public.ducks_alfa_order_transfers (
    order_id bigint PRIMARY KEY REFERENCES public.ducks_coffee_orders_table(id),
    shop_id bigint NOT NULL REFERENCES public.ducks_coffee_shop_table(id),
    cafe_table varchar(100) NOT NULL,
    -- AlfaTransferState.value: 0 PENDING, 1 IN_PROGRESS, 2 TRANSFERRED, 3 NEEDS_REVIEW, 4 CANCELLED.
    state integer NOT NULL CHECK (state BETWEEN 0 AND 4),
    operation_token uuid,
    cashbox_order_number bigint CHECK (cashbox_order_number > 0),
    payload text,
    created_at bigint NOT NULL,
    updated_at bigint NOT NULL,
    CHECK (state NOT IN (1, 2, 3) OR operation_token IS NOT NULL),
    CHECK (state <> 2 OR cashbox_order_number IS NOT NULL)
);

CREATE INDEX ducks_alfa_order_transfers_shop_id_state_created_at
    ON public.ducks_alfa_order_transfers(shop_id, state, created_at);
