package com.zack.monitor.binder

class BinderTransactMonitorConfig(
    val monitorBlockOnMainThread: Boolean = true,
    val blockTimeThresholdMs: Long = DEFAULT_BLOCK_TIME_THRESHOLD_MS,
    val monitorDataTooLarge: Boolean = true,
    val dataTooLargeFactor: Float = DEFAULT_DATA_TOO_LARGE_FACTOR
) {
    companion object {
        const val DEFAULT_BLOCK_TIME_THRESHOLD_MS = 100L
        const val BLOCK_TIME_THRESHOLD_SYNC_AS_BLOCK = 0L

        const val SYNC_IPC_DATA_SIZE_THRESHOLD = 1 * 1024 * 1024
        const val ASYNC_IPC_DATA_SIZE_THRESHOLD = SYNC_IPC_DATA_SIZE_THRESHOLD / 2
        const val DEFAULT_DATA_TOO_LARGE_FACTOR = 0.75f

        val DEFAULT = BinderTransactMonitorConfig()
    }
}
