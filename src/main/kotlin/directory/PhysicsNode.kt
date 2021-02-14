package directory

import java.io.File
import java.io.FileNotFoundException
import java.lang.Exception
import java.io.RandomAccessFile;
import java.nio.file.Paths
import java.nio.file.Files


interface AccessNode {
    fun readSector(start : Int, end : Int) : ByteArray
    fun writeSector(start : Int,data : ByteArray)
}

internal class PhysicsNode : AccessNode {
    companion object {
        fun delete(node : PhysicsNode) {
            node.file.delete()
        }

        fun createTemp(tempDirPath : String,name : String,sectorSize: Int) : PhysicsNode {
            val file = Paths.get(tempDirPath,name).toFile()

            try {
                val isOk = file.createNewFile()
                if(!isOk)throw IllegalArgumentException("Already Exist File")
            }catch(e : Exception) {
                throw e
            }
            return PhysicsNode(file,sectorSize)
        }

        fun move(src : PhysicsNode,root : String)  {
            val dst = Paths.get(root,src.hashPath)
            Files.move(src.file.toPath(),dst)
        }

        fun load(root : String,name : String,sectorSize: Int) : PhysicsNode {
            val file = Paths.get(root,name).toFile()
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
    val hashPath : String get() {return file.name}

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



    override fun readSector(start : Int, end : Int) : ByteArray {
        if(end > sectorCount) {
            throw ArrayIndexOutOfBoundsException()


        }

        val access = fileAccess(sectorSize * start,"r")
        var buf = ByteArray((end-start) * sectorSize)
        access.read(buf,0,buf.size)
        access.close()
        return buf
    }

    private inline fun checkWriteConditional(dataSize : Int,start :Int) : Boolean {
        return (dataSize % sectorSize == 0) && (((dataSize / sectorSize) + start) < sectorCount)
    }
    override fun writeSector(start : Int,data : ByteArray) {
        if(!checkWriteConditional(data.size,start)) {
            throw IllegalArgumentException()
        }


        val access = fileAccess(start,"w")
        access.write(data,0,data.size)
        access.close()
    }
}
