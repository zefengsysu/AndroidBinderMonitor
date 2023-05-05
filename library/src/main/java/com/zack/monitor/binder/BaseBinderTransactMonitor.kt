package com.zack.monitor.binder

import androidx.annotation.GuardedBy

abstract class BaseBinderTransactMonitor(
    override val dispatcher: BinderTransactDispatcher
) : IBinderTransactMonitor {
    companion object {
        private const val TAG = "AndroidBinderMonitor.BaseBinderTransactMonitor"
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
        isMonitored = doEnableMonitor()
        return isMonitored
    }

    override fun disableMonitor(): Boolean {
        if (!isMonitored) {
            Log.i(TAG, "disableMonitor, not monitor now")
            return true
        }
        Log.i(TAG, "disableMonitor")
        isMonitored = !doDisableMonitor()
        return !isMonitored
    }

    protected abstract fun doEnableMonitor(): Boolean
    protected abstract fun doDisableMonitor(): Boolean
}
