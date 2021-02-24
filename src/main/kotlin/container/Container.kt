package container

import java.io.RandomAccessFile
import java.io.File
import java.security.MessageDigest

import database.dir.*
import errors.*
import java.io.FileNotFoundException

private fun createTemp(f : File,size : Long) {
    f.createNewFile()
    val rf = RandomAccessFile(f,"w")
    rf.setLength(size)
    rf.close()
}

class Container(val root : String,private val db : DirDB) {
    val getDb : OnlyGetInfoDb get() {return db}

    private inline fun sha256Path(path : String) : ByteArray {
        val md = MessageDigest.getInstance("SHA-256");
        return md.digest(path.toByteArray())

    }
    private inline fun combinePath(tree : String,fileName : String)  = tree + File.separator + fileName


    fun createWriteNode(info : NodeInfo,totalSectorCount : Int) : WriteNode {
        if(db.existFileInOrigin(info.tree,info.fileName))
            throw AlreadyExistFileException()

        val shaPath = sha256Path(combinePath(info.tree,info.fileName)).toString()
        val f = File(shaPath)

        db.insertTempOriginNodeInfo(info) {
            createTemp(f,(info.sectorSize * totalSectorCount).toLong())
        }

        return WriteNode(f,info.sectorSize)
    }

    fun writeNodeClose(node : WriteNode,tree : String, fileName : String) {
        node.close()
        db.switchTempToFalse(tree,fileName)
    }

    fun createReadNode(tree : String,fileName : String) : ReadNode? {
        if(!db.existFileInOrigin(tree,fileName))
            throw FileNotFoundException()
        else if(db.existTempInOrigin(tree,fileName))
            throw DbIntegrityViolationException()

        val shaPath = sha256Path(combinePath(tree,fileName)).toString()
        val f = File(shaPath)

        val sSize = db.getNodeSectorSize(tree,fileName)

        return ReadNode(f,sSize)
    }

    fun delete(tree : String,fileName : String) {
        if(!db.existFileInOrigin(tree,fileName))
            throw FileNotFoundException()

        val shaPath = sha256Path(combinePath(tree,fileName)).toString()

        db.deleteOriginNodeInfo(tree,fileName) {
            File(shaPath).delete()
        }
    }
}