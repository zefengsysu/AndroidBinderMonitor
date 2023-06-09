package com.zack.monitor.binder

import androidx.annotation.GuardedBy
import androidx.annotation.Keep
import com.bytedance.android.bytehook.ByteHook

object BpBinderTransactHooker {
    private const val TAG = "AndroidBinderMonitor.BpBinderTransactHooker"

    private val initSuccess: Boolean

    @GuardedBy("this")
    private var hasTryHook = false
    @GuardedBy("this")
    private var hookResult = false

    private var monitorFilter: BinderTransactMonitorFilter? = null

    init {
        val bhookInitRet = ByteHook.init(
            ByteHook.ConfigBuilder()
                .setLibLoader(LibraryLoader::load)
//                .setDebug(true)
                .build()
        )
        initSuccess =
            if (0 == bhookInitRet) LibraryLoader.load("binder_monitor")
            else false
        Log.i(TAG, "<cinit>, bhookInitRet: $bhookInitRet, initSuccess: $initSuccess")
    }

    @Synchronized
    fun hook(config: BinderTransactMonitorConfig, skipTransactNative: Boolean): Boolean {
        if (hasTryHook) {
            return hookResult
        }
        if (initSuccess) {
            hookResult = nativeHook(
                config.monitorBlockOnMainThread, config.blockTimeThresholdMs,
                config.monitorDataTooLarge, config.dataTooLargeFactor,
                skipTransactNative
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
        monitorDataTooLarge: Boolean, dataTooLargeFactor: Float,
        skipTransactNative: Boolean
    ): Boolean
    @JvmStatic
    private external fun nativeUnhook(): Boolean

    @Keep
    @JvmStatic
    private fun onTransactStart(
        descriptor: String?, code: Int, dataSize: Int, flags: Int
    ) {
//        Log.d(TAG, "onTransactStart, " +
//                "binder: $descriptor, " +
//                "code: $code, " +
//                "dataSize: $dataSize, " +
//                "flags: $flags")
        val params = BinderTransactParams.General(code, flags, descriptor, dataSize)
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
    private fun onTransactDataTooLarge(descriptor: String?, code: Int, dataSize: Int, flags: Int) {
        val params =
            BinderTransactParams.General(code, flags, descriptor, dataSize)
                .apply { attachBacktrace(resolveJavaBacktrace()) }
        Log.d(TAG, "onTransactDataTooLarge, params: $params")
        BinderTransactDispatchers.dispatchTransactDataTooLarge(params)
    }
    @Keep
    @JvmStatic
    private fun onTransactBlock(
        descriptor: String?, code: Int, dataSize: Int, flags: Int, costTotalTimeMs: Long
    ) {
        val params =
            BinderTransactParams.General(code, flags, descriptor, dataSize)
                .apply { attachBacktrace(resolveJavaBacktrace()) }
        Log.d(TAG, "onTransactBlock, params: $params, costTotalTimeMs: $costTotalTimeMs")
        BinderTransactDispatchers.dispatchTransactBlock(params, costTotalTimeMs)
    }
}
