package com.zack.monitor.binder

interface IBinderTransactMonitor {
    val dispatcher: BinderTransactDispatcher

    fun enableMonitor(config: BinderTransactMonitorConfig = BinderTransactMonitorConfig.DEFAULT): Boolean
    fun disableMonitor(): Boolean
}
