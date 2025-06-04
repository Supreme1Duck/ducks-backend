package util

import org.jetbrains.exposed.sql.Column
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.VarCharColumnType
import org.jetbrains.exposed.sql.statements.api.PreparedStatementApi
import org.postgresql.util.PGobject

fun Table.citext(name: String, length: Int): Column<String> =
    registerColumn(name, CitextColumnType(length))

class CitextColumnType(
    colLength: Int
) : VarCharColumnType(colLength) {
    override fun sqlType(): String = "CITEXT"

    override fun setParameter(stmt: PreparedStatementApi, index: Int, value: Any?) {
        val parameterValue: PGobject? = value?.let {
            PGobject().apply {
                type = sqlType()
                this.value = value as? String
            }
        }
        super.setParameter(stmt, index, parameterValue)
    }
}
