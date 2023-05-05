package com.zack.monitor.binder

interface IBinderTransactListener {
    @Deprecated("Use onTransacted(BinderTransactParams) instead")
    fun onTransactStart(params: BinderTransactParams) {}
    @Deprecated("Use onTransacted(BinderTransactParams) instead")
    fun onTransactEnd() {}

    fun onTransacted(params: BinderTransactParams, costTimeMs: Long)
}
