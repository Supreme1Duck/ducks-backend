#!/usr/bin/env bash
#
# Проставляет координаты кофейне в боевой базе.
#
# Запускать на сервере, после того как туда уехал jar с миграцией V203 —
# колонки latitude/longitude создаёт Flyway при старте приложения.
#
#   ./set-coffee-shop-coordinates.sh --list
#   ./set-coffee-shop-coordinates.sh <shopId> <широта> <долгота>
#
# Можно и с Mac, не копируя файл на сервер:
#
#   ssh root@93.125.82.179 'bash -s' -- --list < scripts/set-coffee-shop-coordinates.sh
#
# Параметры подключения берутся из /opt/ktor/.env, каждый переопределяется
# переменной окружения: ENV_FILE, DB_HOST, DB_PORT, DB_NAME, DB_USER, DB_PASS.

set -euo pipefail

ENV_FILE="${ENV_FILE:-/opt/ktor/.env}"
DB_HOST="${DB_HOST:-localhost}"
DB_PORT="${DB_PORT:-5432}"
DB_NAME="${DB_NAME:-ducksdatabase}"
DB_USER="${DB_USER:-andrewutko}"

# Границы Беларуси с запасом. Ловят главную ошибку при ручном вводе — перепутанные
# местами широту и долготу: у Минска это 53.9 и 27.5, обе половины по отдельности
# выглядят как валидные координаты, и обычная проверка диапазонов их пропускает.
readonly MIN_LATITUDE=51.2
readonly MAX_LATITUDE=56.2
readonly MIN_LONGITUDE=23.1
readonly MAX_LONGITUDE=32.8

die() {
    echo "Ошибка: $*" >&2
    exit 1
}

usage() {
    cat >&2 <<'USAGE'
Использование:
  set-coffee-shop-coordinates.sh --list                        показать кофейни и их координаты
  set-coffee-shop-coordinates.sh <shopId> <широта> <долгота>   проставить координаты

Флаги:
  --yes     не спрашивать подтверждения
  --force   разрешить точку за пределами Беларуси
USAGE
    exit 1
}

# Пароль нужен только если его не передали окружением.
load_password() {
    if [[ -n "${DB_PASS:-}" ]]; then
        return
    fi

    [[ -f "$ENV_FILE" ]] || die "не найден $ENV_FILE, передай пароль через DB_PASS=..."

    DB_PASS="$(grep -E '^DB_PASS=' "$ENV_FILE" | head -1 | cut -d= -f2-)"

    [[ -n "$DB_PASS" ]] || die "в $ENV_FILE нет строки DB_PASS="
}

run_sql() {
    PGPASSWORD="$DB_PASS" psql \
        --host "$DB_HOST" \
        --port "$DB_PORT" \
        --username "$DB_USER" \
        --dbname "$DB_NAME" \
        --no-psqlrc \
        --set ON_ERROR_STOP=1 \
        "$@"
}

# Без миграции V203 колонок нет, и UPDATE упал бы невнятной ошибкой про
# несуществующий столбец.
require_migration() {
    local columns
    columns="$(run_sql --tuples-only --no-align --command "
        SELECT count(*)
        FROM information_schema.columns
        WHERE table_schema = 'public'
          AND table_name = 'ducks_coffee_shop_table'
          AND column_name IN ('latitude', 'longitude');
    ")"

    [[ "$columns" == "2" ]] || die "в базе нет колонок latitude/longitude — сначала задеплой jar с миграцией V203 и дай приложению стартовать"
}

list_shops() {
    run_sql --command "
        SELECT id,
               name,
               address,
               latitude,
               longitude,
               CASE WHEN latitude IS NULL THEN 'нет координат' ELSE '' END AS status
        FROM public.ducks_coffee_shop_table
        ORDER BY id;
    "
}

is_number() {
    [[ "$1" =~ ^-?[0-9]+(\.[0-9]+)?$ ]]
}

# bash не умеет в дробные числа, сравнение отдаём awk.
in_range() {
    local value="$1" min="$2" max="$3"
    awk -v v="$value" -v lo="$min" -v hi="$max" 'BEGIN { exit !(v >= lo && v <= hi) }'
}

main() {
    local skip_confirm=false allow_outside_belarus=false
    local positional=()

    while [[ $# -gt 0 ]]; do
        case "$1" in
            --list) positional+=("--list"); shift ;;
            --yes|-y) skip_confirm=true; shift ;;
            --force) allow_outside_belarus=true; shift ;;
            -h|--help) usage ;;
            -*) die "неизвестный флаг $1" ;;
            *) positional+=("$1"); shift ;;
        esac
    done

    command -v psql >/dev/null || die "не найден psql"
    load_password

    if [[ ${#positional[@]} -eq 1 && "${positional[0]}" == "--list" ]]; then
        require_migration
        list_shops
        return
    fi

    [[ ${#positional[@]} -eq 3 ]] || usage

    local shop_id="${positional[0]}"
    local latitude="${positional[1]}"
    local longitude="${positional[2]}"

    [[ "$shop_id" =~ ^[0-9]+$ ]] || die "shopId должен быть целым числом, получено '$shop_id'"
    is_number "$latitude" || die "широта должна быть числом, получено '$latitude'"
    is_number "$longitude" || die "долгота должна быть числом, получено '$longitude'"

    in_range "$latitude" -90 90 || die "широта вне диапазона -90..90"
    in_range "$longitude" -180 180 || die "долгота вне диапазона -180..180"

    if ! $allow_outside_belarus; then
        in_range "$latitude" "$MIN_LATITUDE" "$MAX_LATITUDE" \
            && in_range "$longitude" "$MIN_LONGITUDE" "$MAX_LONGITUDE" \
            || die "точка $latitude, $longitude вне Беларуси — похоже на перепутанные местами широту и долготу. Если это специально, добавь --force"
    fi

    require_migration

    local shop_name
    shop_name="$(run_sql --tuples-only --no-align --command "
        SELECT name || ' — ' || address ||
               coalesce(' (сейчас ' || latitude || ', ' || longitude || ')', ' (координат нет)')
        FROM public.ducks_coffee_shop_table
        WHERE id = $shop_id;
    ")"

    [[ -n "$shop_name" ]] || die "кофейни с id $shop_id нет, посмотри список: $0 --list"

    echo "Кофейня $shop_id: $shop_name"
    echo "Новые координаты: $latitude, $longitude"

    if ! $skip_confirm; then
        read -r -p "Обновить? [y/N] " answer
        [[ "$answer" == "y" || "$answer" == "Y" ]] || { echo "Отменено."; return; }
    fi

    run_sql --quiet --command "
        BEGIN;
        UPDATE public.ducks_coffee_shop_table
        SET latitude = $latitude,
            longitude = $longitude
        WHERE id = $shop_id;
        COMMIT;
    "

    echo "Готово:"
    run_sql --command "
        SELECT id, name, address, latitude, longitude
        FROM public.ducks_coffee_shop_table
        WHERE id = $shop_id;
    "
}

main "$@"
