package container

import utils.bytesToInt
/*
physicsSector
|--crypto data size(4byte)|------------------crypto data----------------------- |
                                        ^
                                        |
                                        | encode
                                        |
sector
|--- index (4byte) --- | --- originSize (4byte) ----   | -----data (??byte) --- |
: crypto data len < physicsSector
 */
data class Sector(val index : Int,val originSize : Int,val data : ByteArray) {

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Sector

        if (index != other.index) return false
        if (!data.contentEquals(other.data)) return false
        if (originSize != other.originSize) return false

        return true
    }

    override fun hashCode(): Int {
        var result = index
        result = 31 * result + data.contentHashCode()
        result = 31 * result + originSize
        return result
    }
}

inline fun convertBytesToSector(bytes : ByteArray) : Sector {
    val index = bytesToInt(bytes.take(4).toByteArray())
    val size = bytesToInt(bytes.copyOfRange(4,8))
    return Sector(index,size,bytes.copyOfRange(8,size+8))
}

