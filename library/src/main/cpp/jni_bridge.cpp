#include <jni.h>

#include "macros.h"
#include "hidden_api_bypass.h"
#include "binder_proxy_transact_native_hooker.h"
#include "bp_binder_transact_hooker.h"

jboolean com_zack_monitor_binder_HiddenApiBypass_nativeExemptAll(JNIEnv *, jclass);

jboolean com_zack_monitor_binder_TransactNativeHooker_nativeHook(JNIEnv *, jclass, jboolean, jlong, jboolean, jfloat);

jboolean com_zack_monitor_binder_TransactNativeHooker_nativeUnhook(JNIEnv *, jclass);

jboolean com_zack_monitor_binder_TransactHooker_nativeHook(JNIEnv *, jclass, jboolean, jlong, jboolean, jfloat, jboolean);

jboolean com_zack_monitor_binder_TransactHooker_nativeUnhook(JNIEnv *, jclass);

JavaVM *g_vm = nullptr;

static const JNINativeMethod g_hidden_api_bypass_methods[] = {
        {"nativeExemptAll", "()Z", (void *) com_zack_monitor_binder_HiddenApiBypass_nativeExemptAll}
};
static const JNINativeMethod g_transact_native_hooker_methods[] = {
        {"nativeHook", "(ZJZF)Z", (void *) com_zack_monitor_binder_TransactNativeHooker_nativeHook},
        {"nativeUnhook", "()Z", (void *) com_zack_monitor_binder_TransactNativeHooker_nativeUnhook}
};
static const JNINativeMethod g_transact_hooker_methods[] = {
        {"nativeHook",   "(ZJZFZ)Z", (void *) com_zack_monitor_binder_TransactHooker_nativeHook},
        {"nativeUnhook", "()Z", (void *) com_zack_monitor_binder_TransactHooker_nativeUnhook}
};

jint JNI_OnLoad(JavaVM *vm, void *) {
    JNIEnv *env = nullptr;
    if (vm->GetEnv((void **) &env, JNI_VERSION_1_4) != JNI_OK) {
        return JNI_ERR;
    }

    g_vm = vm;
    auto bypass_class = env->FindClass("com/zack/monitor/binder/HiddenApiBypass");
    env->RegisterNatives(bypass_class, g_hidden_api_bypass_methods,
                         NELEM(g_hidden_api_bypass_methods));
    auto transact_native_hooker_class = env->FindClass(
            "com/zack/monitor/binder/BinderProxyTransactNativeHooker");
    env->RegisterNatives(transact_native_hooker_class, g_transact_native_hooker_methods,
                         NELEM(g_transact_native_hooker_methods));
    auto transact_hooker_class = env->FindClass("com/zack/monitor/binder/BpBinderTransactHooker");
    env->RegisterNatives(transact_hooker_class, g_transact_hooker_methods,
                         NELEM(g_transact_hooker_methods));

    return JNI_VERSION_1_4;
}

jboolean com_zack_monitor_binder_HiddenApiBypass_nativeExemptAll(JNIEnv *, jclass) {
    return HiddenApiBypass::ExemptAll(g_vm) ? JNI_TRUE : JNI_FALSE;
}

jboolean com_zack_monitor_binder_TransactNativeHooker_nativeHook(
    JNIEnv *env, jclass,
    jboolean monitor_block_on_main_thread, jlong block_time_threshold_ms,
    jboolean monitor_data_too_large, jfloat data_too_large_factor
) {
    return BinderProxyTransactNativeHooker::Hook(
        env,
        JNI_TRUE == monitor_block_on_main_thread, block_time_threshold_ms,
        JNI_TRUE == monitor_data_too_large, data_too_large_factor
    ) ? JNI_TRUE : JNI_FALSE;
}

jboolean com_zack_monitor_binder_TransactNativeHooker_nativeUnhook(JNIEnv *env, jclass) {
    return BinderProxyTransactNativeHooker::Unhook(env) ? JNI_TRUE : JNI_FALSE;
}

jboolean com_zack_monitor_binder_TransactHooker_nativeHook(
    JNIEnv *env, jclass,
    jboolean monitor_block_on_main_thread, jlong block_time_threshold_ms,
    jboolean monitor_data_too_large, jfloat data_too_large_factor,
    jboolean skip_transact_native
) {
    SkipTransactFn skip_transact_fn =
        JNI_TRUE == skip_transact_native ? [](){ return BinderProxyTransactNativeHooker::InTransactNative(); }
            : BpBinderTransactHooker::NeverSkipTransact;
    return BpBinderTransactHooker::Hook(
        g_vm, env,
        JNI_TRUE == monitor_block_on_main_thread, block_time_threshold_ms,
        JNI_TRUE == monitor_data_too_large, data_too_large_factor,
        skip_transact_fn
    ) ? JNI_TRUE : JNI_FALSE;
}

jboolean com_zack_monitor_binder_TransactHooker_nativeUnhook(JNIEnv *, jclass) {
    return BpBinderTransactHooker::Unhook() ? JNI_TRUE : JNI_FALSE;
}
