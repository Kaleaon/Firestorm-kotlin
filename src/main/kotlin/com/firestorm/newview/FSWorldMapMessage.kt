package com.firestorm.newview

import java.util.UUID

// OpenSim-only hypergrid world-map message extensions.
// Mirrors the OPENSIM-guarded block in fsworldmapmessage.h/.cpp.

object Hypergrid {

    // Identical contract to url_callback_t in llworldmapmessage.h.
    typealias UrlCallback = (regionHandle: ULong, url: String, snapshotId: UUID, teleport: Boolean) -> Unit

    private data class AdoptedRegionNameQuery(
        val key: String,
        val regionName: String,
        val callback: UrlCallback,
        val slurl: String,
        val teleport: Boolean
    )

    // Map from extracted region-name key => pending query.
    private val regionNameQueries: MutableMap<String, AdoptedRegionNameQuery> = mutableMapOf()

    // Exact MapNameRequests are sent flagless (not using LAYER_FLAG) to avoid OpenSim
    // code paths that modify result names; improves hop-URL region matching for
    // grids that host overlapping region names.
    private const val EXACT_FLAG: UInt = 0x00000000u
    private const val LAYER_FLAG: UInt = 0x00000002u

    private val hopSlashSpacePattern = Regex("""/ ([^/:=]+)$""")
    private val hopColonPattern      = Regex("""([^/:=]+)$""")

    private fun extractRegion(s: String): String {
        val ls = s.lowercase()
        hopSlashSpacePattern.find(ls)?.let { return it.groupValues[1] }
        hopColonPattern.find(ls)?.let { return it.groupValues[1] }
        return ""
    }

    fun sendExactNamedRegionRequest(
        regionName: String,
        callback: UrlCallback,
        callbackUrl: String,
        teleport: Boolean
    ): Boolean {
        if (!isInOpenSim() || regionName.isEmpty()) return false

        val key = extractRegion(regionName)
        if (key.isEmpty()) return false

        regionNameQueries.putIfAbsent(key, AdoptedRegionNameQuery(key, regionName, callback, callbackUrl, teleport))
        sendMapNameRequest(regionName, EXACT_FLAG)
        return true
    }

    fun processExactNamedRegionResponse(msg: LLMessageSystem, agentFlags: UInt): Boolean {
        if (!isInOpenSim() || agentFlags and LAYER_FLAG != 0u) return false

        val blocks = readMapBlocks(msg)

        // Special case: a single result with an empty extracted name but a valid region handle
        // and exactly one pending query is treated as a redirect (e.g. "hop://grid:port/$").
        val soloResult = blocks.size == 2 &&
            blocks[0].regionHandle != 0uL &&
            extractRegion(blocks[0].name).isEmpty() &&
            blocks[1].regionHandle == 0uL

        val resolvedBlocks = if (soloResult && regionNameQueries.size == 1) {
            val pendingName = regionNameQueries.values.first().regionName
            listOf(blocks[0].copy(name = pendingName)) + blocks.drop(1)
        } else {
            blocks
        }

        for (block in resolvedBlocks) {
            val key = extractRegion(block.name)
            if (key.isEmpty()) continue

            val pending = regionNameQueries.remove(key) ?: continue

            pending.callback(block.regionHandle, pending.slurl, block.imageId, pending.teleport)
            return true
        }

        return false
    }

    private fun isInOpenSim(): Boolean {
        TODO("APR: use JVM equivalent — query LLGridManager.instance().isInOpenSim()")
    }

    private fun sendMapNameRequest(regionName: String, flags: UInt) {
        TODO("APR: use JVM equivalent — build and send MapNameRequest UDP message via LLMessageSystem")
    }

    private fun readMapBlocks(msg: LLMessageSystem): List<MapBlock> {
        TODO("APR: use JVM equivalent — read number-of-blocks and construct MapBlock list from LLMessageSystem")
    }

    // Encapsulates a single Region Map Block response entry.
    data class MapBlock(
        val index: Int,
        val xRegions: UShort,
        val yRegions: UShort,
        val xSize: UShort,
        val ySize: UShort,
        val name: String,
        val accessCode: UByte,
        val regionFlags: UInt,
        val imageId: UUID
    ) {
        val xWorld: UInt get() = xRegions.toUInt() * 256u
        val yWorld: UInt get() = yRegions.toUInt() * 256u
        val regionHandle: ULong get() = yRegions.toULong().shl(32) or xRegions.toULong()
    }
}

// Opaque stub; real implementation provided by message layer.
class LLMessageSystem
