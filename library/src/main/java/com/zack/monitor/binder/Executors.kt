package com.zack.monitor.binder

import android.os.Handler
import java.util.concurrent.Executor

object ImmediateExecutor : Executor {
    override fun execute(command: Runnable?) {
        command?.run()
    }
}

class HandlerExecutor(private val handler: Handler) : Executor {
    override fun execute(command: Runnable?) {
        command ?: return
        handler.post(command)
    }
}
