package com.zack.monitor.binder

// TODO(zefengwang): pass dispatcher not work
class GeneralBinderTransactMonitor(dispatcher: BinderTransactDispatcher)
    : BaseBinderTransactMonitor(dispatcher) {
    override fun doEnableMonitor() = BpBinderTransactHooker.hook()
    override fun doDisableMonitor() = BpBinderTransactHooker.unhook()
}
