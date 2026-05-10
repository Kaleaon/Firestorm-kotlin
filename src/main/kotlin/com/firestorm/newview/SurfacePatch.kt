package com.firestorm.newview

import kotlin.math.*

class PatchVisibilityInfo {
    var isVisible: Boolean  = false
    var distance: Float     = 0f
    var renderLevel: Int    = 0
    var renderStride: UInt  = 0u
}

class SurfacePatch {

    var hasReceivedData: Boolean = false
    var sTexUpdate: Boolean      = false

    private val neighborPatches: Array<SurfacePatch?> = arrayOfNulls(8)
    private val normalsInvalid: BooleanArray = BooleanArray(9) { true }

    private var dirty: Boolean           = false
    private var dirtyZStats: Boolean     = true
    private var heightsGenerated: Boolean = false

    private var dataOffset: Int          = 0
    private var surfaceZData: FloatArray = floatArrayOf()
    private var normData: Array<Vector3> = emptyArray()

    // Back-reference to the VOSurfacePatch viewer object.
    internal var voObj: VOSurfacePatch? = null

    private val visInfo = PatchVisibilityInfo()

    internal var originGlobal: Vector3d  = Vector3d(0.0, 0.0, 0.0)
    private var originRegion: Vector3   = Vector3(0f, 0f, 0f)

    var centerRegion: Vector3           = Vector3(0f, 0f, 0f)
        private set
    private var minZ: Float             = 0f
    private var maxZ: Float             = 0f
    private var meanZ: Float            = 0f
    private var radius: Float           = 0f

    private var minComposition: Float   = 0f
    private var maxComposition: Float   = 0f
    private var meanComposition: Float  = 0f

    private var connectedEdge: UByte    = NO_EDGE

    private var lastUpdateTime: Long    = 0L

    var surface: Surface? = null
        private set

    // Called by Surface.createPatchData to wire up Z / normal arrays.
    fun setDataOffset(offset: Int, z: FloatArray, n: Array<Vector3>) {
        dataOffset  = offset
        surfaceZData = z
        normData     = n
    }

    fun setSurface(surfacep: Surface) {
        surface = surfacep
        if (voObj == null) {
            voObj = VOSurfacePatch()
            voObj!!.patch = this
            voObj!!.setPositionRegion(centerRegion)
            TODO("GPU: register VOSurfacePatch with pipeline")
        }
    }

    fun reset(id: UInt) {
        for (i in 0..7) setNeighborPatch(i, null)
        for (i in 0..8) normalsInvalid[i] = true
        hasReceivedData = false
        sTexUpdate      = false
        dirty           = false
        dirtyZStats     = true
        heightsGenerated = false
        lastUpdateTime  = 0L
        connectedEdge   = NO_EDGE
    }

    fun connectNeighbor(neighbor: SurfacePatch, direction: Int) {
        normalsInvalid[direction] = true
        neighbor.normalsInvalid[dirOpposite[direction]] = true

        setNeighborPatch(direction, neighbor)
        neighbor.setNeighborPatch(dirOpposite[direction], this)

        when (direction) {
            EAST  -> { connectedEdge = (connectedEdge or EAST_EDGE);  neighbor.connectedEdge = (neighbor.connectedEdge or WEST_EDGE)  }
            NORTH -> { connectedEdge = (connectedEdge or NORTH_EDGE); neighbor.connectedEdge = (neighbor.connectedEdge or SOUTH_EDGE) }
            WEST  -> { connectedEdge = (connectedEdge or WEST_EDGE);  neighbor.connectedEdge = (neighbor.connectedEdge or EAST_EDGE)  }
            SOUTH -> { connectedEdge = (connectedEdge or SOUTH_EDGE); neighbor.connectedEdge = (neighbor.connectedEdge or NORTH_EDGE) }
        }
    }

    fun disconnectNeighbor(surfacep: Surface) {
        for (i in 0..7) {
            val p = getNeighborPatch(i) ?: continue
            if (p.surface === surfacep) {
                when (i) {
                    EAST  -> connectedEdge = (connectedEdge and EAST_EDGE.inv())
                    NORTH -> connectedEdge = (connectedEdge and NORTH_EDGE.inv())
                    WEST  -> connectedEdge = (connectedEdge and WEST_EDGE.inv())
                    SOUTH -> connectedEdge = (connectedEdge and SOUTH_EDGE.inv())
                }
                setNeighborPatch(i, null)
                normalsInvalid[i] = true
            }
        }
    }

    fun setNeighborPatch(direction: Int, neighbor: SurfacePatch?) {
        neighborPatches[direction] = neighbor
        normalsInvalid[direction] = true
        if (direction < 4) {
            normalsInvalid[dirAdjacent[direction][0]] = true
            normalsInvalid[dirAdjacent[direction][1]] = true
        }
    }

    fun getNeighborPatch(direction: Int): SurfacePatch? = neighborPatches[direction]

    // Evaluate the terrain point at grid offset (x, y).
    fun eval(x: UInt, y: UInt, stride: UInt): Triple<Vector3, Vector3, Vector2> {
        val s = surface ?: return Triple(Vector3(0f, 0f, 0f), Vector3.Z_AXIS, Vector2(0f, 0f))
        val surfaceStride = s.gridsPerEdge
        val pointOffset   = (x + y * surfaceStride.toUInt()).toInt()
        val normal        = getNormal(x, y)
        val originAgent   = getOriginAgent()
        val posAgent = Vector3(
            originAgent.x + x.toInt() * s.getMetersPerGrid(),
            originAgent.y + y.toInt() * s.getMetersPerGrid(),
            surfaceZData[dataOffset + pointOffset]
        )
        val rel = posAgent - s.getOriginAgent()
        val tex = rel * (1f / (surfaceStride * s.getMetersPerGrid()))
        return Triple(posAgent, normal, Vector2(tex.x, tex.y))
    }

    // Per-vertex smooth normal (PBR=false) or flat per-triangle normal (PBR=true).
    fun calcNormal(x: UInt, y: UInt, stride: UInt, pbr: Boolean = false) {
        if (pbr) {
            calcNormalFlat(x, y, 0u)
            return
        }
        val s = surface ?: return
        val patchWidth    = s.pvArray.patchWidth.toInt()
        val surfaceStride = s.gridsPerEdge
        val mpg           = s.getMetersPerGrid() * stride.toInt()

        data class Offset(var ox: Int, var oy: Int, var stride: Int)
        val offsets = Array(2) { Array(2) { Offset(0, 0, surfaceStride) } }
        offsets[0][0].ox = x.toInt() - stride.toInt(); offsets[0][0].oy = y.toInt() - stride.toInt()
        offsets[0][1].ox = x.toInt() - stride.toInt(); offsets[0][1].oy = y.toInt() + stride.toInt()
        offsets[1][0].ox = x.toInt() + stride.toInt(); offsets[1][0].oy = y.toInt() - stride.toInt()
        offsets[1][1].ox = x.toInt() + stride.toInt(); offsets[1][1].oy = y.toInt() + stride.toInt()

        val patches = Array(2) { Array<SurfacePatch>(2) { this } }

        for (i in 0..1) for (j in 0..1) {
            val off = offsets[i][j]
            var patch = patches[i][j]
            if (off.ox < 0) {
                val nb = patch.getNeighborPatch(WEST)
                if (nb == null) off.ox = 0
                else { patches[i][j] = nb; off.ox += patchWidth; off.stride = nb.surface?.gridsPerEdge ?: surfaceStride }
            }
            if (off.oy < 0) {
                val nb = patch.getNeighborPatch(SOUTH)
                if (nb == null) off.oy = 0
                else { patches[i][j] = nb; off.oy += patchWidth; off.stride = nb.surface?.gridsPerEdge ?: surfaceStride }
            }
            if (off.ox >= patchWidth) {
                val nb = patch.getNeighborPatch(EAST)
                if (nb == null) off.ox = patchWidth - 1
                else { patches[i][j] = nb; off.ox -= patchWidth; off.stride = nb.surface?.gridsPerEdge ?: surfaceStride }
            }
            if (off.oy >= patchWidth) {
                val nb = patch.getNeighborPatch(NORTH)
                if (nb == null) off.oy = patchWidth - 1
                else { patches[i][j] = nb; off.oy -= patchWidth; off.stride = nb.surface?.gridsPerEdge ?: surfaceStride }
            }
        }

        fun z(pi: Int, pj: Int): Float {
            val off = offsets[pi][pj]
            val p   = patches[pi][pj]
            return p.surfaceZData[p.dataOffset + off.ox + off.oy * off.stride]
        }

        val p00 = Vector3(-mpg, -mpg, z(0, 0))
        val p01 = Vector3(-mpg, +mpg, z(0, 1))
        val p10 = Vector3(+mpg, -mpg, z(1, 0))
        val p11 = Vector3(+mpg, +mpg, z(1, 1))

        val c1 = p11 - p00
        val c2 = p01 - p10
        val normal = c1.cross(c2).normalize()

        normData[surfaceStride * y.toInt() + x.toInt()] = normal
    }

    fun calcNormalFlat(x: UInt, y: UInt, index: UInt) {
        val s = surface ?: return
        val surfaceStride = s.gridsPerEdge
        val patchWidth    = s.pvArray.patchWidth.toInt()
        val mpg           = s.getMetersPerGrid()

        data class Offset(var ox: Int, var oy: Int)
        val offsets = arrayOf(
            arrayOf(Offset(x.toInt(), y.toInt()), Offset(x.toInt(), y.toInt() + 1)),
            arrayOf(Offset(x.toInt() + 1, y.toInt()), Offset(x.toInt() + 1, y.toInt() + 1))
        )
        val patches = Array(2) { Array<SurfacePatch>(2) { this } }

        for (i in 0..1) for (j in 0..1) {
            val off = offsets[i][j]
            if (off.ox < 0) { val nb = patches[i][j].getNeighborPatch(WEST);  if (nb == null) off.ox = 0 else { patches[i][j] = nb; off.ox += patchWidth } }
            if (off.oy < 0) { val nb = patches[i][j].getNeighborPatch(SOUTH); if (nb == null) off.oy = 0 else { patches[i][j] = nb; off.oy += patchWidth } }
            if (off.ox >= patchWidth) { val nb = patches[i][j].getNeighborPatch(EAST);  if (nb == null) off.ox = patchWidth - 1 else { patches[i][j] = nb; off.ox -= patchWidth } }
            if (off.oy >= patchWidth) { val nb = patches[i][j].getNeighborPatch(NORTH); if (nb == null) off.oy = patchWidth - 1 else { patches[i][j] = nb; off.oy -= patchWidth } }
        }

        fun z(pi: Int, pj: Int): Float {
            val off = offsets[pi][pj]; val p = patches[pi][pj]
            return p.surfaceZData[p.dataOffset + off.ox + off.oy * surfaceStride]
        }

        val p00 = Vector3(-mpg, -mpg, z(0, 0))
        val p01 = Vector3(-mpg, +mpg, z(0, 1))
        val p10 = Vector3(+mpg, -mpg, z(1, 0))
        val p11 = Vector3(+mpg, +mpg, z(1, 1))

        val normal = if (index == 0u) {
            (p10 - p00).cross(p01 - p00).normalize()
        } else {
            (p11 - p01).cross(p11 - p10).normalize()
        }
        normData[surfaceStride * y.toInt() + x.toInt()] = normal
    }

    fun getNormal(x: UInt, y: UInt): Vector3 {
        val stride = surface?.gridsPerEdge ?: 1
        return normData[stride * y.toInt() + x.toInt()]
    }

    fun getPointAgent(x: UInt, y: UInt): Vector3 {
        val s = surface ?: return Vector3(0f, 0f, 0f)
        val stride      = s.gridsPerEdge
        val pointOffset = x.toInt() + y.toInt() * stride
        val origin      = getOriginAgent()
        return Vector3(
            origin.x + x.toInt() * s.getMetersPerGrid(),
            origin.y + y.toInt() * s.getMetersPerGrid(),
            surfaceZData[dataOffset + pointOffset]
        )
    }

    fun getTexCoords(x: UInt, y: UInt): Vector2 {
        val s      = surface ?: return Vector2(0f, 0f)
        val stride = s.gridsPerEdge
        val pt     = getPointAgent(x, y)
        val rel    = pt - s.getOriginAgent()
        val sc     = 1f / stride.toFloat()
        return Vector2(rel.x * sc, rel.y * sc)
    }

    fun updateCameraDistanceRegion(posRegion: Vector3) {
        val dv   = posRegion - centerRegion
        val dist = maxOf(0f, dv.length() - radius)
        visInfo.distance = dist / maxOf(VOSurfacePatch.lodFactor, 0.1f)
    }

    fun getDistance(): Float = visInfo.distance

    fun updateVerticalStats() {
        if (!dirtyZStats) return
        val s = surface ?: return
        val gppe = s.getGridsPerPatchEdge()
        val gpe  = s.gridsPerEdge
        val mpg  = s.getMetersPerGrid()

        var zMin = surfaceZData[dataOffset]
        var zMax = zMin
        var total = 0f
        var count = 0
        for (j in 0..gppe) {
            for (i in 0..gppe) {
                val z = surfaceZData[dataOffset + i + j * gpe]
                if (z < zMin) zMin = z
                if (z > zMax) zMax = z
                total += z; count++
            }
        }
        minZ  = zMin; maxZ = zMax; meanZ = total / count
        centerRegion = Vector3(centerRegion.x, centerRegion.y, 0.5f * (minZ + maxZ))

        val diam = Vector3(mpg * gppe, mpg * gppe, maxZ - minZ)
        radius   = diam.length() * 0.5f

        s.maxZ       = maxOf(maxZ, s.maxZ)
        s.minZ       = minOf(minZ, s.minZ)
        s.hasZData   = true
        s.region?.calculateCenterGlobal()

        voObj?.dirtyPatch()
        dirtyZStats = false
    }

    fun updateNormals(pbr: Boolean = false) {
        val s = surface ?: return
        if (s.type == 'w'.toInt().toUInt()) return
        val gppe = s.getGridsPerPatchEdge().toUInt()
        var dirtyPatch = false

        if (normalsInvalid[EAST] || normalsInvalid[NORTHEAST] || normalsInvalid[SOUTHEAST]) {
            for (j in 0u..gppe) { calcNormal(gppe, j, 2u, pbr); calcNormal(gppe - 1u, j, 2u, pbr); calcNormal(gppe - 2u, j, 2u, pbr) }
            dirtyPatch = true
        }
        if (normalsInvalid[NORTHEAST] || normalsInvalid[NORTH] || normalsInvalid[NORTHWEST]) {
            for (i in 0u..gppe) { calcNormal(i, gppe, 2u, pbr); calcNormal(i, gppe - 1u, 2u, pbr); calcNormal(i, gppe - 2u, 2u, pbr) }
            dirtyPatch = true
        }
        if (normalsInvalid[NORTHWEST] || normalsInvalid[WEST] || normalsInvalid[SOUTHWEST]) {
            if (getNeighborPatch(NORTH) == null && getNeighborPatch(NORTHWEST)?.hasReceivedData == true) {
                val nb = getNeighborPatch(NORTHWEST)!!
                surfaceZData[dataOffset + gppe.toInt() * s.gridsPerEdge] =
                    nb.surfaceZData[nb.dataOffset + gppe.toInt()]
            }
            for (j in 0u until gppe) { calcNormal(0u, j, 2u, pbr); calcNormal(1u, j, 2u, pbr) }
            dirtyPatch = true
        }
        if (normalsInvalid[SOUTHWEST] || normalsInvalid[SOUTH] || normalsInvalid[SOUTHEAST]) {
            val seNb = getNeighborPatch(SOUTHEAST)
            if (getNeighborPatch(EAST) == null && seNb?.hasReceivedData == true) {
                surfaceZData[dataOffset + gppe.toInt()] =
                    seNb.surfaceZData[seNb.dataOffset + gppe.toInt() * (seNb.surface?.gridsPerEdge ?: 1)]
            }
            for (i in 0u until gppe) { calcNormal(i, 0u, 2u, pbr); calcNormal(i, 1u, 2u, pbr) }
            dirtyPatch = true
        }
        if (normalsInvalid[NORTHEAST]) {
            // NE corner z fixup — mirrors the complex logic in the C++ for cross-surface boundaries.
            val gpe = s.gridsPerEdge
            val neIdx = dataOffset + gppe.toInt() + gppe.toInt() * gpe
            val diagIdx = dataOffset + (gppe.toInt() - 1) + (gppe.toInt() - 1) * gpe
            val neNb = getNeighborPatch(NORTHEAST)
            val nNb  = getNeighborPatch(NORTH)
            val eNb  = getNeighborPatch(EAST)
            if (neNb == null) {
                surfaceZData[neIdx] = when {
                    nNb == null && eNb == null -> surfaceZData[diagIdx]
                    nNb == null && eNb?.hasReceivedData == true -> {
                        val eGppe = eNb.surface?.getGridsPerPatchEdge() ?: gppe.toInt()
                        val eGpe  = eNb.surface?.gridsPerEdge ?: gpe
                        eNb.surfaceZData[eNb.dataOffset + (eGppe - 1) * eGpe]
                    }
                    eNb == null && nNb?.hasReceivedData == true -> {
                        val nGppe = nNb.surface?.getGridsPerPatchEdge() ?: gppe.toInt()
                        nNb.surfaceZData[nNb.dataOffset + (nGppe - 1)]
                    }
                    else -> surfaceZData[diagIdx]
                }
            }
            calcNormal(gppe, gppe, 2u, pbr); calcNormal(gppe, gppe - 1u, 2u, pbr)
            calcNormal(gppe - 1u, gppe, 2u, pbr); calcNormal(gppe - 1u, gppe - 1u, 2u, pbr)
            dirtyPatch = true
        }
        if (normalsInvalid[MIDDLE]) {
            for (j in 2u until gppe - 1u) for (i in 2u until gppe - 1u) calcNormal(i, j, 2u, pbr)
            dirtyPatch = true
        }
        if (dirtyPatch) s.dirtySurfacePatch(this)
        for (i in 0..8) normalsInvalid[i] = false
    }

    fun updateEastEdge() {
        val s = surface ?: return
        val gppe = s.getGridsPerPatchEdge()
        val gpe  = s.gridsPerEdge
        val eNb  = getNeighborPatch(EAST)
        val westSrc: Int
        val eastSrc: Int
        val eastStride: Int
        when {
            eNb == null -> {
                westSrc   = dataOffset + gppe
                eastSrc   = dataOffset + gppe - 1
                eastStride = gpe
            }
            connectedEdge and EAST_EDGE != NO_EDGE -> {
                westSrc    = dataOffset + gppe
                eastSrc    = eNb.dataOffset
                eastStride = eNb.surface?.gridsPerEdge ?: gpe
            }
            else -> return
        }
        for (j in 0 until gppe) {
            surfaceZData[westSrc + j * gpe] = surfaceZData[eastSrc + j * eastStride]
        }
    }

    fun updateNorthEdge() {
        val s = surface ?: return
        val gppe = s.getGridsPerPatchEdge()
        val gpe  = s.gridsPerEdge
        val nNb  = getNeighborPatch(NORTH)
        val southSrc: Int
        val northSrc: Int
        when {
            nNb == null -> {
                southSrc = dataOffset + gppe * gpe
                northSrc = dataOffset + (gppe - 1) * gpe
            }
            connectedEdge and NORTH_EDGE != NO_EDGE -> {
                southSrc = dataOffset + gppe * gpe
                northSrc = nNb.dataOffset
            }
            else -> return
        }
        for (i in 0 until gppe) {
            surfaceZData[southSrc + i] = surfaceZData[northSrc + i]
        }
    }

    fun updateTexture(): Boolean {
        if (!sTexUpdate) return true
        val s    = surface ?: return false
        val r    = s.region  ?: return false
        val eNb  = getNeighborPatch(EAST)
        val wNb  = getNeighborPatch(WEST)
        val sNb  = getNeighborPatch(SOUTH)
        val nNb  = getNeighborPatch(NORTH)
        if ((eNb == null || eNb.hasReceivedData) &&
            (wNb == null || wNb.hasReceivedData) &&
            (sNb == null || sNb.hasReceivedData) &&
            (nNb == null || nNb.hasReceivedData)) {
            if (!heightsGenerated) {
                TODO("GPU: generate composition heights for patch")
            }
            if (voObj != null) {
                voObj!!.dirtyGeom()
                TODO("GPU: markGLRebuild for patch")
            }
        }
        return false
    }

    fun updateGL() {
        updateCompositionStats()
        sTexUpdate = false
    }

    fun dirtyZ() {
        sTexUpdate = true
        for (i in 0..8) normalsInvalid[i] = true
        for (i in 0..7) {
            val nb = getNeighborPatch(i) ?: continue
            nb.normalsInvalid[dirOpposite[i]] = true
            nb.dirty()
            if (i < 4) {
                nb.normalsInvalid[dirAdjacent[dirOpposite[i]][0]] = true
                nb.normalsInvalid[dirAdjacent[dirOpposite[i]][1]] = true
            }
        }
        dirty()
        lastUpdateTime = System.currentTimeMillis()
    }

    fun dirty() {
        voObj?.dirtyGeom() ?: run { /* no VOSurfacePatch yet */ }
        dirtyZStats       = true
        heightsGenerated  = false
        if (!dirty) {
            dirty = true
            surface?.dirtySurfacePatch(this)
        }
    }

    fun clearDirty() { dirty = false }

    fun setHasReceivedData() { hasReceivedData = true }

    fun getVisible(): Boolean = visInfo.isVisible
    fun getRenderStride(): UInt = visInfo.renderStride
    fun getRenderLevel(): Int   = visInfo.renderLevel

    fun getLastUpdateTime(): Long = lastUpdateTime
    fun getMaxZ(): Float = maxZ
    fun getMinZ(): Float = minZ
    fun getMeanComposition(): Float = meanComposition
    fun getMinComposition(): Float  = minComposition
    fun getMaxComposition(): Float  = maxComposition
    fun isHeightsGenerated(): Boolean = heightsGenerated

    fun getOriginGlobal(): Vector3d = originGlobal
    fun getOriginAgent(): Vector3 {
        TODO("APR: convert global origin via agent")
    }

    fun setOriginGlobal(og: Vector3d) {
        originGlobal  = og
        val s         = surface ?: return
        val rel       = og - s.getOriginGlobal()
        originRegion  = Vector3(rel.x.toFloat(), rel.y.toFloat(), rel.z.toFloat())
        val gppe      = s.getGridsPerPatchEdge().toFloat()
        val mpg       = s.getMetersPerGrid()
        centerRegion  = Vector3(originRegion.x + 0.5f * gppe * mpg, originRegion.y + 0.5f * gppe * mpg, centerRegion.z)
        visInfo.isVisible    = false
        visInfo.distance     = 512f
        visInfo.renderLevel  = 0
        visInfo.renderStride = s.getGridsPerPatchEdge().toUInt()
    }

    fun colorPatch(r: UByte, g: UByte, b: UByte) { TODO("GPU: color terrain patch") }

    fun updateVisibility() {
        val vo = voObj ?: return
        val s  = surface ?: return
        val defaultDeltaAngle = 0.15f
        val stridePerDist = defaultDeltaAngle / s.getMetersPerGrid()
        val gppe = s.getGridsPerPatchEdge().toUInt()

        val inFrustum: Boolean = TODO("GPU: camera frustum check for patch center/radius")
        if (inFrustum) {
            val oldStride = visInfo.renderStride
            val maxStride = minOf((visInfo.distance * stridePerDist).toUInt(), 2u * gppe)
            val newLevel  = s.getRenderLevel(maxStride)
            visInfo.renderLevel  = newLevel.toInt()
            visInfo.renderStride = s.getRenderStride(newLevel)
            if (visInfo.renderStride != oldStride) {
                voObj?.dirtyGeom()
                getNeighborPatch(WEST)?.voObj?.dirtyGeom()
                getNeighborPatch(SOUTH)?.voObj?.dirtyGeom()
            }
            visInfo.isVisible = true
        } else {
            visInfo.isVisible = false
        }
    }

    fun clearVObj() { voObj = null }

    private fun updateCompositionStats() {
        val s   = surface ?: return
        val vlp = s.region?.getComposition() ?: return
        TODO("GPU: sample composition layer for min/mean/max")
    }

    // -- constants mirroring C++ gDirOpposite / gDirAdjacent --
    companion object {
        internal val dirOpposite = intArrayOf(WEST, SOUTH, EAST, NORTH, SOUTHWEST, SOUTHEAST, NORTHEAST, NORTHWEST)
        // Adjacent direction pairs for each cardinal direction.
        internal val dirAdjacent = arrayOf(
            intArrayOf(NORTHEAST, SOUTHEAST), // EAST
            intArrayOf(NORTHEAST, NORTHWEST), // NORTH
            intArrayOf(NORTHWEST, SOUTHWEST), // WEST
            intArrayOf(SOUTHWEST, SOUTHEAST)  // SOUTH
        )
    }
}

// Private top-level alias so Surface.kt can also use them.
private val dirOpposite = SurfacePatch.dirOpposite
private val dirAdjacent = SurfacePatch.dirAdjacent
