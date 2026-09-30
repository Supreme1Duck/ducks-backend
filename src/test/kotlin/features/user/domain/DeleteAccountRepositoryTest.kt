package com.ducks.features.user.domain

import com.ducks.features.user.data.dto.AccountDeletionDTO
import com.ducks.features.user.database.UserTable
import com.ducks.util.DucksBadRequestError
import kotlinx.coroutines.runBlocking
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.insertAndGetId
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.junit.After
import org.junit.Assume.assumeTrue
import org.junit.Before
import java.util.UUID
import kotlin.test.*

/**
 * Удаление анонимного аккаунта. Запускается на отдельном PostgreSQL — схема своя на каждый
 * прогон и удаляется в конце:
 *
 *     DUCKS_USER_TEST_DATABASE_URL=jdbc:postgresql://localhost:5432/ducks_user_test ./gradlew test
 *
 * Без переменной тест пропускается, как и интеграционные тесты кассы.
 */
class DeleteAccountRepositoryTest {

    private val repository = DeleteAccountRepository(AccountDeletionConfirmations())
    private val schema = "user_test_" + UUID.randomUUID().toString().replace("-", "")
    private var database: Database? = null

    @Before
    fun setup() {
        val url = System.getenv("DUCKS_USER_TEST_DATABASE_URL")
        assumeTrue("Для интеграционных тестов нужен отдельный PostgreSQL", !url.isNullOrBlank())
        require(url!!.substringBefore('?').endsWith("/ducks_user_test"))

        val db = Database.connect(
            url + if ('?' in url) "&currentSchema=$schema" else "?currentSchema=$schema",
            driver = "org.postgresql.Driver",
        )
        database = db

        transaction(db) {
            exec("CREATE SCHEMA $schema")
            // Только те колонки, которые читает и пишет удаление аккаунта.
            exec(
                """CREATE TABLE ducks_user_table (
                    id bigserial PRIMARY KEY,
                    phone_number text UNIQUE,
                    name text,
                    second_name text,
                    fcm_token text,
                    deletion_requested_at bigint,
                    deleted_at bigint,
                    deleted_phone_number text)"""
            )
            exec(
                """CREATE TABLE ducks_coffee_orders_table (
                    id bigserial PRIMARY KEY,
                    user_id bigint NOT NULL REFERENCES ducks_user_table(id),
                    finished_time bigint)"""
            )
        }
    }

    @After
    fun tearDown() {
        database?.let { transaction(it) { exec("DROP SCHEMA IF EXISTS $schema CASCADE") } }
    }

    private fun insertUser(phone: String? = null, name: String? = null, fcm: String? = "token"): Long =
        transaction(database!!) {
            UserTable.insertAndGetId {
                it[phoneNumber] = phone
                it[UserTable.name] = name
                it[fcmToken] = fcm
            }.value
        }

    private fun row(userId: Long) = transaction(database!!) {
        UserTable.selectAll().where { UserTable.id eq userId }.single()
    }

    /**
     * Ошибка внутри newSuspendedTransaction валит объемлющий runBlocking целиком, а не
     * только сам вызов, поэтому у каждого вызова свой runBlocking — иначе ожидаемая
     * DucksBadRequestError пролетала бы мимо assertFailsWith.
     */
    private fun deleteAnonymous(userId: Long): AccountDeletionDTO = runBlocking {
        repository.requestAnonymousDeletion(userId)
    }

    @Test
    fun `анонимный аккаунт удаляется без подтверждения и сразу обезличивается`() {
        val userId = insertUser(name = "Старое имя")

        val deletion = deleteAnonymous(userId)

        assertEquals(deletion.requestedAt, deletion.dataRemovalAt, "отсрочки у анонимного аккаунта нет")

        val user = row(userId)
        assertEquals(deletion.requestedAt, user[UserTable.deletionRequestedAt])
        assertEquals(deletion.requestedAt, user[UserTable.deletedAt], "строка обезличена сразу")
        assertNull(user[UserTable.name])
        assertNull(user[UserTable.fcmToken])
    }

    @Test
    fun `аккаунт с номером телефона так удалить нельзя`() {
        val userId = insertUser(phone = "375291234567")

        val error = assertFailsWith<DucksBadRequestError> { deleteAnonymous(userId) }
        assertEquals("Удаление этого аккаунта подтверждается кодом из СМС", error.message)

        // Аккаунт остался нетронутым: код из СМС обойти не удалось.
        assertNull(row(userId)[UserTable.deletionRequestedAt])
    }

    @Test
    fun `незавершённый заказ не даёт удалить аккаунт`() {
        val userId = insertUser()
        transaction(database!!) {
            exec("INSERT INTO ducks_coffee_orders_table (user_id, finished_time) VALUES ($userId, NULL)")
        }

        val error = assertFailsWith<DucksBadRequestError> { deleteAnonymous(userId) }
        assertEquals("Сначала завершите или отмените текущий заказ", error.message)
        assertNull(row(userId)[UserTable.deletionRequestedAt])
    }

    @Test
    fun `завершённый заказ удалению не мешает`() {
        val userId = insertUser()
        transaction(database!!) {
            exec("INSERT INTO ducks_coffee_orders_table (user_id, finished_time) VALUES ($userId, 100)")
        }

        deleteAnonymous(userId)

        assertNotNull(row(userId)[UserTable.deletionRequestedAt])
    }

    @Test
    fun `повторное удаление отвечает ошибкой`() {
        val userId = insertUser()
        deleteAnonymous(userId)

        val error = assertFailsWith<DucksBadRequestError> { deleteAnonymous(userId) }
        assertEquals("Аккаунт уже удалён", error.message)
    }

    @Test
    fun `несуществующий аккаунт отвечает ошибкой`() {
        val error = assertFailsWith<DucksBadRequestError> { deleteAnonymous(4242) }
        assertEquals("Аккаунт не найден", error.message)
    }
}
