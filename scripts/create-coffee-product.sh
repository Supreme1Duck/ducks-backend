#!/usr/bin/env bash
#
# Заводит товар в кофейне через админский API.
#
#   ./scripts/create-coffee-product.sh --list-shops
#   ./scripts/create-coffee-product.sh --list-products 3
#   ./scripts/create-coffee-product.sh --list-categories
#   ./scripts/create-coffee-product.sh \
#       --shop 3 --category 7 --name "Раф лавандовый" \
#       --size "Средний:300 мл:7.50" --size "Большой:400 мл:9.00" \
#       --minutes 5 --image ~/Downloads/raf.jpg
#
# Логин админа берётся из ADMIN_LOGIN, пароль — из ADMIN_PASS либо спрашивается.
# Токен живёт без срока, так что его удобно один раз положить в ADMIN_TOKEN:
#
#   export ADMIN_TOKEN=$(./scripts/create-coffee-product.sh --token)
#
# Переопределяются окружением: API_URL, SSH_HOST, ADMIN_LOGIN, ADMIN_PASS, ADMIN_TOKEN.

set -euo pipefail

API_URL="${API_URL:-http://93.125.82.179:8080}"
SSH_HOST="${SSH_HOST:-root@93.125.82.179}"

die() {
    echo "Ошибка: $*" >&2
    exit 1
}

usage() {
    cat >&2 <<'USAGE'
Использование:
  create-coffee-product.sh --shop <id> --category <id> --name <имя> --size <размер> [опции]
  create-coffee-product.sh --list-shops | --list-products <id кофейни>
  create-coffee-product.sh --list-categories | --token

Обязательное:
  --shop <id>            id кофейни
  --category <id>        id категории товара (--list-categories покажет)
  --name <текст>         название товара
  --size <размер>        "Название:Объём:Цена", можно повторять
                         (для единственного размера — просто "Объём:Цена")
                         Название — короткая подпись кнопки в карточке: S, M, Порция

Опции:
  --image <путь>         картинка; без неё товар заведётся без картинки
  --raw                  слать картинку как есть, без Photoroom и общего холста
  --description <текст>
  --minutes <число>      время приготовления
  --out-of-stock         завести скрытым (по умолчанию товар в наличии)
  --protein/--fats/--carbs <число>   БЖУ, калории сервер посчитает сам
  --dry-run              показать json и запрос, ничего не отправляя
USAGE
    exit 1
}

require_tools() {
    command -v curl >/dev/null || die "не найден curl"
    command -v python3 >/dev/null || die "не найден python3"
}

# Пароль не эхоится и нигде не сохраняется — только в переменную этого процесса.
admin_token() {
    if [[ -n "${ADMIN_TOKEN:-}" ]]; then
        printf '%s' "$ADMIN_TOKEN"
        return
    fi

    local login="${ADMIN_LOGIN:-}" password="${ADMIN_PASS:-}"

    if [[ -z "$login" ]]; then
        read -r -p "Логин админа: " login
    fi

    if [[ -z "$password" ]]; then
        read -r -s -p "Пароль админа: " password
        echo >&2
    fi

    local body
    body="$(python3 -c 'import json,sys; print(json.dumps({"login": sys.argv[1], "password": sys.argv[2]}))' "$login" "$password")"

    local response
    response="$(curl -sS -X POST "$API_URL/admin/login" \
        -H "Content-Type: application/json" \
        -d "$body")"

    printf '%s' "$response" | python3 -c '
import json, sys

raw = sys.stdin.read()

try:
    print(json.loads(raw)["token"], end="")
except Exception:
    sys.exit(f"сервер не отдал токен, ответ: {raw!r}")
' || die "не удалось войти"
}

# Списки берём из базы: админского эндпоинта для них нет, а без id товар не завести.
remote_psql() {
    ssh "$SSH_HOST" 'set -a; . /opt/ktor/.env; set +a; PGPASSWORD="$DB_PASS" psql --username andrewutko --dbname ducksdatabase --no-psqlrc --command "'"$1"'"'
}

list_shops() {
    remote_psql "SELECT id, name, address FROM public.ducks_coffee_shop_table ORDER BY id;"
}

list_products() {
    local shop_id="$1"

    [[ "$shop_id" =~ ^[0-9]+$ ]] || die "--list-products ждёт id кофейни"

    remote_psql "SELECT p.id, p.name, c.name AS category, p.price, p.in_stock, coalesce(nullif(p.\"imageUrl\", ''), '— нет картинки —') AS image FROM public.ducks_coffee_shop_product_table p JOIN public.ducks_coffee_product_category_table c ON c.id = p.category_id WHERE p.shop_id = $shop_id ORDER BY p.id;"
}

list_categories() {
    remote_psql "SELECT c.id, c.name, g.name AS group FROM public.ducks_coffee_product_category_table c JOIN public.ducks_coffee_category_group_table g ON g.id = c.group_id ORDER BY c.id;"
}

main() {
    local shop_id="" category_id="" name="" description="" minutes="" image=""
    local in_stock=true raw=false dry_run=false
    local protein="" fats="" carbs=""
    local sizes=()

    [[ $# -gt 0 ]] || usage

    while [[ $# -gt 0 ]]; do
        case "$1" in
            --shop) shop_id="${2:-}"; shift 2 ;;
            --category) category_id="${2:-}"; shift 2 ;;
            --name) name="${2:-}"; shift 2 ;;
            --description) description="${2:-}"; shift 2 ;;
            --size) sizes+=("${2:-}"); shift 2 ;;
            --minutes) minutes="${2:-}"; shift 2 ;;
            --image) image="${2:-}"; shift 2 ;;
            --protein) protein="${2:-}"; shift 2 ;;
            --fats) fats="${2:-}"; shift 2 ;;
            --carbs) carbs="${2:-}"; shift 2 ;;
            --raw) raw=true; shift ;;
            --out-of-stock) in_stock=false; shift ;;
            --dry-run) dry_run=true; shift ;;
            --list-shops) require_tools; list_shops; return ;;
            --list-products) require_tools; list_products "${2:-}"; return ;;
            --list-categories) require_tools; list_categories; return ;;
            --token) require_tools; admin_token; echo; return ;;
            -h|--help) usage ;;
            *) die "неизвестный аргумент $1" ;;
        esac
    done

    require_tools

    [[ "$shop_id" =~ ^[0-9]+$ ]] || die "--shop должен быть числом"
    [[ "$category_id" =~ ^[0-9]+$ ]] || die "--category должен быть числом"
    [[ -n "$name" ]] || die "нужен --name"
    [[ ${#sizes[@]} -gt 0 ]] || die "нужен хотя бы один --size"

    if [[ -n "$image" ]]; then
        [[ -f "$image" ]] || die "файла $image нет"

        # Проверка здесь, а не на сервере: иначе про формат узнаешь после заливки.
        local extension="${image##*.}"
        case "$(printf '%s' "$extension" | tr '[:upper:]' '[:lower:]')" in
            jpg|jpeg|png|webp) ;;
            *) die "формат .$extension не поддерживается, нужен jpg, jpeg, png или webp" ;;
        esac
    elif $raw; then
        die "--raw без --image ничего не значит"
    fi

    local product_json
    product_json="$(python3 - "$name" "$description" "$category_id" "$minutes" "$in_stock" \
        "$protein" "$fats" "$carbs" "${sizes[@]}" <<'PY'
import json
import sys
import uuid

name, description, category, minutes, in_stock, protein, fats, carbs = sys.argv[1:9]
raw_sizes = sys.argv[9:]


def number(value, field):
    if value == "":
        return None
    try:
        return int(value)
    except ValueError:
        sys.exit(f"Ошибка: {field} должно быть целым числом, получено '{value}'")


sizes = []

single_size = len(raw_sizes) == 1

for raw in raw_sizes:
    parts = raw.split(":")

    # У товара с единственным размером выбирать нечего, подпись не показывается —
    # для него разрешаем короткую форму "Объём:Цена".
    if len(parts) == 2 and single_size:
        size_name, size_value, price = "", parts[0], parts[1]
    elif len(parts) == 3:
        size_name, size_value, price = parts
    elif single_size:
        sys.exit(f"Ошибка: размер '{raw}' — ожидается \"Название:Объём:Цена\" или \"Объём:Цена\"")
    else:
        sys.exit(f"Ошибка: размер '{raw}' — ожидается \"Название:Объём:Цена\"")

    # Когда размеров несколько, без подписи клиент рисует пустые кнопки выбора,
    # поэтому пустое название отбиваем здесь же, не доводя до сервера.
    if not single_size and not size_name.strip():
        sys.exit(f"Ошибка: в размере '{raw}' пустое название")

    try:
        price = float(price.replace(",", "."))
    except ValueError:
        sys.exit(f"Ошибка: цена '{price}' в размере '{raw}' — не число")

    if price <= 0:
        sys.exit(f"Ошибка: цена в размере '{raw}' должна быть больше нуля")

    sizes.append({
        # id размера живёт в заказах, поэтому он должен быть уникальным и стабильным —
        # ровно тот же uuid, что генерит приложение продавца.
        "id": str(uuid.uuid4()),
        # Единственному размеру подпись не нужна — сервер её всё равно обнулит.
        "sizeName": None if single_size else size_name,
        "sizeValue": size_value,
        "price": price,
    })

print(json.dumps({
    "name": name,
    # Пустая строка, а не отсутствие поля: на сборках до дефолта в модели запрос без
    # imageUrl отлетает с 400. Для заливки с картинкой сервер всё равно перезапишет его
    # ссылкой на файл, для запроса без картинки пустая строка и означает «картинки нет».
    "imageUrl": "",
    "description": description or None,
    "categoryId": int(category),
    "minutesToCook": number(minutes, "--minutes"),
    "inStock": in_stock == "true",
    "sizes": sizes,
    "protein": number(protein, "--protein"),
    "fats": number(fats, "--fats"),
    "carbohydrates": number(carbs, "--carbs"),
}, ensure_ascii=False))
PY
)"

    local url
    if [[ -z "$image" ]]; then
        url="$API_URL/coffee-shops/$shop_id/product/create"
    elif $raw; then
        url="$API_URL/coffee-shops/$shop_id/product/create/with-image/raw"
    else
        url="$API_URL/coffee-shops/$shop_id/product/create/with-image"
    fi

    echo "Товар: $product_json"
    echo "Куда:  POST $url"
    if [[ -n "$image" ]]; then
        echo "Файл:  $image"
    fi

    if $dry_run; then
        echo "--dry-run, запрос не отправлен."
        return
    fi

    local token
    token="$(admin_token)"

    local response
    if [[ -z "$image" ]]; then
        response="$(curl -sS -X POST "$url" \
            -H "Authorization: Bearer $token" \
            -H "Content-Type: application/json" \
            -d "$product_json" \
            --write-out '\n%{http_code}')"
    else
        response="$(curl -sS -X POST "$url" \
            -H "Authorization: Bearer $token" \
            -F "product=$product_json" \
            -F "file=@$image" \
            --write-out '\n%{http_code}')"
    fi

    local status="${response##*$'\n'}"
    local body="${response%$'\n'*}"

    if [[ "$status" != "201" ]]; then
        die "сервер ответил $status: $body"
    fi

    echo "Готово: $body"
}

main "$@"
