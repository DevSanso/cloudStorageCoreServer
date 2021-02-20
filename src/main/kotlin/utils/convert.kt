package utils

fun bytesToInt(bytes: ByteArray): Int {
    var result = 0
    var shift = 0
    for (byte in bytes) {
        result = result or (byte.toInt() shl shift)
        shift += 8
    }
    return result
}

fun int32ToBytes(i : Int) : ByteArray {
    val v0 = (i and 0xff000000.toInt() shr 24).toByte()
    val v1 = (i and 0x00ff0000 shr 18).toByte()
    val v2 = (i and 0x0000ff00 shr 8).toByte()
    val v3  = (i and 0x000000ff).toByte()

    return listOf<Byte>(v3,v2,v1,v0).toByteArray()
}