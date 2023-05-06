package com.zack.monitor.binder

interface IBinderTransactListener {
    @Deprecated("Use onTransacted(BinderTransactParams) instead")
    fun onTransactStart(params: BinderTransactParams) {}
    @Deprecated("Use onTransacted(BinderTransactParams) instead")
    fun onTransactEnd() {}

    fun onTransactDataTooLarge(params: BinderTransactParams)
    fun onTransactBlock(params: BinderTransactParams, costTotalTimeMs: Long)
}
