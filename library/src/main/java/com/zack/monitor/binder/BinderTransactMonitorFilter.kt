package com.zack.monitor.binder

import android.os.Binder
import android.os.Looper
import android.os.SystemClock
import java.util.*

typealias OnTransacted = (BinderTransactParams, Long) -> Unit

private class TimeStampedValue<T>(val value: T) {
    val createTimeMs = SystemClock.elapsedRealtime()

    fun timeSinceCreate() = SystemClock.elapsedRealtime() - createTimeMs
}

class BinderTransactMonitorFilter(
    private val config: BinderTransactMonitorConfig,
    private val onTransacted: OnTransacted
) {
    companion object {
        private const val TAG = "AndroidBinderMonitor.BinderTransactMonitorFilter"
    }
    private val transactCallStack =
        object : ThreadLocal<Stack<TimeStampedValue<BinderTransactParams>>>() {
        override fun initialValue() = Stack<TimeStampedValue<BinderTransactParams>>()
    }

    fun onTransactStart(params: BinderTransactParams) {
        transactCallStack.get()!!.push(TimeStampedValue(params))
    }

    fun onTransactEnd() {
        val myCallStack = transactCallStack.get()!!
        if (myCallStack.isEmpty()) {
            Log.e(TAG, "onTransactEnd, myCallStack is empty")
            return
        }
        val timeStampedParams = myCallStack.pop()
        val isAsyncIPC = 0 != timeStampedParams.value.flags and Binder.FLAG_ONEWAY
        val costTimeMs = timeStampedParams.timeSinceCreate()
        if (config.monitorBlockOnMainThread && isMainThread()) {
            if (BinderTransactMonitorConfig.BLOCK_TIME_THRESHOLD_SYNC_AS_BLOCK == config.blockTimeThresholdMs) {
                if (!isAsyncIPC) {
                    onTransacted(
                        timeStampedParams.value.attachBacktrace(
                            Thread.currentThread().stackTrace.joinToString(separator = "\n") { it.toString() }
                        ),
                        costTimeMs
                    )
                    return
                }
            }
            if (costTimeMs >= config.blockTimeThresholdMs) {
                onTransacted(
                    timeStampedParams.value.attachBacktrace(
                        Thread.currentThread().stackTrace.joinToString(separator = "\n") { it.toString() }
                    ),
                    costTimeMs
                )
                return
            }
        }
        if (config.monitorDataTooLarge) {
            val threshold =
                if (isAsyncIPC) BinderTransactMonitorConfig.ASYNC_IPC_DATA_SIZE_THRESHOLD
                else BinderTransactMonitorConfig.SYNC_IPC_DATA_SIZE_THRESHOLD
            if (timeStampedParams.value.dataSize() >= config.dataTooLargeFactor * threshold) {
                onTransacted(
                    timeStampedParams.value.attachBacktrace(
                        Thread.currentThread().stackTrace.joinToString(separator = "\n") { it.toString() }
                    ),
                    costTimeMs
                )
                return
            }
        }
    }

    private fun isMainThread() = Looper.getMainLooper() == Looper.myLooper()
}
