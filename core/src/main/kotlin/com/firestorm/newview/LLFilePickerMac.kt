package com.firestorm.newview

// Platform-specific macOS file-picker helpers.
// On JVM these delegate to a native file-chooser dialog; the Objective-C sheet
// and modeless-window behaviour is replaced with JVM equivalents via TODO stubs.

object FilePickerFlags {
    const val F_FILE: UInt        = 0x00000001u
    const val F_DIRECTORY: UInt   = 0x00000002u
    const val F_MULTIPLE: UInt    = 0x00000004u
    const val F_NAV_SUPPORT: UInt = 0x00000008u
}

fun doLoadDialog(allowedTypes: List<String>, flags: UInt): List<String>? {
    return null
}

fun doLoadDialogModeless(
    allowedTypes: List<String>,
    flags: UInt,
    callback: (Boolean, MutableList<String>) -> Unit
) {
    System.err.println("LLFilePickerMac: doLoadDialogModeless not yet implemented")
}

fun doSaveDialog(
    file: String,
    type: String,
    creator: String,
    extension: String,
    flags: UInt
): String? {
    return null
}

fun doSaveDialogModeless(
    file: String,
    type: String,
    creator: String,
    extension: String,
    flags: UInt,
    callback: (Boolean, String) -> Unit
) {
    System.err.println("LLFilePickerMac: doSaveDialogModeless not yet implemented")
}
