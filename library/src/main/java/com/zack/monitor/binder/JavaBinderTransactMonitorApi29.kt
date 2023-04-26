package com.zack.monitor.binder

import android.annotation.SuppressLint
import android.os.Build
import android.os.IBinder
import androidx.annotation.GuardedBy
import java.lang.reflect.Field
import java.lang.reflect.InvocationHandler
import java.lang.reflect.Method
import java.lang.reflect.Proxy

class JavaBinderTransactMonitorApi29(
    override val dispatcher: BinderTransactDispatcher
) : IBinderTransactMonitor, InvocationHandler {
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

    @GuardedBy("this")
    private var isMonitored = false

    private val originTransactListener: Any? by lazy {
        if (null == sTransactListenerField) {
            return@lazy null
        }
        try {
            sTransactListenerField!!.get(null)
        } catch (e: Exception) {
            Log.e(TAG, "reflect originTransactListener fail", e)
            null
        }
    }
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

    @Synchronized
    override fun enableMonitor(): Boolean {
        if (isMonitored) {
            Log.i(TAG, "enableMonitor, already monitor")
            return true
        }
        if (Build.VERSION.SDK_INT < 29) {
            Log.i(TAG, "enableMonitor, sdk version mismatch")
            return false
        }
        if (null == newTransactListener) {
            Log.e(TAG, "enableMonitor, newTransactListener is null")
            return false
        }
        Log.i(TAG, "enableMonitor")
        isMonitored = try {
            sTransactListenerField!!.set(null, newTransactListener)
            true
        } catch (e: Exception) {
            Log.e(TAG, "reflect set newTransactListener fail", e)
            false
        }
        return isMonitored
    }

    @Synchronized
    override fun disableMonitor(): Boolean {
        if (!isMonitored) {
            Log.i(TAG, "disableMonitor, not monitor now")
            return true
        }
        Log.i(TAG, "disableMonitor")
        isMonitored = try {
            sTransactListenerField!!.set(null, originTransactListener!!)
            false
        } catch (e: Exception) {
            Log.e(TAG, "reflect set originTransactListener fail", e)
            true
        }
        return !isMonitored
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
                        Log.d(TAG, "onTransactStarted, params: $params")
                        dispatcher.dispatchTransactStart(params)
                    } catch (e: Exception) {
                        Log.e(TAG, "proxy onTransactStarted call fail", e)
                    }
                } else {
                    Log.e(TAG, "proxy onTransactStarted call, illegal args: ${args.contentToString()}")
                }
            }
            "onTransactEnded" -> {
                if (args.isNotEmpty()) {
                    Log.d(TAG, "onTransactEnded")
                    dispatcher.dispatchTransactEnd()
                } else {
                    Log.e(TAG, "proxy onTransactEnded call, illegal args")
                }
            }
        }
        return null
    }
}
