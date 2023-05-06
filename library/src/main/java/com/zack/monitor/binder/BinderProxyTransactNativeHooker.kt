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
            // TODO(zefengwang):
//            hookResult = nativeHook(
//                config.monitorBlockOnMainThread, config.blockTimeThresholdMs,
//                config.monitorDataTooLarge, config.dataTooLargeFactor
//            )
            hookResult = nativeHook()
            if (hookResult) {
                monitorFilter = BinderTransactMonitorFilter(config, this::dispatchTransactDataTooLarge, this::dispatchTransactBlock)
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

    // TODO(zefengwang):
//    private external fun nativeHook(
//        monitorBlockOnMainThread: Boolean, blockTimeThresholdMs: Long,
//        monitorDataTooLarge: Boolean, dataTooLargeFactor: Float
//    ): Boolean
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

    private fun dispatchTransactDataTooLarge(params: BinderTransactParams) {
        Log.d(TAG, "dispatchTransactDataTooLarge, params: $params")
        BinderTransactDispatchers.dispatchTransactDataTooLarge(params)
    }

    private fun dispatchTransactBlock(params: BinderTransactParams, costTotalTimeMs: Long) {
        Log.d(TAG, "dispatchTransactBlock, params: $params, costTotalTimeMs: $costTotalTimeMs")
        BinderTransactDispatchers.dispatchTransactBlock(params, costTotalTimeMs)
    }
}
