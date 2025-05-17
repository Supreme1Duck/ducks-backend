package database

import MigrationUtils
import org.flywaydb.core.Flyway
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.transactions.transaction
import org.reflections.Reflections
import java.io.File
import java.security.MessageDigest

object DatabaseFactory {
    fun init(password: String) {
        Database.connect(
            url = "jdbc:postgresql://localhost:5432/ducksdatabase",
            driver = "org.postgresql.Driver",
            user = "andrewutko",
            password = password
        )
        initMigrations(password = password)
    }

    private fun initMigrations(password: String) {
        transaction {
            val allTables = findAllTables()
            println("All tables -> ${allTables.map { it.tableName }}")

            generateMigrationScripts(tables = allTables)
            migrate(password)

            // Какая-то хуйня с Flyway, при первом запуске может не создать таблицы, приходится писать это.
            SchemaUtils.create(*allTables.toTypedArray())
        }
    }

    private fun migrate(password: String) {
        val flyway = Flyway.configure()
            .dataSource("jdbc:postgresql://localhost:5432/ducksdatabase", "andrewutko", password)
            .locations("classpath:db/migration")
            .load()

        flyway.migrate()
    }

    private fun generateMigrationScripts(tables: List<Table>) {
        val statements = MigrationUtils.statementsRequiredForDatabaseMigration(*tables.toTypedArray())
        val migrationDir = "src/main/resources/db/migration"

        File(migrationDir).mkdirs()

        val existingHashes = getExistingHashes(migrationDir)

        var lastMigrationVersion = existingHashes.count()

        println("lastMigrationVersion - $lastMigrationVersion")

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