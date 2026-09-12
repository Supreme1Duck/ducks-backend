package com.ducks.features.cashregister

import com.ducks.util.DucksBadRequestError
import kotlinx.coroutines.*
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.junit.After
import org.junit.Assume.assumeTrue
import org.junit.Before
import java.util.UUID
import kotlin.test.*

/** Запускается на отдельном PostgreSQL, см. docs/alfa-cash-register.md. */
class AlfaCashRegisterRepositoryTest {
    private val repository = AlfaCashRegisterRepository()
    private val schema = "alfa_test_" + UUID.randomUUID().toString().replace("-", "")
    private var database: Database? = null

    @Before
    fun setup() {
        val url = System.getenv("DUCKS_ALFA_TEST_DATABASE_URL")
        assumeTrue("Для интеграционных тестов нужен отдельный PostgreSQL", !url.isNullOrBlank())
        require(url!!.substringBefore('?').endsWith("/ducks_alfa_test"))
        val db = Database.connect(url + if ('?' in url) "&currentSchema=$schema" else "?currentSchema=$schema",
            driver = "org.postgresql.Driver", user = "ducks_alfa_test", password = "")
        database = db
        transaction(db) {
            exec("CREATE SCHEMA $schema")
            exec("CREATE TABLE ducks_coffee_shop_table (id bigint PRIMARY KEY)")
            exec("""CREATE TABLE ducks_coffee_orders_table (
                id bigint PRIMARY KEY, coffee_shop_id bigint NOT NULL REFERENCES ducks_coffee_shop_table(id),
                accepted_timestamp bigint, ready_timestamp bigint DEFAULT 2, finished_time bigint DEFAULT 3,
                "isCancelledBySeller" boolean NOT NULL DEFAULT false,
                "isCancelledByClient" boolean NOT NULL DEFAULT false,
                "isExpired" boolean NOT NULL DEFAULT false, "isNotPickedUp" boolean NOT NULL DEFAULT false, price numeric(15,2),
                is_takeaway boolean NOT NULL DEFAULT false, comment text)""")
            exec("""CREATE TABLE ducks_coffee_ordered_products_table (
                id bigint PRIMARY KEY, order_id bigint, product_id bigint, product_name text,
                selected_size json, constructors json, quantity integer, price numeric(15,2))""")
            val migration = checkNotNull(javaClass.getResource("/db/migration/V211__alfa_cash_register.sql")).readText()
            exec(migration.replace("public.", "$schema."))
            exec("INSERT INTO ducks_coffee_shop_table VALUES (1), (2)")
            exec("INSERT INTO ducks_coffee_orders_table (id, coffee_shop_id, accepted_timestamp, price) VALUES (11,1,1,13.40), (12,1,1,13.40), (21,2,1,13.40)")
            for (id in listOf(11, 12, 21)) {
                exec("""INSERT INTO ducks_coffee_ordered_products_table VALUES
                    ($id,$id,42,'Капучино','{"id":"large","sizeName":"Большой","sizeValue":"350 мл","price":"6.70"}',null,2,13.40)""")
            }
        }
    }

    @After
    fun cleanup() {
        database?.let { db -> transaction(db) { exec("DROP SCHEMA $schema CASCADE") } }
    }

    private fun enqueue(orderId: Long = 11, shopId: Long = 1) = transaction(database!!) {
        repository.enqueueCompletedOrder(orderId, shopId)
    }

    private suspend fun enable() = repository.setSettings(1, AlfaCashRegisterSettings(true, "Ducks"))
    private fun token() = UUID.randomUUID().toString()

    @Test
    fun `удаление чаевых сохраняет итог старого заказа и читаемость данных кассы`() = runBlocking<Unit> {
        enable()
        enqueue()
        val owner = token()
        val original = repository.startOrderSendingToCashRegister(1, 11, AlfaClaimRequest(owner))
        transaction(database!!) {
            exec("ALTER TABLE ducks_coffee_orders_table ADD COLUMN tips numeric(15,2), ADD COLUMN total_price numeric(15,2)")
            exec("UPDATE ducks_coffee_orders_table SET tips=2.00, total_price=15.40 WHERE id=11")
            exec("""UPDATE ducks_alfa_order_transfers SET payload=(payload::jsonb || '{"tips":"2.00"}'::jsonb)::text WHERE order_id=11""")
            val migration = checkNotNull(javaClass.getResource("/db/migration/V213__remove_order_tips.sql")).readText()
            exec(migration.replace("public.", "$schema."))
            assertEquals("15.40", exec("SELECT total_price FROM ducks_coffee_orders_table WHERE id=11") {
                it.next()
                it.getBigDecimal(1).toPlainString()
            })
            assertEquals(0L, exec("SELECT count(*) FROM information_schema.columns WHERE table_schema='$schema' AND table_name='ducks_coffee_orders_table' AND column_name='tips'") {
                it.next()
                it.getLong(1)
            })
        }
        assertEquals(original, repository.startOrderSendingToCashRegister(1, 11, AlfaClaimRequest(owner)))
    }

    @Test
    fun `выданный заказ передаётся в кассу несмотря на finishedTime`() = runBlocking<Unit> {
        enable()
        enqueue()
        assertEquals(AlfaTransferState.PENDING, repository.listTransfers(1).single().state)
        val sendingResult = repository.startOrderSendingToCashRegister(1, 11, AlfaClaimRequest(token()))
        assertTrue(sendingResult.transfer.sourceFinished)
        assertFalse(sendingResult.transfer.sourceCancelled)
    }

    @Test
    fun `принятые отменённые и незабранные заказы не ставятся в очередь`() = runBlocking<Unit> {
        enable()
        for (assignment in listOf("finished_time=null", "ready_timestamp=null", "accepted_timestamp=null",
            "\"isCancelledBySeller\"=true", "\"isCancelledByClient\"=true", "\"isExpired\"=true", "\"isNotPickedUp\"=true")) {
            transaction(database!!) {
                exec("""UPDATE ducks_coffee_orders_table SET accepted_timestamp=1, ready_timestamp=2, finished_time=3,
                    "isCancelledBySeller"=false, "isCancelledByClient"=false, "isExpired"=false, "isNotPickedUp"=false WHERE id=11""")
                exec("UPDATE ducks_coffee_orders_table SET $assignment WHERE id=11")
            }
            assertFailsWith<DucksBadRequestError> { enqueue() }
            assertTrue(repository.listTransfers(1).isEmpty())
        }
    }

    @Test
    fun `подключение выборочное и очередь откатывается вместе с транзакцией`() = runBlocking<Unit> {
        assertFalse(repository.getSettings(1).enabled)
        enqueue()
        assertTrue(repository.listTransfers(1).isEmpty())
        enable()
        assertFalse(repository.getSettings(2).enabled)
        assertFailsWith<IllegalStateException> {
            transaction(database!!) {
                repository.enqueueCompletedOrder(11, 1)
                error("rollback")
            }
        }
        assertTrue(repository.listTransfers(1).isEmpty())
        enqueue()
        enqueue()
        assertEquals(1, repository.listTransfers(1).size)
        assertTrue(repository.listTransfers(2).isEmpty())
    }

    @Test
    fun `чужая кофейня не читает не забирает и не подтверждает задание`() = runBlocking<Unit> {
        enable()
        enqueue()
        val owner = token()
        repository.startOrderSendingToCashRegister(1, 11, AlfaClaimRequest(owner))
        assertFailsWith<DucksBadRequestError> { repository.getTransfer(2, 11) }
        assertFailsWith<DucksBadRequestError> { repository.startOrderSendingToCashRegister(2, 11, AlfaClaimRequest(owner)) }
        assertFailsWith<DucksBadRequestError> {
            repository.reportOrderSendingResult(2, 11, AlfaReportRequest(owner, AlfaTransferResult.TRANSFERRED, 123))
        }
    }

    @Test
    fun `два исполнителя не забирают один заказ и таймаут не сбрасывает владельца`() = runBlocking<Unit> {
        enable()
        enqueue()
        val outcomes = (1..2).map {
            async(Dispatchers.IO) {
                val token = token()
                runCatching { repository.startOrderSendingToCashRegister(1, 11, AlfaClaimRequest(token)); token }
            }
        }.awaitAll()
        assertEquals(1, outcomes.count { it.isSuccess }, outcomes.toString())
        val owner = outcomes.single { it.isSuccess }.getOrThrow()
        val first = repository.startOrderSendingToCashRegister(1, 11, AlfaClaimRequest(owner))
        val second = repository.startOrderSendingToCashRegister(1, 11, AlfaClaimRequest(owner))
        assertEquals(first, second)
        repository.reportOrderSendingResult(1, 11, AlfaReportRequest(owner, AlfaTransferResult.NEEDS_REVIEW))
        enqueue(12)
        assertFailsWith<DucksBadRequestError> { repository.startOrderSendingToCashRegister(1, 12, AlfaClaimRequest(token())) }
        repository.reportOrderSendingResult(1, 11, AlfaReportRequest(owner, AlfaTransferResult.CANCELLED))
        assertEquals(AlfaTransferState.IN_PROGRESS, repository.startOrderSendingToCashRegister(1, 12, AlfaClaimRequest(token())).transfer.state)
    }

    @Test
    fun `одна касса не получает два разных задания одновременно`() = runBlocking<Unit> {
        enable()
        enqueue(11)
        enqueue(12)
        val results = listOf(11L, 12L).map { id ->
            async(Dispatchers.IO) { runCatching { repository.startOrderSendingToCashRegister(1, id, AlfaClaimRequest(token())) } }
        }.awaitAll()
        assertEquals(1, results.count { it.isSuccess })
        assertEquals(1, repository.listTransfers(1).count { it.state == AlfaTransferState.IN_PROGRESS })
    }

    @Test
    fun `повторный запрос на начало отправки сохраняет снимок и не принимает чужой токен`() = runBlocking<Unit> {
        enable()
        enqueue()
        val owner = token()
        val original = repository.startOrderSendingToCashRegister(1, 11, AlfaClaimRequest(owner))
        transaction(database!!) {
            exec("UPDATE ducks_coffee_ordered_products_table SET product_name='Изменённое название' WHERE order_id=11")
        }
        assertEquals(original, repository.startOrderSendingToCashRegister(1, 11, AlfaClaimRequest(owner)))
        assertFailsWith<DucksBadRequestError> { repository.startOrderSendingToCashRegister(1, 11, AlfaClaimRequest(token())) }
        assertFailsWith<DucksBadRequestError> { repository.startOrderSendingToCashRegister(1, 11, AlfaClaimRequest("not-a-uuid")) }
    }

    @Test
    fun `номер кассы сохраняется а подтверждение повторяется без смены результата`() = runBlocking<Unit> {
        enable()
        enqueue()
        val owner = token()
        repository.startOrderSendingToCashRegister(1, 11, AlfaClaimRequest(owner))
        repository.reportOrderSendingResult(1, 11, AlfaReportRequest(owner, AlfaTransferResult.CREATED, 123))
        assertEquals(123L, repository.startOrderSendingToCashRegister(1, 11, AlfaClaimRequest(owner)).transfer.cashboxOrderNumber)
        assertFailsWith<DucksBadRequestError> {
            repository.reportOrderSendingResult(1, 11, AlfaReportRequest(owner, AlfaTransferResult.TRANSFERRED, 124))
        }
        val report = AlfaReportRequest(owner, AlfaTransferResult.TRANSFERRED, 123)
        assertEquals(repository.reportOrderSendingResult(1, 11, report), repository.reportOrderSendingResult(1, 11, report))
        assertTrue(repository.listTransfers(1).isEmpty())
        assertFailsWith<DucksBadRequestError> {
            repository.reportOrderSendingResult(1, 11, AlfaReportRequest(owner, AlfaTransferResult.NEEDS_REVIEW))
        }
    }

    @Test
    fun `отключение и отмена заказа запрещают новую передачу но сохраняют начатую`() = runBlocking<Unit> {
        enable()
        enqueue()
        repository.setSettings(1, AlfaCashRegisterSettings(false))
        assertFailsWith<DucksBadRequestError> { repository.startOrderSendingToCashRegister(1, 11, AlfaClaimRequest(token())) }
        enable()
        transaction(database!!) { exec("""UPDATE ducks_coffee_orders_table SET "isCancelledBySeller"=true WHERE id=11""") }
        assertTrue(repository.listTransfers(1).isEmpty())
        assertEquals(AlfaTransferState.CANCELLED, repository.getTransfer(1, 11).state)
        assertFailsWith<DucksBadRequestError> { repository.startOrderSendingToCashRegister(1, 11, AlfaClaimRequest(token())) }
        enqueue(12)
        val owner = token()
        repository.startOrderSendingToCashRegister(1, 12, AlfaClaimRequest(owner))
        transaction(database!!) { exec("""UPDATE ducks_coffee_orders_table SET "isCancelledBySeller"=true WHERE id=12""") }
        assertTrue(repository.startOrderSendingToCashRegister(1, 12, AlfaClaimRequest(owner)).transfer.sourceCancelled)
    }
}
