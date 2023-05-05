package com.zack.monitor.binder

import android.os.IBinder
import android.os.Parcel
import androidx.annotation.GuardedBy
import androidx.annotation.Keep

object BinderProxyTransactNativeHooker {
    private const val TAG = "AndroidBinderMonitor.BinderProxyTransactNativeHooker"

    // exemptAll success means lib loaded
    private val initSuccess = HiddenApiBypass.exemptAll()

    @GuardedBy("this")
    private var hasTryHook = false
    @GuardedBy("this")
    private var hookResult = false

    private var monitorFilter: BinderTransactMonitorFilter? = null

    @Synchronized
    fun hook(config: BinderTransactMonitorConfig): Boolean {
        if (hasTryHook) {
            return hookResult
        }
        if (initSuccess) {
            hookResult = nativeHook()
            if (hookResult) {
                monitorFilter = BinderTransactMonitorFilter(config, this::dispatchTransacted)
            }
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
//        Log.d(TAG, "onTransactStart, " +
//                "binder: ${binder.interfaceDescriptor}, " +
//                "code: $code, " +
//                "dataSize: ${data?.dataSize() ?: BinderTransactParams.INVALID_DATA_SIZE}, " +
//                "flags: $flags")
        val params = BinderTransactParams.Java(code, flags, binder, data)
//        BinderTransactDispatchers.dispatchTransactStart(params)
        monitorFilter?.onTransactStart(params)
    }
    @Keep
    @JvmStatic
    private fun onTransactEnd() {
//        Log.d(TAG, "onTransactEnd")
//        BinderTransactDispatchers.dispatchTransactEnd()
        monitorFilter?.onTransactEnd()
    }

    private fun dispatchTransacted(params: BinderTransactParams, costTimeMs: Long) {
        Log.d(TAG, "dispatchTransacted, params: $params, costTimeMs: $costTimeMs")
        BinderTransactDispatchers.dispatchTransacted(params, costTimeMs)
    }
}
