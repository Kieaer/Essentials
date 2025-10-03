package ksp.table

/**
 * Annotation to generate code for data classes.
 * 
 * When applied to a data class, this annotation will generate:
 * 1. An extension function for the corresponding table class to convert a ResultRow to the data class
 * 2. An extension function for ResultRow to convert it to the data class (using the table's toData method)
 * 3. An extension function for Query to map query results to a list of data class instances
 * 4. A function for the corresponding table class to create a data class instance from the ID returned by insertReturning
 * 5. An extension function for the data class to update the corresponding database record
 * 
 * Example usage:
 * ```
 * @GenerateCode
 * data class MyData(
 *     val id: UInt,
 *     var name: String,
 *     var value: Int
 * )
 * ```
 * 
 * Generated code:
 * ```
 * fun MyDataTable.toData(row: ResultRow): MyData {
 *     val id = row[MyDataTable.id]
 *     val name = row[MyDataTable.name]
 *     val value = row[MyDataTable.value]
 *     
 *     return MyData(
 *         id = id,
 *         name = name,
 *         value = value
 *     )
 * }
 * 
 * fun ResultRow.toMyData(): MyData {
 *     return MyDataTable.toData(this)
 * }
 * 
 * fun Query.mapToMyDataList(): List<MyData> {
 *     return this.map { MyDataTable.toData(it) }
 * }
 * 
 * fun <T> MyDataTable.fromInsertReturning(id: T): MyData {
 *     return transaction {
 *         val query = MyDataTable.selectAll()
 *         when (id) {
 *             is Int -> query.where { MyDataTable.id eq id.toUInt() }
 *             is UInt -> query.where { MyDataTable.id eq id }
 *             else -> throw IllegalArgumentException("Unsupported ID type: ${id!!::class.java}")
 *         }
 *         query.map { row -> row.toMyData() }.first()
 *     }
 * }
 * 
 * fun MyData.update(): Boolean {
 *     return transaction {
 *         MyDataTable.update({ MyDataTable.id eq this@update.id }) {
 *             it[MyDataTable.name] = this@update.name
 *             it[MyDataTable.value] = this@update.value
 *         } > 0
 *     }
 * }
 * ```
 * 
 * The generated code creates local variables for each property value from the Table class before using them to create the data class instance.
 * This allows for checking or transforming the values if needed before using them to create the data class instance.
 * 
 * These functions are useful for mapping values obtained from database queries, including those from insertReturning and updateReturning operations.
 * For example, you can use the fromInsertReturning function to create a data class instance from the ID returned by insertReturning:
 * 
 * ```
 * val id = suspendTransaction {
 *     MyDataTable.insertReturning {
 *         it[MyDataTable.name] = "John"
 *         it[MyDataTable.value] = 42
 *     }
 * }
 * 
 * val data = MyDataTable.fromInsertReturning(id)
 * ```
 * 
 * This is much more convenient than the previous approach, which required a separate query:
 * 
 * ```
 * val id = suspendTransaction {
 *     MyDataTable.insertReturning {
 *         it[MyDataTable.name] = "John"
 *         it[MyDataTable.value] = 42
 *     }
 * }
 * 
 * val data = suspendTransaction {
 *     MyDataTable.select { MyDataTable.id eq id }
 *         .map { it.toMyData() }
 *         .first()
 * }
 * ```
 */
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.SOURCE)
annotation class GenerateCode()
