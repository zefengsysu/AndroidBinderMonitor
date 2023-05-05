package com.zack.monitor.binder

object DummyBinderTransactMonitor : IBinderTransactMonitor {
    override val dispatcher = BinderTransactDispatcher()

    override fun enableMonitor(config: BinderTransactMonitorConfig) = false
    override fun disableMonitor() = false
}
