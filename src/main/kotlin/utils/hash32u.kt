package utils

private const val mulp = 231232u

fun light32Hash(str : String,mix : UInt) : UInt {
    var mix1 = mix xor 2301023u
    var res : UInt = 0u
    var expr : UInt = mix1 shr 16
    str.forEach {
        res += (it.toInt().toUInt()  * mulp) xor (expr)
        expr = expr shr 16
    }
    return res
}