# Технические части

# Архитектура
Файлы с суффиксом DTO -> модели данных возвращаемые сервисом

Файлы с суффиксом Model -> промежуточные модели данных необходимые для работы доменного слоя

# Информация о категориях товаров

Категории магазинов разделены.
Они могут быть любой вложенности и каждый уровень ссылается на предыдущий.
Также существуют супер категории, это категории 1 уровня. У них поле isSuperCategory = true.

# ducks-backend

This project was created using the [Ktor Project Generator](https://start.ktor.io).

Here are some useful links to get you started:

- [Ktor Documentation](https://ktor.io/docs/home.html)
- [Ktor GitHub page](https://github.com/ktorio/ktor)
- The [Ktor Slack chat](https://app.slack.com/client/T09229ZC6/C0A974TJ9). You'll need to [request an invite](https://surveys.jetbrains.com/s3/kotlin-slack-sign-up) to join.

## Features

Here's a list of features included in this project:

| Name                                                                   | Description                                                                        |
| ------------------------------------------------------------------------|------------------------------------------------------------------------------------ |
| [Koin](https://start.ktor.io/p/koin)                                   | Provides dependency injection                                                      |
| [Content Negotiation](https://start.ktor.io/p/content-negotiation)     | Provides automatic content conversion according to Content-Type and Accept headers |
| [Routing](https://start.ktor.io/p/routing)                             | Provides a structured routings DSL                                                  |
| [kotlinx.serialization](https://start.ktor.io/p/kotlinx-serialization) | Handles JSON serialization using kotlinx.serialization library                     |
| [Authentication](https://start.ktor.io/p/auth)                         | Provides extension point for handling the Authorization header                     |
| [Authentication JWT](https://start.ktor.io/p/auth-jwt)                 | Handles JSON Web Token (JWT) bearer authentication scheme                          |

## Building & Running

To build or run the project, use one of the following tasks:

| Task                          | Description                                                          |
| -------------------------------|---------------------------------------------------------------------- |
| `./gradlew test`              | Run the tests                                                        |
| `./gradlew build`             | Build everything                                                     |
| `buildFatJar`                 | Build an executable JAR of the server with all dependencies included |
| `buildImage`                  | Build the docker image to use with the fat JAR                       |
| `publishImageToLocalRegistry` | Publish the docker image locally                                     |
| `run`                         | Run the server                                                       |
| `runDocker`                   | Run using the local docker image                                     |

If the server starts successfully, you'll see the following output:

```
2024-12-04 14:32:45.584 [main] INFO  Application - Application started in 0.303 seconds.
2024-12-04 14:32:45.682 [main] INFO  Application - Responding at http://0.0.0.0:8080
```
# 🛒 API для продавца (модуль магазинов)

Все URL начинаются с базового пути: `/shops/seller`

---

## 🔐 Авторизация

### POST `shops/sellers/auth`  

**Описание:** Вход продавца и получение JWT-токена.

### Требуемые параметры:

| Параметр   | Тип      | Описание |
| ---------- | -------- | -------- |
| `login`    | `string` | Логин    |
| `password` | `string` | Пароль   |

### Ответ:

- `200 OK`: Возвращает объект с полем `token` типа `string`.

---

## ✅ Создание товара

### POST `shops/sellers/create/product`  

**Описание:** Создание нового товара.  
**Требуется авторизация через JWT токен.**

### Заголовки:

- `Authorization: Bearer <ваш_jwt_токен>`

### Тело запроса:

| Поле           | Тип                     | Обязательный | Описание                          |
| -------------- | ----------------------- | ------------ | --------------------------------- |
| `name`         | `string`                | ✅            | Название товара                   |
| `description`  | `string`                | ❌            | Описание товара                   |
| `sizeIds`      | `List<Long>`            | ❌            | Список ID размеров                |
| `categoryId`   | `Long`                  | ✅            | ID категории                      |
| `colorId`      | `Long`                  | ❌            | ID цвета                          |
| `seasonId`     | `Int`                   | ❌            | ID сезона                         |
| `brandName`    | `string`                | ✅            | Название бренда                   |
| `mainImageUrl` | `string`                | ✅            | URL главной картинки              |
| `imageUrls`    | `List<String>`          | ✅            | Список дополнительных изображений |
| `price`        | `BigDecimal` (`number`) | ❌            | Цена товара                       |

### Ответ:

- `204 No Content`: Успех, товар создан.

---

## ❌ Удаление товара

### DELETE `shops/sellers/delete/product`  

**Описание:** Удаление товара по ID.  
**Требуется авторизация через JWT токен.**

### Заголовки:

- `Authorization: Bearer <ваш_jwt_токен>`

### Параметры запроса:

| Параметр    | Тип    | Обязательный | Описание             |
| ----------- | ------ | ------------ | -------------------- |
| `productId` | `Long` | ✅            | ID удаляемого товара |
| `shopId`    | `Long` | ✅            | ID магазина          |

### Ответ:

- `204 No Content`: Успех, товар удален.

---

## 📤 Загрузка изображения

### POST `shops/sellers/upload/image`  

**Описание:** Загрузка одного или нескольких изображений.  
**Формат загрузки: `multipart/form-data`.**

### Заголовки:

- `Authorization: Bearer <ваш_jwt_токен>`

### Требуемое поле:

| Поле     | Тип              | Обязательный | Описание              |
| -------- | ---------------- | ------------ | --------------------- |
| `images` | `array of files` | ✅            | Картинки для загрузки |

### Ответ:

- `200 OK`: Возвращает список строк — URL загруженных изображений.

---

## 🧾 Итоговые коды ответов

| Код   | Описание                                                |
| ----- | ------------------------------------------------------- |
| `200` | Успех (для авторизации и загрузки изображений)          |
| `204` | Успех без тела ответа (для создания и удаления товаров) |
| `401` | Неавторизован                                           |
| `400` | Неверные данные запроса                                 |


## 1. /shops/products/search

## 📝 Описание

Выполняет поиск товаров по заданным фильтрам и возвращает:

- Список найденных товаров
- Фильтры, доступные на основе текущего запроса (цвета, размеры, категории и т.д.)

---

## ⚙️ Query параметры

| Параметр    | Тип          | Обязательный | Описание                                        |
| ----------- | ------------ | ------------ | ----------------------------------------------- |
| `query`     | `String`     | ❌            | Текстовый поиск по названию или артикулу товара |
| `category`  | `String`     | ❌            | Фильтрация по категории товаров                 |
| `sizeIds`   | `List<Long>` | ❌            | Фильтрация по ID размеров                       |
| `colorIds`  | `List<Int>`  | ❌            | Фильтрация по ID цветов                         |
| `seasonIds` | `List<Int>`  | ❌            | Фильтрация по сезонам                           |
| `lastId`    | `Long`       | ❌            | Используется для пагинации                      |
| `limit`     | `Int`        | ❌            | Максимальное количество товаров в ответе        |

---

## 📥 Пример запроса

```http
GET /products/search?query=shirt&category=clothes&sizeIds=101,102&colorIds=5,7&seasonIds=1,3&lastId=200&limit=20
```

---

## 2. GET `/products/all`

### 📝 Описание

Возвращает список всех товаров с возможностью пагинации, а также фильтры, доступные на основе текущего запроса.

### ⚙️ Query параметры

| Параметр | Тип    | Обязательный | Описание                        |
| -------- | ------ | ------------ | ------------------------------- |
| `lastId` | `Long` | ❌            | Используется для пагинации      |
| `limit`  | `Int`  | ❌            | Максимальное количество товаров |

### 📥 Пример запроса

`GET /products/all?lastId=200&limit=20`

### 📤 Пример успешного ответа (`200 OK`)

```json
{
  "products": [
    {
      "id": 1,
      "name": "Хлопковая футболка",
      "brandName": "CoolBrand",
      "shopName": "FashionShop",
      "price": "999.99",
      "imageUrls": [
        "https://example.com/images/1_front.jpg ",
        "https://example.com/images/1_back.jpg "
      ]
    },
    {
      "id": 2,
      "name": "Спортивные шорты",
      "brandName": null,
      "shopName": "SportGear",
      "price": null,
      "imageUrls": [
        "https://example.com/images/2_shorts.jpg "
      ]
    }
  ],
  "filters": {
    "sizes": [
      {
        "id": 101,
        "name": "S"
      },
      {
        "id": 102,
        "name": "M"
      }
    ],
    "categories": [
      {
        "id": 1,
        "name": "Одежда",
        "subcategories": [
          {
            "id": 10,
            "name": "Футболки"
          }
        ]
      }
    ],
    "colors": [
      {
        "id": 5,
        "name": "Красный"
      },
      {
        "id": 7,
        "name": "Чёрный"
      }
    ],
    "seasons": [
      {
        "id": 1,
        "name": "Лето"
      },
      {
        "id": 3,
        "name": "Весна"
      }
    ]
  }
}
```



## 3. GET `/products/all`

## 📝 Описание

Возвращает подробную информацию о товаре по его идентификатору.

---

## ⚙️ Path параметры

| Параметр    | Тип    | Обязательный | Описание             |
| ----------- | ------ | ------------ | -------------------- |
| `productId` | `Long` | ✅            | Идентификатор товара |

---

## 📤 Пример успешного ответа (`200 OK`)

```json
{
  "id": 123,
  "name": "Джинсы Slim Fit",
  "description": "Мужские джинсы из плотного денима.",
  "price": "1499.99",
  "categoryDTO": {
    "id": 1,
    "name": "Одежда",
    "subcategories": [
      {
        "id": 10,
        "name": "Футболки"
      }
    ]
  },
  "color": {
    "id": 5,
    "name": "Чёрный"
  },
  "season": {
    "id": 1,
    "name": "Лето"
  },
  "shopId": 1001,
  "shopName": "FashionShop",
  "brandName": "CoolBrand",
  "imageUrls": [
    "https://example.com/images/123_front.jpg ",
    "https://example.com/images/123_back.jpg "
  ],
  "sizes": [
    {
      "id": 101,
      "name": "S"
    },
    {
      "id": 102,
      "name": "M"
    }
  ]
}
```



## 4. GET /shops/products/by-shop/{shopId}

## 📝 Описание

Возвращает список товаров, принадлежащих конкретному магазину.  
Поддерживает пагинацию через параметры `lastId` и `limit`.

---

## ⚙️ Path и Query параметры

| Параметр | Тип     | Обязательный | Описание                           |
| -------- | ------- | ------------ | ---------------------------------- |
| `shopId` | `Long`  | ✅            | Идентификатор магазина             |
| `lastId` | `Long?` | ❌            | ID последнего товара для пагинации |
| `limit`  | `Int?`  | ❌            | Максимальное количество товаров    |

---

## 📥 Пример запроса

```json
[
  {
    "id": 1,
    "imgUrl": "https://example.com/images/1.jpg "
  },
  {
    "id": 2,
    "imgUrl": "https://example.com/images/2.jpg "
  }
]
```
