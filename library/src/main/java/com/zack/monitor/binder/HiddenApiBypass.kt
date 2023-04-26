package com.zack.monitor.binder

import androidx.annotation.GuardedBy

object HiddenApiBypass {
    init {
        System.loadLibrary("binder_monitor")
    }

    @GuardedBy("this")
    private var hasTryExemptAll = false
    @GuardedBy("this")
    private var exemptAllResult = false

    @Synchronized
    fun exemptAll(): Boolean {
        if (hasTryExemptAll) {
            return exemptAllResult
        }
        exemptAllResult = nativeExemptAll()
        hasTryExemptAll = true
        return exemptAllResult
    }

    private external fun nativeExemptAll(): Boolean
}
