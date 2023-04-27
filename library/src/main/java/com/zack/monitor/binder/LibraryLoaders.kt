package com.zack.monitor.binder

private const val TAG = "AndroidBinderMonitor.LibraryLoaders"

interface ILibraryLoader {
    fun load(libname: String): Boolean
}

object LibraryLoader : ILibraryLoader {
    @Volatile
    var loaderImpl: ILibraryLoader = DefaultLibraryLoaderImpl

    override fun load(libname: String) = loaderImpl.load(libname)
}

private object DefaultLibraryLoaderImpl : ILibraryLoader {
    override fun load(libname: String) =
        try {
            System.loadLibrary(libname)
            true
        } catch (tr: Throwable) {
            Log.e(TAG, "load $libname fail", tr)
            false
        }
}
