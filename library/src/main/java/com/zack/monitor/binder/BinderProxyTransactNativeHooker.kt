package com.zack.monitor.binder

import android.os.IBinder
import android.os.Parcel
import androidx.annotation.GuardedBy
import androidx.annotation.Keep

object BinderProxyTransactNativeHooker {
    private const val TAG = "AndroidBinderMonitor.BinderProxyTransactNativeHooker"

    private val initSuccess: Boolean

    @GuardedBy("this")
    private var hasTryHook = false
    @GuardedBy("this")
    private var hookResult = false

    private var monitorFilter: BinderTransactMonitorFilter? = null

    init {
        val exemptSuccess = HiddenApiBypass.exemptAll()
        initSuccess =
            if (exemptSuccess) LibraryLoader.load("binder_monitor")
            else false
        Log.i(TAG, "<cinit>, exemptSuccess: $exemptSuccess, initSuccess: $initSuccess")
    }

    @Synchronized
    fun hook(config: BinderTransactMonitorConfig): Boolean {
        if (hasTryHook) {
            return hookResult
        }
        if (initSuccess) {
            hookResult = nativeHook(
                config.monitorBlockOnMainThread, config.blockTimeThresholdMs,
                config.monitorDataTooLarge, config.dataTooLargeFactor
            )
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

    @JvmStatic
    private external fun nativeHook(
        monitorBlockOnMainThread: Boolean, blockTimeThresholdMs: Long,
        monitorDataTooLarge: Boolean, dataTooLargeFactor: Float
    ): Boolean
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

    @Keep
    @JvmStatic
    private fun onTransactDataTooLarge(binder: IBinder, code: Int, data: Parcel?, flags: Int) {
        val params =
            BinderTransactParams.Java(code, flags, binder, data)
                .apply { attachBacktrace(resolveJavaBacktrace()) }
        Log.d(TAG, "onTransactDataTooLarge, params: $params")
        BinderTransactDispatchers.dispatchTransactDataTooLarge(params)
    }
    @Keep
    @JvmStatic
    private fun onTransactBlock(
        binder: IBinder, code: Int, data: Parcel?, flags: Int, costTotalTimeMs: Long
    ) {
        val params =
            BinderTransactParams.Java(code, flags, binder, data)
                .apply { attachBacktrace(resolveJavaBacktrace()) }
        Log.d(TAG, "onTransactBlock, params: $params, costTotalTimeMs: $costTotalTimeMs")
        BinderTransactDispatchers.dispatchTransactBlock(params, costTotalTimeMs)
    }
}
