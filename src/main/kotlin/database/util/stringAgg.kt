package database.util

import org.jetbrains.exposed.v1.core.*
import org.jetbrains.exposed.v1.core.Function

fun Column<String>.stringAgg(separator: String = ",") =
    CustomFunction("STRING_AGG", columnType = TextColumnType(), this, stringLiteral(separator))

class CustomJsonbAgg(
    val table: Table,
) : Function<String>(TextColumnType()) {
    override fun toQueryBuilder(queryBuilder: QueryBuilder) {
        queryBuilder {
            append("jsonb_agg(${table.tableName})")
        }
    }
}