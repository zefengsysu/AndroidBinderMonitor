//
// Created by 王泽锋 on 2023/4/27.
//

#include "binder_proxy_transact_native_hooker.h"

#include <cstring>
#include <memory>

#include "xdl.h"

#include "binder_transact_monitor_filter.h"
#include "log.h"
#include "macros.h"
#include "utils.h"

#define TAG "AndroidBinderMonitor.BinderProxyTransactNativeHooker"

// NOT THREAD SAFE

namespace {

// const char* GetMethodShorty(JNIEnv* env, jmethodID mid)
typedef const char *(*GetMethodShorty)(JNIEnv *, jmethodID);

// uint32_t GetNativeMethodCount(JNIEnv* env, jclass clazz)
typedef uint32_t(*GetNativeMethodCount)(JNIEnv *, jclass);

// uint32_t GetNativeMethods(JNIEnv* env, jclass clazz, JNINativeMethod* methods, uint32_t method_count)
typedef uint32_t(*GetNativeMethods)(JNIEnv *, jclass, JNINativeMethod *, uint32_t);

typedef jboolean(*TransactNative)(JNIEnv *, jobject, jint, jobject, jobject, jint);

const char *kTransactNativeName = "transactNative";
const char *kTransactNativeSig = "(ILandroid/os/Parcel;Landroid/os/Parcel;I)Z";

jboolean HijackedTransactNative(JNIEnv *, jobject, jint, jobject, jobject, jint);

bool g_has_try_init = false;
bool g_init_success = false;

GetMethodShorty g_get_method_shorty = nullptr;
GetNativeMethodCount g_get_native_method_count = nullptr;
GetNativeMethods g_get_native_methods = nullptr;

jclass g_binder_proxy_class = nullptr;
TransactNative g_origin_transact_native = nullptr;

jclass g_hooker_class = nullptr;
jmethodID g_on_transact_start_method = nullptr;
jmethodID g_on_transact_end_method = nullptr;
jmethodID g_on_transact_data_too_large_method = nullptr;
jmethodID g_on_transact_block_method = nullptr;

jmethodID g_data_size_method = nullptr;

class BinderProxyTransactNativeCallInfo;
std::unique_ptr<BinderTransactMonitorFilter<BinderProxyTransactNativeCallInfo>> g_monitor_filter = nullptr;

static const JNINativeMethod g_hijacked_binder_proxy_methods[] = {
        {kTransactNativeName, kTransactNativeSig, (void *) HijackedTransactNative}
};
static /*const*/ JNINativeMethod g_origin_binder_proxy_methods[] = {
        {kTransactNativeName, kTransactNativeSig, (void *) g_origin_transact_native}
};

class BinderProxyTransactNativeCallInfo : public BinderTransactCallInfo {
public:
    BinderProxyTransactNativeCallInfo(
        JNIEnv *env, jobject obj, jint code, jobject data_obj, jint flags
    ) : BinderTransactCallInfo(),
        env_(env), obj_(obj), code_(code), data_obj_(data_obj), flags_(flags) {}

public:
    int Flags() const override {
        return flags_;
    }

    int DataSize() const override {
        return env_->CallIntMethod(data_obj_, g_data_size_method);
    }

public:
    JNIEnv *env_;
    jobject obj_;
    jint code_;
    jobject data_obj_;
    jint flags_;
};

bool InitIfNeed(JNIEnv *env) {
    if (g_has_try_init) {
        return g_init_success;
    }
    g_has_try_init = true;

    XDLHandle art_handle("libart.so");
    if (nullptr == *art_handle) {
        LOGE(TAG, "dlopen art fail");
        return g_init_success;
    }
    // use xdl_dsym but not xdl_sym here
    g_get_method_shorty = (GetMethodShorty) xdl_dsym(*art_handle,
                                                     "_ZN3artL15GetMethodShortyEP7_JNIEnvP10_jmethodID",
                                                     nullptr);
    if (nullptr == g_get_method_shorty) {
        LOGE(TAG, "dlsym GetMethodShorty fail");
        return g_init_success;
    }
    g_get_native_method_count = (GetNativeMethodCount) xdl_dsym(*art_handle,
                                                                "_ZN3artL20GetNativeMethodCountEP7_JNIEnvP7_jclass",
                                                                nullptr);
    if (nullptr == g_get_native_method_count) {
        LOGE(TAG, "dlsym GetNativeMethodCount fail");
        return g_init_success;
    }
    g_get_native_methods = (GetNativeMethods) xdl_dsym(*art_handle,
                                                       "_ZN3artL16GetNativeMethodsEP7_JNIEnvP7_jclassP15JNINativeMethodj",
                                                       nullptr);
    if (nullptr == g_get_native_methods) {
        LOGE(TAG, "dlsym GetNativeMethods fail");
        return g_init_success;
    }

    g_binder_proxy_class = env->FindClass("android/os/BinderProxy");
    if (nullptr == g_binder_proxy_class) {
        LOGE(TAG, "resolve BinderProxy class fail");
        return false;
    }
    g_binder_proxy_class = (jclass) env->NewGlobalRef(g_binder_proxy_class);
    auto transact_native_method = env->GetMethodID(g_binder_proxy_class, kTransactNativeName,
                                                   kTransactNativeSig);
    if (nullptr == transact_native_method) {
        LOGE(TAG, "resolve transactNative method fail");
        return false;
    }
    auto transact_native_sig_shorty = g_get_method_shorty(env, transact_native_method);
    if (nullptr == transact_native_sig_shorty) {
        LOGE(TAG, "GetMethodShorty of transactNative fail");
        return false;
    }
    auto binder_proxy_native_method_count = g_get_native_method_count(env, g_binder_proxy_class);
    if (0 == binder_proxy_native_method_count) {
        LOGE(TAG, "GetNativeMethodCount of BinderProxy fail");
        return false;
    }
    JNINativeMethod native_methods[binder_proxy_native_method_count];
    auto got_binder_proxy_native_method_count = g_get_native_methods(env, g_binder_proxy_class,
                                                                     native_methods,
                                                                     binder_proxy_native_method_count);
    if (binder_proxy_native_method_count != got_binder_proxy_native_method_count) {
        LOGE(TAG, "GetNativeMethods of BinderProxy fail");
        return false;
    }
    for (auto &native_method: native_methods) {
        if (0 == strcmp(transact_native_sig_shorty, native_method.signature)) {
            g_origin_transact_native = (TransactNative) native_method.fnPtr;
        }
    }
    if (nullptr == g_origin_transact_native) {
        LOGE(TAG, "resolve origin TransactNative fail");
        return false;
    }
    g_origin_binder_proxy_methods[0].fnPtr = (void *) g_origin_transact_native;

    g_hooker_class = env->FindClass("com/zack/monitor/binder/BinderProxyTransactNativeHooker");
    g_hooker_class = (jclass) env->NewGlobalRef(g_hooker_class);
//    g_on_transact_start_method = env->GetStaticMethodID(g_hooker_class, "onTransactStart",
//                                                        "(Landroid/os/IBinder;ILandroid/os/Parcel;I)V");
//    g_on_transact_end_method = env->GetStaticMethodID(g_hooker_class, "onTransactEnd", "()V");
    g_on_transact_data_too_large_method = env->GetStaticMethodID(g_hooker_class, "onTransactDataTooLarge",
                                                                 "(Landroid/os/IBinder;ILandroid/os/Parcel;I)V");
    g_on_transact_block_method = env->GetStaticMethodID(g_hooker_class, "onTransactBlock",
                                                                 "(Landroid/os/IBinder;ILandroid/os/Parcel;IJ)V");

    jclass parcel_class = env->FindClass("android/os/Parcel");
    g_data_size_method = env->GetMethodID(parcel_class, "dataSize", "()I");

    g_init_success = true;
    return g_init_success;
}

jboolean HijackedTransactNative(
        JNIEnv *env, jobject obj,
        jint code, jobject dataObj, jobject replyObj, jint flags
) {
//    env->CallStaticVoidMethod(g_hooker_class, g_on_transact_start_method, obj, code, dataObj,
//                              flags);
    g_monitor_filter->OnTransactStart({env, obj, code, dataObj, flags});
    auto transact_ret = g_origin_transact_native(env, obj, code, dataObj, replyObj, flags);
//    env->CallStaticVoidMethod(g_hooker_class, g_on_transact_end_method);
    g_monitor_filter->OnTransactEnd();
    return transact_ret;
}

void onTransactDataTooLarge(BinderProxyTransactNativeCallInfo call_info) {
    call_info.env_->CallStaticVoidMethod(
        g_hooker_class, g_on_transact_data_too_large_method,
        call_info.obj_, call_info.code_, call_info.data_obj_, call_info.flags_
    );
}

void onTransactBlock(BinderProxyTransactNativeCallInfo call_info, long cost_total_time_ms) {
    call_info.env_->CallStaticVoidMethod(
        g_hooker_class, g_on_transact_block_method,
        call_info.obj_, call_info.code_, call_info.data_obj_, call_info.flags_,
        cost_total_time_ms
    );
}

} // namespace anon

bool BinderProxyTransactNativeHooker::Hook(
    JNIEnv *env,
    bool monitor_block_on_main_thread, long block_time_threshold_ms,
    bool monitor_data_too_large, float data_too_large_factor
) {
    if (!InitIfNeed(env)) {
        return false;
    }
    g_monitor_filter = std::make_unique<BinderTransactMonitorFilter<BinderProxyTransactNativeCallInfo>>(
        monitor_block_on_main_thread, block_time_threshold_ms,
        monitor_data_too_large, data_too_large_factor,
        onTransactDataTooLarge, onTransactBlock
    );
    auto register_ret = env->RegisterNatives(g_binder_proxy_class, g_hijacked_binder_proxy_methods,
                                             NELEM(g_hijacked_binder_proxy_methods));
    if (JNI_OK != register_ret) {
        LOGE(TAG, "register hijacked transactNative fail");
        g_monitor_filter = nullptr;
        return false;
    }
    return true;
}

bool BinderProxyTransactNativeHooker::Unhook(JNIEnv *env) {
    if (!g_init_success) {
        return true;
    }
    auto register_ret = env->RegisterNatives(g_binder_proxy_class, g_origin_binder_proxy_methods,
                                             NELEM(g_origin_binder_proxy_methods));
    if (JNI_OK != register_ret) {
        LOGE(TAG, "register origin transactNative fail");
        return false;
    }
    g_monitor_filter = nullptr;
    return true;
}
