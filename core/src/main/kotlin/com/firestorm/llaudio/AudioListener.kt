package com.firestorm.llaudio

// Converted from indra/llaudio/lllistener.h
// Abstract listener (camera/ear) position for the 3D audio engine.

import com.firestorm.llmath.Vector3

/**
 * Immutable snapshot of the audio listener's spatial state.
 *
 * Used as a value type when passing listener geometry between subsystems.
 * Corresponds to the four position/orientation vectors that [LLListener]
 * stores as member variables in the C++ source.
 *
 * @param position   World-space position of the listener (metres).
 * @param velocity   Velocity vector used for Doppler calculations (m/s).
 * @param upVector   Normalised "up" axis of the listener's reference frame.
 * @param atVector   Normalised "look-at" / forward axis of the reference frame.
 */
data class AudioListener(
    var position:  Vector3 = Vector3.ZERO,
    var velocity:  Vector3 = Vector3.ZERO,
    var upVector:  Vector3 = Vector3(0f, 0f, 1f),
    var atVector:  Vector3 = Vector3(0f, 1f, 0f),
)

/**
 * Mutable 3D audio listener, mirroring the [LLListener] C++ base class.
 *
 * Engine-specific subclasses (e.g. an OpenAL or FMOD Studio listener)
 * override [commitDeferredChanges] to push the accumulated state to the
 * hardware layer.
 *
 * The C++ class exposes separate `setDopplerFactor` / `setRolloffFactor`
 * methods that control global distance-attenuation behaviour; those are
 * preserved here as open functions so subclasses can relay them to the
 * underlying SDK.
 */
open class Listener {

    // ---- Spatial state (mirrors C++ protected members) ----------------------

    var position:  Vector3 = Vector3.ZERO
    var velocity:  Vector3 = Vector3.ZERO
    var listenAt:  Vector3 = Vector3(0f, 1f, 0f)   // "at" (forward) vector
    var listenUp:  Vector3 = Vector3(0f, 0f, 1f)   // "up" vector

    // ---- Lifecycle ----------------------------------------------------------

    /** Initialise listener to default values (origin, stationary, standard orientation). */
    open fun init() {
        position  = Vector3.ZERO
        velocity  = Vector3.ZERO
        listenAt  = Vector3(0f, 1f, 0f)
        listenUp  = Vector3(0f, 0f, 1f)
    }

    // ---- Bulk update --------------------------------------------------------

    /**
     * Set all spatial parameters at once.
     *
     * Mirrors `LLListener::set(pos, vel, up, at)`.
     *
     * @param pos World-space position.
     * @param vel Velocity for Doppler effect.
     * @param up  Up vector defining the listener's roll.
     * @param at  Forward/"look-at" vector.
     */
    open fun set(pos: Vector3, vel: Vector3, up: Vector3, at: Vector3) {
        position  = pos
        velocity  = vel
        listenUp  = up
        listenAt  = at
    }

    // ---- Individual setters -------------------------------------------------

    /** Set listener world-space [pos]ition. */
    open fun setPosition(pos: Vector3) { position = pos }

    /** Set listener [vel]ocity (used for Doppler calculations). */
    open fun setVelocity(vel: Vector3) { velocity = vel }

    /**
     * Orient the listener.
     *
     * @param up Normalised up vector.
     * @param at Normalised forward (look-at) vector.
     */
    open fun orient(up: Vector3, at: Vector3) {
        listenUp = up
        listenAt = at
    }

    /** Translate the listener by [offset]. */
    open fun translate(offset: Vector3) {
        position = Vector3(
            position.x + offset.x,
            position.y + offset.y,
            position.z + offset.z,
        )
    }

    // ---- Distance model parameters (engine-specific) -----------------------

    /**
     * Set the Doppler scale factor.
     * 0 = no Doppler, 1 = physically accurate (speed of sound ~343 m/s).
     * Default no-op; override in engine-specific subclass.
     */
    open fun setDopplerFactor(factor: Float) {}

    /** Get the current Doppler scale factor. */
    open fun getDopplerFactor(): Float = 1f

    /**
     * Set the rolloff (distance attenuation) factor.
     * 1.0 = physically based inverse-square rolloff.
     * Default no-op; override in engine-specific subclass.
     */
    open fun setRolloffFactor(factor: Float) {}

    /** Get the current rolloff factor. */
    open fun getRolloffFactor(): Float = 1f

    // ---- Getters ------------------------------------------------------------

    open fun getPosition(): Vector3 = position
    open fun getAt():       Vector3 = listenAt
    open fun getUp():       Vector3 = listenUp

    // ---- Deferred commit (engine hook) -------------------------------------

    /**
     * Flush any buffered listener changes to the audio hardware.
     *
     * Called at the end of each frame after all position updates have been
     * applied.  Default implementation is a no-op — override in
     * platform-specific subclasses (e.g. OpenAL, FMOD Studio).
     *
     * Mirrors `LLListener::commitDeferredChanges()`.
     */
    open fun commitDeferredChanges() {
        System.err.println("AudioListener: commitDeferredChanges not yet implemented")
    }

    // ---- Conversion to/from the value-type snapshot -----------------------

    /** Capture the current state as an immutable [AudioListener] snapshot. */
    fun toSnapshot(): AudioListener = AudioListener(
        position  = position,
        velocity  = velocity,
        upVector  = listenUp,
        atVector  = listenAt,
    )

    /** Restore listener state from an [AudioListener] snapshot. */
    fun fromSnapshot(snapshot: AudioListener) {
        set(
            pos = snapshot.position,
            vel = snapshot.velocity,
            up  = snapshot.upVector,
            at  = snapshot.atVector,
        )
    }
}
