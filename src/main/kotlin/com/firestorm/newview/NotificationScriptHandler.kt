package com.firestorm.newview

import java.util.UUID

// ScriptHandler is already defined in NotificationHandler.kt.
// This file extends it with the full processNotification logic that was in
// llnotificationscripthandler.cpp, which is the only .cpp for this handler.
//
// All logic is inlined into ScriptHandler inside NotificationHandler.kt;
// this file documents the mapping and provides any additional constants.

// NOTIFY_BOX_WIDTH is a viewer UI constant used when positioning the channel.
// The value matches the C++ NOTIFY_BOX_WIDTH macro.
const val SCRIPT_HANDLER_NOTIFY_BOX_WIDTH = 320

// No further declarations needed — see ScriptHandler in NotificationHandler.kt.
