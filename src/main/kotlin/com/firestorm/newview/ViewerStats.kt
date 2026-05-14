package com.firestorm.newview

import com.firestorm.llcommon.LLTimer
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.sqrt

enum class SimStatId(val index: Int) {
    TIME_DILATION(0),
    FPS(1),
    PHYS_FPS(2),
    AGENT_UPS(3),
    FRAME_MS(4),
    NET_MS(5),
    SIM_OTHER_MS(6),
    SIM_PHYSICS_MS(7),
    AGENT_MS(8),
    IMAGES_MS(9),
    SCRIPT_MS(10),
    NUM_TASKS(11),
    NUM_TASKS_ACTIVE(12),
    NUM_AGENT_MAIN(13),
    NUM_AGENT_CHILD(14),
    NUM_SCRIPTS_ACTIVE(15),
    LSL_IPS(16),
    IN_PPS(17),
    OUT_PPS(18),
    PENDING_DOWNLOADS(19),
    PENDING_UPLOADS(20),
    VIRTUAL_SIZE_KB(21),
    RESIDENT_SIZE_KB(22),
    PENDING_LOCAL_UPLOADS(23),
    TOTAL_UNACKED_BYTES(24),
    PHYSICS_PINNED_TASKS(25),
    PHYSICS_LOD_TASKS(26),
    SIM_PHYSICS_STEP_MS(27),
    SIM_PHYSICS_SHAPE_MS(28),
    SIM_PHYSICS_OTHER_MS(29),
    SIM_PHYSICS_MEMORY(30),
    SCRIPT_EPS(31),
    SIM_SPARE_TIME(32),
    SIM_SLEEP_TIME(33),
    IO_PUMP_TIME(34),
    PCT_SCRIPTS_RUN(35),
    REGION_IDLE(36),
    REGION_IDLE_POSSIBLE(37),
    SIM_AI_STEP_MS(38),
    SKIPPED_AI_SIL_STEPS_PS(39),
    PCT_STEPPED_CHARACTERS(40),
}

data class StatSample(val name: String, val description: String) {
    var value: Double = 0.0
    var count: Long = 0L
    var sum: Double = 0.0
    var sumSquares: Double = 0.0

    fun record(v: Double) {
        value = v
        count++
        sum += v
        sumSquares += v * v
    }

    fun getMean(): Double = if (count == 0L) 0.0 else sum / count

    fun getStdDev(): Double {
        if (count < 2) return 0.0
        val mean = getMean()
        return sqrt((sumSquares / count) - (mean * mean))
    }

    fun reset() {
        value = 0.0; count = 0L; sum = 0.0; sumSquares = 0.0
    }
}

object StatViewer {
    val FPS                           = StatSample("FPS",                   "Frames rendered")
    val PACKETS_IN                    = StatSample("Packets In",            "Packets received")
    val PACKETS_LOST                  = StatSample("packetsloststat",       "Packets lost")
    val PACKETS_OUT                   = StatSample("packetsoutstat",        "Packets sent")
    val TEXTURE_PACKETS               = StatSample("texturepacketsstat",    "Texture data packets received")
    val CHAT_COUNT                    = StatSample("chatcount",             "Chat messages sent")
    val IM_COUNT                      = StatSample("imcount",               "IMs sent")
    val OBJECT_CREATE                 = StatSample("objectcreate",          "Number of objects created")
    val OBJECT_REZ                    = StatSample("objectrez",             "Object rez count")
    val LOGIN_TIMEOUTS                = StatSample("logintimeouts",         "Number of login attempts that timed out")
    val LSL_SAVES                     = StatSample("lslsaves",              "Number of times user has saved a script")
    val ANIMATION_UPLOADS             = StatSample("animationuploads",      "Animations uploaded")
    val FLY                           = StatSample("fly",                   "Fly count")
    val TELEPORT                      = StatSample("teleport",              "Teleport count")
    val DELETE_OBJECT                 = StatSample("deleteobject",          "Objects deleted")
    val SNAPSHOT                      = StatSample("snapshot",              "Snapshots taken")
    val UPLOAD_SOUND                  = StatSample("uploadsound",           "Sounds uploaded")
    val UPLOAD_TEXTURE                = StatSample("uploadtexture",         "Textures uploaded")
    val EDIT_TEXTURE                  = StatSample("edittexture",           "Changes to textures on objects")
    val KILLED                        = StatSample("killed",                "Number of times killed")
    val TEX_BAKES                     = StatSample("texbakes",              "Number of times avatar textures have been baked")
    val TEX_REBAKES                   = StatSample("texrebakes",            "Number of times avatar textures have been forced to rebake")
    val NUM_NEW_OBJECTS               = StatSample("numnewobjectsstat",     "Number of objects in scene that were not previously in cache")
    val TRIANGLES_DRAWN               = StatSample("trianglesdrawnstat",    "Triangles drawn (kilotriangles)")
    val ACTIVE_MESSAGE_DATA_RECEIVED  = StatSample("activemessagedatareceived",   "Message system data received on all active regions (kilobits)")
    val LAYERS_NETWORK_DATA_RECEIVED  = StatSample("layersdatareceived",    "Network data received for layer data (terrain)")
    val OBJECT_NETWORK_DATA_RECEIVED  = StatSample("objectdatareceived",    "Network data received for objects")
    val ASSET_UDP_DATA_RECEIVED       = StatSample("assetudpdatareceived",  "Network data received for assets over UDP")
    val TEXTURE_NETWORK_DATA_RECEIVED = StatSample("texturedatareceived",   "Network data received for textures")
    val MESSAGE_SYSTEM_DATA_IN        = StatSample("messagedatain",         "Incoming message system network data")
    val MESSAGE_SYSTEM_DATA_OUT       = StatSample("messagedataout",        "Outgoing message system network data")

    val SIM_TIME_DILATION             = StatSample("simtimedilation",       "Simulator time scale")
    val SIM_FPS                       = StatSample("simfps",                "Simulator framerate")
    val SIM_PHYSICS_FPS               = StatSample("simphysicsfps",         "Simulator physics framerate")
    val SIM_AGENT_UPS                 = StatSample("simagentups",           "")
    val SIM_SCRIPT_EPS                = StatSample("simscripteps",          "")
    val SIM_SKIPPED_SILHOUETTE        = StatSample("simsimskippedsilhouettesteps", "")
    val SIM_MAIN_AGENTS               = StatSample("simmainagents",         "Number of avatars in current region")
    val SIM_CHILD_AGENTS              = StatSample("simchildagents",        "Number of avatars in neighboring regions")
    val SIM_OBJECTS                   = StatSample("simobjects",            "")
    val SIM_ACTIVE_OBJECTS            = StatSample("simactiveobjects",      "Number of scripted and/or moving objects")
    val SIM_ACTIVE_SCRIPTS            = StatSample("simactivescripts",      "Number of scripted objects")
    val SIM_IN_PACKETS_PER_SEC        = StatSample("siminpps",              "")
    val SIM_OUT_PACKETS_PER_SEC       = StatSample("simoutpps",             "")
    val SIM_PENDING_DOWNLOADS         = StatSample("simpendingdownloads",   "")
    val SIM_PENDING_UPLOADS           = StatSample("simpendinguploads",     "")
    val SIM_PENDING_LOCAL_UPLOADS     = StatSample("simpendinglocaluploads","")
    val SIM_PHYSICS_PINNED_TASKS      = StatSample("physicspinnedtasks",    "")
    val SIM_PHYSICS_LOD_TASKS         = StatSample("physicslodtasks",       "")
    val SIM_PERCENTAGE_SCRIPTS_RUN    = StatSample("simpctscriptsrun",      "")
    val SIM_SKIPPED_CHARACTERS_PCT    = StatSample("simsimpctsteppedcharacters", "")

    val FPS_SAMPLE                    = StatSample("fpssample",             "")
    val NUM_IMAGES                    = StatSample("numimagesstat",         "")
    val NUM_RAW_IMAGES                = StatSample("numrawimagesstat",      "")
    val NUM_OBJECTS                   = StatSample("numobjectsstat",        "")
    val NUM_MATERIALS                 = StatSample("nummaterials",          "")
    val NUM_ACTIVE_OBJECTS            = StatSample("numactiveobjectsstat",  "")
    val ENABLE_VBO                    = StatSample("enablevbo",             "Vertex Buffers Enabled")
    val LIGHTING_DETAIL               = StatSample("lightingdetail",        "")
    val VISIBLE_AVATARS               = StatSample("visibleavatars",        "Visible Avatars")
    val SHADER_OBJECTS                = StatSample("shaderobjects",         "Object Shaders")
    val DRAW_DISTANCE                 = StatSample("drawdistance",          "Draw Distance")
    val WINDOW_WIDTH                  = StatSample("windowwidth",           "Window width")
    val WINDOW_HEIGHT                 = StatSample("windowheight",          "Window height")
    val PACKETS_LOST_PERCENT          = StatSample("packetslostpercentstat","")
    val FORMATTED_MEM                 = StatSample("formattedmemstat",      "")

    val SIM_FRAME_TIME                = StatSample("simframemsec",          "")
    val SIM_NET_TIME                  = StatSample("simnetmsec",            "")
    val SIM_OTHER_TIME                = StatSample("simsimothermsec",       "")
    val SIM_PHYSICS_TIME              = StatSample("simsimphysicsmsec",     "")
    val SIM_PHYSICS_STEP_TIME         = StatSample("simsimphysicsstepmsec", "")
    val SIM_PHYSICS_SHAPE_UPDATE_TIME = StatSample("simsimphysicsshapeupdatemsec", "")
    val SIM_PHYSICS_OTHER_TIME        = StatSample("simsimphysicsothermsec","")
    val SIM_AI_TIME                   = StatSample("simsimaistepmsec",      "")
    val SIM_AGENTS_TIME               = StatSample("simagentmsec",          "")
    val SIM_IMAGES_TIME               = StatSample("simimagesmsec",         "")
    val SIM_SCRIPTS_TIME              = StatSample("simscriptmsec",         "")
    val SIM_SPARE_TIME                = StatSample("simsparemsec",          "")
    val SIM_SLEEP_TIME                = StatSample("simsleepmsec",          "")
    val SIM_PUMP_IO_TIME              = StatSample("simpumpiomsec",         "")
    val SIM_UNACKED_BYTES             = StatSample("simtotalunackedbytes",  "")
    val SIM_PHYSICS_MEM               = StatSample("physicsmemoryallocated","")

    val FRAMETIME_JITTER              = StatSample("frametimejitter",       "Average delta between successive frame times")
    val FRAMETIME                     = StatSample("frametime",             "Measured frame time")
    val SIM_PING                      = StatSample("simpingstat",           "")
    val FRAMETIME_JITTER_99TH         = StatSample("frametimejitter99",     "99th percentile of frametime jitter over the last 5 seconds")
    val FRAMETIME_JITTER_95TH         = StatSample("frametimejitter95",     "95th percentile of frametime jitter over the last 5 seconds")
    val FRAMETIME_99TH                = StatSample("frametime99",           "99th percentile of frametime over the last 5 seconds")
    val FRAMETIME_95TH                = StatSample("frametime95",           "95th percentile of frametime over the last 5 seconds")
    val FRAMETIME_JITTER_CUMULATIVE   = StatSample("frametimejitcumulative","Cumulative frametime jitter over the session")
    val FRAMETIME_JITTER_STDDEV       = StatSample("frametimejitterstddev", "Standard deviation of frametime jitter in a 5 second period")
    val FRAMETIME_STDDEV              = StatSample("frametimestddev",       "Standard deviation of frametime in a 5 second period")
    val FRAMETIME_JITTER_EVENTS       = StatSample("frametimeevents",       "Number of frametime events in the session")
    val FRAMETIME_JITTER_EVENTS_PER_MINUTE = StatSample("frametimeeventspm","Average number of frametime events per minute")
    val FRAMETIME_JITTER_EVENTS_LAST_MINUTE = StatSample("frametimeeventslastmin", "Number of frametime events in the last minute")
    val NORMALIZED_FRAMETIME_JITTER_SESSION = StatSample("normalizedframetimejitter", "Normalized frametime jitter over the session")
    val NFTV                          = StatSample("nftv",                  "Normalized frametime variation")
    val NORMALIZED_FRAMETIME_JITTER_PERIOD  = StatSample("normalizedframetimejitterperiod", "Normalized frametime jitter over the last 5 seconds")

    val AGENT_POSITION_SNAP           = StatSample("agentpositionsnap",     "agent position corrections (meters)")
    val LOADING_WEARABLES_LONG_DELAY  = StatSample("loadingwearableslongdelay", "Wearables took too long to load")
    val REGION_CROSSING_TIME          = StatSample("regioncrossingtime",    "CROSSING_AVG")
    val FRAME_STACKTIME               = StatSample("framestacktime",        "FRAME_SECS")
    val UPDATE_STACKTIME              = StatSample("updatestacktime",       "UPDATE_SECS")
    val NETWORK_STACKTIME             = StatSample("networkstacktime",      "NETWORK_SECS")
    val IMAGE_STACKTIME               = StatSample("imagestacktime",        "IMAGE_SECS")
    val REBUILD_STACKTIME             = StatSample("rebuildstacktime",      "REBUILD_SECS")
    val RENDER_STACKTIME              = StatSample("renderstacktime",       "RENDER_SECS")
    val AVATAR_EDIT_TIME              = StatSample("avataredittime",        "Seconds in Edit Appearance")
    val TOOLBOX_TIME                  = StatSample("toolboxtime",           "Seconds using Toolbox")
    val MOUSELOOK_TIME                = StatSample("mouselooktime",         "Seconds in Mouselook")
    val OBJECT_CACHE_HIT_RATE         = StatSample("object_cache_hits",     "")

    val SCENERY_FRAME_PCT             = StatSample("scenery_frame_pct",     "")
    val AVATAR_FRAME_PCT              = StatSample("avatar_frame_pct",      "")
    val HUDS_FRAME_PCT                = StatSample("huds_frame_pct",        "")
    val UI_FRAME_PCT                  = StatSample("ui_frame_pct",          "")
    val SWAP_FRAME_PCT                = StatSample("swap_frame_pct",        "")
    val IDLE_FRAME_PCT                = StatSample("idle_frame_pct",        "")
}

const val SEND_STATS_PERIOD: Float = 300.0f

private fun <T : Comparable<T>> calcPercentile(sorted: List<T>, percent: Double): T? {
    if (sorted.isEmpty()) return null
    val idx = percent * (sorted.size - 1)
    val idxBelow = floor(idx).toInt()
    val idxAbove = ceil(idx).toInt()
    return sorted[if (idxBelow == idxAbove) idxBelow else idxBelow]
}

private fun calcStddevDoubles(values: List<Double>): Double {
    if (values.size < 2) return 0.0
    val mean = values.average()
    val variance = values.sumOf { (it - mean) * (it - mean) } / values.size
    return sqrt(variance)
}

object ViewerStats {

    private var lastTimeDiff: Double = 0.0
    private var totalFrametimeJitter: Double = 0.0
    private var frameJitterEvents: UInt = 0u
    private var frameJitterEventsLastMinute: UInt = 0u
    private var eventMinutes: UInt = 0u
    private var totalTime: Double = 0.0
    private var lastFrameTimeSample: Double = 0.0
    private var timeSinceLastEventSample: Double = 0.0
    private val frameTimes: MutableList<Double> = mutableListOf()
    private val frameTimesJitter: MutableList<Double> = mutableListOf()

    var lastNormalizedSessionJitter: Double = 0.0
        private set
    var lastNormalizedFrametimeVariance: Double = 0.0
        private set
    var lastNormalizedPeriodJitter: Double = 0.0
        private set

    var frameCount: Long = 0L

    fun resetStats() {
        StatViewer.FPS.reset()
        StatViewer.FRAMETIME_JITTER.reset()
        frameTimes.clear()
        frameTimesJitter.clear()
        lastTimeDiff = 0.0
        totalFrametimeJitter = 0.0
        frameJitterEvents = 0u
        frameJitterEventsLastMinute = 0u
        eventMinutes = 0u
        totalTime = 0.0
        lastFrameTimeSample = 0.0
        timeSinceLastEventSample = 0.0
    }

    fun updateFrameStats(timeDiff: Double) {
        if (frameCount > 0 && lastTimeDiff > 0.0) {
            totalTime += timeDiff
            StatViewer.FRAMETIME.record(timeDiff)

            val jit = abs(lastTimeDiff - timeDiff)
            StatViewer.FRAMETIME_JITTER.record(jit)
            totalFrametimeJitter += jit
            StatViewer.FRAMETIME_JITTER_CUMULATIVE.record(totalFrametimeJitter)

            val normalizedSession = if (totalTime > 0.0) totalFrametimeJitter / totalTime else 0.0
            StatViewer.NORMALIZED_FRAMETIME_JITTER_SESSION.record(normalizedSession)
            lastNormalizedSessionJitter = normalizedSession

            val frameTimeEventThreshold = 0.1
            if (timeDiff - lastTimeDiff > lastTimeDiff * frameTimeEventThreshold) {
                StatViewer.FRAMETIME_JITTER_EVENTS.record(frameJitterEvents.toDouble())
                frameJitterEvents++
                frameJitterEventsLastMinute++
            }

            frameTimes.add(timeDiff)
            frameTimesJitter.add(jit)
            lastFrameTimeSample += timeDiff
            timeSinceLastEventSample += timeDiff

            val frameTimeSampleSeconds = 5.0
            if (lastFrameTimeSample >= frameTimeSampleSeconds) {
                val sortedTimes = frameTimes.sorted()
                val sortedJitters = frameTimesJitter.sorted()

                val ftStddev = calcStddevDoubles(sortedTimes)
                StatViewer.FRAMETIME_STDDEV.record(ftStddev)

                calcPercentile(sortedTimes, 0.99)?.let { StatViewer.FRAMETIME_99TH.record(it) }
                calcPercentile(sortedTimes, 0.95)?.let { StatViewer.FRAMETIME_95TH.record(it) }

                val jitterStddev = calcStddevDoubles(sortedJitters)
                StatViewer.FRAMETIME_JITTER_STDDEV.record(jitterStddev)

                calcPercentile(sortedJitters, 0.99)?.let { StatViewer.FRAMETIME_JITTER_99TH.record(it) }
                calcPercentile(sortedJitters, 0.95)?.let { StatViewer.FRAMETIME_JITTER_95TH.record(it) }

                val avgFrameTime = if (sortedTimes.isEmpty()) 0.0 else sortedTimes.average()
                val nftv = if (avgFrameTime != 0.0) jitterStddev / avgFrameTime else 0.0
                StatViewer.NFTV.record(nftv)
                lastNormalizedFrametimeVariance = nftv

                val totalJitter = frameTimesJitter.sum()
                lastNormalizedPeriodJitter = if (lastFrameTimeSample > 0.0) totalJitter / lastFrameTimeSample else 0.0
                StatViewer.NORMALIZED_FRAMETIME_JITTER_PERIOD.record(lastNormalizedPeriodJitter)

                frameTimes.clear()
                frameTimesJitter.clear()
                lastFrameTimeSample = 0.0
            }

            if (timeSinceLastEventSample >= 60.0) {
                eventMinutes++
                val eventsPerMinute = if (eventMinutes > 0u) frameJitterEvents.toLong() / eventMinutes.toLong() else 0L
                StatViewer.FRAMETIME_JITTER_EVENTS_PER_MINUTE.record(eventsPerMinute.toDouble())
                StatViewer.FRAMETIME_JITTER_EVENTS_LAST_MINUTE.record(frameJitterEventsLastMinute.toDouble())
                frameJitterEventsLastMinute = 0u
                timeSinceLastEventSample = 0.0
            }
        }
        lastTimeDiff = timeDiff
    }

    fun addToMessage(body: MutableMap<String, Any>) {
        val misc = mutableMapOf<String, Any>()
        misc["Version"] = true
        misc["Vertex Buffers Enabled"] = StatViewer.ENABLE_VBO.value
        body["misc"] = misc
        body["AgentPositionSnaps"] = StatViewer.AGENT_POSITION_SNAP.sum
    }

    class PhaseMap {
        private val phaseMap: MutableMap<String, LLTimer> = mutableMapOf()

        fun getPhaseTimer(phaseName: String): LLTimer {
            return phaseMap.getOrPut(phaseName) { LLTimer() }
        }

        fun getPhaseValues(phaseName: String): Triple<Boolean, Float, Boolean> {
            val timer = phaseMap[phaseName] ?: return Triple(false, 0f, false)
            return Triple(true, timer.getElapsedTimeF32(), !timer.getStarted())
        }

        fun startPhase(phaseName: String) {
            getPhaseTimer(phaseName).start()
        }

        fun stopPhase(phaseName: String) {
            val timer = phaseMap[phaseName] ?: return
            if (timer.getStarted()) timer.stop()
        }

        fun clearPhases() {
            phaseMap.clear()
        }

        fun asMap(): Map<String, Map<String, Any>> {
            return phaseMap.map { (name, timer) ->
                name to mapOf<String, Any>(
                    "completed" to if (!timer.getStarted()) 1 else 0,
                    "elapsed" to timer.getElapsedTimeF32()
                )
            }.toMap()
        }

        companion object {
            private val stats: MutableMap<String, StatsAccumulator> = mutableMapOf()

            fun getPhaseStats(phaseName: String): StatsAccumulator {
                return stats.getOrPut(phaseName) { StatsAccumulator() }
            }

            fun recordPhaseStat(phaseName: String, value: Float) {
                getPhaseStats(phaseName).push(value)
            }
        }
    }
}

class StatsAccumulator {
    private var count: Int = 0
    private var sum: Double = 0.0
    private var sumSquares: Double = 0.0
    private var min: Float = Float.MAX_VALUE
    private var max: Float = Float.MIN_VALUE

    fun push(value: Float) {
        count++
        sum += value
        sumSquares += value.toDouble() * value
        if (value < min) min = value
        if (value > max) max = value
    }

    fun getCount(): Int = count
    fun getMean(): Float = if (count == 0) 0f else (sum / count).toFloat()
    fun getMin(): Float = if (count == 0) 0f else min
    fun getMax(): Float = if (count == 0) 0f else max
    fun getStdDev(): Float {
        if (count < 2) return 0f
        val mean = sum / count
        return sqrt((sumSquares / count) - mean * mean).toFloat()
    }
}

var gTotalTextureData: Long = 0L
var gTotalObjectData: Long = 0L
val gTotalTextureBytesPerBoostLevel: MutableList<Long> = MutableList(32) { 0L }

fun updateStatistics() {
    System.err.println("ViewerStats: updateStatistics not yet implemented")
}

fun sendViewerStats(includePreferences: Boolean) {
    System.err.println("ViewerStats: sendViewerStats not yet implemented")
}

fun updateTextureTime() {
    System.err.println("ViewerStats: updateTextureTime not yet implemented")
}
