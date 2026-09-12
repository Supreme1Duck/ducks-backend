package com.ducks.features.user.domain

import com.ducks.features.coffeeshops.checkShopIsNotTemporaryClosed
import com.ducks.features.coffeeshops.client.data.CoffeeProductsDataSource
import com.ducks.features.coffeeshops.client.data.model.dto.CoffeeProductSizeDTO
import com.ducks.features.coffeeshops.client.routings.request.CreateOrderRequest
import com.ducks.features.coffeeshops.database.CoffeeConstructorsTable
import com.ducks.features.coffeeshops.database.CoffeeProductTable
import com.ducks.features.coffeeshops.database.CoffeeProductsWithConstructorsTable
import com.ducks.features.coffeeshops.database.CoffeeShopTable
import com.ducks.features.orders.database.CoffeeOrderedProductsTable
import com.ducks.features.orders.database.CoffeeOrdersTable
import com.ducks.features.orders.data.repository.FetchAvailableOrdersTimeListRepository
import com.ducks.features.orders.database.model.OrderedProductConstructorDBModel
import com.ducks.features.orders.service.CalculateCoffeeShopsOrdersTimeService
import com.ducks.features.user.database.UserTable
import com.ducks.service.PushNotificationService
import com.ducks.service.PushType
import com.ducks.util.DucksBadRequestError
import com.ducks.util.ceilToMinute
import io.ktor.server.application.*
import kotlinx.datetime.Clock
import org.jetbrains.exposed.v1.core.JoinType
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.jdbc.*
import org.jetbrains.exposed.v1.jdbc.transactions.experimental.newSuspendedTransaction
import org.koin.ktor.ext.inject
import java.math.BigDecimal

class ClientCreateOrdersRepository(
    application: Application,
    private val coffeeProductsDataSource: CoffeeProductsDataSource,
    private val availableOrdersTimeListRepository: FetchAvailableOrdersTimeListRepository,
) {

    private val calculateCoffeeShopsOrdersTimeService by application.inject<CalculateCoffeeShopsOrdersTimeService>()
    private val pushNotificationService by application.inject<PushNotificationService>()

    suspend fun createOrder(
        request: CreateOrderRequest,
        // TODO вернуть clientPhoneNumber, когда вернётся вход по номеру телефона.
//        clientPhoneNumber: String,
        clientId: Long,
    ) {
        val (createdOrderId, sellerFcmToken) = newSuspendedTransaction {
            // Существование клиента уже проверил валидатор токена.
//            val clientId = getUserIdByPhone(clientPhoneNumber)

            if (request.products.isEmpty()) {
                throw DucksBadRequestError("Заказ не может быть пустым.")
            }

            if (request.products.any { (it.quantity ?: 1) < 1 }) {
                throw DucksBadRequestError("Количество товара должно быть больше нуля.")
            }

            // Получаем полный список продуктов с дубликатами если их несколько.
            val allProducts = buildList {
                request.products.forEach { request ->
                    repeat(request.quantity ?: 1) {
                        add(request)
                    }
                }
            }

            // Дальше идёт чтение занятости кофейни и вставка заказа в выбранный слот —
            // всё это должно быть под замком, иначе два одновременных заказа встанут
            // в одно и то же время.
            lockShop(request.shopId)

            checkShopIsNotTemporaryClosed(request.shopId)

            if (userHasActiveOrder(clientId)) {
                throw DucksBadRequestError("Вы не можете создать новый заказ, когда у вас есть активный заказ")
            }

            val productsById = fetchShopProducts(
                shopId = request.shopId,
                productIds = request.products.map { it.productId },
            )

            val currentTime = Clock.System.now().toEpochMilliseconds()
            val minutesToCookAllProducts = coffeeProductsDataSource
                .calculateMinutesToCook(request.shopId, allProducts.map { it.productId })

            // Список доступных времён отдаётся по целым минутам — выравниваем, чтобы в базу
            // не попал заказ с секундами и не ломал проверки пересечений.
            val orderFinishTime = ceilToMinute(request.estimatedTimeToFinish)

            validateOrderTime(
                shopId = request.shopId,
                finishTime = orderFinishTime,
                minutesToCook = minutesToCookAllProducts,
                currentTime = currentTime,
            )

            val orderId = CoffeeOrdersTable.insertAndGetId {
                it[createdTime] = currentTime
                it[coffeeShop] = request.shopId
                it[userId] = clientId
                it[comment] = request.comment

                it[estimatedFinishTime] = orderFinishTime
                it[timeToCookInMinutes] = minutesToCookAllProducts

                it[isToTime] = request.isToTime
                it[isTakeaway] = request.isTakeaway

                // будут посчитаны в конце
                it[price] = 0.toBigDecimal()
                it[totalPrice] = 0.toBigDecimal()
            }

            val orderedProducts = request.products.map { requestProduct ->
                val product = productsById.getValue(requestProduct.productId)
                val quantity = requestProduct.quantity ?: 1

                val size = product.sizes.firstOrNull { it.id == requestProduct.sizeId }
                    ?: throw DucksBadRequestError("Выбранного размера больше нет в меню, обновите корзину.")

                val constructors = getSelectedConstructors(
                    productId = requestProduct.productId,
                    requestedConstructorIds = requestProduct.constructorIds ?: emptyList(),
                )

                OrderedProduct(
                    id = product.id,
                    name = product.name,
                    imageUrl = product.imageUrl,
                    minutesToCook = product.minutesToCook,
                    constructors = constructors,
                    size = size,
                    quantity = quantity,
                    price = calculateProductPrice(
                        size = size,
                        constructors = constructors,
                        quantity = quantity,
                    ),
                )
            }

            val orderPrice = orderedProducts.sumOf { it.price }

            CoffeeOrderedProductsTable.batchInsert(orderedProducts) { product ->
                this[CoffeeOrderedProductsTable.orderId] = orderId
                this[CoffeeOrderedProductsTable.productName] = product.name
                this[CoffeeOrderedProductsTable.productId] = product.id
                this[CoffeeOrderedProductsTable.imageUrl] = product.imageUrl
                this[CoffeeOrderedProductsTable.selectedSize] = product.size
                this[CoffeeOrderedProductsTable.constructors] = product.constructors
                this[CoffeeOrderedProductsTable.minutesToCook] = product.minutesToCook?.let {
                    it * product.quantity
                }
                this[CoffeeOrderedProductsTable.quantity] = product.quantity
                this[CoffeeOrderedProductsTable.price] = product.price
            }

            CoffeeOrdersTable.update(
                where = {
                    CoffeeOrdersTable.id eq orderId
                }
            ) {
                it[price] = orderPrice
                it[totalPrice] = orderPrice
            }

            orderId.value to getShopFcmToken(request.shopId)
        }

        calculateCoffeeShopsOrdersTimeService(request.shopId)

        sellerFcmToken?.let {
            pushNotificationService.sendToSeller(
                fcmToken = it,
                title = "Новый заказ",
                body = "У вас новый заказ, посмотрите детали.",
                type = PushType.NEW_ORDER,
                orderId = createdOrderId,
            )
        }
    }

    private fun getShopFcmToken(shopId: Long): String? {
        return CoffeeShopTable
            .select(CoffeeShopTable.fcmToken)
            .where { CoffeeShopTable.id eq shopId }
            .map { it[CoffeeShopTable.fcmToken] }
            .firstOrNull()
    }

    /**
     * Продукты заказа, проверенные по кофейне из запроса.
     *
     * Привязка к shopId обязательна: без неё клиент со старым меню (или с подменённым
     * запросом) мог заказать товар чужого заведения — цена и время готовки уехали бы
     * из чужой кофейни, а продавец увидел бы у себя незнакомую позицию.
     */
    private fun fetchShopProducts(shopId: Long, productIds: List<Long>): Map<Long, ShopProduct> {
        val products = CoffeeProductTable
            .select(
                CoffeeProductTable.id,
                CoffeeProductTable.name,
                CoffeeProductTable.imageUrl,
                CoffeeProductTable.sizes,
                CoffeeProductTable.minutesToCook,
                CoffeeProductTable.inStock,
            )
            .where {
                (CoffeeProductTable.id inList productIds) and (CoffeeProductTable.shopId eq shopId)
            }
            .associate {
                it[CoffeeProductTable.id].value to ShopProduct(
                    id = it[CoffeeProductTable.id].value,
                    name = it[CoffeeProductTable.name],
                    imageUrl = it[CoffeeProductTable.imageUrl],
                    sizes = it[CoffeeProductTable.sizes],
                    minutesToCook = it[CoffeeProductTable.minutesToCook],
                    inStock = it[CoffeeProductTable.inStock],
                )
            }

        if (productIds.any { it !in products }) {
            throw DucksBadRequestError("Некоторых товаров больше нет в меню кофейни, обновите корзину.")
        }

        val outOfStock = products.values.filterNot { it.inStock }
        if (outOfStock.isNotEmpty()) {
            throw DucksBadRequestError("Закончилось: ${outOfStock.joinToString { it.name }}.")
        }

        return products
    }

    private fun getSelectedConstructors(
        requestedConstructorIds: List<Long>,
        productId: Long,
    ): List<OrderedProductConstructorDBModel> {
        if (requestedConstructorIds.isEmpty()) return emptyList()

        val constructors = CoffeeProductsWithConstructorsTable
            .join(
                otherTable = CoffeeConstructorsTable,
                joinType = JoinType.LEFT,
                onColumn = CoffeeProductsWithConstructorsTable.constructor,
                otherColumn = CoffeeConstructorsTable.id,
            )
            .select(
                CoffeeConstructorsTable.id,
                CoffeeConstructorsTable.name,
                CoffeeConstructorsTable.price,
                CoffeeConstructorsTable.isInStock,
            )
            .where {
                (CoffeeProductsWithConstructorsTable.product eq productId) and
                        (CoffeeProductsWithConstructorsTable.constructor inList requestedConstructorIds)
            }.map {
                it[CoffeeConstructorsTable.isInStock] to OrderedProductConstructorDBModel(
                    it[CoffeeConstructorsTable.id].value,
                    it[CoffeeConstructorsTable.name],
                    it[CoffeeConstructorsTable.price],
                )
            }

        // Раньше недоступные добавки просто отваливались из выборки: заказ создавался
        // молча дешевле, чем видел клиент, и без того, что он выбирал.
        if (constructors.size != requestedConstructorIds.distinct().size) {
            throw DucksBadRequestError("Некоторых добавок больше нет в меню, обновите корзину.")
        }

        val outOfStock = constructors.filterNot { (isInStock, _) -> isInStock }
        if (outOfStock.isNotEmpty()) {
            throw DucksBadRequestError("Закончилось: ${outOfStock.joinToString { (_, it) -> it.name }}.")
        }

        return constructors.map { (_, constructor) -> constructor }
    }

    private fun userHasActiveOrder(userId: Long): Boolean {
        return CoffeeOrdersTable
            .select(CoffeeOrdersTable.id)
            .where {
                (CoffeeOrdersTable.userId eq userId) and (CoffeeOrdersTable.finishedTime eq null)
            }
            .limit(1)
            .empty()
            .not()
    }

//    private fun getUserIdByPhone(clientPhoneNumber: String): Long {
//        return UserTable
//            .select(UserTable.id)
//            .where {
//                UserTable.phoneNumber eq clientPhoneNumber
//            }
//            .map {
//                it[UserTable.id].value
//            }
//            .firstOrNull()
//            ?: throw DucksBadRequestError("Пользователь не найден.")
//    }

    private fun calculateProductPrice(
        size: CoffeeProductSizeDTO?,
        constructors: List<OrderedProductConstructorDBModel>,
        quantity: Int,
    ): BigDecimal {
        val sizePrice = size?.price ?: 0.toBigDecimal()
        val constructorsPrice = constructors.sumOf { it.price ?: 0.toBigDecimal() }

        val singleProductPrice = sizePrice + constructorsPrice

        return singleProductPrice.times(quantity.toBigDecimal())
    }

    /**
     * Блокирует строку кофейни до конца транзакции.
     *
     * Без замка два клиента, оформляющиеся одновременно, читают одну и ту же занятость,
     * оба видят слот свободным и оба в него встают: допуск в 2 минуты сравнивал
     * вычисленное время с запрошенным, а не с занятостью, и такую пару пропускал.
     * Замок выстраивает оформление заказов внутри одной кофейни в очередь, кофейни
     * друг друга при этом не ждут.
     */
    private fun lockShop(shopId: Long) {
        CoffeeShopTable
            .select(CoffeeShopTable.id)
            .where { CoffeeShopTable.id eq shopId }
            .forUpdate()
            .firstOrNull()
            ?: throw DucksBadRequestError("Кофешоп не найден.")
    }

    /**
     * Проверяет, что выбранное клиентом время готовности можно занять прямо сейчас.
     *
     * Вызывать только под [lockShop] и в одной транзакции со вставкой заказа: занятость
     * читается здесь же, и между проверкой и вставкой никто не должен успеть занять
     * тот же слот.
     *
     * Раньше сервер сверял только «посчитанное ближайшее время не позже выбранного» —
     * а выбранное могло лежать поверх чужого заказа или не влезать в окно между двумя
     * заказами, и такой заказ спокойно создавался.
     */
    private fun validateOrderTime(
        shopId: Long,
        finishTime: Long,
        minutesToCook: Int,
        currentTime: Long,
    ) {
        val startTime = finishTime - minutesToCook.times(60_000L)

        if (finishTime > currentTime + MAX_ORDER_AHEAD_MS) {
            throw DucksBadRequestError("Время заказа должно быть не позже полутора часа.")
        }

        // Слоты выдаются по целым минутам, и пока клиент дожимает оформление, выбранная
        // минута успевает уйти в прошлое — на эти секунды даём допуск.
        if (startTime < currentTime - START_TIME_TOLERANCE_MS) {
            throw DucksBadRequestError("Это время уже прошло, выберите другое.")
        }

        val busySlots = availableOrdersTimeListRepository.getAllBusyTimeSlots(shopId)
        val isSlotTaken = busySlots.any { slot -> startTime < slot.endTime && slot.startTime < finishTime }

        if (isSlotTaken) {
            throw DucksBadRequestError("Это время было только что занято, попробуйте еще раз.")
        }

        val workTime = availableOrdersTimeListRepository.findShopsCurrentWorkTime(shopId)

        if (workTime == null || workTime.isClosed || currentTime !in workTime.startTime..workTime.endTime) {
            throw DucksBadRequestError("Кофейня сейчас не принимает заказы.")
        }

        if (startTime < workTime.startTime || finishTime > workTime.endTime) {
            throw DucksBadRequestError("Заказ не успеет приготовиться до закрытия кофейни.")
        }
    }

    private companion object {
        // Заказ ко времени — не дальше чем на полтора часа вперёд.
        const val MAX_ORDER_AHEAD_MS = 90 * 60_000L

        // Допуск на оформление: выбранная минута к моменту POST'а уже могла начаться.
        const val START_TIME_TOLERANCE_MS = 2 * 60_000L
    }

    private data class ShopProduct(
        val id: Long,
        val name: String,
        val imageUrl: String,
        val sizes: List<CoffeeProductSizeDTO>,
        val minutesToCook: Int?,
        val inStock: Boolean,
    )

    private data class OrderedProduct(
        val id: Long,
        val name: String,
        val size: CoffeeProductSizeDTO,
        val imageUrl: String,
        val constructors: List<OrderedProductConstructorDBModel>,
        val minutesToCook: Int?,
        val quantity: Int,
        val price: BigDecimal,
    )
}