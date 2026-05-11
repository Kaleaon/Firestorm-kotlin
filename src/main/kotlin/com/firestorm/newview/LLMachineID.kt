package com.firestorm.newview

// ---------------------------------------------------------------------------
// LLMachineID
//
// The C++ implementation is entirely platform-specific (WMI on Windows,
// IOKit on macOS, MAC-address node-id on Linux).  All platform calls are
// replaced with TODO stubs pointing at JVM equivalents.
//
// Return convention mirrors the C++ original:
//   init()       → 0 on success, non-zero on failure
//   getUniqueID  → 1 if an id was available and copied, 0 otherwise
//   getLegacyID  → 1 if a legacy id was available and copied, 0 otherwise
// ---------------------------------------------------------------------------

class LLMachineID {

    companion object {

        private val staticUniqueId: UByteArray = UByteArray(6)
        private val staticLegacyId: UByteArray = UByteArray(6)
        private var hasStaticUniqueId: Boolean = false
        private var hasStaticLegacyId: Boolean = false

        fun init(): Int {
            staticUniqueId.fill(0u)
            staticLegacyId.fill(0u)
            hasStaticUniqueId = false
            hasStaticLegacyId = false

            TODO("APR: use JVM equivalent – on JVM, obtain a machine-unique id via:\n" +
                 "  • java.net.NetworkInterface MAC address (analogous to Linux node-id path)\n" +
                 "  • or a persistent random UUID stored in a well-known local file\n" +
                 "  Populate staticUniqueId/staticLegacyId, set hasStaticUniqueId/hasStaticLegacyId,\n" +
                 "  return 0 on success or 1 on failure.")
        }

        fun getUniqueID(uniqueId: UByteArray, len: Int): Int {
            if (!hasStaticUniqueId) return 0
            val copyLen = minOf(len, staticUniqueId.size)
            staticUniqueId.copyInto(uniqueId, endIndex = copyLen)
            return 1
        }

        fun getLegacyID(uniqueId: UByteArray, len: Int): Int {
            if (!hasStaticLegacyId) return 0
            val copyLen = minOf(len, staticLegacyId.size)
            staticLegacyId.copyInto(uniqueId, endIndex = copyLen)
            return 1
        }
    }
}
