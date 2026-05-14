package com.firestorm.newview

import com.firestorm.llcharacter.GestureStep
import com.firestorm.llcharacter.GestureStepAnimation
import com.firestorm.llcharacter.GestureStepSound
import com.firestorm.llcharacter.GestureStepWait
import com.firestorm.llcharacter.GestureStepChat
import com.firestorm.llcharacter.Key
import com.firestorm.llcharacter.Mask
import com.firestorm.llcharacter.KEY_NONE
import com.firestorm.llcharacter.MultiGesture
import com.firestorm.llcharacter.StepType
import com.firestorm.llcharacter.WaitFlags
import com.firestorm.llcharacter.ANIM_FLAG_STOP
import com.firestorm.llcommon.LLUUID

private const val MAX_WAIT_ANIM_SECS: Float = 60.0f
private const val MAX_WAIT_KEY_SECS: Float = 60.0f * 10.0f

interface GestureManagerObserver {
    fun changed()
}

object GestureMgr {

    val active: MutableMap<LLUUID, MultiGesture?> = mutableMapOf()
    private val playing: MutableList<MultiGesture> = mutableListOf()
    private val observers: MutableList<GestureManagerObserver> = mutableListOf()
    private val callbackMap: MutableMap<LLUUID, (MultiGesture) -> Unit> = mutableMapOf()
    private val loadingAssets: MutableSet<LLUUID> = mutableSetOf()
    private var loadingCount: Int = 0
    private var deactivateSimilarNames: String = ""

    fun init() {
        System.err.println("GestureMgr: APR: use JVM equivalent — register as inventory observer not yet implemented")
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

        val (stillPlaying, done) = playing.partition { it.isPlaying }
        playing.clear()
        playing.addAll(stillPlaying)

        if (done.isNotEmpty()) {
            for (gesture in done) {
                gesture.doneCallback?.invoke(gesture)
            }
            notifyObservers()
        }
    }

    fun activateGesture(itemId: LLUUID) {
        System.err.println("GestureMgr: APR: look up inventory item by itemId to get assetId, then call activateGestureWithAsset not yet implemented")
    }

    fun activateGestures(items: List<ViewerInventoryItemRef>) {
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

        System.err.println("GestureMgr: APR: send bulk ActivateGestures message to server for all newly active items not yet implemented")
    }

    fun activateGestureWithAsset(itemId: LLUUID, assetId: LLUUID, informServer: Boolean, deactivateSimilar: Boolean) {
        val baseItemId = linkedItemId(itemId)
        if (isGestureActive(itemId)) return

        active[baseItemId] = null

        if (assetId != LLUUID.NULL) {
            System.err.println("GestureMgr: APR: fetch asset data for assetId (AT_GESTURE), pass LoadInfo(baseItemId, informServer, deactivateSimilar) to onLoadComplete callback not yet implemented")
        } else {
            notifyObservers()
        }
    }

    fun deactivateGesture(itemId: LLUUID) {
        val baseItemId = linkedItemId(itemId)
        if (!active.containsKey(baseItemId)) return

        val gesture = active[baseItemId]
        gesture?.let { stopGesture(it) }
        active.remove(baseItemId)

        System.err.println("GestureMgr: APR: send DeactivateGestures message to server, then call AppearanceMgr.removeCOFItemLinks for baseItemId not yet implemented")
    }

    fun deactivateSimilarGestures(inGesture: MultiGesture, inItemId: LLUUID) {
        val baseInItemId = linkedItemId(inItemId)
        val gestureItemIds = mutableListOf<LLUUID>()

        val iter = active.entries.iterator()
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
                System.err.println("GestureMgr: APR: mark inventory LABEL changed for itemId not yet implemented")
            }
        }

        System.err.println("GestureMgr: APR: send bulk DeactivateGestures message for gestureItemIds not yet implemented")

        for (id in gestureItemIds) {
            val name = inventoryItemName(id) ?: continue
            deactivateSimilarNames += name + "\n"
        }

        notifyObservers()
    }

    fun isGestureActive(itemId: LLUUID): Boolean = active.containsKey(linkedItemId(itemId))

    fun isGesturePlaying(itemId: LLUUID): Boolean =
        active[linkedItemId(itemId)]?.isPlaying ?: false

    fun isGesturePlaying(gesture: MultiGesture?): Boolean = gesture?.isPlaying ?: false

    fun replaceGesture(itemId: LLUUID, newGesture: MultiGesture, assetId: LLUUID) {
        val baseItemId = linkedItemId(itemId)
        val oldGesture = active[baseItemId] ?: return

        stopGesture(oldGesture)
        active.remove(baseItemId)
        active[baseItemId] = newGesture

        if (assetId != LLUUID.NULL) {
            loadingCount = 1
            deactivateSimilarNames = ""
            System.err.println("GestureMgr: APR: fetch asset data for assetId (AT_GESTURE) and call onLoadComplete not yet implemented")
        }

        notifyObservers()
    }

    fun replaceGesture(itemId: LLUUID, newAssetId: LLUUID) {
        val baseItemId = linkedItemId(itemId)
        val gesture = active[baseItemId] ?: return
        replaceGesture(baseItemId, gesture, newAssetId)
    }

    fun getActiveGestures(): Map<LLUUID, MultiGesture?> = active

    fun playGesture(gesture: MultiGesture, fromKeyPress: Boolean = false) {
        if (!areGesturesEnabled()) return
        if (!canPlayGestures()) return

        gesture.reset()
        gesture.triggeredByKey = fromKeyPress
        gesture.start()
        playing.add(gesture)

        for (step in gesture.steps) {
            when (step.type) {
                StepType.ANIMATION -> {
                    val animStep = step as GestureStepAnimation
                    val animId = animStep.animAssetId
                    if (animId != LLUUID.NULL && (animStep.flags and ANIM_FLAG_STOP) == 0u) {
                        loadingAssets.add(animId)
                        System.err.println("GestureMgr: APR: fetch AT_ANIMATION asset for animId, call onAssetLoadComplete when done not yet implemented")
                    }
                }
                StepType.SOUND -> {
                    val soundStep = step as GestureStepSound
                    val soundId = soundStep.soundAssetId
                    if (soundId != LLUUID.NULL) {
                        loadingAssets.add(soundId)
                        System.err.println("GestureMgr: APR: fetch AT_SOUND asset for soundId, call onAssetLoadComplete when done not yet implemented")
                    }
                }
                else -> {}
            }
        }

        stepGesture(gesture)
        notifyObservers()
    }

    fun playGesture(itemId: LLUUID) {
        if (!areGesturesEnabled()) return
        val gesture = active[linkedItemId(itemId)] ?: return
        playGesture(gesture)
    }

    fun stopGesture(gesture: MultiGesture?) {
        gesture ?: return

        for (animId in gesture.requestedAnimIds) {
            System.err.println("GestureMgr: APR: send ANIM_REQUEST_STOP for animId via agent not yet implemented")
        }
        for (animId in gesture.playingAnimIds) {
            System.err.println("GestureMgr: APR: send ANIM_REQUEST_STOP for animId via agent not yet implemented")
        }

        playing.removeAll { it === gesture }
        gesture.reset()
        gesture.doneCallback?.invoke(gesture)

        notifyObservers()
    }

    fun stopGesture(itemId: LLUUID) {
        val gesture = active[linkedItemId(itemId)] ?: return
        stopGesture(gesture)
    }

    fun setGestureLoadedCallback(invItemId: LLUUID, cb: (MultiGesture) -> Unit) {
        callbackMap[invItemId] = cb
    }

    fun triggerGesture(key: Key, mask: Mask): Boolean {
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

    fun triggerGestureRelease(key: Key, mask: Mask): Boolean {
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
                        revisedString?.append(
                            if (gesture.replaceText.equals(token, ignoreCase = true)) token
                            else gesture.replaceText
                        )
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

    fun isKeyBound(key: Key, mask: Mask): Boolean =
        active.values.filterNotNull().any { it.key == key && it.mask == mask }

    fun getPlayingCount(): Int = playing.size

    fun addObserver(observer: GestureManagerObserver) { observers.add(observer) }

    fun removeObserver(observer: GestureManagerObserver) { observers.remove(observer) }

    fun notifyObservers() {
        for (observer in observers.toList()) observer.changed()
    }

    fun onInventoryChanged(mask: UInt) {
        val GESTURE: UInt = 0x40u
        val LABEL: UInt = 0x2u
        val ADD: UInt = 0x4u
        val REMOVE: UInt = 0x8u
        val STRUCTURE: UInt = 0x10u

        if (mask and GESTURE != 0u) {
            if (mask and LABEL != 0u) {
                for ((itemId, gesture) in active) {
                    if (gesture != null) {
                        val itemName = inventoryItemName(itemId)
                        if (itemName != null) gesture.name = itemName
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
            if (inStr.equals(gesture.trigger, ignoreCase = true)) {
                outStr.append(gesture.trigger)
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
            if (restOfMatch.isEmpty()) restOfMatch = curRest

            var buf = ""
            var i = 0
            while (i < restOfMatch.length && i < curRest.length) {
                if (restOfMatch[i] == curRest[i]) {
                    buf += restOfMatch[i]
                } else {
                    if (i == 0) restOfMatch = ""
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

    fun getItemIDs(ids: MutableList<LLUUID>) {
        ids.addAll(active.keys)
    }

    fun done() {
        var notify = false
        for ((itemId, gesture) in active) {
            if (gesture != null && gesture.name.isEmpty()) {
                val itemName = inventoryItemName(itemId)
                if (itemName != null) {
                    gesture.name = itemName
                    notify = true
                }
            }
        }
        if (notify) notifyObservers()
    }

    private fun stepGesture(gesture: MultiGesture) {
        if (!isAgentAvatarValid()) return
        if (hasLoadingAssets(gesture)) return

        System.err.println("GestureMgr: APR: sync gesture.playingAnimIds and gesture.requestedAnimIds against avatar's signaledAnimations map not yet implemented")

        var waiting = false
        while (!waiting && gesture.isPlaying) {
            val step: GestureStep? = if (gesture.currentStep < gesture.steps.size) {
                gesture.steps[gesture.currentStep]
            } else {
                gesture.waitingAtEnd = true
                null
            }

            if (gesture.waitingAtEnd) {
                if (gesture.requestedAnimIds.isEmpty() && gesture.playingAnimIds.isEmpty()) {
                    gesture.waitingAtEnd = false
                    gesture.stop()
                } else {
                    waiting = true
                }
                continue
            }

            if (gesture.waitingKeyRelease) {
                when {
                    gesture.keyReleased -> {
                        gesture.waitingKeyRelease = false
                        gesture.currentStep++
                    }
                    gesture.waitElapsed > MAX_WAIT_KEY_SECS -> {
                        gesture.waitingKeyRelease = false
                        gesture.currentStep++
                    }
                    else -> waiting = true
                }
                continue
            }

            if (gesture.waitingAnimations) {
                when {
                    gesture.requestedAnimIds.isEmpty() && gesture.playingAnimIds.isEmpty() -> {
                        gesture.waitingAnimations = false
                        gesture.currentStep++
                    }
                    gesture.waitElapsed > MAX_WAIT_ANIM_SECS -> {
                        gesture.waitingAnimations = false
                        gesture.currentStep++
                    }
                    else -> waiting = true
                }
                continue
            }

            if (gesture.waitingTimer) {
                val waitStep = step as GestureStepWait
                if (gesture.waitElapsed > waitStep.waitSeconds) {
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
        when (step.type) {
            StepType.ANIMATION -> {
                val animStep = step as GestureStepAnimation
                if (animStep.animAssetId == LLUUID.NULL) {
                    gesture.currentStep++
                    return
                }
                if (animStep.flags and ANIM_FLAG_STOP != 0u) {
                    System.err.println("GestureMgr: APR: send ANIM_REQUEST_STOP for animStep.animAssetId via agent not yet implemented")
                    gesture.requestedAnimIds.remove(animStep.animAssetId)
                } else {
                    System.err.println("GestureMgr: APR: send ANIM_REQUEST_START for animStep.animAssetId via agent not yet implemented")
                    gesture.requestedAnimIds.add(animStep.animAssetId)
                }
                gesture.currentStep++
            }
            StepType.SOUND -> {
                val soundStep = step as GestureStepSound
                System.err.println("GestureMgr: APR: call send_sound_trigger for ${soundStep.soundAssetId} at volume 1.0f not yet implemented")
                gesture.currentStep++
            }
            StepType.CHAT -> {
                val chatStep = step as GestureStepChat
                System.err.println("GestureMgr: APR: send chat '${chatStep.chatText}' as CHAT_TYPE_NORMAL via FSNearbyChat (no animate); check cmd_line_chat first not yet implemented")
                gesture.currentStep++
            }
            StepType.WAIT -> {
                val waitStep = step as GestureStepWait
                when {
                    gesture.triggeredByKey && !gesture.waitingKeyRelease &&
                            (waitStep.flags and WaitFlags.KEY_RELEASE != 0u) -> {
                        gesture.waitingKeyRelease = true
                        gesture.keyReleased = false
                        gesture.waitElapsed = 0f
                    }
                    (waitStep.flags and WaitFlags.TIME != 0u) -> {
                        gesture.waitingTimer = true
                        gesture.waitElapsed = 0f
                    }
                    (waitStep.flags and WaitFlags.ALL_ANIM != 0u) -> {
                        gesture.waitingAnimations = true
                        gesture.waitElapsed = 0f
                    }
                    else -> gesture.currentStep++
                }
            }
            else -> {}
        }
    }

    fun onLoadComplete(
        assetUuid: LLUUID,
        itemId: LLUUID,
        informServer: Boolean,
        deactivateSimilar: Boolean,
        status: Int
    ) {
        loadingCount--
        if (status == 0) {
            System.err.println("GestureMgr: APR: read and deserialize gesture asset from file system for assetUuid; update active[itemId], send ActivateGestures if informServer, invoke callbackMap entry if present not yet implemented")
        } else {
            System.err.println("GestureMgr: APR: handle load error (status $status) — show delayed gesture error notification, clean up active[itemId] not yet implemented")
        }
    }

    fun onAssetLoadComplete(assetUuid: LLUUID, isAnimation: Boolean, status: Int) {
        if (isAnimation) {
            System.err.println("GestureMgr: APR: call KeyframeMotion.onLoadComplete for assetUuid not yet implemented")
        } else {
            System.err.println("GestureMgr: APR: call AudioEngine.assetCallback for assetUuid not yet implemented")
        }
        loadingAssets.remove(assetUuid)
    }

    private fun hasLoadingAssets(gesture: MultiGesture): Boolean {
        for (step in gesture.steps) {
            when (step.type) {
                StepType.ANIMATION -> {
                    val animStep = step as GestureStepAnimation
                    val animId = animStep.animAssetId
                    if (animId != LLUUID.NULL && (animStep.flags and ANIM_FLAG_STOP) == 0u && animId in loadingAssets)
                        return true
                }
                StepType.SOUND -> {
                    val soundStep = step as GestureStepSound
                    val soundId = soundStep.soundAssetId
                    if (soundId != LLUUID.NULL && soundId in loadingAssets)
                        return true
                }
                else -> {}
            }
        }
        return false
    }

    private fun areGesturesEnabled(): Boolean {
        System.err.println("GestureMgr: APR: read FSGesturesEnabled from saved per-account settings (gSavedPerAccountSettings) not yet implemented")
        @Suppress("UNREACHABLE_CODE")
        return true
    }

    private fun canPlayGestures(): Boolean {
        System.err.println("GestureMgr: APR: check RLVa @sendgesture restriction via RlvActions.canPlayGestures() not yet implemented")
        @Suppress("UNREACHABLE_CODE")
        return true
    }

    private fun isAgentAvatarValid(): Boolean {
        System.err.println("GestureMgr: APR: check isAgentAvatarValid() / gAgentAvatarp != null not yet implemented")
        @Suppress("UNREACHABLE_CODE")
        return false
    }

    private fun linkedItemId(itemId: LLUUID): LLUUID {
        System.err.println("GestureMgr: APR: call gInventory.getLinkedItemID(itemId) not yet implemented")
        @Suppress("UNREACHABLE_CODE")
        return itemId
    }

    private fun inventoryItemName(itemId: LLUUID): String? {
        System.err.println("GestureMgr: APR: call gInventory.getItem(itemId)?.getName() not yet implemented")
        @Suppress("UNREACHABLE_CODE")
        return null
    }
}

interface ViewerInventoryItemRef {
    val uuid: LLUUID
    val assetUuid: LLUUID
    val name: String
}
