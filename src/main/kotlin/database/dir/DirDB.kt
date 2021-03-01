package database.dir

import java.io.File
import java.nio.file.Paths
import java.lang.Exception

import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.jodatime.datetime
import org.jetbrains.exposed.sql.transactions.transaction
import org.joda.time.DateTime

import errors.*

data class NodeInfo(val fileName : String,val tree : String,
                    val fileDate : DateTime,val size : Int,val permission : Int)

interface OnlyGetInfoDb {
    fun existFileInOrigin(base: String,name : String) : Boolean
    fun getTrees(tree : String) : ArrayList<String>
    fun getNodeOriginInfos(tree : String) : ArrayList<NodeInfo>
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
        val fileDate : Column<DateTime> = datetime("file_date")
        val permission : Column<Int> = integer("permission")
        val isTemp : Column<Boolean> = bool("is_temp")
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


    fun existTree(tree : String) : Boolean {
        val ptr = transaction(conn) {
            NodeTree.select {
                NodeTree.tree.eq(tree)
            }.firstOrNull()
        }

        return ptr != null
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
    fun insertTempOriginNodeInfo(info : NodeInfo,after : () -> Unit) {
        if(existFileInOrigin(info.tree,info.fileName)) {
            throw AlreadyExistFileException()
        }
        transaction(conn) {
            try {
                transaction(conn) {
                    NodeOriginInfo.insert {
                        it[fileName] = info.fileName
                        it[tree] = info.tree
                        it[fileDate] = info.fileDate
                        it[size] = info.size
                        it[permission] = info.permission
                        it[isTemp] = true
                    }
                }
                after()
                commit()

            }catch(e :Exception) {
                rollback()
                throw e
            }
        }
    }
    fun insertTempOriginNodeInfo(info : NodeInfo) {
        if(existFileInOrigin(info.tree,info.fileName)) {
            throw AlreadyExistFileException()
        }

        transaction(conn) {
            NodeOriginInfo.insert {
                it[fileName] = info.fileName
                it[tree] = info.tree
                it[fileDate] = info.fileDate
                it[size] = info.size
                it[permission] = info.permission
                it[isTemp] = true
            }
            commit()
        }
    }

    fun switchTempToFalse(tree : String,fileName : String) {
        transaction(conn) {

            NodeOriginInfo.update({
                NodeOriginInfo.tree.eq(tree) and
                        NodeOriginInfo.fileName.eq(fileName)
            }) {
                it[isTemp] = false
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


    fun existTempInOrigin(tree: String,fileName : String) : Boolean {
        val exist = transaction(conn) {
            val qb = QueryBuilder(false).append("SELECT ").append(
                exists(NodeOriginInfo.select{
                    NodeOriginInfo.tree.eq(tree) and NodeOriginInfo.fileName.eq(fileName) and
                            NodeOriginInfo.isTemp.eq(true)
                })
            )

            exec(qb.toString()) {
                it.next()
                it.getInt(1)
            }
        }
        return exist != null
    }

    override fun existFileInOrigin(tree: String,fileName : String) : Boolean {
        val exist = transaction(conn) {
            val qb = QueryBuilder(false).append("SELECT ").append(
                exists(NodeOriginInfo.select{
                    NodeOriginInfo.tree.eq(tree) and NodeOriginInfo.fileName.eq(fileName) and
                            NodeOriginInfo.isTemp.eq(false)
                })
            )

            exec(qb.toString()) {
                it.next()
                it.getInt(1)
            }
        }
        return exist != null
    }
    override fun getTrees(base : String) : ArrayList<String> {
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
    override fun getNodeOriginInfos(tree : String) : ArrayList<NodeInfo> {
        return transaction(conn) {
            val list = ArrayList<NodeInfo>()
            NodeOriginInfo.select {
                NodeOriginInfo.tree.eq(tree) and NodeOriginInfo.isTemp.eq(false)
            }.forEach {
                list.add(NodeInfo(
                    it[NodeOriginInfo.fileName],
                    it[NodeOriginInfo.tree],
                    it[NodeOriginInfo.fileDate],
                    it[NodeOriginInfo.size],
                    it[NodeOriginInfo.permission]
                ))
            }
            list
        }
    }
    override fun getNodeOriginInfo(base: String,name : String) : NodeInfo? {
        return transaction(conn) {
            val find = NodeOriginInfo.select {
                NodeOriginInfo.tree.eq(base) and NodeOriginInfo.fileName.eq(name) and NodeOriginInfo.isTemp.eq(false)
            }.firstOrNull()
            if (find == null) null
            else {
                NodeInfo(find[NodeOriginInfo.fileName],find[NodeOriginInfo.tree],
                    find[NodeOriginInfo.fileDate],find[NodeOriginInfo.size],
                    find[NodeOriginInfo.permission])
            }
        }
    }




}