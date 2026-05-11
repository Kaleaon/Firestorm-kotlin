package com.firestorm.newview

// libnotify is a Linux-only shared library loaded at runtime via dlopen.
// On the JVM we replicate this with a ProcessBuilder call to notify-send,
// which ships with the same libnotify stack and avoids JNI / JNA complexity.
// All direct dlopen/dlsym/GLib calls are replaced with TODO("APR: ...") markers
// or the notify-send equivalent where a clean mapping exists.

import java.io.File
import java.util.concurrent.TimeUnit

abstract class GrowlNotifier {
    abstract fun showNotification(
        notificationTitle: String,
        notificationMessage: String,
        notificationType: String
    )
    abstract fun isUsable(): Boolean
    abstract fun registerApplication(application: String, notificationTypes: Set<String>)
    abstract fun needsThrottle(): Boolean
}

class DesktopNotifierLinux : GrowlNotifier() {

    // Icon path resolved from the viewer's read-only data directory.
    private val iconPath: String
    private var libAvailable: Boolean = false

    init {
        iconPath = findIconResource(smallIcon = true)
        libAvailable = tryInitLibnotify()
    }

    override fun showNotification(
        notificationTitle: String,
        notificationMessage: String,
        notificationType: String
    ) {
        if (!isUsable()) return

        // notify-send is the CLI front-end to the libnotify stack; it avoids
        // needing a JNI wrapper for the raw C library.
        val result = runCatching {
            ProcessBuilder(
                "notify-send",
                "--icon", iconPath,
                "--category", notificationType,
                "--expire-time", NOTIFICATION_TIMEOUT_MS.toString(),
                notificationTitle,
                notificationMessage
            )
                .redirectErrorStream(true)
                .start()
                .waitFor(5, TimeUnit.SECONDS)
        }

        if (result.isFailure) {
            TODO("APR: use JVM equivalent — notify-send invocation failed: ${result.exceptionOrNull()?.message}")
        }
    }

    override fun isUsable(): Boolean = libAvailable

    override fun registerApplication(application: String, notificationTypes: Set<String>) {
        // libnotify registers the app name at init time; no further per-type
        // registration is needed.
    }

    override fun needsThrottle(): Boolean = false

    // ------------------------------------------------------------------
    // Helpers
    // ------------------------------------------------------------------

    private fun tryInitLibnotify(): Boolean {
        // Probe whether notify-send (the libnotify CLI) is available on PATH.
        // This mirrors the C++ dlopen loop that tried libnotify.so.{1..7}.
        return try {
            val probe = ProcessBuilder("which", "notify-send")
                .redirectErrorStream(true)
                .start()
            probe.waitFor(2, TimeUnit.SECONDS) && probe.exitValue() == 0
        } catch (e: Exception) {
            TODO("APR: use JVM equivalent — could not probe for notify-send: ${e.message}")
        }
    }

    private fun findIconResource(smallIcon: Boolean): String {
        // In the C++ code gDirUtilp->getAppRODataDir() locates the viewer's
        // resource directory at runtime.
        TODO("APR: use JVM equivalent — resolve viewer app-data directory, then return path to " +
            if (smallIcon) "res-sdl/firestorm_icon128.png" else "res-sdl/firestorm_icon.png")
    }

    companion object {
        private const val NOTIFICATION_TIMEOUT_MS = 5000
    }
}
