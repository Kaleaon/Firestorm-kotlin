/**
 * IOPipe.kt
 * Kotlin port of lliopipe.h
 *
 * Declares the abstract base class for pipeline stages in the Second Life
 * viewer's non-blocking I/O system.  Each concrete pipe processes data from
 * an [LLBufferArray] on a given pair of channels and returns a status code
 * indicating what the pump should do next.
 */
package com.firestorm.llmessage

import com.firestorm.llcommon.LLSD

// ---------------------------------------------------------------------------
// EStatus
// ---------------------------------------------------------------------------

/**
 * Status codes returned by [LLIOPipe.process].
 *
 * Positive (or zero) values indicate success; negative values indicate errors.
 * Mirrors the C++ `LLIOPipe::EStatus` enum.
 */
enum class EStatus(val code: Int) {
    /** Processing occurred normally; future calls will be accepted. */
    STATUS_OK(0),

    /** Processing occurred normally but stop unsolicited future calls. */
    STATUS_STOP(1),

    /** This pipe is done; future calls accepted only when new data arrives. */
    STATUS_DONE(2),

    /** This pipe requests to become the head of the current process chain. */
    STATUS_BREAK(3),

    /** This pipe requests another process call. */
    STATUS_NEED_PROCESS(4),

    // --- error codes (negative) ---

    /** A generic error occurred. */
    STATUS_ERROR(-1),

    /** The implementation is not yet complete. */
    STATUS_NOT_IMPLEMENTED(-2),

    /** A required precondition was not met (e.g., no next pipe in chain). */
    STATUS_PRECONDITION_NOT_MET(-3),

    /** Could not connect to the remote host. */
    STATUS_NO_CONNECTION(-4),

    /** The connection was lost after it was established. */
    STATUS_LOST_CONNECTION(-5),

    /** The total process time exceeded the allowed timeout. */
    STATUS_EXPIRED(-6);

    /** `true` when this status represents a failure. */
    val isError: Boolean get() = code < 0

    /** `true` when this status represents success. */
    val isSuccess: Boolean get() = code >= 0

    companion object {
        /** Look up a status by its integer [code], or `null` if unknown. */
        fun fromCode(code: Int): EStatus? = entries.firstOrNull { it.code == code }

        /** Return the name of [status], or an empty string if not found. */
        fun lookupStatusString(status: EStatus): String = status.name
    }
}

// ---------------------------------------------------------------------------
// LLIOPipe
// ---------------------------------------------------------------------------

/**
 * Abstract base class for data-processing stages in the viewer I/O pipeline.
 *
 * A pipe receives a buffer view (described by [LLChannelDescriptors]), reads
 * from the *in* channel, writes results to the *out* channel, and returns an
 * [EStatus] describing what the driving [pump][Any] should do next.
 *
 * Concrete implementations should override [processImpl].  The public
 * [process] method delegates to [processImpl] and provides a hook for
 * cross-cutting concerns (e.g. error handling, reference counting).
 *
 * The C++ class used `boost::intrusive_ptr` for reference counting.  In
 * Kotlin we rely on the JVM garbage collector, so [referenceCount] is
 * retained only to support code that explicitly tracks references for
 * diagnostic purposes.
 */
abstract class LLIOPipe {

    // Mirrors `mReferenceCount` in C++; the JVM GC makes it redundant for
    // memory management, but we keep it for API compatibility.
    private var referenceCount: Int = 0

    fun addRef() { referenceCount++ }
    fun release() { if (referenceCount > 0) referenceCount-- }
    val refCount: Int get() = referenceCount

    /** Returns `true` if this pipe is in a valid state for processing. */
    open fun isValid(): Boolean = true

    /**
     * Process data in [buffer] using the channel pair described by [channels].
     *
     * This is the public entry point called by the pump.  It delegates to
     * [processImpl] after any necessary setup.
     *
     * @param channels  Describes the in/out channel pair inside [buffer].
     * @param buffer    The shared buffer array for this pipeline.
     * @param eos       `true` if no more data will arrive (end-of-stream).
     * @param context   Metadata shared among all pipes in the chain.
     * @param pump      The driving pump (may be `null` in unit tests).
     * @return An [EStatus] code indicating what the pump should do next.
     */
    fun process(
        channels: LLChannelDescriptors,
        buffer: LLBufferArray,
        eos: Boolean,
        context: LLSD,
        pump: Any? = null
    ): EStatus {
        return processImpl(channels, buffer, eos, context, pump)
    }

    /**
     * Handle a propagated error from another pipe in the chain.
     *
     * The pump rewinds the chain on error, calling this method on each pipe to
     * see if it can recover.  Return [EStatus.STATUS_OK] to signal recovery,
     * or the original [status] (or another error) to propagate further.
     *
     * The default implementation always propagates the error unchanged.
     */
    open fun handleError(status: EStatus, pump: Any?): EStatus = status

    // -------------------------------------------------------------------------
    // Abstract interface for subclasses
    // -------------------------------------------------------------------------

    /**
     * Template-method hook: subclasses implement their processing logic here.
     *
     * Implementations should:
     *  - Read from `buffer` on `channels.in`
     *  - Write results to `buffer` on `channels.out`
     *  - Return an appropriate [EStatus]
     */
    protected abstract fun processImpl(
        channels: LLChannelDescriptors,
        buffer: LLBufferArray,
        eos: Boolean,
        context: LLSD,
        pump: Any?
    ): EStatus
}
