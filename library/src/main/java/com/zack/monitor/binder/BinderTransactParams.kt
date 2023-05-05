package com.zack.monitor.binder

import android.os.IBinder
import android.os.Parcel

sealed class BinderTransactParams(
    val code: Int, val flags: Int = INVALID_FLAGS
) {
    companion object {
        const val INVALID_FLAGS = -1
        const val INVALID_DATA_SIZE = -1
    }

    private var _flattenBacktrace: String? = null
    val flattenBacktrace get() = _flattenBacktrace

    internal fun attachBacktrace(flattenBacktrace: String): BinderTransactParams {
        _flattenBacktrace = flattenBacktrace
        return this
    }

    open fun dataSize(): Int = INVALID_DATA_SIZE

    class Java(
        code: Int, flags: Int,
        val binder: IBinder, val data: Parcel? = null,
    ) : BinderTransactParams(code, flags) {
        override fun dataSize() = data?.dataSize() ?: super.dataSize()

        override fun toString(): String {
            return "JavaBinderTransactParams(" +
                    "binder=${binder.interfaceDescriptor}, " +
                    "code=$code, flags=$flags, " +
                    "dataSize=${data?.dataSize() ?: INVALID_DATA_SIZE}, " +
                    "backtrace=${flattenBacktrace})"
        }
    }

    class General(
        code: Int, flags: Int,
        val descriptor: String?, val dataSize: Int,
    ) : BinderTransactParams(code, flags) {
        override fun dataSize() = dataSize

        override fun toString(): String {
            return "GeneralBinderTransactParams(" +
                    "binder=$descriptor, " +
                    "code=$code, flags=$flags, " +
                    "dataSize=$dataSize, " +
                    "backtrace=${flattenBacktrace})"
        }
    }
}
