package wanghe.jdbc

import java.sql.*
import javax.sql.DataSource
import kotlin.concurrent.getOrSet

private val CURRENT_CONNECTION = ThreadLocal<Connection>()

fun <T> DataSource.query(sql: String, vararg params: Any?, mapper: (rs: ResultSet, index: Int) -> T): List<T> {
    connection.use { con->
        return con.query(sql, *params, mapper = mapper)
    }
}

fun <T> DataSource.query(sql: String, vararg params: Any?, extractor: (rs: ResultSet) -> T?): T? {
    connection.use { con->
        return con.query(sql, *params, extractor = extractor)
    }
}

fun <T> DataSource.first(sql: String, vararg params: Any?, mapper: (ResultSet, Int) -> T): T? {
    return query(sql, *params, extractor = {rs->
        if (rs.first()) {
            mapper(rs, 0)
        } else {
            null
        }
    })
}

fun DataSource.update(sql: String, vararg params: Any?): Int {
    connection.use { con->
        return con.update(sql, *params)
    }
}

fun DataSource.insert(sql: String, vararg params: Any?): Int {
    connection.use { con->
        return con.insert(sql, *params)
    }
}

/**
 * connectionReuse: transaction嵌套的情况，保证使用的是同一个connection，而且只让最外层处理commit和close
 */
fun <R> DataSource.transaction(block: Connection.() -> R): R {

    var connectionReuse = true

    val con = CURRENT_CONNECTION.getOrSet {
        connectionReuse = false
        connection.apply { autoCommit = false }
    }

    if (connectionReuse) {
        val savepoint = con.setSavepoint()
        try {
            return con.block()
        } catch (e: Exception) {
            con.rollback(savepoint)
            throw e
        }
    } else {
        con.use {
            try {
                val result = con.block()
                con.commit()
                return result
            } catch (e: Exception) {
                con.rollback()
                throw e
            } finally {
                CURRENT_CONNECTION.remove()
            }
        }
    }

}

/**
 * 该方法适用自增主键表插入，返回生成的自增id，如果不是这种情况，请使用update
 * @param sql
 * @param params
 *
 * @return 自增主键
 */
fun Connection.insert(sql: String, vararg params: Any?): Int {
    val statement = sql.trimIndent().replace("\n", " ")

//    if (logger.isTraceEnabled) {
//        logger.trace("[  SQL]$statement")
//        logger.trace("[Param]" + params.mapIndexed {index, value-> "[${index + 1}]$value"}.joinToString(","))
//    }

    prepareStatementWithParameter(statement, *params).use { ps->
        ps.executeUpdate()
    }

    prepareStatement("select last_insert_id() as last_insert_id").use { ps->
        ps.executeQuery().use { rs->
            rs.next()
            return rs.getInt("last_insert_id")
        }
    }
}

/**
 * @return 更新条数
 */
fun Connection.update(sql: String, vararg params: Any?): Int {

    val statement = sql.trimIndent().replace("\n", " ")

//    if (logger.isTraceEnabled) {
//        logger.trace("[  SQL]$statement")
//        logger.trace("[Param]" + params.mapIndexed {index, value-> "[${index + 1}]$value"}.joinToString(","))
//    }

    prepareStatementWithParameter(statement, *params).use { ps->

        return ps.executeUpdate()
    }
}

fun <T> Connection.query(sql: String, vararg params: Any?, extractor: (ResultSet) -> T?): T? {
    val statement = sql.trimIndent().replace("\n", " ")

//    if (logger.isTraceEnabled) {
//        logger.trace("[  SQL]$statement")
//        logger.trace("[Param]" + params.mapIndexed {index, value-> "[${index + 1}]$value"}.joinToString(","))
//    }

    prepareStatementWithParameter(statement, *params).use { ps->
        return ps.executeQuery().use(extractor)
    }
}

fun <T> Connection.query(sql: String, vararg params: Any?, mapper: (ResultSet, Int) -> T): List<T> {
    return query(sql, *params) {rs->
        mutableListOf<T>().apply {
            var index = 0
            while (rs.next()) {
                add(mapper(rs, index++))
            }
        }
    }!!
}

private fun Connection.prepareStatementWithParameter(sql: String, vararg params: Any?): PreparedStatement {
    return prepareStatement(sql).apply {
        if (params.isNotEmpty()) {
            params.forEachIndexed { index, value ->

                val paramIndex = index + 1

                if (value == null) {
                    setNull(paramIndex, Types.NULL)
                    return@forEachIndexed
                }

                when(value) {
                    is Boolean -> setBoolean(paramIndex, value)
                    is Byte -> setByte(paramIndex, value)
                    is Short -> setShort(paramIndex, value)
                    is Int -> setInt(paramIndex, value)
                    is Long -> setLong(paramIndex, value)
                    is Float -> setFloat(paramIndex, value)
                    is Double -> setDouble(paramIndex, value)
                    is String, Char -> setString(paramIndex, value.toString())
                    is ByteArray -> setBytes(paramIndex, value)
                    is Timestamp -> setTimestamp(paramIndex, value)
                    is java.sql.Date -> setDate(paramIndex, value)
                    is Time -> setTime(paramIndex, value)
                    is java.util.Date -> setTimestamp(paramIndex, Timestamp(value.time))
                    is Collection<*> -> setString(paramIndex, value.joinToString(","))
                    else -> throw SQLDataException("unknown type [$paramIndex]${value.javaClass}")
                }
            }

        }
    }
}