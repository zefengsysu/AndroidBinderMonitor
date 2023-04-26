package com.zack.monitor.binder

interface IBinderTransactMonitor {
    val dispatcher: BinderTransactDispatcher

    fun enableMonitor(): Boolean
    fun disableMonitor(): Boolean
}
