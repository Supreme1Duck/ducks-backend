-- Конфиг клиентского приложения: чем можно пользоваться прямо сейчас.
-- Нужен как рубильник — выключить фичу на всех клиентах, не дожидаясь релиза в сторах
-- и не перезапуская сервер.
--
-- В таблице лежат только отличия от дефолтов: список флагов и их значения по умолчанию
-- живут в коде (ClientFeature), строка появляется здесь лишь когда флаг переключили.
-- Поэтому пустая таблица — это рабочее состояние «всё как задумано», а не потеря данных.

CREATE TABLE public.ducks_client_config_table (
    id bigserial PRIMARY KEY,
    feature_key varchar(64) NOT NULL,
    is_enabled boolean NOT NULL,
    updated_at bigint NOT NULL
);

CREATE UNIQUE INDEX client_config_feature_key_index
    ON public.ducks_client_config_table (feature_key);
