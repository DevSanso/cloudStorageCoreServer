package database

import java.io.File
import java.nio.file.Paths;

import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.jodatime.datetime;
import org.jetbrains.exposed.sql.transactions.transaction
import org.joda.time.DateTime;



class DirDB {
    companion object {
        val driverName = "jdbc:sqlite"

    }
    private object NodeTree : Table() {
        val tree : Column<String> = varchar("tree",128).uniqueIndex()
        override val primaryKey = PrimaryKey(tree)
    }
    private object NodeOriginInfo : Table() {
        val fileName : Column<String> = varchar("filename",128)
        val tree : Column<String> = reference("tree",NodeTree.tree).uniqueIndex()
        val size : Column<Int> = integer("size")
        val fileDate : Column<DateTime> = datetime("file_date")
        val permission : Column<Int> = integer("permission")
    }

    private var conn : Database

    constructor(dbPath : String) {
        conn = if(!File(dbPath).exists()) {
            var temp = Database.connect("jdbc:sqlite:"+dbPath, "org.sqlite.JDBC")
            transaction(temp) {
                SchemaUtils.create(NodeTree)
                SchemaUtils.create(NodeOriginInfo)
            }
            temp

        }else {
            Database.connect("jdbc:sqlite:"+dbPath, "org.sqlite.JDBC")
        }

    }
    data class NodeInfo(val fileName : String,val tree : String,
                        val fileDate : DateTime,val size : Int,val permission : Int)

    fun insertNodeTree(treePath : String) {
        transaction (conn){
            NodeTree.insert {
                it[tree] = treePath
            }
            commit()
        }
    }
    private fun existFileInOrigin(info : NodeInfo) : Boolean {
        val exist = transaction(conn) {
            val qb = QueryBuilder(false).append("SELECT ").append(
                exists(NodeOriginInfo.select{
                    NodeOriginInfo.tree.eq(info.tree) and NodeOriginInfo.fileName.eq(info.fileName)
                })
            )

            exec(qb.toString()) {
                it.next()
                it.getInt(1)
            }
        }
        return exist != null
    }
    fun insertOriginNodeInfo(info : NodeInfo) {
        if(existFileInOrigin(info)) {
            throw IllegalArgumentException("Already Exist")
        }

        transaction(conn) {
            NodeOriginInfo.insert {
                it[fileName] = fileName
                it[tree] = tree
                it[fileDate] = fileDate
                it[size] = size
                it[permission] = permission
            }
            commit()
        }
    }

    fun getNodeTrees(base : String) : ArrayList<String> {
        return transaction(conn) {
            var list = ArrayList<String>()
            NodeTree.selectAll().forEach {
                val row = Paths.get(it[NodeTree.tree])
                if(row.parent.toString().equals(base)){
                   list.add(row.fileName.toString())
                }
            }
            list
        }

    }
    fun getNodeOriginNames(base : String) : ArrayList<String> {
        return transaction(conn) {
            var list = ArrayList<String>()
            NodeOriginInfo.select {
                NodeOriginInfo.tree.eq(base)
            }.forEach {
                list.add(it[NodeOriginInfo.fileName])
            }
            list
        }
    }
    fun getNodeOriginInfo(base: String,name : String) : NodeInfo? {
        return transaction(conn) {
            val find = NodeOriginInfo.select {
                NodeOriginInfo.tree.eq(base) and NodeOriginInfo.fileName.eq(name)
            }.firstOrNull()
            if (find == null) null
            else {
                NodeInfo(find[NodeOriginInfo.fileName],find[NodeOriginInfo.tree],
                    find[NodeOriginInfo.fileDate],find[NodeOriginInfo.size],find[NodeOriginInfo.permission])
            }
        }
    }




}