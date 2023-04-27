package com.zack.monitor.binder

import androidx.annotation.GuardedBy

// TODO(zefengwang): pass dispatcher not work
class JavaBinderTransactMonitor(
    override val dispatcher: BinderTransactDispatcher
) : IBinderTransactMonitor {
    companion object {
        private const val TAG = "AndroidBinderMonitor.JavaBinderTransactMonitor"

        init {
            val exemptSuccess = HiddenApiBypass.exemptAll()
            Log.i(TAG, "<cinit>, exempt hidden api ${if (exemptSuccess) "success" else "fail"}")
        }
    }

    @GuardedBy("this")
    private var isMonitored = false

    @Synchronized
    override fun enableMonitor(): Boolean {
        if (isMonitored) {
            Log.i(TAG, "enableMonitor, already monitor")
            return true
        }
        Log.i(TAG, "enableMonitor")
        isMonitored = TransactNativeHooker.hook()
        return isMonitored
    }

    override fun disableMonitor(): Boolean {
        if (!isMonitored) {
            Log.i(TAG, "disableMonitor, not monitor now")
            return true
        }
        Log.i(TAG, "disableMonitor")
        isMonitored = !TransactNativeHooker.unhook()
        return !isMonitored
    }
}
