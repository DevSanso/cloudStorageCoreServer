package container

import java.io.File
import java.io.RandomAccessFile
import javax.crypto.Cipher
import javax.crypto.spec.SecretKeySpec


import utils.bytesToInt
import utils.int32ToBytes
import java.io.FileNotFoundException
import java.lang.IllegalArgumentException
import java.nio.file.Path


const val cryptoName = "AES/CTR/NoPadding"
typealias SecretKey = String




private inline fun getCipher(op : Int,key : SecretKey) : Cipher {
    val c = Cipher.getInstance(cryptoName)
    val ks = SecretKeySpec(key.toByteArray(),"AES")
    c.init(op,ks)
    return c
}





class ReadNode(private val file : File,val physicsSectorSize : Int) {
    val sectorCount = file.length() / physicsSectorSize.toLong()
    val hash : Path get() {return file.toPath().fileName}

    fun read(index : Int,key : SecretKey) : Sector {
        if(index > sectorCount)throw ArrayIndexOutOfBoundsException()

        val s = file.inputStream()
        s.skip(index.toLong())
        val buf = ByteArray(physicsSectorSize)
        s.read(buf)
        s.close()

        val c = getCipher(Cipher.DECRYPT_MODE,key)
        val decrypt = c.doFinal(buf)
        val len = bytesToInt(decrypt.take(4).toByteArray())
        return convertBytesToSector(decrypt.copyOfRange(4,len+4))
    }
}

class WriteNode(private val file : File,val physicsSectorSize : Int) {
    val sectorCount = file.length() / physicsSectorSize.toLong()
    private val access = RandomAccessFile(file,"w")
    val hash : Path get() {return file.toPath().fileName}

    init {
        if(!file.exists())throw FileNotFoundException()
        access.seek(0)
    }
    fun write(index : Int,key : SecretKey,sector: Sector)  {
        if(index > sectorCount)throw ArrayIndexOutOfBoundsException()


        access.seek((physicsSectorSize * index).toLong())

        val index = int32ToBytes(sector.index)
        val size = int32ToBytes(sector.originSize)
        val origin = index + size + sector.data

        val c = getCipher(Cipher.ENCRYPT_MODE,key)
        val encrypt = c.doFinal(origin)

        if(encrypt.size > physicsSectorSize-4)throw IllegalArgumentException()
        val output = int32ToBytes(encrypt.size) + encrypt
        access.write(output)
    }

    internal fun close() = access.close()

}