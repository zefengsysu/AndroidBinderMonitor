package com.zack.monitor.binder

import android.os.IBinder
import android.os.Parcel
import androidx.annotation.GuardedBy
import androidx.annotation.Keep

object TransactNativeHooker {
    private const val TAG = "AndroidBinderMonitor.TransactNativeHooker"

    private val isLibLoaded = LibraryLoader.load("binder_monitor")

    @GuardedBy("this")
    private var hasTryHook = false
    @GuardedBy("this")
    private var hookResult = false

    @Synchronized
    fun hook(): Boolean {
        if (hasTryHook) {
            return hookResult
        }
        if (isLibLoaded) {
            hookResult = nativeHook()
        }
        hasTryHook = true
        return hookResult
    }

    @Synchronized
    fun unhook(): Boolean {
        if (!hookResult) {
            return true
        }
        hookResult = !nativeUnhook()
        hasTryHook = hookResult
        return !hookResult
    }

    @JvmStatic
    private external fun nativeHook(): Boolean
    @JvmStatic
    private external fun nativeUnhook(): Boolean

    @Keep
    @JvmStatic
    private fun onTransactStart(
        binder: IBinder, code: Int, data: Parcel?, flags: Int
    ) {
        Log.d(TAG, "onTransactStart, " +
                "binder: ${binder.interfaceDescriptor}, " +
                "code: $code, " +
                "dataSize: ${data?.dataSize() ?: BinderTransactParams.INVALID_DATA_SIZE}, " +
                "flags: $flags")
        val params = BinderTransactParams.Java(code, flags, binder, data)
        BinderTransactDispatchers.dispatchTransactStart(params)
    }
    @Keep
    @JvmStatic
    private fun onTransactEnd() {
        Log.d(TAG, "onTransactEnd")
        BinderTransactDispatchers.dispatchTransactEnd()
    }
}
