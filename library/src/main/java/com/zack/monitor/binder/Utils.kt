package com.zack.monitor.binder

fun resolveJavaBacktrace() =
//    dalvik.system.VMStack.getThreadStackTrace(Native Method)
//    java.lang.Thread.getStackTrace(Thread.java:1538)
//    com.zack.monitor.binder.BinderTransactMonitorFilter.onTransactEnd(BinderTransactMonitorFilter.kt:73)
//    com.zack.monitor.binder.BpBinderTransactHooker.onTransactEnd(BpBinderTransactHooker.kt:81)
    Thread.currentThread().stackTrace
        .filterIndexed { i, element ->
            i >= 2 && true != element.className?.startsWith("com.zack.monitor.binder")
        }
        .joinToString(separator = "\n") { it.toString() }
