package me.hiawui.hiassist

import android.util.Log

const val logTag = "hiawui-log"

fun logI(msgGenerator: () -> String) {
    if (Log.isLoggable(logTag, Log.INFO)) {
        Log.i(logTag, msgGenerator())
    }
}

fun logE(err: Throwable?, msgGenerator: () -> String) {
    if (Log.isLoggable(logTag, Log.ERROR)) {
        Log.e(logTag, msgGenerator(), err)
    }
}

fun logE(msgGenerator: () -> String) {
    logE(null, msgGenerator)
}

fun logW(err: Throwable?, msgGenerator: () -> String) {
    if (Log.isLoggable(logTag, Log.WARN)) {
        Log.w(logTag, msgGenerator(), err)
    }
}

fun logW(msgGenerator: () -> String) {
    logW(null, msgGenerator)
}
