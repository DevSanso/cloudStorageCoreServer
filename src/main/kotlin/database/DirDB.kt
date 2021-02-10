package database

import java.io.File

import java.sql.Connection
import java.sql.DriverManager
import java.sql.ResultSet
import java.sql.SQLException
import java.sql.Statement




class DirDB {
    companion object {
        val driverName = "jdbc:sqlite"
        private fun initDb(conn : Connection) {
            val stat = conn.createStatement()
            initTable(stat)
            stat.close()

        }
        private fun initTable(stat : Statement) {
            stat.executeUpdate("CREATE TABLE node_tree (name VARCHAR(128),PRIMARY KEY(name));")
            stat.executeUpdate("CREATE TABLE node_origin_info (" +
                    "filename VARCHAR(256)," +
                    "tree VARCHAR(128)," +
                    "size BIGINT," +
                    "file_date DATETIME,"+
                    "permission CHAR(1)," +
                    "FOREIGN KEY(tree) REFERENCES node_tree(name));")
        }
    }

    private var conn : Connection

    constructor(dbPath : String) {
        conn = if(!File(dbPath).exists()) {
            var temp = DriverManager.getConnection(driverName+":"+dbPath)
            DirDB.initDb(temp)
            temp
        }else {
            DriverManager.getConnection(driverName+":"+dbPath)
        }
    }




}