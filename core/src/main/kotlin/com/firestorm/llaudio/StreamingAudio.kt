package com.firestorm.llaudio

// Converted from indra/llaudio/llstreamingaudio.h
// Interface for streaming internet audio (e.g. Shoutcast / Icecast URLs).

/**
 * Callback invoked when the stream delivers updated metadata (track title,
 * artist, etc.).  Corresponds to the Boost.Signals2 slot type declared in the
 * C++ header for [LLStreamingAudioInterface].
 */
typealias MetadataUpdateCallback = (metadata: Map<String, Any>) -> Unit

/**
 * Abstract interface for a streaming internet-audio back-end.
 *
 * Mirrors `LLStreamingAudioInterface` from `llstreamingaudio.h`.  The C++
 * class is entirely pure-virtual; the Kotlin equivalent is an `interface`
 * so that engine-specific implementations (FMOD Studio, OpenAL + libavcodec,
 * etc.) can implement it without carrying an unneeded class hierarchy.
 *
 * Play-state values mirror [SoundFlags.STATE_STOPPED], [SoundFlags.STATE_PLAYING],
 * and [SoundFlags.STATE_PAUSED] so callers can map them without switching on
 * magic integers.
 *
 * ### Typical frame-update sequence
 * ```kotlin
 * streamingAudio.start("https://example.com/stream")
 * // … every frame …
 * streamingAudio.update()
 * val playing = streamingAudio.isPlaying() == SoundFlags.STATE_PLAYING
 * ```
 */
interface StreamingAudio {

    // ---- Playback control ---------------------------------------------------

    /**
     * Begin streaming from [url].
     *
     * If a stream is already playing it must be stopped before the new one
     * starts.  An empty [url] is equivalent to [stop].
     *
     * Mirrors `void LLStreamingAudioInterface::start(const std::string& url)`.
     */
    fun start(url: String)

    /**
     * Stop the current stream and release any associated decoder resources.
     *
     * Mirrors `void LLStreamingAudioInterface::stop()`.
     */
    fun stop()

    /**
     * Pause or resume the current stream.
     *
     * @param pause `true` to pause, `false` to resume.
     *
     * Mirrors `void LLStreamingAudioInterface::pause(int pause)`.
     */
    fun pause(pause: Boolean)

    /**
     * Advance the stream state machine; expected to be called every frame.
     *
     * Used to pump decoder buffers, handle connection errors, and fire
     * metadata callbacks.
     *
     * Mirrors `void LLStreamingAudioInterface::update()`.
     */
    fun update()

    // ---- State query --------------------------------------------------------

    /**
     * Return the current playback state.
     *
     * Returns one of:
     * - [SoundFlags.STATE_STOPPED] (0) — not playing
     * - [SoundFlags.STATE_PLAYING] (1) — actively streaming
     * - [SoundFlags.STATE_PAUSED]  (2) — paused
     *
     * Mirrors `int LLStreamingAudioInterface::isPlaying()`.
     */
    fun isPlaying(): Int

    /**
     * Return the URL that was last passed to [start], or an empty string if
     * no stream has been started.
     *
     * Mirrors `std::string LLStreamingAudioInterface::getURL()`.
     */
    fun getUrl(): String

    // ---- Gain ---------------------------------------------------------------

    /**
     * Set the stream output volume in the range [0.0, 1.0].
     *
     * Mirrors `void LLStreamingAudioInterface::setGain(F32 vol)`.
     */
    fun setGain(gain: Float)

    /**
     * Return the current stream output volume in the range [0.0, 1.0].
     *
     * Mirrors `F32 LLStreamingAudioInterface::getGain()`.
     */
    fun getGain(): Float

    // ---- Optional: buffer size tuning (Firestorm extension) -----------------

    /**
     * Whether the back-end supports adjustable stream and decode buffer sizes.
     *
     * Mirrors `bool LLStreamingAudioInterface::supportsAdjustableBufferSizes()`.
     * Default: `false`.
     */
    fun supportsAdjustableBufferSizes(): Boolean = false

    /**
     * Adjust stream and decode buffer durations (milliseconds).
     *
     * Only called when [supportsAdjustableBufferSizes] returns `true`.
     *
     * Mirrors `void LLStreamingAudioInterface::setBufferSizes(U32, U32)`.
     * Default: no-op.
     *
     * @param streamBufferMs  Total stream ring-buffer length in milliseconds.
     * @param decodeBufferMs  Decode look-ahead length in milliseconds.
     */
    fun setBufferSizes(streamBufferMs: UInt, decodeBufferMs: UInt) {}

    // ---- Optional: metadata callbacks (Firestorm addition) ------------------

    /**
     * Register [callback] to be invoked when the stream provides updated
     * metadata (track title, artist, artwork URL, etc.).
     *
     * The callback receives a string-keyed property map whose schema is
     * implementation-defined (typically LLSD-equivalent JSON decoded by the
     * back-end).
     *
     * Returns a handle that can be passed to [removeMetadataUpdateCallback]
     * to unregister the callback.  Default implementation is a no-op that
     * returns an opaque sentinel handle.
     *
     * Mirrors the Boost.Signals2-based
     * `LLStreamingAudioInterface::setMetadataUpdateCallback(...)`.
     */
    fun addMetadataUpdateCallback(callback: MetadataUpdateCallback): Any = Unit

    /**
     * Unregister a previously registered metadata callback.
     *
     * [handle] must be the value returned by [addMetadataUpdateCallback].
     * Default implementation is a no-op.
     */
    fun removeMetadataUpdateCallback(handle: Any) {}

    /**
     * Return the most recently received metadata snapshot, or an empty map if
     * none has been received yet.
     *
     * Mirrors `LLSD LLStreamingAudioInterface::getCurrentMetadata() const`.
     */
    fun getCurrentMetadata(): Map<String, Any> = emptyMap()
}

// ---------------------------------------------------------------------------
// Null / stub implementation
// ---------------------------------------------------------------------------

/**
 * No-op [StreamingAudio] implementation used when no real streaming back-end
 * is available (e.g. during unit tests or when the audio subsystem is
 * disabled).
 *
 * All playback calls are stubbed with [TODO]("AUDIO: ...") so that a real
 * engine implementation can replace them.
 */
class NullStreamingAudio : StreamingAudio {

    private var currentUrl: String = ""
    private var gain: Float        = 1f
    private var state: Int         = SoundFlags.STATE_STOPPED

    override fun start(url: String) {
        currentUrl = url
        if (url.isEmpty()) {
            state = SoundFlags.STATE_STOPPED
        } else {
            System.err.println("NullStreamingAudio: start not yet implemented")
        }
    }

    override fun stop() {
        currentUrl = ""
        state = SoundFlags.STATE_STOPPED
        System.err.println("NullStreamingAudio: stop not yet implemented")
    }

    override fun pause(pause: Boolean) {
        state = if (pause) SoundFlags.STATE_PAUSED else SoundFlags.STATE_PLAYING
        System.err.println("NullStreamingAudio: pause not yet implemented")
    }

    override fun update() {
        System.err.println("NullStreamingAudio: update not yet implemented")
    }

    override fun isPlaying(): Int = state

    override fun getUrl(): String = currentUrl

    override fun setGain(gain: Float) {
        this.gain = gain.coerceIn(0f, 1f)
        System.err.println("NullStreamingAudio: setGain not yet implemented")
    }

    override fun getGain(): Float = gain
}
