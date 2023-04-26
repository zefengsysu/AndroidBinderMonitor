package com.zack.monitor.binder

class JavaBinderTransactMonitor(
    override val dispatcher: BinderTransactDispatcher
) : IBinderTransactMonitor {
    override fun enableMonitor(): Boolean {
        TODO("Not yet implemented")
    }

    override fun disableMonitor(): Boolean {
        TODO("Not yet implemented")
    }
}
