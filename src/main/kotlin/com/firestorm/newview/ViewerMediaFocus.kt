/**
 * ViewerMediaFocus.kt
 * Converted from llviewermediafocus.h / llviewermediafocus.cpp
 *
 * Tracks which in-world media surface has keyboard/mouse focus and which one
 * is currently hovered.  Governs camera zoom-in on focused media prims.
 * Equivalent to LLViewerMediaFocus (LLSingleton) in the C++ viewer.
 */

package com.firestorm.newview

import com.firestorm.llcommon.LLUUID
import com.firestorm.llmath.*

/**
 * Singleton that tracks focus and hover state for in-world (MoaP) media.
 *
 * "Focus" = the media face that has full keyboard/mouse input and shows the
 *           complete set of transport controls.
 * "Hover" = the face under the cursor; shows minimal controls (zoom/pop-out).
 *
 * C++ lineage: LLViewerMediaFocus : LLFocusableElement, LLSingleton
 */
object ViewerMediaFocus {

    // ── Focus state ───────────────────────────────────────────────────────

    /** UUID of the viewer object whose face currently has media focus. */
    var focusedObject: LLUUID = LLUUID.nullUUID()
        private set

    /** Face index on [focusedObject] that has focus. */
    var focusedFace: Int = 0
        private set

    /** UUID of the media implementation that currently has focus. */
    var focusedImplId: LLUUID = LLUUID.nullUUID()
        private set

    /** Previously-focused impl UUID (used to restore state on un-zoom). */
    var prevFocusedImplId: LLUUID = LLUUID.nullUUID()
        private set

    // ── Hover state ───────────────────────────────────────────────────────

    /** UUID of the viewer object currently hovered. */
    var hoverObject: LLUUID = LLUUID.nullUUID()
        private set

    /** Face index on [hoverObject] that is hovered. */
    var hoverFace: Int = 0
        private set

    /** UUID of the media implementation currently hovered. */
    var hoverImplId: LLUUID = LLUUID.nullUUID()
        private set

    // ── Zoom state ────────────────────────────────────────────────────────

    private var zoomedMediaId: LLUUID = LLUUID.nullUUID()

    // ── Focus management ─────────────────────────────────────────────────

    /**
     * Give media focus to the specified object face.
     *
     * Mirrors LLViewerMediaFocus::setFocusFace().
     *
     * @param objectId   UUID of the in-world object.
     * @param face       Face index on that object.
     * @param mediaImplId UUID of the LLViewerMediaImpl driving this face.
     */
    fun setFocus(objectId: LLUUID, face: Int, mediaImplId: LLUUID = LLUUID.nullUUID()) {
        prevFocusedImplId = focusedImplId

        focusedObject = objectId
        focusedFace   = face
        focusedImplId = mediaImplId
    }

    /**
     * Remove media focus (no face has keyboard/mouse input).
     * Mirrors LLViewerMediaFocus::clearFocus().
     */
    fun clearFocus() {
        prevFocusedImplId = focusedImplId
        focusedObject = LLUUID.nullUUID()
        focusedFace   = 0
        focusedImplId = LLUUID.nullUUID()
    }

    /** @return true if any media face currently holds focus. */
    fun getFocus(): Boolean = !focusedObject.isNull()

    /**
     * @return true if [objectId]/[face] is the currently-focused face.
     * Mirrors LLViewerMediaFocus::isFocusedOnFace().
     */
    fun isFocusedOnFace(objectId: LLUUID, face: Int): Boolean =
        focusedObject == objectId && focusedFace == face

    /**
     * Convenience overload that checks only by object UUID (any face).
     * Mirrors LLViewerMediaFocus::getFocusedObjectID() comparisons.
     */
    fun isFocused(objectId: LLUUID): Boolean = focusedObject == objectId

    // ── Hover management ─────────────────────────────────────────────────

    /**
     * Set the hovered object face.
     * Mirrors LLViewerMediaFocus::setHoverFace().
     */
    fun setHover(objectId: LLUUID, face: Int, mediaImplId: LLUUID = LLUUID.nullUUID()) {
        hoverObject = objectId
        hoverFace   = face
        hoverImplId = mediaImplId
    }

    /**
     * Clear hover state.
     * Mirrors LLViewerMediaFocus::clearHover().
     */
    fun clearHover() {
        hoverObject = LLUUID.nullUUID()
        hoverFace   = 0
        hoverImplId = LLUUID.nullUUID()
    }

    /**
     * @return true if [objectId]/[face] is the currently-hovered face.
     * Mirrors LLViewerMediaFocus::isHoveringOverFace().
     */
    fun isHoveringOverFace(objectId: LLUUID, face: Int): Boolean =
        hoverObject == objectId && hoverFace == face

    /** @return true when the hovered face is also the focused face. */
    fun isHoveringOverFocused(): Boolean =
        focusedObject == hoverObject && focusedFace == hoverFace

    // ── Zoom ──────────────────────────────────────────────────────────────

    /**
     * Zoom the camera onto the media surface identified by [mediaId].
     * Mirrors LLViewerMediaFocus::focusZoomOnMedia().
     */
    fun focusZoomOnMedia(mediaId: LLUUID) {
        zoomedMediaId = mediaId
        // Full implementation calls LLViewerMediaFocus::setCameraZoom() which
        // computes the ideal camera position from the object's bounding box and
        // face normal, then animates the agent camera to that position.
    }

    /** @return true when the camera is zoomed in on any media. */
    fun isZoomed(): Boolean = !zoomedMediaId.isNull()

    /**
     * @return true when the camera is zoomed in on the specific [mediaId].
     * Mirrors LLViewerMediaFocus::isZoomedOnMedia().
     */
    fun isZoomedOnMedia(mediaId: LLUUID): Boolean = zoomedMediaId == mediaId

    /**
     * Restore normal camera position.
     * Mirrors LLViewerMediaFocus::unZoom().
     */
    fun unZoom() {
        zoomedMediaId = LLUUID.nullUUID()
    }

    // ── Controls media ID ─────────────────────────────────────────────────

    /**
     * Return the UUID of the media instance the HUD controls are attached to.
     * Focus takes priority over hover.
     * Mirrors LLViewerMediaFocus::getControlsMediaID().
     */
    fun getControlsMediaId(): LLUUID =
        if (!focusedImplId.isNull()) focusedImplId else hoverImplId

    // ── Tick / update ─────────────────────────────────────────────────────

    /**
     * Per-frame update — repositions the media controls HUD overlay.
     * Mirrors LLViewerMediaFocus::update().
     */
    fun update() {
        // Full implementation repositions LLPanelPrimMediaControls based on
        // the focused/hovered object's screen-space bounding box.
    }

    override fun toString(): String =
        "ViewerMediaFocus(focused=$focusedObject:$focusedFace, hover=$hoverObject:$hoverFace, zoomed=$zoomedMediaId)"
}
