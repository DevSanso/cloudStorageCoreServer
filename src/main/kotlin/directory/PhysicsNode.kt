package directory

import java.io.File
import java.io.FileNotFoundException
import java.lang.Exception
import java.io.RandomAccessFile;




internal class PhysicsNode  {
    companion object {
        fun delete(node : PhysicsNode) {
            try {
                val isOk = node.file.delete()
                if(!isOk)throw FileNotFoundException()
            }catch(e : Exception) {
                throw e
            }
        }
        fun create(path : String,sectorSize: Int) : PhysicsNode {
            val file = File(path)

            try {
                val isOk = file.createNewFile()
                if(!isOk)throw IllegalArgumentException("Already Exist File")
            }catch(e : Exception) {
                throw e
            }
            return PhysicsNode(file,sectorSize)
        }

        fun load(path : String,sectorSize: Int) : PhysicsNode {
            val file = File(path)
            try {
                if(!file.exists())throw FileNotFoundException()
            }catch(e : Exception) {
                throw e
            }
            return PhysicsNode(file,sectorSize)
        }
    }
    private val file : File
    private val sectorSize : Int
    private var privateSectorCount : Long = 0L
    val sectorCount : Long get() {return privateSectorCount}
    val path : String get() {return file.path}

    private constructor(file : File,sectorSize : Int) {
        this.file = file
        this.sectorSize = sectorSize
        calculateSectorCount()
    }
    private fun calculateSectorCount() {
        privateSectorCount = if (0L != file.length() % sectorSize.toLong()) {
            (file.length() / sectorSize.toLong()) + 1L
        }else {
            file.length() / sectorSize.toLong()
        }
    }

    private inline fun fileAccess(index : Int,mode : String) : RandomAccessFile{
        val access = RandomAccessFile(file,mode)
        access.seek(index.toLong())
        return access
    }

    fun readSector(index : Int) : Byte {
        val access = fileAccess(index,"r")
        var res = access.read().toByte()
        access.close()
        return res
    }

    fun readSector(start : Int, end : Int) : ByteArray {
        val access = fileAccess(start,"r")
        var buf = ByteArray(end-start)
        access.read(buf,0,end-start)
        access.close()
        return buf
    }

    fun writeSector(index : Int,data : Byte) {
        val access = fileAccess(index,"w")
        access.write(data.toInt())
        access.close()
    }
    fun writeSector(start : Int,dataOffset : Int,data : ByteArray) {
        val access = fileAccess(start,"w")
        access.write(data,dataOffset,data.size)
        access.close()
    }






}