package utils

import java.security.MessageDigest
import java.io.File

inline fun pathHashing256(parent : String,name : String) : ByteArray {
    val md = MessageDigest.getInstance("SHA-256");
    return md.digest((parent+File.separator+name).toByteArray())
}

