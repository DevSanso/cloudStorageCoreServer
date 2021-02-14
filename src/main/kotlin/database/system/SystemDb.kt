package database.system

import java.security.MessageDigest

import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.jodatime.datetime
import org.jetbrains.exposed.sql.transactions.transaction
import org.joda.time.DateTime
import org.joda.time.format.DateTimeFormat
data class DirInfo(val id : Int,val created : DateTime,
                   val hash : String,val localdbName : String,
                   val dir_base_name : String)


class SystemDb(private val systemDb : Database) {


    private object DirectoryTable : Table() {
        val id : Column<Int> = integer("dir_id").uniqueIndex().autoIncrement()
        val createDate : Column<DateTime> = datetime("create_date")
        val checkHash : Column<String> = char("check_hash",256)
        val localDbName : Column<String> = char("localdb_name",64).uniqueIndex()
        val dirBaseName : Column<String> = varchar("dir_base_name",128).uniqueIndex()

        override val primaryKey = PrimaryKey(DirectoryTable.id)
    }
    private inline fun makeLocalDbName(d :MessageDigest ,h : String, n : DateTime) : ByteArray {
        val date = n.toString(DateTimeFormat.fullDateTime()).toByteArray()
        return d.digest(h.toByteArray() + date)
    }
    private inline fun makeDirBaseName(d: MessageDigest,prefix : ByteArray) : ByteArray {
        d.update(prefix);return d.digest()
    }




    fun createDir(hash : String) : Int {
        val digest = MessageDigest.getInstance("MD5")
        val now = DateTime.now()
        val localDb = makeLocalDbName(digest,hash,now)
        val dirBase = makeDirBaseName(digest,localDb)

        transaction (systemDb){
            DirectoryTable.insert {
                it[createDate] = now
                it[checkHash] = hash
                it[localDbName] = localDb.toString()
                it[dirBaseName] = dirBase.toString()
            }
            commit()
        }
        return transaction<Int>(systemDb) {
            DirectoryTable.select {
                DirectoryTable.localDbName.eq(localDb.toString())
            }.first()[DirectoryTable.id]
        }
    }
    fun getDirInfo(id : Int)  : DirInfo {
        val row = transaction(systemDb) {
            DirectoryTable.select {
                DirectoryTable.id.eq(id)
            }.first()
        }
        return DirInfo(
            row[DirectoryTable.id],
            row[DirectoryTable.createDate],
            row[DirectoryTable.checkHash],
            row[DirectoryTable.localDbName],
            row[DirectoryTable.dirBaseName])
    }
    fun deleteDir(id : Int)  {
       transaction(systemDb) {
           try {
               DirectoryTable.deleteWhere {
                   DirectoryTable.id.eq(id)
               }
               commit()
           }catch(e : Exception) {
               rollback()
               throw e
           }
       }
    }
}