# 将Jdbc.kt复制到项目中，DataSource对象可以直接调用insert、query...

```kotlin
val lookup: Lookup? = Ds.first("""
    select id,type,value,text,inactive
    from sys_lookup
    where id=?
""", id) {rs, _->
    // mapping
    Lookup(rs.getInt("id"), rs.getString("type"), rs.getString("value"), rs.getString("text"), rs.getBoolean("inactive"))
}
```

```kotlin
val lookups: List<Lookup> = Ds.query("""
    select id,type,value,text,inactive
    from sys_lookup
    where type=?
    and inactive=?
""", type, false) {rs, _->
    // map to obj
    Lookup(rs.getInt("id"), rs.getString("type"), rs.getString("value"), rs.getString("text"), rs.getBoolean("inactive"))
}
```

```kotlin
val lookup = Lookup(..)
val operator = ... // created,created_by,updated,updated_by

lookup.id = Ds.insert("""
    insert into sys_lookup(created, created_by, updated, updated_by, type, value, text, inactive)
    values(?,?,?,?,?,?,?,?)
""", operator.timestamp, operator.id, operator.timestamp, operator.id, Lookup.type, Lookup.value, Lookup.text, Lookup.inactive)
```

```kotlin
Ds.update("delete from sys_lookup where id=?", id)
```

# Smaple

- DataSource

```kotlin
object Ds : BasicDataSource() {
    init {
        url = "jdbc:mysql://localhost:3306/teamwork_lumen?useSSL=false"
        driverClassName = "com.mysql.jdbc.Driver"
        username = "root"
        password = ""
    }
}
```
- domain/model

```kotlin
data class Lookup (
        var id: Int,
        var type: String,
        var value: String,
        var text: String,
        var inactive: Boolean
)
```

- DAO

```kotlin
object LookupDao {
    fun get(id: Int): Lookup? {
        return Ds.first("""
            select id,type,value,text,inactive
            from sys_lookup
            where id=?
        """, id) {rs, _->
            Lookup(rs.getInt("id"), rs.getString("type"), rs.getString("value"), rs.getString("text"), rs.getBoolean("inactive"))
        }
    }

    fun listByType(type: String): List<Lookup> {
        return Ds.query("""
            select id,type,value,text,inactive
            from sys_lookup
            where type=?
            and inactive=?
        """, type, false) {rs, _->
            Lookup(rs.getInt("id"), rs.getString("type"), rs.getString("value"), rs.getString("text"), rs.getBoolean("inactive"))
        }
    }

    fun create(Lookup: Lookup, operator: Operator) {
        Lookup.id = Ds.insert("""
                insert into sys_lookup(created, created_by, updated, updated_by, type, value, text, inactive)
                values(?,?,?,?,?,?,?,?)
            """, operator.timestamp, operator.id, operator.timestamp, operator.id, Lookup.type, Lookup.value, Lookup.text, Lookup.inactive)
    }

    fun delete(id: Int) {
        Ds.update("delete from sys_lookup where id=?", id)
    }

    fun deleteByType(type: String): Int {
        return Ds.update("delete from sys_lookup where type=?", type)
    }

    fun update(id: Int, cv: MutableMap<String, Any?>, operator: Operator) {

        cv["updated"] = operator.timestamp
        cv["updated_by"] = operator.id

        val args = mutableListOf<Any?>()
        val sql = buildString {
            append("update sys_lookup set ")

            var i = 0
            cv.forEach { t, u ->
                if (i++ > 0) {
                    append(",")
                }
                append("$t=?")
                args.add(u)
            }

            append(" where id=?")
            args.add(id)
        }

        Ds.update(sql, *(args.toTypedArray()))
    }
}
```

- tests
```kotlin
val operator = object : Operator {
    override val id: Int = 1
    override val timestamp = Date()
}

val Lookups = listOf(
        Lookup(0, "genders", "1", "未知", false),
        Lookup(0, "genders", "2", "男", false),
        Lookup(0, "genders", "3", "女", false)
)

Lookups.forEach { Lookup ->
    LookupDao.create(Lookup, operator)
}

LookupDao.listByType("genders").forEach { Lookup->
    println(Lookup)
}

LookupDao.deleteByType("genders")
```