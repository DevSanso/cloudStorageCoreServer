package container

import java.io.RandomAccessFile
import java.io.File

import database.dir.*
import errors.*
import utils.pathHashing256

import java.io.FileNotFoundException

private fun createTemp(f : File,size : Long) {
    f.createNewFile()
    val rf = RandomAccessFile(f,"w")
    rf.setLength(size)
    rf.close()
}

class Container(val root : String,val hashKey : ByteArray,
                private val db : DirDB,val sectorSize : Int) {
    val getDb : OnlyGetInfoDb get() {return db}

    fun createWriteNode(tree : String,fileName : String) : WriteNode {
        if(db.existFileInOrigin(tree,fileName))
            throw AlreadyExistFileException()
        else if(!db.existTempInOrigin(tree,fileName))
            throw ViolationOfAccessException()

        val shaPath = pathHashing256(tree,fileName).toString()
        val f = File(shaPath)



        return WriteNode(f,sectorSize)
    }
    private fun calcFileSize(sectorSize: Long,originSize : Long) : Long {
        return if(originSize % sectorSize  != 0L) {
            (originSize / sectorSize + 1L) * sectorSize
        }else {
            (originSize / sectorSize) * sectorSize
        }
    }
    fun createTemp(info : NodeInfo) {


        val shaPath =  pathHashing256(info.tree,info.fileName).toString()
        val f = File(shaPath)


        db.insertTempOriginNodeInfo(info) {
            createTemp(f,calcFileSize(sectorSize.toLong(),info.size).toLong())
        }
    }



    fun writeNodeClose(node : WriteNode,tree : String, fileName : String) {
        node.close()
        db.switchTempToFalse(tree,fileName)
    }

    fun createTree(tree : String) {
        db.insertNodeTree(tree)
    }

    fun deleteTree(tree : String) {
        db.deleteTree(tree)
    }

    fun createReadNode(tree : String,fileName : String) : ReadNode {
        if(!db.existFileInOrigin(tree,fileName))
            throw FileNotFoundException()
        else if(db.existTempInOrigin(tree,fileName))
            throw DbIntegrityViolationException()

        val shaPath = pathHashing256(tree,fileName).toString()
        val f = File(shaPath)

        return ReadNode(f, sectorSize)
    }

    fun delete(tree : String,fileName : String) {
        if(!db.existFileInOrigin(tree,fileName))
            throw FileNotFoundException()

        val shaPath = pathHashing256(tree,fileName).toString()

        db.deleteOriginNodeInfo(tree,fileName) {
            File(shaPath).delete()
        }
    }
}