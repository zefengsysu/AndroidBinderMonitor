package com.zack.monitor.binder

import android.app.Application
import android.content.Context

class SampleApplication : Application() {
    override fun attachBaseContext(base: Context?) {
        super.attachBaseContext(base)

        BinderTransactMonitor.enableMonitor(
            BinderTransactMonitorConfig(
                blockTimeThresholdMs = 4L,
                dataTooLargeFactor = 0.1f
            )
        )
    }
}
