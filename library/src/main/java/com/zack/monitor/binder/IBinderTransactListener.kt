package com.zack.monitor.binder

interface IBinderTransactListener {
    fun onTransactStart(params: BinderTransactParams)
    fun onTransactEnd()
}
