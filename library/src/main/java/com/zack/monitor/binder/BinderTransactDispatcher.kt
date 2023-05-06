package com.zack.monitor.binder

import android.os.Handler
import java.util.concurrent.CopyOnWriteArraySet
import java.util.concurrent.Executor

class BinderTransactDispatcher(private val dispatchExecutor: Executor = ImmediateExecutor) {
    constructor(dispatchHandler: Handler) : this(HandlerExecutor(dispatchHandler))

    init {
        BinderTransactDispatchers.add(this)
    }

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

    fun dispatchTransactDataTooLarge(params: BinderTransactParams) {
        dispatchExecutor.execute {
            listeners.forEach { listener -> listener.onTransactDataTooLarge(params) }
        }
    }
    fun dispatchTransactBlock(params: BinderTransactParams, costTotalTimeMs: Long) {
        dispatchExecutor.execute {
            listeners.forEach { listener -> listener.onTransactBlock(params, costTotalTimeMs) }
        }
    }
}

internal object BinderTransactDispatchers {
    private val dispatchers: MutableSet<BinderTransactDispatcher> = CopyOnWriteArraySet()

    fun add(dispatcher: BinderTransactDispatcher) = dispatchers.add(dispatcher)
    fun remove(dispatcher: BinderTransactDispatcher) = dispatchers.remove(dispatcher)

    fun dispatchTransactStart(params: BinderTransactParams) {
        dispatchers.forEach { dispatcher -> dispatcher.dispatchTransactStart(params) }
    }
    fun dispatchTransactEnd() {
        dispatchers.forEach(BinderTransactDispatcher::dispatchTransactEnd)
    }

    fun dispatchTransactDataTooLarge(params: BinderTransactParams) {
        dispatchers.forEach { dispatcher -> dispatcher.dispatchTransactDataTooLarge(params) }
    }
    fun dispatchTransactBlock(params: BinderTransactParams, costTotalTimeMs: Long) {
        dispatchers.forEach { dispatcher -> dispatcher.dispatchTransactBlock(params, costTotalTimeMs) }
    }
}
