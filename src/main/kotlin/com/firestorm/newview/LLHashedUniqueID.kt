package com.firestorm.newview

import java.security.MessageDigest
import java.net.NetworkInterface

const val MD5HEX_STR_SIZE = 32

fun llHashedUniqueID(): Pair<Boolean, String> {
    val macBytes = getMachineUniqueBytes()
    return if (macBytes != null) {
        val md5 = MessageDigest.getInstance("MD5")
        md5.update(macBytes)
        val digest = md5.digest()
        val hex = digest.joinToString("") { "%02x".format(it) }
        Pair(true, hex)
    } else {
        Pair(false, "00000000000000000000000000000000")
    }
}

private fun getMachineUniqueBytes(): ByteArray? {
    return try {
        val interfaces = NetworkInterface.getNetworkInterfaces()
        while (interfaces.hasMoreElements()) {
            val ni = interfaces.nextElement()
            val mac = ni.hardwareAddress
            if (mac != null && mac.isNotEmpty()) return mac
        }
        null
    } catch (e: Exception) {
        null
    }
}
