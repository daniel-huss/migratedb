package migratedb.v1.integrationtest.database

import migratedb.v1.core.api.internal.jdbc.JdbcTemplate
import migratedb.v1.core.internal.database.db2.DB2Database
import migratedb.v1.core.internal.database.db2.DB2Schema
import migratedb.v1.core.internal.exception.MigrateDbSqlException
import migratedb.v1.core.internal.jdbc.JdbcUtils
import java.sql.ResultSet
import java.sql.SQLException

internal class Db2SchemaDropper(
    private val schema: DB2Schema,
    private val database: DB2Database,
    private val jdbcTemplate: JdbcTemplate
) {
    fun drop() {
        if (!schema.exists()) return
        dropContent()
        jdbcTemplate.execute("DROP SCHEMA " + database.quote(schema.name) + " RESTRICT")
    }

    private fun dropContent() {
        // MQTs are dropped when the backing views or tables are dropped
        // Indexes in DB2 are dropped when the corresponding table is dropped

        // drop versioned table link -> not supported for DB2 9.x
        val dropVersioningStatements = generateDropVersioningStatement()
        if (dropVersioningStatements.isNotEmpty()) {
            // Do an explicit drop of MQTs in order to be able to drop the Versioning
            for (dropTableStatement in generateDropStatements("S", "TABLE")) {
                jdbcTemplate.execute(dropTableStatement)
            }
        }
        for (dropVersioningStatement in dropVersioningStatements) {
            jdbcTemplate.execute(dropVersioningStatement)
        }

        // views
        for (dropStatement in generateDropStatementsForViews()) {
            jdbcTemplate.execute(dropStatement)
        }

        // aliases
        for (dropStatement in generateDropStatements("A", "ALIAS")) {
            jdbcTemplate.execute(dropStatement)
        }

        // temporary Tables
        for (dropStatement in generateDropStatements("G", "TABLE")) {
            jdbcTemplate.execute(dropStatement)
        }
        for (table in schema.allTables()) {
            dropTable(table.name)
        }

        // sequences
        for (dropStatement in generateDropStatementsForSequences()) {
            jdbcTemplate.execute(dropStatement)
        }

        // procedures
        for (dropStatement in generateDropStatementsForProcedures()) {
            jdbcTemplate.execute(dropStatement)
        }

        // triggers
        for (dropStatement in generateDropStatementsForTriggers()) {
            jdbcTemplate.execute(dropStatement)
        }

        // modules
        for (dropStatement in generateDropStatementsForModules()) {
            jdbcTemplate.execute(dropStatement)
        }
        for (function in schema.allFunctions()) {
            dropFunction(function.toString())
        }
        for (type in allTypes()) {
            dropType(type)
        }
    }


    private fun allTypes(): List<String> {
        var resultSet: ResultSet? = null
        return try {
            resultSet = database.jdbcMetaData.getUDTs(null, schema.name, null, null)
            val types = mutableListOf<String>()
            while (resultSet.next()) {
                types.add(resultSet.getString("TYPE_NAME"))
            }
            types
        } catch (e: SQLException) {
            throw MigrateDbSqlException("Unable to retrieve all types in schema $this", e)
        } finally {
            JdbcUtils.closeResultSet(resultSet)
        }
    }


    /**
     * Generates DROP statements for the procedures in this schema.
     */
    private fun generateDropStatementsForProcedures(): List<String> {
        val dropProcGenQuery =
            "select SPECIFICNAME from SYSCAT.ROUTINES where ROUTINETYPE='P' and ROUTINESCHEMA = '" + schema.name + "'" +
                    " and ROUTINEMODULENAME IS NULL"
        return buildDropStatements("DROP SPECIFIC PROCEDURE", dropProcGenQuery)
    }

    /**
     * Generates DROP statements for the triggers in this schema.
     */
    private fun generateDropStatementsForTriggers(): List<String> {
        val dropTrigGenQuery = "select TRIGNAME from SYSCAT.TRIGGERS where TRIGSCHEMA = '${schema.name}'"
        return buildDropStatements("DROP TRIGGER", dropTrigGenQuery)
    }

    /**
     * Generates DROP statements for the sequences in this schema.
     */
    private fun generateDropStatementsForSequences(): List<String> {
        val dropSeqGenQuery = ("select SEQNAME from SYSCAT.SEQUENCES where SEQSCHEMA = '" + schema.name
                + "' and SEQTYPE='S'")
        return buildDropStatements("DROP SEQUENCE", dropSeqGenQuery)
    }

    /**
     * Generates DROP statements for the views in this schema.
     */
    private fun generateDropStatementsForViews(): List<String> {
        val dropSeqGenQuery =
            "select TABNAME from SYSCAT.TABLES where TYPE='V' AND TABSCHEMA = '" + schema.name + "'" +
                    // Filter out statistical view for an index with an expression-based key
                    // See https://www.ibm.com/support/knowledgecenter/SSEPGG_10.5.0/com.ibm.db2.luw.sql.ref.doc/doc/r0001063.html
                    " and substr(property,19,1) <> 'Y'"
        return buildDropStatements("DROP VIEW", dropSeqGenQuery)
    }

    private fun generateDropStatementsForModules(): List<String> {
        val dropSeqGenQuery = ("select MODULENAME from syscat.modules where MODULESCHEMA = '"
                + schema.name
                + "' and OWNERTYPE='U'")
        return buildDropStatements("DROP MODULE", dropSeqGenQuery)
    }

    /**
     * Generates DROP statements for this type of table, representing this type of object in this schema.
     *
     * @param tableType  The type of table (Can be T, V, S, ...).
     * @param objectType The type of object.
     */
    private fun generateDropStatements(tableType: String, objectType: String): List<String> {
        val dropTablesGenQuery =
            ("select TABNAME from SYSCAT.TABLES where TYPE='" + tableType + "' and TABSCHEMA = '"
                    + schema.name + "'")
        return buildDropStatements("DROP $objectType", dropTablesGenQuery)
    }

    /**
     * Builds the drop statements for database objects in this schema.
     *
     * @param dropPrefix The drop command for the database object (e.g. 'drop table').
     * @param query      The query to get all present database objects
     */
    private fun buildDropStatements(dropPrefix: String, query: String): List<String> {
        val dropStatements: MutableList<String> = ArrayList()
        val dbObjects: List<String> = jdbcTemplate.queryForStringList(query)
        for (dbObject in dbObjects) {
            dropStatements.add(dropPrefix + " " + database.quote(schema.name, dbObject))
        }
        return dropStatements
    }

    /**
     * @return All tables that have versioning associated with them.
     */
    private fun generateDropVersioningStatement(): List<String> {
        val dropVersioningStatements: MutableList<String> = ArrayList()
        val versioningTables = schema.findTables(
            "select TABNAME from SYSCAT.TABLES where TEMPORALTYPE <> 'N' and TABSCHEMA = ?",
            schema.name
        )
        for (table in versioningTables) {
            dropVersioningStatements.add("ALTER TABLE " + table.toString() + " DROP VERSIONING")
        }
        return dropVersioningStatements
    }

    private fun dropType(name: String) {
        jdbcTemplate.execute("DROP TYPE " + database.quote(schema.name, name))
    }

    private fun dropFunction(name: String?) {
        jdbcTemplate.execute("DROP SPECIFIC FUNCTION " + database.quote(schema.name, name))
    }


    private fun dropTable(name: String) {
        jdbcTemplate.execute("DROP TABLE " + database.quote(name))
    }
}
