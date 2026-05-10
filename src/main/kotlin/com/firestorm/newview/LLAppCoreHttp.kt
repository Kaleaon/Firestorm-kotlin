package com.firestorm.newview

class LLAppCoreHttp {

    companion object {
        const val PIPELINING_DEPTH: Long = 5L
        private const val MAX_THREAD_WAIT_TIME: Double = 10.0
    }

    enum class EAppPolicy {
        AP_DEFAULT,
        AP_ASSET,
        AP_TEXTURE,
        AP_MESH1,
        AP_MESH2,
        AP_LARGE_MESH,
        AP_UPLOADS,
        AP_LONG_POLL,
        AP_INVENTORY,
        AP_MATERIALS,
        AP_AGENT;

        companion object {
            val AP_REPORTING = AP_INVENTORY
        }
    }

    private data class InitData(
        val default: UInt,
        val min: UInt,
        val max: UInt,
        val rate: UInt,
        val pipelined: Boolean,
        val key: String,
        val usage: String
    )

    private val initData: List<InitData> = listOf(
        InitData(8u, 8u, 8u, 0u, false, "", "other"),                                   // AP_DEFAULT
        InitData(12u, 1u, 16u, 0u, true, "AssetFetchConcurrency", "asset fetch"),        // AP_ASSET
        InitData(8u, 1u, 12u, 0u, true, "TextureFetchConcurrency", "texture fetch"),     // AP_TEXTURE
        InitData(32u, 1u, 128u, 0u, false, "MeshMaxConcurrentRequests", "mesh fetch"),   // AP_MESH1
        InitData(8u, 1u, 32u, 0u, true, "Mesh2MaxConcurrentRequests", "mesh2 fetch"),    // AP_MESH2
        InitData(2u, 1u, 8u, 0u, false, "", "large mesh fetch"),                         // AP_LARGE_MESH
        InitData(2u, 1u, 8u, 0u, false, "", "asset upload"),                             // AP_UPLOADS
        InitData(32u, 32u, 32u, 0u, false, "", "long poll"),                             // AP_LONG_POLL
        InitData(4u, 1u, 4u, 0u, false, "", "inventory"),                                // AP_INVENTORY
        InitData(2u, 1u, 8u, 0u, false, "RenderMaterials", "material manager requests"), // AP_MATERIALS
        InitData(2u, 1u, 32u, 0u, false, "Agent", "Agent requests")                     // AP_AGENT
    )

    private inner class HttpClass {
        var policy: Int = 0
        var connLimit: UInt = 0u
        var pipelined: Boolean = false
        var settingsSignal: (() -> Unit)? = null
    }

    private val httpClasses: Array<HttpClass> = Array(EAppPolicy.entries.size) { HttpClass() }

    private var stopRequested: Double = 0.0
    private var stopped: Boolean = false
    private var pipelined: Boolean = true
    private var pipelinedSignal: (() -> Unit)? = null
    private var sslNoVerifySignal: (() -> Unit)? = null

    fun init() {
        TODO("APR: use JVM equivalent — initialize HTTP service, set CA file, SSL verify callback, proxy, trace level, create policy classes, start service thread, apply pipelining and connection settings from saved prefs")
    }

    fun requestStop() {
        TODO("APR: use JVM equivalent — request HTTP service thread shutdown, record mStopRequested timestamp")
    }

    fun cleanup() {
        TODO("APR: use JVM equivalent — dump HTTP stats, wait up to MAX_THREAD_WAIT_TIME for thread stop, disconnect all setting signals, destroy HTTP service")
    }

    fun onCompleted() {
        stopped = true
    }

    fun getPolicy(policy: EAppPolicy): Int {
        return httpClasses[policy.ordinal].policy
    }

    fun isPipelined(policy: EAppPolicy): Boolean {
        return httpClasses[policy.ordinal].pipelined
    }

    fun refreshSettings(initial: Boolean) {
        for (i in initData.indices) {
            val appPolicy = EAppPolicy.entries[i]
            val cls = httpClasses[i]
            val data = initData[i]

            if (initial && data.rate > 0u) {
                TODO("APR: use JVM equivalent — setStaticPolicyOption PO_THROTTLE_RATE for ${data.usage}")
            }

            if (initial) {
                val toPipeline = pipelined && data.pipelined
                if (toPipeline != cls.pipelined) {
                    val newDepth = if (toPipeline) PIPELINING_DEPTH else 0L
                    TODO("APR: use JVM equivalent — setPolicyOption PO_PIPELINING_DEPTH newDepth=$newDepth for ${data.usage}")
                    cls.pipelined = toPipeline
                }
            }

            var setting = data.default
            if (data.key.isNotEmpty()) {
                TODO("APR: use JVM equivalent — look up saved setting ${data.key}, clamp to [${data.min}, ${data.max}]")
            }

            if (initial || setting != cls.connLimit) {
                val connLimit = if (cls.pipelined) setting * 2u else setting
                TODO("APR: use JVM equivalent — setPolicyOption PO_CONNECTION_LIMIT=$connLimit and PO_PER_HOST_CONNECTION_LIMIT=$setting for ${data.usage}")
                cls.connLimit = setting
            }
        }
    }

    private fun sslVerify(url: String): Boolean {
        TODO("APR: use JVM equivalent — validate SSL certificate chain for url using security API; return false on trust/cert exceptions")
    }
}
