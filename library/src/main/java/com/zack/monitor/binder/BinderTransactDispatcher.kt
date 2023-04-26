package com.zack.monitor.binder

import android.os.Handler
import java.util.concurrent.CopyOnWriteArraySet
import java.util.concurrent.Executor

class BinderTransactDispatcher(private val dispatchExecutor: Executor = ImmediateExecutor) {
    constructor(dispatchHandler: Handler) : this(HandlerExecutor(dispatchHandler))

    private val listeners: MutableSet<IBinderTransactListener> = CopyOnWriteArraySet()

    fun addListener(listener: IBinderTransactListener) = listeners.add(listener)
    fun removeBinderTransactListener(listener: IBinderTransactListener) = listeners.remove(listener)

    fun dispatchTransactStart(params: BinderTransactParams) {
        dispatchExecutor.execute {
            listeners.forEach { listener -> listener.onTransactStart(params) }
        }
    }
    fun dispatchTransactEnd() {
        dispatchExecutor.execute { listeners.forEach(IBinderTransactListener::onTransactEnd) }
    }
}
