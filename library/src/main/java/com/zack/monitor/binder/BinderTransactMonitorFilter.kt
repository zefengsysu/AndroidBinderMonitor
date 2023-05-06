package com.zack.monitor.binder

import android.os.Binder
import android.os.Looper
import android.os.SystemClock
import java.util.*

typealias OnTransactDataTooLarge = (BinderTransactParams) -> Unit
typealias OnTransactBlock = (BinderTransactParams, Long) -> Unit

private class TimeStampedValue<T>(val value: T) {
    val createTotalTimeMs = SystemClock.elapsedRealtime()
    val createSelfTimeMs = SystemClock.currentThreadTimeMillis()

    fun totalTimeSinceCreate() = SystemClock.elapsedRealtime() - createTotalTimeMs
    fun selfTimeSinceCreate() = SystemClock.currentThreadTimeMillis() - createSelfTimeMs
}

class BinderTransactMonitorFilter(
    private val config: BinderTransactMonitorConfig,
    private val onTransactDataTooLarge: OnTransactDataTooLarge,
    private val onTransactBlock: OnTransactBlock
) {
    companion object {
        private const val TAG = "AndroidBinderMonitor.BinderTransactMonitorFilter"
    }
    private val transactCallStack =
        object : ThreadLocal<Stack<TimeStampedValue<BinderTransactParams>>>() {
        override fun initialValue() = Stack<TimeStampedValue<BinderTransactParams>>()
    }

    fun onTransactStart(params: BinderTransactParams) {
        if (config.monitorDataTooLarge) {
            val isAsyncIPC = 0 != params.flags and Binder.FLAG_ONEWAY
            val threshold =
                if (isAsyncIPC) BinderTransactMonitorConfig.ASYNC_IPC_DATA_SIZE_THRESHOLD
                else BinderTransactMonitorConfig.SYNC_IPC_DATA_SIZE_THRESHOLD
            if (params.dataSize() >= config.dataTooLargeFactor * threshold) {
                onTransactDataTooLarge(params.attachBacktrace(resolveJavaBacktrace()))
            }
        }
        transactCallStack.get()!!.push(TimeStampedValue(params))
    }

    fun onTransactEnd() {
        val myCallStack = transactCallStack.get()!!
        if (myCallStack.isEmpty()) {
            Log.e(TAG, "onTransactEnd, myCallStack is empty")
            return
        }
        val timeStampedParams = myCallStack.pop()
        if (config.monitorBlockOnMainThread && isMainThread()) {
            val costTotalTimeMs = timeStampedParams.totalTimeSinceCreate()
            if (BinderTransactMonitorConfig.BLOCK_TIME_THRESHOLD_SYNC_AS_BLOCK == config.blockTimeThresholdMs) {
                val isAsyncIPC = 0 != timeStampedParams.value.flags and Binder.FLAG_ONEWAY
                if (!isAsyncIPC) {
                    onTransactBlock(
                        timeStampedParams.value.attachBacktrace(resolveJavaBacktrace()),
                        costTotalTimeMs
                    )
                    return
                }
            }
            if (costTotalTimeMs >= config.blockTimeThresholdMs) {
                onTransactBlock(
                    timeStampedParams.value.attachBacktrace(resolveJavaBacktrace()),
                    costTotalTimeMs
                )
                return
            }
        }
    }

    private fun isMainThread() = Looper.getMainLooper() == Looper.myLooper()
}
