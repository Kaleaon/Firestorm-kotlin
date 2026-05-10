package com.firestorm.newview

import java.util.UUID

const val MAX_WAIT_ANIM_SECS: Float = 60.0f
const val MAX_WAIT_KEY_SECS: Float = 60.0f * 10.0f

interface GestureManagerObserver {
    fun changed()
}

object GestureMgr {

    val active: MutableMap<UUID, MultiGesture?> = mutableMapOf()
    private val playing: MutableList<MultiGesture> = mutableListOf()
    private val observers: MutableList<GestureManagerObserver> = mutableListOf()
    private val callbackMap: MutableMap<UUID, (MultiGesture) -> Unit> = mutableMapOf()
    private val loadingAssets: MutableSet<UUID> = mutableSetOf()
    private var loadingCount: Int = 0
    private var deactivateSimilarNames: String = ""
    private var valid: Boolean = false

    fun init() {
        TODO("APR: use JVM equivalent — hook inventory observer registration")
    }

    fun update() {
        if (!areGesturesEnabled()) {
            val copy = playing.toList()
            for (gesture in copy) stopGesture(gesture)
            return
        }

        for (i in playing.indices) {
            stepGesture(playing[i])
        }

        val newEnd = playing.partition { it.playing }
        val done = newEnd.second
        playing.clear()
        playing.addAll(newEnd.first)

        if (done.isNotEmpty()) {
            for (gesture in done) {
                gesture.doneCallback?.invoke(gesture, gesture.callbackData)
            }
            notifyObservers()
        }
    }

    fun activateGesture(itemId: UUID) {
        TODO("APR: look up inventory item by itemId to get assetId, then call activateGestureWithAsset")
    }

    fun activateGestures(items: List<InventoryItem>) {
        var count = 0
        for (item in items) {
            if (isGestureActive(item.uuid)) continue
            activateGesture(item.uuid)
            count++
        }

        loadingCount = count
        deactivateSimilarNames = ""

        for (item in items) {
            if (isGestureActive(item.uuid)) continue
            activateGestureWithAsset(item.uuid, item.assetUuid, informServer = false, deactivateSimilar = true)
        }

        TODO("APR: send ActivateGestures bulk message to server")
    }

    fun activateGestureWithAsset(itemId: UUID, assetId: UUID, informServer: Boolean, deactivateSimilar: Boolean) {
        val baseItemId = linkedItemId(itemId)

        if (isGestureActive(itemId)) {
            return
        }

        active[baseItemId] = null

        if (assetId != UUID(0, 0)) {
            TODO("APR: fetch asset data for assetId, then call onLoadComplete with LoadInfo(baseItemId, informServer, deactivateSimilar)")
        } else {
            notifyObservers()
        }
    }

    fun deactivateGesture(itemId: UUID) {
        val baseItemId = linkedItemId(itemId)
        val gesture = active[baseItemId]
        if (!active.containsKey(baseItemId)) {
            return
        }

        gesture?.let { stopGesture(it) }
        active.remove(baseItemId)

        TODO("APR: send DeactivateGestures message to server and remove COF item link")
    }

    fun deactivateSimilarGestures(inGesture: MultiGesture, inItemId: UUID) {
        val baseInItemId = linkedItemId(inItemId)
        val gestureItemIds = mutableListOf<UUID>()

        val iter = active.iterator()
        while (iter.hasNext()) {
            val entry = iter.next()
            val itemId = entry.key
            val gest = entry.value

            if (gest == null || itemId == baseInItemId) continue

            val triggerMatch = gest.trigger.isNotEmpty() && gest.trigger == inGesture.trigger
            val keyMatch = gest.key != KEY_NONE && gest.key == inGesture.key && gest.mask == inGesture.mask

            if (triggerMatch || keyMatch) {
                gestureItemIds.add(itemId)
                stopGesture(gest)
                iter.remove()
                TODO("APR: mark inventory label changed for itemId")
            }
        }

        TODO("APR: send bulk DeactivateGestures message for gestureItemIds")

        for (id in gestureItemIds) {
            val name = inventoryItemName(id) ?: continue
            deactivateSimilarNames += name + "\n"
        }

        notifyObservers()
    }

    fun isGestureActive(itemId: UUID): Boolean {
        val baseItemId = linkedItemId(itemId)
        return active.containsKey(baseItemId)
    }

    fun isGesturePlaying(itemId: UUID): Boolean {
        val baseItemId = linkedItemId(itemId)
        return active[baseItemId]?.playing ?: false
    }

    fun isGesturePlaying(gesture: MultiGesture?): Boolean = gesture?.playing ?: false

    fun replaceGesture(itemId: UUID, newGesture: MultiGesture, assetId: UUID) {
        val baseItemId = linkedItemId(itemId)
        val oldGesture = active[baseItemId] ?: return

        stopGesture(oldGesture)
        active.remove(baseItemId)
        active[baseItemId] = newGesture

        if (oldGesture !== newGesture) {
            // oldGesture is no longer needed
        }

        if (assetId != UUID(0, 0)) {
            loadingCount = 1
            deactivateSimilarNames = ""
            TODO("APR: fetch asset data for assetId, then call onLoadComplete")
        }

        notifyObservers()
    }

    fun replaceGesture(itemId: UUID, newAssetId: UUID) {
        val baseItemId = linkedItemId(itemId)
        val gesture = active[baseItemId] ?: return
        replaceGesture(baseItemId, gesture, newAssetId)
    }

    fun playGesture(gesture: MultiGesture, fromKeyPress: Boolean = false) {
        if (!areGesturesEnabled()) return
        if (!canPlayGestures()) return

        gesture.reset()
        gesture.triggeredByKey = fromKeyPress
        gesture.playing = true
        playing.add(gesture)

        for (step in gesture.steps) {
            when (step.getType()) {
                StepType.STEP_ANIMATION -> {
                    val animStep = step as GestureStepAnimation
                    val animId = animStep.animAssetID
                    if (animId != UUID(0, 0) && (animStep.flags and ANIM_FLAG_STOP) == 0) {
                        loadingAssets.add(animId)
                        TODO("APR: fetch AT_ANIMATION asset for animId via asset storage, remove from loadingAssets on complete")
                    }
                }
                StepType.STEP_SOUND -> {
                    val soundStep = step as GestureStepSound
                    val soundId = soundStep.soundAssetID
                    if (soundId != UUID(0, 0)) {
                        loadingAssets.add(soundId)
                        TODO("APR: fetch AT_SOUND asset for soundId via asset storage, remove from loadingAssets on complete")
                    }
                }
                else -> {}
            }
        }

        stepGesture(gesture)
        notifyObservers()
    }

    fun playGesture(itemId: UUID) {
        if (!areGesturesEnabled()) return
        val baseItemId = linkedItemId(itemId)
        val gesture = active[baseItemId] ?: return
        playGesture(gesture)
    }

    fun stopGesture(gesture: MultiGesture?) {
        gesture ?: return

        for (animId in gesture.requestedAnimIDs) {
            TODO("APR: send ANIM_REQUEST_STOP for animId via agent")
        }
        for (animId in gesture.playingAnimIDs) {
            TODO("APR: send ANIM_REQUEST_STOP for animId via agent")
        }

        playing.removeAll { it === gesture }

        gesture.reset()

        gesture.doneCallback?.invoke(gesture, gesture.callbackData)

        notifyObservers()
    }

    fun stopGesture(itemId: UUID) {
        val baseItemId = linkedItemId(itemId)
        val gesture = active[baseItemId] ?: return
        stopGesture(gesture)
    }

    fun setGestureLoadedCallback(invItemId: UUID, cb: (MultiGesture) -> Unit) {
        callbackMap[invItemId] = cb
    }

    fun triggerGesture(key: Int, mask: Int): Boolean {
        if (!areGesturesEnabled()) return false

        val matching = active.values.filterNotNull().filter {
            it.key == key && it.mask == mask && !it.waitingKeyRelease
        }

        if (matching.isNotEmpty()) {
            val gesture = matching[(Math.random() * matching.size).toInt()]
            playGesture(gesture, fromKeyPress = true)
            return true
        }
        return false
    }

    fun triggerGestureRelease(key: Int, mask: Int): Boolean {
        if (!areGesturesEnabled()) return false

        val matching = active.values.filterNotNull().filter {
            it.key == key && it.mask == mask
        }
        for (gesture in matching) {
            gesture.keyReleased = true
        }
        return matching.isNotEmpty()
    }

    fun triggerAndReviseString(str: String, revisedString: StringBuilder? = null): Boolean {
        if (!areGesturesEnabled()) {
            revisedString?.append(str)
            return false
        }

        val tokens = str.split(" ").filter { it.isNotEmpty() }
        var foundGestures = false
        var firstToken = true

        for (token in tokens) {
            var gesture: MultiGesture? = null

            if (!foundGestures) {
                val matching = active.values.filterNotNull().filter {
                    it.trigger.equals(token, ignoreCase = true)
                }

                if (matching.isNotEmpty()) {
                    gesture = matching[(Math.random() * matching.size).toInt()]
                    playGesture(gesture)

                    if (gesture.replaceText.isNotEmpty()) {
                        if (!firstToken) revisedString?.append(" ")
                        if (gesture.replaceText.equals(token, ignoreCase = true)) {
                            revisedString?.append(token)
                        } else {
                            revisedString?.append(gesture.replaceText)
                        }
                    }
                    foundGestures = true
                }
            }

            if (gesture == null) {
                if (!firstToken) revisedString?.append(" ")
                revisedString?.append(token)
            }

            firstToken = false
        }
        return foundGestures
    }

    fun isKeyBound(key: Int, mask: Int): Boolean {
        return active.values.filterNotNull().any { it.key == key && it.mask == mask }
    }

    fun getPlayingCount(): Int = playing.size

    fun addObserver(observer: GestureManagerObserver) {
        observers.add(observer)
    }

    fun removeObserver(observer: GestureManagerObserver) {
        observers.remove(observer)
    }

    fun notifyObservers() {
        for (observer in observers.toList()) {
            observer.changed()
        }
    }

    fun onInventoryChanged(mask: UInt) {
        val GESTURE = 0x40u
        val LABEL = 0x2u
        val ADD = 0x4u
        val REMOVE = 0x8u
        val STRUCTURE = 0x10u

        if (mask and GESTURE != 0u) {
            if (mask and LABEL != 0u) {
                for ((itemId, gesture) in active) {
                    if (gesture != null) {
                        val name = inventoryItemName(itemId)
                        if (name != null) gesture.name = name
                    }
                }
                notifyObservers()
            } else if (mask and ADD != 0u || mask and REMOVE != 0u || mask and STRUCTURE != 0u) {
                notifyObservers()
            }
        }
    }

    fun matchPrefix(inStr: String, outStr: StringBuilder): Boolean {
        if (!areGesturesEnabled()) return false

        val inLen = inStr.length

        for ((_, gesture) in active) {
            gesture ?: continue
            val trigger = gesture.trigger
            if (inStr.equals(trigger, ignoreCase = true)) {
                outStr.append(trigger)
                return true
            }
        }

        var restOfMatch = ""
        for ((_, gesture) in active) {
            gesture ?: continue
            val trigger = gesture.trigger
            if (inLen > trigger.length) continue

            val triggerTrunc = trigger.substring(0, inLen)
            if (!inStr.equals(triggerTrunc, ignoreCase = true)) continue

            val curRest = trigger.substring(inLen)
            if (restOfMatch.isEmpty()) {
                restOfMatch = curRest
            }

            var buf = ""
            var i = 0
            while (i < restOfMatch.length && i < curRest.length) {
                if (restOfMatch[i] == curRest[i]) {
                    buf += restOfMatch[i]
                } else {
                    if (i == 0) {
                        restOfMatch = ""
                    }
                    break
                }
                i++
            }

            if (restOfMatch.isEmpty()) return true
            if (buf.isNotEmpty()) restOfMatch = buf
        }

        if (restOfMatch.isNotEmpty()) {
            outStr.append(inStr + restOfMatch)
            return true
        }
        return false
    }

    fun getItemIDs(ids: MutableList<UUID>) {
        ids.addAll(active.keys)
    }

    fun done() {
        var notify = false
        for ((itemId, gesture) in active) {
            if (gesture != null && gesture.name.isEmpty()) {
                val name = inventoryItemName(itemId)
                if (name != null) {
                    gesture.name = name
                    notify = true
                }
            }
        }
        if (notify) notifyObservers()
    }

    private fun stepGesture(gesture: MultiGesture) {
        if (!isAgentAvatarValid()) return
        if (hasLoadingAssets(gesture)) return

        TODO("APR: sync gesture.playingAnimIDs and gesture.requestedAnimIDs against avatar's signaled animations")

        var waiting = false
        while (!waiting && gesture.playing) {
            val step: GestureStep? = if (gesture.currentStep < gesture.steps.size) {
                gesture.steps[gesture.currentStep]
            } else {
                gesture.waitingAtEnd = true
                null
            }

            if (gesture.waitingAtEnd) {
                if (gesture.requestedAnimIDs.isEmpty() && gesture.playingAnimIDs.isEmpty()) {
                    gesture.waitingAtEnd = false
                    gesture.playing = false
                } else {
                    waiting = true
                }
                continue
            }

            if (gesture.waitingKeyRelease) {
                if (gesture.keyReleased) {
                    gesture.waitingKeyRelease = false
                    gesture.currentStep++
                } else if (elapsedSeconds(gesture.waitTimerStart) > MAX_WAIT_KEY_SECS) {
                    gesture.waitingKeyRelease = false
                    gesture.currentStep++
                } else {
                    waiting = true
                }
                continue
            }

            if (gesture.waitingAnimations) {
                if (gesture.requestedAnimIDs.isEmpty() && gesture.playingAnimIDs.isEmpty()) {
                    gesture.waitingAnimations = false
                    gesture.currentStep++
                } else if (elapsedSeconds(gesture.waitTimerStart) > MAX_WAIT_ANIM_SECS) {
                    gesture.waitingAnimations = false
                    gesture.currentStep++
                } else {
                    waiting = true
                }
                continue
            }

            if (gesture.waitingTimer) {
                val waitStep = step as GestureStepWait
                if (elapsedSeconds(gesture.waitTimerStart) > waitStep.waitSeconds) {
                    gesture.waitingTimer = false
                    gesture.currentStep++
                } else {
                    waiting = true
                }
                continue
            }

            runStep(gesture, step!!)
        }
    }

    private fun runStep(gesture: MultiGesture, step: GestureStep) {
        when (step.getType()) {
            StepType.STEP_ANIMATION -> {
                val animStep = step as GestureStepAnimation
                if (animStep.animAssetID == UUID(0, 0)) {
                    gesture.currentStep++
                    return
                }
                if (animStep.flags and ANIM_FLAG_STOP != 0) {
                    TODO("APR: send ANIM_REQUEST_STOP for animStep.animAssetID")
                    gesture.requestedAnimIDs.remove(animStep.animAssetID)
                } else {
                    TODO("APR: send ANIM_REQUEST_START for animStep.animAssetID")
                    gesture.requestedAnimIDs.add(animStep.animAssetID)
                }
                gesture.currentStep++
            }
            StepType.STEP_SOUND -> {
                val soundStep = step as GestureStepSound
                TODO("APR: trigger sound ${soundStep.soundAssetID} at volume 1.0f")
                gesture.currentStep++
            }
            StepType.STEP_CHAT -> {
                val chatStep = step as GestureStepChat
                TODO("APR: send chat message '${chatStep.chatText}' as CHAT_TYPE_NORMAL (no animate)")
                gesture.currentStep++
            }
            StepType.STEP_WAIT -> {
                val waitStep = step as GestureStepWait
                when {
                    gesture.triggeredByKey && !gesture.waitingKeyRelease &&
                            (waitStep.flags and WAIT_FLAG_KEY_RELEASE != 0) -> {
                        gesture.waitingKeyRelease = true
                        gesture.keyReleased = false
                        gesture.waitTimerStart = currentTimeSeconds()
                    }
                    (waitStep.flags and WAIT_FLAG_TIME != 0) -> {
                        gesture.waitingTimer = true
                        gesture.waitTimerStart = currentTimeSeconds()
                    }
                    (waitStep.flags and WAIT_FLAG_ALL_ANIM != 0) -> {
                        gesture.waitingAnimations = true
                        gesture.waitTimerStart = currentTimeSeconds()
                    }
                    else -> gesture.currentStep++
                }
            }
            else -> {}
        }
    }

    fun onLoadComplete(assetUuid: UUID, itemId: UUID, informServer: Boolean, deactivateSimilar: Boolean, status: Int) {
        loadingCount--

        if (status == 0) {
            TODO("APR: read gesture asset from file system for assetUuid and deserialize into MultiGesture")
        } else {
            TODO("APR: handle load error status $status, show delayed gesture error notification, clean up active[itemId]")
        }
    }

    fun onAssetLoadComplete(assetUuid: UUID, type: AssetType, status: Int) {
        when (type) {
            AssetType.AT_ANIMATION -> {
                TODO("APR: call KeyframeMotion.onLoadComplete for assetUuid")
                loadingAssets.remove(assetUuid)
            }
            AssetType.AT_SOUND -> {
                TODO("APR: call AudioEngine.assetCallback for assetUuid")
                loadingAssets.remove(assetUuid)
            }
            else -> throw IllegalArgumentException("Unexpected asset type: $type")
        }
    }

    private fun hasLoadingAssets(gesture: MultiGesture): Boolean {
        for (step in gesture.steps) {
            when (step.getType()) {
                StepType.STEP_ANIMATION -> {
                    val animStep = step as GestureStepAnimation
                    val animId = animStep.animAssetID
                    if (animId != UUID(0, 0) && (animStep.flags and ANIM_FLAG_STOP) == 0 && animId in loadingAssets) {
                        return true
                    }
                }
                StepType.STEP_SOUND -> {
                    val soundStep = step as GestureStepSound
                    val soundId = soundStep.soundAssetID
                    if (soundId != UUID(0, 0) && soundId in loadingAssets) {
                        return true
                    }
                }
                else -> {}
            }
        }
        return false
    }

    private fun areGesturesEnabled(): Boolean {
        TODO("APR: read FSGesturesEnabled from saved per-account settings")
        @Suppress("UNREACHABLE_CODE")
        return true
    }

    private fun canPlayGestures(): Boolean {
        TODO("APR: check RLVa sendgesture restriction")
        @Suppress("UNREACHABLE_CODE")
        return true
    }

    private fun isAgentAvatarValid(): Boolean {
        TODO("APR: check if agent avatar is valid/loaded")
        @Suppress("UNREACHABLE_CODE")
        return false
    }

    private fun linkedItemId(itemId: UUID): UUID {
        TODO("APR: resolve linked inventory item ID for $itemId")
        @Suppress("UNREACHABLE_CODE")
        return itemId
    }

    private fun inventoryItemName(itemId: UUID): String? {
        TODO("APR: look up inventory item name for $itemId")
        @Suppress("UNREACHABLE_CODE")
        return null
    }

    private fun elapsedSeconds(startTime: Long): Float {
        return (System.currentTimeMillis() - startTime) / 1000.0f
    }

    private fun currentTimeSeconds(): Long = System.currentTimeMillis()
}

const val KEY_NONE: Int = 0
const val ANIM_FLAG_STOP: Int = 0x01
const val WAIT_FLAG_KEY_RELEASE: Int = 0x01
const val WAIT_FLAG_ALL_ANIM: Int = 0x02
const val WAIT_FLAG_TIME: Int = 0x04

enum class StepType { STEP_ANIMATION, STEP_SOUND, STEP_CHAT, STEP_WAIT, STEP_EOF }
enum class AssetType { AT_ANIMATION, AT_SOUND, AT_GESTURE }

interface GestureStep {
    fun getType(): StepType
}

data class GestureStepAnimation(
    var animAssetID: UUID = UUID(0, 0),
    var animName: String = "",
    var flags: Int = 0
) : GestureStep {
    override fun getType() = StepType.STEP_ANIMATION
}

data class GestureStepSound(
    var soundAssetID: UUID = UUID(0, 0),
    var soundName: String = "",
    var flags: Int = 0
) : GestureStep {
    override fun getType() = StepType.STEP_SOUND
}

data class GestureStepChat(
    var chatText: String = "",
    var flags: Int = 0
) : GestureStep {
    override fun getType() = StepType.STEP_CHAT
}

data class GestureStepWait(
    var waitSeconds: Float = 0.0f,
    var flags: Int = 0
) : GestureStep {
    override fun getType() = StepType.STEP_WAIT
}

class MultiGesture {
    var name: String = ""
    var trigger: String = ""
    var replaceText: String = ""
    var key: Int = KEY_NONE
    var mask: Int = 0
    var playing: Boolean = false
    var triggeredByKey: Boolean = false
    var waitingAtEnd: Boolean = false
    var waitingKeyRelease: Boolean = false
    var waitingAnimations: Boolean = false
    var waitingTimer: Boolean = false
    var keyReleased: Boolean = false
    var currentStep: Int = 0
    var waitTimerStart: Long = 0L
    val steps: MutableList<GestureStep> = mutableListOf()
    val requestedAnimIDs: MutableSet<UUID> = mutableSetOf()
    val playingAnimIDs: MutableSet<UUID> = mutableSetOf()
    var doneCallback: ((MultiGesture, Any?) -> Unit)? = null
    var callbackData: Any? = null

    fun reset() {
        currentStep = 0
        playing = false
        waitingAtEnd = false
        waitingKeyRelease = false
        waitingAnimations = false
        waitingTimer = false
        keyReleased = false
        requestedAnimIDs.clear()
        playingAnimIDs.clear()
    }

    fun getTrigger(): String = trigger
}

interface InventoryItem {
    val uuid: UUID
    val assetUuid: UUID
    val name: String
}
