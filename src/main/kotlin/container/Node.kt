package container

import java.io.File
import java.io.RandomAccessFile
import javax.crypto.Cipher
import javax.crypto.spec.SecretKeySpec

import utils.bytesToInt
import utils.int32ToBytes
import java.lang.IllegalArgumentException


const val cryptoName = "AES/CTR/NoPadding"
typealias SecretKey = String




private inline fun getCipher(op : Int,key : SecretKey) : Cipher {
    val c = Cipher.getInstance(cryptoName)
    val ks = SecretKeySpec(key.toByteArray(),"AES")
    c.init(op,ks)
    return c
}
private fun checkLimitDataLen(phySize : Int,d : ByteArray) = d.size > (phySize / 3) * 2




class ReadNode(val file : File,val physicsSectorSize : Int) {
    val sectorCount = file.length() / physicsSectorSize.toLong()


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

class WriteNode(val file : File,val physicsSectorSize : Int) {
    val sectorCount = file.length() / physicsSectorSize.toLong()
    private val access = RandomAccessFile(file,"w")

    init {
        access.seek(0)
    }
    fun write(index : Int,key : SecretKey,sector: Sector)  {
        if(index > sectorCount)throw ArrayIndexOutOfBoundsException()
        else if(checkLimitDataLen(physicsSectorSize,sector.data))throw IllegalArgumentException()

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


}