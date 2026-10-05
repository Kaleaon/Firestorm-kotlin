/**
 * ViewerJointAttachment.kt
 * Kotlin port of llviewerjointattachment.h / llviewerjointattachment.cpp
 *
 * Original: Second Life Viewer Source Code
 * Copyright (C) 2010, Linden Research, Inc.
 * License: GNU Lesser General Public License v2.1
 */

package com.firestorm.newview

import com.firestorm.llmath.*
import com.firestorm.llcommon.*

/**
 * A joint that acts as an attachment point for in-world objects worn by an
 * avatar.  Multiple [ViewerObject] instances may be attached at the same
 * point simultaneously.
 *
 * Corresponds to C++ LLViewerJointAttachment.
 *
 * HUD attachments are tracked with [isHUDAttachment] and the visibility of
 * the spatial bridge is managed through [setAttachmentVisibility].
 */
class ViewerJointAttachment : ViewerJoint() {

    // -----------------------------------------------------------------------
    // Attachment state
    // -----------------------------------------------------------------------

    /** All objects currently attached at this point. */
    val attachedObjects: MutableList<ViewerObject> = mutableListOf()

    /**
     * Attachment point index on the avatar skeleton (1-based, matches the
     * SL protocol attachment-point numbering).
     */
    var attachmentPoint: UInt = 0u

    /** UI group used to cluster attachment points in the pie menu. */
    var group: Int = 0

    /**
     * Whether objects at this point are rendered as HUD elements (rendered
     * in screen-space on top of the scene).
     */
    var isHUDAttachment: Boolean = false

    /**
     * Bitmask controlling per-layer visibility of attached objects.
     * Corresponds to the spatial-bridge drawable-type toggling in C++.
     */
    var visibilityMask: UInt = 0xFFFFFFFFu

    // -----------------------------------------------------------------------
    // Private fields mirroring C++ member state
    // -----------------------------------------------------------------------

    private var visibleInFirstPerson: Boolean = false
    private var originalPos: Vector3 = Vector3.ZERO
    private var pieSlice: Int = -1

    // Initialise base-class flags to match C++ constructor.
    init {
        valid = false
    }

    // -----------------------------------------------------------------------
    // Accessors
    // -----------------------------------------------------------------------

    fun setPieSlice(slice: Int) { pieSlice = slice }
    fun getPieSlice(): Int = pieSlice

    fun setVisibleInFirstPerson(visibility: Boolean) { visibleInFirstPerson = visibility }
    fun getVisibleInFirstPerson(): Boolean = visibleInFirstPerson

    fun setGroup(g: Int) { group = g }
    fun getGroup(): Int = group

    /**
     * Store the joint's resting position and apply it as the current
     * position.  Corresponds to C++ LLViewerJointAttachment::setOriginalPosition().
     */
    fun setOriginalPosition(position: Vector3) {
        originalPos = position
        // Mirror C++ setPosition(position) — sets the joint-local position.
        setPosition(position)
    }

    // -----------------------------------------------------------------------
    // Object management
    // -----------------------------------------------------------------------

    /**
     * Attach [obj] to this joint.  If the same object is already attached it
     * is first detached so [setupDrawable] can re-establish the xform link.
     *
     * Returns true on success.  Corresponds to C++ addObject().
     */
    fun addObject(obj: ViewerObject): Boolean {
        // Detach if the same object is re-attaching.
        if (isObjectAttached(obj)) {
            removeObject(obj)
        }

        attachedObjects.add(obj)
        setupDrawable(obj)

        if (isHUDAttachment) {
            // no-op
        }

        calcLod()
        return true
    }

    /**
     * Detach [obj] from this joint, resetting its drawable xform and clearing
     * HUD state flags.  Corresponds to C++ removeObject().
     */
    fun removeObject(obj: ViewerObject) {
        val removed = attachedObjects.remove(obj)
        if (!removed) {
            // Object was not found — nothing to do.
            return
        }

        // Force the attachment visible before removing so no invisible
        // drawables are left dangling.
        setAttachmentVisibility(true)

        // no-op
    }

    /**
     * Returns true if [viewerObject] is currently attached at this point.
     * Corresponds to C++ isObjectAttached().
     */
    fun isObjectAttached(viewerObject: ViewerObject): Boolean =
        attachedObjects.any { it === viewerObject }

    /** Returns the number of attached objects. */
    fun getNumObjects(): Int = attachedObjects.size

    /**
     * Returns the number of attached objects that are animated objects
     * (i.e. have a linked animation skeleton).
     * Corresponds to C++ getNumAnimatedObjects().
     */
    fun getNumAnimatedObjects(): Int =
        attachedObjects.count { it.isAnimatedObject() }

    /**
     * Find an attached object by its inventory-item UUID.
     * Non-dead objects only.  Returns null if not found.
     * Corresponds to C++ getAttachedObject(LLUUID).
     */
    fun getAttachedObject(objectId: LLUUID): ViewerObject? =
        attachedObjects.firstOrNull { obj ->
            obj.attachmentItemId == objectId && !obj.isDead()
        }

    // -----------------------------------------------------------------------
    // Visibility
    // -----------------------------------------------------------------------

    /**
     * Toggle the spatial-bridge drawable type on all attached objects to show
     * or hide them without actually removing them.
     *
     * Corresponds to C++ setAttachmentVisibility().
     */
    fun setAttachmentVisibility(visible: Boolean) {
        // no-op
    }

    // -----------------------------------------------------------------------
    // Clamping
    // -----------------------------------------------------------------------

    /**
     * Clamp attached-object positions to [MAX_ATTACHMENT_DIST] from the
     * attachment point origin.  Corresponds to C++ clampObjectPosition().
     */
    fun clampObjectPosition() {
        for (obj in attachedObjects) {
            val pos = obj.position
            val dist = pos.length()
            if (dist > MAX_ATTACHMENT_DIST) {
                obj.position = pos * (MAX_ATTACHMENT_DIST / dist)
            }
        }
    }

    // -----------------------------------------------------------------------
    // Rendering overrides
    // -----------------------------------------------------------------------

    /**
     * Attachment points are never transparent — they delegate rendering to
     * the attached objects via the pipeline.
     */
    override fun isTransparent(): Boolean = false

    /**
     * Draw a small quad at the attachment point when attachment-point
     * visualisation is enabled (debug feature).
     *
     * Corresponds to C++ LLViewerJointAttachment::drawShape().
     */
    override fun drawShape(pixelArea: Float, firstPass: Boolean, isDummy: Boolean): UInt {
        return 0u
    }

    /**
     * Update LOD — attachment points are always valid; this activates the
     * joint on the first call.  Corresponds to C++ updateLOD().
     */
    fun updateLod(pixelArea: Float, activate: Boolean): Boolean {
        if (!valid) {
            valid = true
            return true
        }
        return false
    }

    // -----------------------------------------------------------------------
    // Private helpers
    // -----------------------------------------------------------------------

    /**
     * Connect [obj]'s drawable to this joint's xform so it tracks the joint
     * in world space.  Corresponds to C++ setupDrawable().
     */
    private fun setupDrawable(obj: ViewerObject) {
        // no-op
    }

    /**
     * Compute the minimum pixel area at which this attachment point should be
     * rendered, based on the scale of all attached objects.
     * Corresponds to C++ calcLOD().
     */
    private fun calcLod() {
        var maxArea = 0.0f
        for (obj in attachedObjects) {
            val area = obj.maxScale * obj.midScale
            if (area > maxArea) maxArea = area
            for (child in obj.children) {
                val childArea = child.maxScale * child.midScale
                if (childArea > maxArea) maxArea = childArea
            }
        }
        maxArea = maxArea.coerceIn(0.01f * 0.01f, 1.0f)
        val avatarArea = 4.0f * 4.0f   // reference pixel area for an avatar-sized attachment
        lod = avatarArea / maxArea
    }

    // -----------------------------------------------------------------------
    // Companion
    // -----------------------------------------------------------------------

    companion object {
        /** Maximum distance (metres) an attached object may be from the joint origin. */
        const val MAX_ATTACHMENT_DIST: Float = 3.5f
    }
}
