package com.zack.monitor.binder

import android.annotation.SuppressLint
import android.os.Build
import android.os.IBinder
import java.lang.reflect.Field
import java.lang.reflect.InvocationHandler
import java.lang.reflect.Method
import java.lang.reflect.Proxy

class JavaBinderTransactMonitorApi29(dispatcher: BinderTransactDispatcher)
    : BaseBinderTransactMonitor(dispatcher), InvocationHandler {
    companion object {
        private const val TAG = "AndroidBinderMonitor.JavaBinderTransactMonitorApi29"

        private val binderProxyClass: Class<*>? by lazy {
            @SuppressLint("PrivateApi")
            val clazz = try {
                Class.forName("android.os.BinderProxy")
            } catch (e: Exception) {
                Log.e(TAG, "reflect binderProxyClass fail", e)
                null
            }
            clazz
        }
        private val sTransactListenerField: Field? by lazy {
            if (null == binderProxyClass) {
                return@lazy null
            }
            // Accessing hidden field Landroid/os/BinderProxy;->sTransactListener:Landroid/os/Binder$ProxyTransactListener; (blocked, reflection, denied)
            try {
                binderProxyClass!!.getDeclaredField("sTransactListener").apply {
                    isAccessible = true
                }
            } catch (e: Exception) {
                Log.e(TAG, "reflect sTransactListenerField fail", e)
                null
            }
        }

        private val proxyTransactListenerClass: Class<*>? by lazy {
            @SuppressLint("PrivateApi")
            val clazz = try {
                Class.forName("android.os.Binder\$ProxyTransactListener")
            } catch (e: Exception) {
                Log.e(TAG, "reflect proxyTransactListenerClass fail", e)
                null
            }
            clazz
        }

        init {
            val exemptSuccess = HiddenApiBypass.exemptAll()
            Log.i(TAG, "<cinit>, exempt hidden api ${if (exemptSuccess) "success" else "fail"}")
        }
    }

    @Volatile
    private var originTransactListener: Any? = null
    private val newTransactListener: Any? by lazy {
        if (null == proxyTransactListenerClass) {
            return@lazy null
        }
        Proxy.newProxyInstance(
            JavaBinderTransactMonitorApi29::class.java.classLoader,
            arrayOf(proxyTransactListenerClass!!),
            this
        )
    }

    private var monitorFilter: BinderTransactMonitorFilter? = null

    override fun doEnableMonitor(config: BinderTransactMonitorConfig): Boolean {
        if (Build.VERSION.SDK_INT < 29) {
            Log.i(TAG, "enableMonitor, sdk version mismatch")
            return false
        }
        resolveOriginTransactListener()
        if (null == newTransactListener) {
            Log.e(TAG, "enableMonitor, newTransactListener is null")
            return false
        }
        return try {
            sTransactListenerField!!.set(null, newTransactListener)
            monitorFilter = BinderTransactMonitorFilter(config, this::dispatchTransacted)
            true
        } catch (e: Exception) {
            Log.e(TAG, "reflect set newTransactListener fail", e)
            false
        }
    }

    override fun doDisableMonitor() =
        try {
            sTransactListenerField!!.set(null, originTransactListener)
            false
        } catch (e: Exception) {
            Log.e(TAG, "reflect set originTransactListener fail", e)
            true
        }

    override fun invoke(proxy: Any?, method: Method?, args: Array<out Any>?): Any? {
        method ?: return null
        args ?: return null
        when (method.name) {
            "onTransactStarted" -> {
                if (args.size >= 2) {
                    try {
                        val binder = args[0] as IBinder
                        val transactionCode = args[1] as Int
                        val flags = if (args.size >= 3) args[2] as Int else BinderTransactParams.INVALID_FLAGS
                        val params = BinderTransactParams.Java(transactionCode, flags, binder)
//                        Log.d(TAG, "onTransactStarted, params: $params")
//                        dispatcher.dispatchTransactStart(params)
                        monitorFilter?.onTransactStart(params)
                    } catch (e: Exception) {
                        Log.e(TAG, "proxy onTransactStarted call fail", e)
                    }
                } else {
                    Log.e(TAG, "proxy onTransactStarted call, illegal args: ${args.contentToString()}")
                }
            }
            "onTransactEnded" -> {
                if (args.isNotEmpty()) {
//                    Log.d(TAG, "onTransactEnded")
//                    dispatcher.dispatchTransactEnd()
                    monitorFilter?.onTransactEnd()
                } else {
                    Log.e(TAG, "proxy onTransactEnded call, illegal args")
                }
            }
        }
        return originTransactListener?.let { method.invoke(it, args) }
    }

    private fun resolveOriginTransactListener() {
        if (null == sTransactListenerField) {
            return
        }
        originTransactListener = try {
            sTransactListenerField!!.get(null)
        } catch (e: Exception) {
            Log.e(TAG, "resolveOriginTransactListener, reflect originTransactListener fail", e)
            null
        }
    }

    private fun dispatchTransacted(params: BinderTransactParams, costTimeMs: Long) {
        Log.d(TAG, "dispatchTransacted, params: $params, costTimeMs: $costTimeMs")
        dispatcher.dispatchTransacted(params, costTimeMs)
    }
}
