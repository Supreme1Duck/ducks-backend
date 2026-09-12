package com.ducks.features.user.database

import org.jetbrains.exposed.v1.core.dao.id.LongIdTable

object UserTable: LongIdTable("ducks_user_table") {

    // Временно клиент входит анонимно, и у новых аккаунтов номера нет.
    // TODO вернуть not null, когда вернётся вход по номеру телефона.
    val phoneNumber = text("phone_number").nullable().uniqueIndex()
    val name = text("name").nullable()
    val secondName = text("second_name").nullable()
    val fcmToken = text("fcm_token").nullable()

    // Заявка на удаление аккаунта: доступ закрывается сразу и навсегда, номер освобождается.
    val deletionRequestedAt = long("deletion_requested_at").nullable()
    // Момент, когда строку обезличили окончательно: персональных данных в ней не осталось.
    val deletedAt = long("deleted_at").nullable()
    // Настоящий номер удаляемого аккаунта — живёт от заявки до обезличивания.
    // Вне уникального индекса, чтобы не мешать регистрации нового аккаунта на тот же номер.
    val deletedPhoneNumber = text("deleted_phone_number").nullable()
}
