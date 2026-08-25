package com.ducks.features.user.domain

import com.ducks.features.user.database.UserTable
import org.jetbrains.exposed.v1.core.ResultRow
import kotlin.time.Duration.Companion.days

/**
 * Сколько данные удалённого аккаунта живут в базе после заявки. Всё это время аккаунт уже
 * недоступен и номер освобождён, восстановить его нельзя — отсрочка нужна только затем,
 * чтобы продавец мог опознать клиента по заказу последних дней.
 */
val ACCOUNT_DELETION_GRACE_PERIOD = 14.days

// Заголовок с одноразовым подтверждением из /users/account/deletion/verify-otp.
// Тело у DELETE проксировать умеют не все, поэтому передаём заголовком.
const val DELETION_CONFIRMATION_HEADER = "X-Deletion-Confirmation"

/**
 * Телефон удалённого аккаунта. Занулить колонку нельзя — она not null и под уникальным
 * индексом, поэтому кладём плейсхолдер: он заведомо не совпадёт с реальным номером и не
 * помешает клиенту зарегистрироваться заново с тем же телефоном.
 */
fun deletedClientPhoneNumber(userId: Long): String = "deleted_$userId"

// Что видит продавец вместо телефона в истории заказов обезличенного клиента.
const val DELETED_CLIENT_PHONE_TITLE = "Аккаунт удалён"

/**
 * Телефон клиента для показа продавцу. Требует, чтобы в строке были [UserTable.phoneNumber],
 * [UserTable.deletedPhoneNumber] и [UserTable.deletedAt].
 */
fun ResultRow.clientPhoneNumberOrDeletedTitle(): String {
    // У аккаунта с заявкой на удаление в phone_number лежит плейсхолдер, а настоящий номер —
    // в отдельной колонке. При обезличивании её очищают, и показывать становится нечего.
    this[UserTable.deletedPhoneNumber]?.let { return it }

    return if (this[UserTable.deletedAt] == null) {
        this[UserTable.phoneNumber]
    } else {
        DELETED_CLIENT_PHONE_TITLE
    }
}
