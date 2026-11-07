package com.firestorm.newview

import kotlin.math.*

abstract class SettingsDayCycle : SettingsBase() {

    companion object {
        val DEFAULT_ASSET_ID = LLUUID("5646d39e-d3d7-6aff-ed71-30fc87d64a91")

        const val SETTING_KEYID      = "key_id"
        const val SETTING_KEYNAME    = "key_name"
        const val SETTING_KEYKFRAME  = "key_keyframe"
        const val SETTING_KEYHASH    = "key_hash"
        const val SETTING_TRACKS     = "tracks"
        const val SETTING_FRAMES     = "frames"

        const val MINIMUM_DAYLENGTH: Int =  14400  // 4 hours
        const val DEFAULT_DAYLENGTH: Int =  14400
        const val MAXIMUM_DAYLENGTH: Int = 604800  // 7 days
        const val MINIMUM_DAYOFFSET: Int =      0
        const val DEFAULT_DAYOFFSET: Int =  57600  // +16h == -8h SLT
        const val MAXIMUM_DAYOFFSET: Int =  86400  // 24 hours
        const val INVALID_DAYOFFSET: Int =     -1

        val TRACK_WATER: UInt        = 0u
        val TRACK_GROUND_LEVEL: UInt = 1u
        val TRACK_MAX: UInt          = 5u
        val FRAME_MAX: UInt          = 56u

        const val DEFAULT_FRAME_SLOP_FACTOR: Float  = 0.02501f
        private const val MULTISLIDER_INCREMENT: Float = 0.005f

        fun getDefaultAssetId(): LLUUID = DEFAULT_ASSET_ID

        fun defaults(): LLSD {
            val frameCount = 8
            val frameStep  = 1f / frameCount.toFloat()
            val frames: LLSD = mutableMapOf()
            val waterTrackArray = mutableListOf<LLSD>()
            val skyTrackArray   = mutableListOf<LLSD>()

            var time = 0f
            for (i in 0 until frameCount) {
                val suffix = ('a' + i).toString()
                val name        = "${DEFAULT_SETTINGS_NAME}$suffix"
                val waterName   = "water:$name"
                val skyName     = "sky:$name"

                waterTrackArray += mutableMapOf(
                    SETTING_KEYKFRAME to time,
                    SETTING_KEYNAME   to waterName
                )
                skyTrackArray += mutableMapOf(
                    SETTING_KEYKFRAME to time,
                    SETTING_KEYNAME   to skyName
                )
                frames[waterName] = SettingsWater.defaults(time)
                frames[skyName]   = SettingsSky.defaults(time)
                time += frameStep
            }

            val tracks = mutableListOf(waterTrackArray, skyTrackArray)
            return mutableMapOf(
                SETTING_NAME   to DEFAULT_SETTINGS_NAME,
                SETTING_TYPE   to "daycycle",
                SETTING_TRACKS to tracks,
                SETTING_FRAMES to frames
            )
        }

        fun validationList(): MutableList<Validator> = mutableListOf(
            Validator(SETTING_TRACKS, true, "Array", ::validateDayCycleTracks),
            Validator(SETTING_FRAMES, true, "Map",   ::validateDayCycleFrames)
        )

        private fun validateDayCycleTracks(value: Any?, flags: UInt): Boolean {
            val tracks = value as? MutableList<*> ?: return false
            while (tracks.size > TRACK_MAX.toInt()) tracks.removeAt(tracks.size - 1)
            var frameCount = 0
            for (track in tracks) {
                val t = track as? MutableList<*> ?: continue
                val iter = t.iterator()
                while (iter.hasNext()) {
                    val elem = iter.next() as? LLSD ?: continue
                    frameCount++
                    if (frameCount > FRAME_MAX.toInt()) { iter.remove(); continue }
                    if (!elem.containsKey(SETTING_KEYKFRAME)) { iter.remove(); continue }
                    val kf = (elem[SETTING_KEYKFRAME] as? Number)?.toFloat() ?: run { iter.remove(); continue }
                    if (kf < 0f || kf > 1f) elem[SETTING_KEYKFRAME] = kf.coerceIn(0f, 1f)
                    if (!elem.containsKey(SETTING_KEYNAME) && !elem.containsKey(SETTING_KEYID)) { iter.remove(); continue }
                }
            }
            val waterCount = (tracks.firstOrNull() as? List<*>)?.size ?: 0
            if (waterCount < 1) return false
            if ((frameCount - waterCount) < 1) return false
            return true
        }

        private fun validateDayCycleFrames(value: Any?, flags: UInt): Boolean {
            val framesMap = value as? LLSD ?: return false
            var hasSky = false; var hasWater = false
            for ((_, frameVal) in framesMap) {
                val frame = frameVal as? LLSD ?: continue
                when (frame[SETTING_TYPE] as? String) {
                    "sky"   -> hasSky   = true
                    "water" -> hasWater = true
                    else    -> return false
                }
            }
            val partial = (flags and Validator.VALIDATION_PARTIAL) != 0u
            if (!partial && (!hasSky || !hasWater)) return false
            return true
        }

        private fun getWrappingDistance(begin: Float, end: Float): Float = when {
            begin < end -> end - begin
            begin > end -> 1f - (begin - end)
            else        -> 0f
        }

        private fun getWrappingAtAfter(collection: MutableMap<Float, SettingsBase>, key: Float): Float? {
            if (collection.isEmpty()) return null
            return collection.keys.firstOrNull { it > key } ?: collection.keys.first()
        }

        private fun getWrappingAtBefore(collection: MutableMap<Float, SettingsBase>, key: Float): Float? {
            if (collection.isEmpty()) return null
            val before = collection.keys.lastOrNull { it <= key }
            return before ?: collection.keys.last()
        }
    }

    var initialized: Boolean = false
        protected set

    private val dayTracks: MutableList<MutableMap<Float, SettingsBase>> =
        MutableList(TRACK_MAX.toInt()) { mutableMapOf() }

    abstract fun buildClone(): SettingsDayCycle
    abstract fun buildDeepCloneAndUncompress(): SettingsDayCycle
    abstract fun getDefaultSky(): SettingsSky
    abstract fun getDefaultWater(): SettingsWater
    abstract fun buildSky(data: LLSD): SettingsSky?
    abstract fun buildWater(data: LLSD): SettingsWater?

    override fun getSettingsType(): String = "daycycle"

    override fun getValidationList(): MutableList<Validator> = validationList()

    override fun blend(end: SettingsBase, mix: Double) {
        error("Day cycles are not blendable")
    }

    override fun buildDerivedClone(): SettingsBase = buildClone()

    override fun getSettings(): LLSD {
        val daySettings: LLSD = mutableMapOf()
        val base = super.getSettings()
        if (base.containsKey(SETTING_NAME))    daySettings[SETTING_NAME]    = base[SETTING_NAME]
        if (base.containsKey(SETTING_ID))      daySettings[SETTING_ID]      = base[SETTING_ID]
        if (base.containsKey(SETTING_ASSETID)) daySettings[SETTING_ASSETID] = base[SETTING_ASSETID]
        daySettings[SETTING_TYPE] = getSettingsType()

        val inUse: MutableMap<String, SettingsBase> = mutableMapOf()
        val tracks = mutableListOf<List<LLSD>>()

        for (track in dayTracks) {
            val trackOut = mutableListOf<LLSD>()
            for ((frame, data) in track) {
                val keyName = data.getHash().toString()
                trackOut += mutableMapOf(
                    SETTING_KEYKFRAME to frame,
                    SETTING_KEYNAME   to keyName
                )
                inUse[keyName] = data
            }
            tracks += trackOut
        }
        daySettings[SETTING_TRACKS] = tracks

        val frames: LLSD = mutableMapOf()
        for ((name, data) in inUse) {
            val fs = data.cloneSettings()
            fs.remove(SETTING_NAME); fs.remove(SETTING_ID); fs.remove(SETTING_HASH)
            frames[name] = fs
        }
        daySettings[SETTING_FRAMES] = frames
        return daySettings
    }

    override fun setLLSDDirty() {
        super.setLLSDDirty()
    }

    fun initialize(validateFrames: Boolean = false): Boolean {
        val s = super.getSettings()
        @Suppress("UNCHECKED_CAST")
        val tracksData = s[SETTING_TRACKS] as? List<List<LLSD>> ?: return false
        @Suppress("UNCHECKED_CAST")
        val framesData = s[SETTING_FRAMES] as? LLSD ?: return false

        val assetId = (s[SETTING_ASSETID] as? String)?.let { LLUUID(it) }

        val used: MutableMap<String, SettingsBase> = mutableMapOf()
        for ((name, data) in framesData) {
            @Suppress("UNCHECKED_CAST")
            val frameData = data as? LLSD ?: continue
            val typeStr = frameData[SETTING_TYPE] as? String ?: continue
            val keyframe: SettingsBase? = when (typeStr) {
                "sky"   -> buildSky(frameData)
                "water" -> buildWater(frameData)
                else    -> null
            }
            if (keyframe != null) used[name] = keyframe
        }

        var hasWater = false; var hasSky = false

        for ((i, trackData) in tracksData.withIndex()) {
            if (i >= TRACK_MAX.toInt()) break
            dayTracks[i].clear()
            for (entry in trackData) {
                var kf = (entry[SETTING_KEYKFRAME] as? Number)?.toFloat() ?: continue
                kf = kf.coerceIn(0f, 1f)
                val keyName = entry[SETTING_KEYNAME] as? String ?: continue
                val setting = used[keyName] ?: continue
                val typeOk = if (i == TRACK_WATER.toInt()) setting.getSettingsType() == "water"
                             else                          setting.getSettingsType() == "sky"
                if (!typeOk) continue

                if (i == TRACK_WATER.toInt()) hasWater = true else hasSky = true

                if (validateFrames && dayTracks[i].isNotEmpty()) {
                    val near = getSettingsNearKeyframe(kf, i, DEFAULT_FRAME_SLOP_FACTOR)
                    if (near != null) {
                        kf = resolveFrameConflict(kf, near.first, i) ?: kf
                    }
                }
                dayTracks[i][kf] = setting
            }
        }

        if (!hasWater || !hasSky) return false

        s.remove(SETTING_TRACKS)
        s.remove(SETTING_FRAMES)
        if (assetId != null && !assetId.isNull()) s[SETTING_ASSETID] = assetId.value

        loadValuesFromLLSD()
        initialized = true
        return true
    }

    private fun resolveFrameConflict(keyframe: Float, found: Float, track: Int): Float? {
        val moveFactor = DEFAULT_FRAME_SLOP_FACTOR + MULTISLIDER_INCREMENT
        var newFrame = keyframe
        val moveForward = !((newFrame < found && (found - newFrame) <= DEFAULT_FRAME_SLOP_FACTOR)
                           || (newFrame > found && (newFrame - found) > DEFAULT_FRAME_SLOP_FACTOR))
        var totalShift = 0f
        var current = found
        if (moveForward) {
            while (totalShift < 1f) {
                val next = dayTracks[track].keys.firstOrNull { it > current } ?: dayTracks[track].keys.first()
                totalShift += moveFactor + (if (current >= newFrame) current else current + 1f) - newFrame
                newFrame = current + moveFactor
                if (newFrame > 1f) newFrame--
                val encroach = (next >= (newFrame - MULTISLIDER_INCREMENT) && (newFrame + DEFAULT_FRAME_SLOP_FACTOR) >= next)
                            || (next < newFrame && (newFrame + DEFAULT_FRAME_SLOP_FACTOR) >= (next + 1f))
                if (!encroach) break
                current = next
            }
        } else {
            while (totalShift < 1f) {
                val prev = dayTracks[track].keys.lastOrNull { it < current } ?: dayTracks[track].keys.last()
                totalShift += moveFactor + newFrame - (if (current <= newFrame) current else current - 1f)
                newFrame = current - moveFactor
                if (newFrame < 0f) newFrame++
                val encroach = (prev <= (newFrame + MULTISLIDER_INCREMENT) && (newFrame - DEFAULT_FRAME_SLOP_FACTOR) <= prev)
                            || (prev > newFrame && (newFrame - DEFAULT_FRAME_SLOP_FACTOR) <= (prev - 1f))
                if (!encroach) break
                current = prev
            }
        }
        return if (totalShift >= 1f) null else newFrame
    }

    override fun updateSettings() {}

    fun getCycleTrack(track: Int): MutableMap<Float, SettingsBase> =
        dayTracks.getOrNull(track) ?: mutableMapOf()

    fun getCycleTrackConst(track: Int): Map<Float, SettingsBase> =
        dayTracks.getOrNull(track) ?: emptyMap()

    fun clearCycleTrack(track: Int): Boolean {
        if (track < 0 || track >= TRACK_MAX.toInt()) return false
        dayTracks[track].clear()
        clearAssetId()
        setDirtyFlag(true)
        return true
    }

    fun replaceCycleTrack(track: Int, source: Map<Float, SettingsBase>): Boolean {
        if (source.isEmpty()) return false
        val firstType = source.values.first().getSettingsType()
        if ((firstType == "water" && track != 0) || (firstType == "sky" && track == 0)) return false
        if (!clearCycleTrack(track)) return false
        dayTracks[track].putAll(source)
        return true
    }

    fun isTrackEmpty(track: Int): Boolean {
        if (track < 0 || track >= TRACK_MAX.toInt()) return true
        return dayTracks[track].isEmpty()
    }

    fun startDayCycle() {
        if (!initialized) error("Attempt to start day cycle on uninitialized object")
    }

    fun getTrackKeyframes(trackNo: Int): List<Float> {
        if (trackNo < 0 || trackNo >= TRACK_MAX.toInt()) return emptyList()
        return dayTracks[trackNo].keys.toList()
    }

    fun moveTrackKeyframe(trackNo: Int, oldFrame: Float, newFrame: Float): Boolean {
        if (trackNo < 0 || trackNo >= TRACK_MAX.toInt()) return false
        if (abs(oldFrame - newFrame) < 1e-6f) return false
        val track = dayTracks[trackNo]
        val setting = track.remove(oldFrame) ?: return false
        track[newFrame.coerceIn(0f, 1f)] = setting
        return true
    }

    fun removeTrackKeyframe(trackNo: Int, frame: Float): Boolean {
        if (trackNo < 0 || trackNo >= TRACK_MAX.toInt()) return false
        return dayTracks[trackNo].remove(frame) != null
    }

    fun setWaterAtKeyframe(water: SettingsWater, keyframe: Float) =
        setSettingsAtKeyframe(water, keyframe, TRACK_WATER.toInt())

    fun getWaterAtKeyframe(keyframe: Float): SettingsWater? =
        getSettingsAtKeyframe(keyframe, TRACK_WATER.toInt()) as? SettingsWater

    fun setSkyAtKeyframe(sky: SettingsSky, keyframe: Float, track: Int) {
        if (track < 1 || track >= TRACK_MAX.toInt()) return
        setSettingsAtKeyframe(sky, keyframe, track)
    }

    fun getSkyAtKeyframe(keyframe: Float, track: Int): SettingsSky? {
        if (track < 1 || track >= TRACK_MAX.toInt()) return null
        return getSettingsAtKeyframe(keyframe, track) as? SettingsSky
    }

    fun setSettingsAtKeyframe(settings: SettingsBase, keyframe: Float, track: Int) {
        if (track < 0 || track >= TRACK_MAX.toInt()) return
        val type = settings.getSettingsType()
        if (track == TRACK_WATER.toInt() && type != "water") return
        if (track != TRACK_WATER.toInt() && type != "sky") return
        dayTracks[track][keyframe.coerceIn(0f, 1f)] = settings
        setDirtyFlag(true)
    }

    fun getSettingsAtKeyframe(keyframe: Float, track: Int): SettingsBase? {
        if (track < 0 || track >= TRACK_MAX.toInt()) return null
        return dayTracks[track][keyframe]
    }

    fun getSettingsNearKeyframe(keyframe: Float, track: Int, fudge: Float): Pair<Float, SettingsBase?>? {
        if (track < 0 || track >= TRACK_MAX.toInt()) return null
        val t = dayTracks[track]
        if (t.isEmpty()) return null

        var startFrame = keyframe - fudge
        if (startFrame < 0f) startFrame += 1f

        val afterKey = getWrappingAtAfter(t, startFrame) ?: return null
        val dist = getWrappingDistance(startFrame, afterKey)

        val nextKey = t.keys.firstOrNull { it > afterKey } ?: t.keys.first()
        return when {
            dist <= MULTISLIDER_INCREMENT && nextKey != afterKey ->
                Pair(nextKey, t[nextKey])
            dist <= fudge * 2f ->
                Pair(afterKey, t[afterKey])
            else ->
                Pair(INVALID_TRACKPOS, null)
        }
    }

    fun getUpperBoundFrame(track: Int, keyframe: Float): Float {
        val t = dayTracks.getOrNull(track) ?: return INVALID_TRACKPOS
        return getWrappingAtAfter(t, keyframe) ?: INVALID_TRACKPOS
    }

    fun getLowerBoundFrame(track: Int, keyframe: Float): Float {
        val t = dayTracks.getOrNull(track) ?: return INVALID_TRACKPOS
        return getWrappingAtBefore(t, keyframe) ?: INVALID_TRACKPOS
    }

    fun setInitialized(value: Boolean = true) { initialized = value }
}
