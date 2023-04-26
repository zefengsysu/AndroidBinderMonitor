package com.zack.monitor.binder

object DummyBinderTransactMonitor : IBinderTransactMonitor {
    override val dispatcher = BinderTransactDispatcher()

    override fun enableMonitor() = false
    override fun disableMonitor() = false
}
