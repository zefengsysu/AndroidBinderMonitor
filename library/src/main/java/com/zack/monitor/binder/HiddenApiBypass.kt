package com.zack.monitor.binder

import androidx.annotation.GuardedBy
import java.lang.reflect.Method

object HiddenApiBypass {
    private const val TAG = "AndroidBinderMonitor.HiddenApiBypass"

    private val isLibLoaded by lazy {
        LibraryLoader.load("binder_monitor")
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
        exemptAllResult = reflectExemptAll()
        Log.i(TAG, "exemptAll, reflect exempt success: $exemptAllResult")
        if (!exemptAllResult && isLibLoaded) {
            exemptAllResult = nativeExemptAll()
            Log.i(TAG, "exemptAll, jni exempt success: $exemptAllResult")
        }
        hasTryExemptAll = true
        return exemptAllResult
    }

    @JvmStatic
    private external fun nativeExemptAll(): Boolean

    private fun reflectExemptAll(): Boolean {
        return try {
            val getDeclaredMethod = MetaReflectHelper.resolveMethodOfGetDeclaredMethod() ?: run {
                Log.e(TAG, "reflectExemptAll, getDeclaredMethod fail")
                return false
            }
            val runtimeClass = Class.forName("dalvik.system.VMRuntime")
            val getRuntimeMethod = getDeclaredMethod.invoke(runtimeClass, "getRuntime", null) as Method
            val runtime = getRuntimeMethod.invoke(null)
            val setHiddenApiExemptionsMethod =
                getDeclaredMethod.invoke(runtimeClass, "setHiddenApiExemptions", arrayOf(Array<String>::class.java))
                        as Method
            setHiddenApiExemptionsMethod.invoke(runtime, arrayOf("L"))
            true
        } catch (e: Exception) {
            Log.e(TAG, "reflectExemptAll fail", e)
            false
        }
    }
}
