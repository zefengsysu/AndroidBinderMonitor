package com.zack.monitor.binder

import androidx.annotation.GuardedBy

object HiddenApiBypass {
    private val isLibLoaded = LibraryLoader.load("binder_monitor")

    @GuardedBy("this")
    private var hasTryExemptAll = false
    @GuardedBy("this")
    private var exemptAllResult = false

    @Synchronized
    fun exemptAll(): Boolean {
        if (hasTryExemptAll) {
            return exemptAllResult
        }
        if (isLibLoaded) {
            exemptAllResult = nativeExemptAll()
        }
        hasTryExemptAll = true
        return exemptAllResult
    }

    @JvmStatic
    private external fun nativeExemptAll(): Boolean
}
