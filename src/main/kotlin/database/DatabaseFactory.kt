package database

import org.flywaydb.core.Flyway
import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.jetbrains.exposed.v1.migration.MigrationUtils
import org.reflections.Reflections
import java.io.File
import java.security.MessageDigest

object DatabaseFactory {

    private const val DB_URL = "jdbc:postgresql://localhost:5432/ducksdatabase"
    private const val DB_USER = "andrewutko"

    private const val MIGRATION_DIR = "src/main/resources/db/migration"

    /**
     * Версия, на которой стоит схема, приведённая руками 21.07.2026. Всё, что не новее её,
     * считается уже применённым. Менять не нужно: новые миграции просто пойдут с V201.
     */
    private const val BASELINE_VERSION = "200"

    /**
     * Генерация скриптов сравнивает Kotlin-таблицы с реальной схемой и пишет новые .sql
     * в исходники, поэтому запускать её можно только на машине разработчика.
     *
     * На сервере исходников нет: папка создавалась заново рядом с jar, нумерация файлов
     * начиналась с единицы и расходилась с [flyway_schema_history], из-за чего Flyway
     * молча пропускал все свежие миграции.
     */
    private val shouldGenerateMigrations: Boolean
        get() = System.getenv("DUCKS_GENERATE_MIGRATIONS")?.toBooleanStrictOrNull() == true

    fun init(password: String) {
        Database.connect(
            url = DB_URL,
            driver = "org.postgresql.Driver",
            user = DB_USER,
            password = password
        )
        initMigrations(password = password)
    }

    private fun initMigrations(password: String) {
        if (shouldGenerateMigrations) {
            transaction {
                val allTables = findAllTables()
                println("All tables -> ${allTables.map { it.tableName }}")

                generateMigrationScripts(tables = allTables)
            }
        }

        migrate(password)
    }

    /**
     * Файлы миграций — это инкрементальный diff, который генератор снимал с живой dev-базы,
     * а не воспроизводимая с нуля история: на пустой схеме набор падает на V91 (там FK на
     * таблицу, которая создаётся более поздней миграцией). Поэтому базу нельзя поднять
     * прогоном всех миграций подряд — существующая схема помечается baseline'ом, и реально
     * применяются только миграции новее [BASELINE_VERSION].
     *
     * baselineOnMigrate срабатывает лишь когда схема непустая, а flyway_schema_history нет,
     * так что на машинах с уже заполненной историей это ничего не меняет.
     */
    private fun migrate(password: String) {
        val flyway = Flyway.configure()
            .dataSource(DB_URL, DB_USER, password)
            // Только classpath: так и dev, и прод применяют один и тот же набор файлов из jar.
            .locations("classpath:db/migration")
            .baselineOnMigrate(true)
            .baselineVersion(BASELINE_VERSION)
            .load()

        flyway.migrate()
    }

    private fun generateMigrationScripts(tables: List<Table>) {
        val statements = MigrationUtils.statementsRequiredForDatabaseMigration(*tables.toTypedArray())
        val migrationDir = MIGRATION_DIR

        File(migrationDir).mkdirs()

        val existingHashes = getExistingHashes(migrationDir)

        val maxVersionOnDisk = File(migrationDir)
            .listFiles { file -> file.name.endsWith(".sql") }
            ?.mapNotNull { file ->
                file.name.removePrefix("V").substringBefore("__").toIntOrNull()
            }?.maxOrNull() ?: 0

        // Нумерация обязана начинаться выше baseline: всё, что не больше [BASELINE_VERSION],
        // Flyway на существующих базах считает уже применённым и пропускает молча — без ошибки
        // и без предупреждения. После squash'а на диске лежит только V1, поэтому без этого
        // maxOf генератор выдал бы V2, и миграция никуда бы не поехала.
        var lastMigrationVersion = maxOf(maxVersionOnDisk, BASELINE_VERSION.toInt())

        println("lastMigrationVersion - $lastMigrationVersion (на диске $maxVersionOnDisk)")

        statements.forEach { statement ->
            val hash = calculateHash(statement)

            if (hash !in existingHashes) {
                ++lastMigrationVersion
                val fileName = "$migrationDir/V${lastMigrationVersion}__Migration_hsh=$hash.sql"
                File(fileName).writeText(statement)
                println("Migration file created: $fileName")
            } else {
                println("Migration file with hash $hash already exists!")
            }
        }
    }

    private fun getExistingHashes(migrationDir: String): List<String> {
        return File(migrationDir)
            .listFiles { file -> file.name.endsWith(".sql") }
            ?.mapNotNull { it.name.substringAfter("hsh=").substringBefore(".sql") }
            ?.onEach { println("Existing hash -> $it") }
            ?: emptyList()
    }

    private fun calculateHash(input: String): String {
        val digest = MessageDigest.getInstance("SHA-256").digest(input.toByteArray())
        return digest.joinToString("") { "%02x".format(it) }.take(8) // Берем первые 8 символов хэша
    }

    /**
     * Рефлексивно находит всех наследников [Table]
     * То есть все классы таблиц бд
     */
    private fun findAllTables(): List<Table> {
        val reflections = Reflections("com.ducks")
        return reflections.getSubTypesOf(Table::class.java)
            .mapNotNull { clazz ->
                clazz.kotlin.objectInstance
            }
    }
}