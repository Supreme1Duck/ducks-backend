package com.ducks.admin.repository

import com.ducks.admin.database.CoffeeShopCredentialsTable
import com.ducks.admin.request.CategoriesForGroup
import com.ducks.admin.request.CreateCoffeeShopRequest
import com.ducks.admin.request.SetCoffeeShopCoordinatesRequest
import com.ducks.common.geo.GeoPoint
import com.ducks.features.coffeeshops.database.CoffeeCategoryGroupTable
import com.ducks.features.coffeeshops.database.CoffeeProductCategoryTable
import com.ducks.features.coffeeshops.database.CoffeeProductTable
import com.ducks.features.coffeeshops.database.CoffeeShopTable
import com.ducks.features.coffeeshops.seller.domain.SellerCoffeeProductRepository
import com.ducks.features.coffeeshops.seller.domain.SellerCoffeeShopRepository
import com.ducks.features.coffeeshops.seller.domain.SellerConstructorsRepository
import com.ducks.features.coffeeshops.seller.routings.request.products.CreateCoffeeProductRequest
import com.ducks.features.coffeeshops.seller.routings.request.shop.SetCoffeeShopScheduleRequest
import com.ducks.util.DucksBadRequestError
import org.jetbrains.exposed.v1.core.dao.id.EntityID
import org.jetbrains.exposed.v1.exceptions.ExposedSQLException
import org.jetbrains.exposed.v1.jdbc.batchInsert
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.insertAndGetId
import org.jetbrains.exposed.v1.jdbc.select
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.update
import org.jetbrains.exposed.v1.jdbc.transactions.experimental.newSuspendedTransaction

class AdminCoffeeShopsRepository(
    private val sellersConstructorsRepository: SellerConstructorsRepository,
    private val repository: SellerCoffeeShopRepository,
    private val productsRepository: SellerCoffeeProductRepository,
) {

    suspend fun createNewShop(
        data: CreateCoffeeShopRequest,
        createdByAdminID: Long,
    ) {
        val shopId = createShop(data, createdByAdminID)

        addSchedules(shopId, data.workTime)

        sellersConstructorsRepository.insertBasic(shopId = shopId)
    }

    private suspend fun addSchedules(shopId: Long, request: SetCoffeeShopScheduleRequest) {
        repository.setSchedule(
            shopId = shopId,
            schedule = request,
        )
    }

    private suspend fun createShop(
        data: CreateCoffeeShopRequest,
        createdByAdminID: Long,
    ): Long {
        val coordinates = GeoPoint.parse(data.latitude, data.longitude)

        return try {
            newSuspendedTransaction {
                val shopId = CoffeeShopTable.insertAndGetId {
                    it[name] = data.name
                    it[address] = data.address
                    it[rating] = data.rating
                    it[latitude] = coordinates?.latitude
                    it[longitude] = coordinates?.longitude
                }.value

                CoffeeShopCredentialsTable.insert {
                    it[login] = data.unp
                    it[password] = data.initialPass
                    it[createdBy] = createdByAdminID
                    it[CoffeeShopCredentialsTable.shopId] = shopId
                    it[pinCode] = data.pinCode
                }

                shopId
            }
        } catch (e: ExposedSQLException) {
            if (e.message?.contains("unique constraint") == true) {
                throw DucksBadRequestError("Кофешоп с таким унп уже существует")
            } else {
                throw DucksBadRequestError("Неизвестная SQL ошибка")
            }
        } catch (e: Exception) {
            throw Exception("Ошибка в процессе создания магазина, ${e.stackTrace}")
        }
    }

    /**
     * Проставляет координаты уже заведённой кофейне: без них она не участвует
     * в сортировке по удалённости и висит в конце списка.
     */
    suspend fun setCoordinates(request: SetCoffeeShopCoordinatesRequest) {
        val coordinates = GeoPoint.parse(request.latitude, request.longitude)
            ?: throw DucksBadRequestError("Нужны и широта, и долгота.")

        newSuspendedTransaction {
            val updated = CoffeeShopTable.update({ CoffeeShopTable.id eq request.shopId }) {
                it[latitude] = coordinates.latitude
                it[longitude] = coordinates.longitude
            }

            if (updated == 0) {
                throw DucksBadRequestError("Кофешоп с id ${request.shopId} не найден")
            }
        }
    }

    /**
     * Заводит категории сразу по нескольким группам за один запрос.
     * Группы проверяются до вставки: неизвестный groupId отменяет всю пачку целиком,
     * чтобы не оставить половину категорий созданными.
     */
    suspend fun insertNewCategories(
        categories: List<CategoriesForGroup>,
    ) {
        newSuspendedTransaction {
            val requestedGroupIds = categories.map { it.groupId }.toSet()

            val existingGroupIds = CoffeeCategoryGroupTable
                .select(CoffeeCategoryGroupTable.id)
                .where { CoffeeCategoryGroupTable.id inList requestedGroupIds }
                .map { it[CoffeeCategoryGroupTable.id].value }
                .toSet()

            val unknownGroupIds = requestedGroupIds - existingGroupIds
            if (unknownGroupIds.isNotEmpty()) {
                throw DucksBadRequestError(
                    "Групп категорий с id ${unknownGroupIds.joinToString()} не существует"
                )
            }

            val newCategories = categories.flatMap { group ->
                group.names.map { name -> name to group.groupId }
            }

            CoffeeProductCategoryTable
                .batchInsert(newCategories) { (name, groupId) ->
                    this[CoffeeProductCategoryTable.name] = name
                    this[CoffeeProductCategoryTable.groupId] = EntityID(groupId, CoffeeCategoryGroupTable)
                }
        }
    }

    /**
     * Заводит товар в указанной кофейне. Сама вставка — общий с продавцом код: состав
     * товара, добавки и пересчёт калорий у админа ровно те же, расходится только то,
     * откуда берётся кофейня — из запроса, а не из токена.
     */
    suspend fun createProduct(shopId: Long, data: CreateCoffeeProductRequest): Long {
        ensureShopAndCategoryExist(shopId = shopId, categoryId = data.categoryId)

        return productsRepository.insert(shopId = shopId, data = data)
    }

    /**
     * Проверки ради внятного ответа: без них несуществующая кофейня или категория упали бы
     * нарушением внешнего ключа, то есть пятисоткой без объяснений. Гарантии не дают —
     * вставка идёт отдельной транзакцией.
     *
     * Отдельным методом, потому что при заливке товара вместе с картинкой это надо
     * проверить до вызова Photoroom: опечатка в id иначе стоит кредита.
     */
    suspend fun ensureShopAndCategoryExist(shopId: Long, categoryId: Long) {
        newSuspendedTransaction {
            val shopExists = CoffeeShopTable
                .selectAll()
                .where { CoffeeShopTable.id eq shopId }
                .empty()
                .not()

            if (!shopExists) {
                throw DucksBadRequestError("Кофейни с id $shopId не существует")
            }

            val categoryExists = CoffeeProductCategoryTable
                .selectAll()
                .where { CoffeeProductCategoryTable.id eq categoryId }
                .empty()
                .not()

            if (!categoryExists) {
                throw DucksBadRequestError("Категории товара с id $categoryId не существует")
            }
        }
    }

    /**
     * Проверка «товар существует» до заливки картинки: с опечаткой в id иначе сгорел бы
     * кредит Photoroom, а в хранилище остался бы файл, за который никто не отвечает.
     */
    suspend fun ensureProductExists(productId: Long) {
        val exists = newSuspendedTransaction {
            CoffeeProductTable
                .selectAll()
                .where { CoffeeProductTable.id eq productId }
                .limit(1)
                .any()
        }

        if (!exists) {
            throw DucksBadRequestError("Товар с id $productId не найден")
        }
    }

    /** Проверка «кофейня существует»: без неё опечатка в id молча отдавала бы пустой результат. */
    suspend fun ensureShopExists(shopId: Long) {
        val exists = newSuspendedTransaction {
            CoffeeShopTable
                .selectAll()
                .where { CoffeeShopTable.id eq shopId }
                .limit(1)
                .any()
        }

        if (!exists) {
            throw DucksBadRequestError("Кофейня с id $shopId не найдена")
        }
    }

    /**
     * Ставит товару картинку. В отличие от продавцовского пути кофейня не проверяется:
     * админ правит карточки любой из них.
     */
    suspend fun setProductImage(productId: Long, imageUrl: String) {
        newSuspendedTransaction {
            val updated = CoffeeProductTable.update({ CoffeeProductTable.id eq productId }) {
                it[CoffeeProductTable.imageUrl] = imageUrl
            }

            if (updated == 0) {
                throw DucksBadRequestError("Товар с id $productId не найден")
            }
        }
    }
}
