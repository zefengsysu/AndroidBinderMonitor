package com.zack.monitor.binder

// TODO(zefengwang): pass dispatcher not work
class GeneralBinderTransactMonitor(dispatcher: BinderTransactDispatcher)
    : BaseBinderTransactMonitor(dispatcher) {
    override fun doEnableMonitor(config: BinderTransactMonitorConfig) =
        BpBinderTransactHooker.hook(config, false)
    override fun doDisableMonitor() = BpBinderTransactHooker.unhook()
}
