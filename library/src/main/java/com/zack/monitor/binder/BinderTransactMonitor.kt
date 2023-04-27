package com.zack.monitor.binder

import android.os.Build

object BinderTransactMonitor : IBinderTransactMonitor {
    override val dispatcher = BinderTransactDispatcher()

    // TODO(zefengwang):
    private val monitorImpl =
        JavaBinderTransactMonitor(dispatcher)
//        if (Build.VERSION.SDK_INT >= 29) JavaBinderTransactMonitorApi29(dispatcher)
//        else DummyBinderTransactMonitor

    override fun enableMonitor(): Boolean {
        return monitorImpl.enableMonitor()
    }

    override fun disableMonitor(): Boolean {
        return monitorImpl.disableMonitor()
    }
}
