package com.zack.monitor.binder

// TODO(zefengwang): pass dispatcher not work
// depend JavaBinderTransactMonitor
class NativeBinderTransactMonitor(dispatcher: BinderTransactDispatcher)
    : BaseBinderTransactMonitor(dispatcher)  {
    override fun doEnableMonitor(config: BinderTransactMonitorConfig) =
        BpBinderTransactHooker.hook(config, true)
    override fun doDisableMonitor() = BpBinderTransactHooker.unhook()
}
