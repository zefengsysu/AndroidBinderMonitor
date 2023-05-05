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
    fun hook(): Boolean {
        if (hasTryHook) {
            return hookResult
        }
        if (initSuccess) {
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
        descriptor: String?, code: Int, dataSize: Int, flags: Int
    ) {
        Log.d(TAG, "onTransactStart, " +
                "binder: $descriptor, " +
                "code: $code, " +
                "dataSize: $dataSize, " +
                "flags: $flags")
        val params = BinderTransactParams.General(code, flags, descriptor, dataSize)
        BinderTransactDispatchers.dispatchTransactStart(params)
    }
    @Keep
    @JvmStatic
    private fun onTransactEnd() {
        Log.d(TAG, "onTransactEnd")
        BinderTransactDispatchers.dispatchTransactEnd()
    }
}
