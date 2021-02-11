package database

import java.io.File

import java.util.*
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.jodatime.datetime;
import org.jetbrains.exposed.sql.transactions.transaction
import org.joda.time.DateTime;



class DirDB {
    companion object {
        val driverName = "jdbc:sqlite"

    }
    private object NodeTree : Table() {
        val tree : Column<String> = varchar("tree",128)
    }
    private object NodeOriginInfo : Table() {
        val fileName : Column<String> = varchar("filename",128).uniqueIndex()
        override val primaryKey = PrimaryKey(fileName)

        val tree : Column<String> = reference("tree",NodeTree.tree).uniqueIndex()
        val size : Column<Int> = integer("size")
        val fileDate : Column<DateTime> = datetime("file_date")
        val permission : Column<Char> = char("permission")
    }

    private var conn : Database

    constructor(dbPath : String) {
        conn = if(!File(dbPath).exists()) {
            var temp = Database.connect("jdbc:sqlite:"+dbPath, "org.sqlite.JDBC")
            SchemaUtils.create(NodeTree)
            SchemaUtils.create(NodeOriginInfo)
            temp

        }else {
            Database.connect("jdbc:sqlite:"+dbPath, "org.sqlite.JDBC")
        }

    }
    data class NodeInfo(val fileName : String,val tree : String,
                        val fileDate : Date,val size : Long,val permission : Byte)

    fun pushNodeTree(tree : String) {

    }

    fun pushOriginNodeInfo(info : NodeInfo) {

    }
    fun getOriginNodeInfo() {

    }



}