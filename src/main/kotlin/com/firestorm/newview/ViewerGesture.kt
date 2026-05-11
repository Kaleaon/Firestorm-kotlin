/**
 * ViewerGesture.kt
 * Kotlin port of llviewergesture.h / llviewergesture.cpp
 *
 * Original: Second Life Viewer Source Code
 * Copyright (C) 2010, Linden Research, Inc.
 * License: GNU Lesser General Public License v2.1
 */

package com.firestorm.newview

import com.firestorm.llmath.*
import com.firestorm.llcommon.*

// ---------------------------------------------------------------------------
// Data model
// ---------------------------------------------------------------------------

/**
 * Represents a single viewer-side gesture definition.
 *
 * A gesture is triggered by either a keyboard shortcut (key + mask) or by
 * typing a chat trigger string.  When fired it can:
 * - play a sound asset
 * - start an animation
 * - send a chat string
 *
 * Corresponds to C++ LLViewerGesture.
 *
 * @param key           Keyboard key code that triggers this gesture, or 0 for none.
 * @param mask          Modifier mask (Shift/Ctrl/Alt) paired with [key].
 * @param trigger       Chat-text trigger string (case-insensitive match).
 * @param soundItemId   Inventory item UUID of the sound to play, or [LLUUID.NULL].
 * @param animation     Animation name / state string (empty = none).
 * @param outputString  Chat text to send when the gesture fires (empty = none).
 */
data class ViewerGesture(
    val key: Int = 0,
    val mask: Int = 0,
    val trigger: String = "",
    val soundItemId: LLUUID = LLUUID.NULL,
    val animation: String = "",
    val outputString: String = "",
) {
    companion object {
        /** Volume used when playing gesture sounds. Mirrors C++ SOUND_VOLUME = 1.f. */
        const val SOUND_VOLUME: Float = 1.0f
    }

    /** Lower-cased trigger for efficient comparison. */
    private val triggerLower: String = trigger.lowercase()

    /**
     * Returns true and fires the gesture if [key] and [mask] match.
     * Corresponds to C++ LLViewerGesture::trigger(KEY, MASK).
     */
    fun trigger(key: Int, mask: Int): Boolean {
        return if (this.key == key && this.mask == mask) {
            doTrigger(sendChat = true)
            true
        } else {
            false
        }
    }

    /**
     * Returns true and fires the gesture if [string] (already lower-cased)
     * matches the trigger exactly.
     * Corresponds to C++ LLViewerGesture::trigger(const std::string&).
     */
    fun trigger(string: String): Boolean {
        return if (triggerLower == string) {
            doTrigger(sendChat = false)
            true
        } else {
            false
        }
    }

    /**
     * Execute the gesture's side-effects: play sound, start animation,
     * optionally send chat.
     * Corresponds to C++ LLViewerGesture::doTrigger(bool).
     *
     * Complex agent/audio interactions are stubbed with TODO().
     */
    fun doTrigger(sendChat: Boolean) {
        if (soundItemId != LLUUID.NULL) {
            // TODO: look up soundItemId in inventory, then call sendSoundTrigger(assetId, SOUND_VOLUME)
        }

        if (animation.isNotEmpty()) {
            if (animation == "enter_away_from_keyboard_state" || animation == "away") {
                // TODO: gAgent.setAFK()
            } else {
                // TODO: resolve animation name → UUID via gAnimLibrary, then gAgent.sendAnimationRequest(animId, ANIM_REQUEST_START)
            }
        }

        if (sendChat && outputString.isNotEmpty()) {
            // TODO: FSNearbyChat.instance().sendChatFromViewer(outputString, CHAT_TYPE_NORMAL, false)
        }
    }
}

// ---------------------------------------------------------------------------
// Gesture list / manager
// ---------------------------------------------------------------------------

/**
 * Manages the ordered list of all active viewer gestures and exposes
 * playback and query operations.
 *
 * Combines the responsibilities of C++ LLViewerGestureList (the ordered list)
 * and the higher-level gesture-manager pattern used in the gesture system.
 *
 * Implemented as a singleton object so callers do not need to pass a reference.
 */
object ViewerGestureManager {

    // -----------------------------------------------------------------------
    // Internal state
    // -----------------------------------------------------------------------

    private val gestures: MutableList<ViewerGesture> = mutableListOf()
    private val playing: MutableSet<LLUUID> = mutableSetOf()

    /** True once the gesture list has been loaded from the server / disk. */
    var isLoaded: Boolean = false
        private set

    // -----------------------------------------------------------------------
    // List management
    // -----------------------------------------------------------------------

    /** Add a gesture definition to the active list. */
    fun add(gesture: ViewerGesture) {
        gestures.add(gesture)
    }

    /** Remove all gestures and reset load state. */
    fun clear() {
        gestures.clear()
        playing.clear()
        isLoaded = false
    }

    /** Return an immutable snapshot of the current gesture list. */
    fun getAll(): List<ViewerGesture> = gestures.toList()

    // -----------------------------------------------------------------------
    // Playback API
    // -----------------------------------------------------------------------

    /**
     * Begin playback of the gesture identified by [itemId].
     * Corresponds to the pattern used by LLGestureManager::playGesture().
     * Full implementation would locate the gesture asset and drive the
     * sequencer; stubbed here with TODO().
     */
    fun playGesture(itemId: LLUUID) {
        playing.add(itemId)
        // TODO: locate gesture asset by itemId, build step sequence, start playback
    }

    /**
     * Stop playback of the gesture identified by [itemId].
     * Corresponds to LLGestureManager::stopGesture().
     */
    fun stopGesture(itemId: LLUUID) {
        playing.remove(itemId)
        // TODO: interrupt active steps (animation, sound) for this gesture
    }

    /**
     * Returns true if the gesture identified by [itemId] is currently playing.
     */
    fun isGesturePlaying(itemId: LLUUID): Boolean = itemId in playing

    /**
     * Returns the list of item UUIDs for all currently-playing gestures.
     */
    fun getActiveGestures(): List<LLUUID> = playing.toList()

    // -----------------------------------------------------------------------
    // Trigger matching (mirrors LLViewerGestureList)
    // -----------------------------------------------------------------------

    /**
     * Attempt to trigger a gesture by key + mask.
     * Returns true if a matching gesture was found and fired.
     */
    fun triggerByKey(key: Int, mask: Int): Boolean =
        gestures.any { it.trigger(key, mask) }

    /**
     * Attempt to trigger a gesture by chat string (lower-cased exact match).
     * Returns true if a matching gesture was found and fired.
     */
    fun triggerByString(string: String): Boolean =
        gestures.any { it.trigger(string.lowercase()) }

    /**
     * Check whether any gesture trigger starts with [prefix].
     * If so, write the full trigger into the returned string (or null).
     * Corresponds to C++ LLViewerGestureList::matchPrefix().
     */
    fun matchPrefix(prefix: String): String? {
        val lowerPrefix = prefix.lowercase()
        for (gesture in gestures) {
            val trig = gesture.trigger
            if (trig.length >= lowerPrefix.length &&
                trig.lowercase().startsWith(lowerPrefix)
            ) {
                return trig
            }
        }
        return null
    }

    // -----------------------------------------------------------------------
    // Serialization (stub)
    // -----------------------------------------------------------------------

    /**
     * Deserialize the gesture list from a raw byte buffer received via the
     * xfer subsystem.  Corresponds to C++ LLViewerGestureList::xferCallback().
     */
    fun deserializeFromXfer(data: ByteArray, status: Int) {
        if (status == 0 /* LL_ERR_NOERR */) {
            // TODO: parse data into ViewerGesture instances, call add() for each
            isLoaded = true
        } else {
            System.err.println("ViewerGestureManager: Unable to load gesture list (status=$status)")
        }
    }
}
