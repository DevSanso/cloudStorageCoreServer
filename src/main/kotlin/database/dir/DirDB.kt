package database.dir

import java.io.File
import java.nio.file.Paths

import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.jodatime.datetime
import org.jetbrains.exposed.sql.transactions.transaction
import org.joda.time.DateTime
import java.lang.Exception


data class NodeInfo(val fileName : String,val tree : String,
                    val fileDate : DateTime,val size : Int,val sectorSize : Int,val permission : Int)

interface OnlyGetInfoDb {
    fun existFileInOrigin(base: String,name : String) : Boolean
    fun getNodeTrees(tree : String) : ArrayList<String>
    fun getNodeOriginNames(tree : String) : ArrayList<String>
    fun getNodeOriginInfo(tree: String,name : String) : NodeInfo?
}

class DirDB(dbPath : String) : OnlyGetInfoDb {
    companion object {
        const val driverName = "jdbc:sqlite:"

    }
    private object NodeTree : Table() {

        val tree : Column<String> = varchar("tree",128).uniqueIndex()
        val base : Column<String> = reference("base",NodeTree.tree)

        override val primaryKey = PrimaryKey(tree)
    }
    private object NodeOriginInfo : Table() {
        val fileName : Column<String> = varchar("filename",128)
        val tree : Column<String> = reference("tree",NodeTree.tree).uniqueIndex()
        val size : Column<Int> = integer("size")
        val sectorSize : Column<Int> = integer("sector_size")
        val fileDate : Column<DateTime> = datetime("file_date")
        val permission : Column<Int> = integer("permission")
    }

    private var conn : Database = if(!File(dbPath).exists()) {
        val temp = Database.connect(driverName+dbPath, "org.sqlite.JDBC")
        transaction(temp) {
            exec("PRAGMA foreign_keys=ON")
            SchemaUtils.create(NodeTree)
            SchemaUtils.create(NodeOriginInfo)
            NodeTree.insert {
                it[tree] = "/"
                it[base] = "/"
            }
        }

        temp

    }else {
        val d = Database.connect(driverName+dbPath, "org.sqlite.JDBC")
        transaction (d){
            exec("PRAGMA foreign_keys=ON")
        }
        d
    }




    fun insertNodeTree(treePath : String) {
        transaction (conn){
           try {
               NodeTree.insert {
                   it[base] = Paths.get(treePath).parent.toString()
                   it[tree] = treePath
               }
               commit()
           }catch(e : Exception) {
               rollback()
               throw e
           }
        }
    }

    fun insertOriginNodeInfo(info : NodeInfo) {
        if(existFileInOrigin(info.tree,info.fileName)) {
            throw IllegalArgumentException("Already Exist")
        }

        transaction(conn) {
            NodeOriginInfo.insert {
                it[fileName] = info.fileName
                it[tree] = info.tree
                it[fileDate] = info.fileDate
                it[size] = info.size
                it[sectorSize] = info.sectorSize
                it[permission] = info.permission
            }
            commit()
        }
    }
    fun deleteOriginNodeInfo(tree : String,fileName :String) {
        transaction(conn) {
           try {
               NodeOriginInfo.deleteWhere {
                   NodeOriginInfo.tree.eq(tree) and NodeOriginInfo.fileName.eq(fileName)
               }
               commit()
           }catch(e :Exception) {
               rollback()
           }
        }
    }
    fun deleteOriginNodeInfo(tree : String,fileName :String,after : () -> Unit) {
        transaction(conn) {
            try {
                NodeOriginInfo.deleteWhere {
                    NodeOriginInfo.tree.eq(tree) and NodeOriginInfo.fileName.eq(fileName)
                }
                commit()
                after()
            }catch(e :Exception) {
                rollback()
                throw e
            }
        }
    }

    fun deleteTree(tree : String) {
        transaction(conn) {
            try {
                NodeTree.deleteWhere {
                    NodeTree.tree.eq(tree)
                }
                commit()
            }catch (e :Exception) {
                rollback()
                throw e
            }
        }
    }


    override fun existFileInOrigin(tree: String,fileName : String) : Boolean {
        val exist = transaction(conn) {
            val qb = QueryBuilder(false).append("SELECT ").append(
                exists(NodeOriginInfo.select{
                    NodeOriginInfo.tree.eq(tree) and NodeOriginInfo.fileName.eq(fileName)
                })
            )

            exec(qb.toString()) {
                it.next()
                it.getInt(1)
            }
        }
        return exist != null
    }
    override fun getNodeTrees(base : String) : ArrayList<String> {
        return transaction(conn) {
            val list = ArrayList<String>()
            NodeTree.select {
                NodeTree.base.eq(base)
            }.forEach {
                list.add(it[NodeTree.tree])
            }
            list
        }

    }
    override fun getNodeOriginNames(base : String) : ArrayList<String> {
        return transaction(conn) {
            val list = ArrayList<String>()
            NodeOriginInfo.select {
                NodeOriginInfo.tree.eq(base)
            }.forEach {
                list.add(it[NodeOriginInfo.fileName])
            }
            list
        }
    }
    override fun getNodeOriginInfo(base: String,name : String) : NodeInfo? {
        return transaction(conn) {
            val find = NodeOriginInfo.select {
                NodeOriginInfo.tree.eq(base) and NodeOriginInfo.fileName.eq(name)
            }.firstOrNull()
            if (find == null) null
            else {
                NodeInfo(find[NodeOriginInfo.fileName],find[NodeOriginInfo.tree],
                    find[NodeOriginInfo.fileDate],find[NodeOriginInfo.size],
                    find[NodeOriginInfo.sectorSize],find[NodeOriginInfo.permission])
            }
        }
    }




}