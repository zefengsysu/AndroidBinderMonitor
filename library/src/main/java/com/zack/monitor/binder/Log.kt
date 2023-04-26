package com.zack.monitor.binder

interface ILog {
    fun println(priority: Int, tag: String, msg: String)
}

object Log {
    @Volatile
    var logImpl: ILog = AndroidLogImpl

    fun v(tag: String, msg: String, tr: Throwable? = null) =
        println(android.util.Log.VERBOSE, tag, msg, tr)
    fun d(tag: String, msg: String, tr: Throwable? = null) =
        println(android.util.Log.DEBUG, tag, msg, tr)
    fun i(tag: String, msg: String, tr: Throwable? = null) =
        println(android.util.Log.INFO, tag, msg, tr)
    fun w(tag: String, msg: String, tr: Throwable? = null) =
        println(android.util.Log.WARN, tag, msg, tr)
    fun e(tag: String, msg: String, tr: Throwable? = null) =
        println(android.util.Log.ERROR, tag, msg, tr)

    private fun println(priority: Int, tag: String, msg: String, tr: Throwable? = null) {
        val logMsg = if (null == tr) msg else "$msg\n${android.util.Log.getStackTraceString(tr)}"
        logImpl.println(priority, tag, logMsg)
    }
}

private object AndroidLogImpl : ILog {
    override fun println(priority: Int, tag: String, msg: String) {
        android.util.Log.println(priority, tag, msg)
    }
}
