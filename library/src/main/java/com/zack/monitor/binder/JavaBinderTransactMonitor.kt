package com.zack.monitor.binder

// TODO(zefengwang): pass dispatcher not work
class JavaBinderTransactMonitor(dispatcher: BinderTransactDispatcher)
    : BaseBinderTransactMonitor(dispatcher) {
    override fun doEnableMonitor() = BinderProxyTransactNativeHooker.hook()
    override fun doDisableMonitor() = BinderProxyTransactNativeHooker.unhook()
}
