package com.zack.monitor.binder

object BinderTransactMonitor : IBinderTransactMonitor {
    override val dispatcher = BinderTransactDispatcher()

    private val monitors = arrayOf<IBinderTransactMonitor>(
//        GeneralBinderTransactMonitor(dispatcher),
        JavaBinderTransactMonitor(dispatcher),
//        JavaBinderTransactMonitorApi29(dispatcher),
    )

    override fun enableMonitor(config: BinderTransactMonitorConfig): Boolean {
        for (monitor in monitors) {
            if (!monitor.enableMonitor(config)) {
                for (monitor2 in monitors) {
                    monitor2.disableMonitor()
                }
                return false
            }
        }
        return true
    }

    override fun disableMonitor(): Boolean {
        var allDisable = true
        for (monitor in monitors) {
            allDisable = allDisable && monitor.disableMonitor()
        }
        return allDisable
    }
}
