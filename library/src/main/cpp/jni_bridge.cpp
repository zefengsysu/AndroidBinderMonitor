#include <jni.h>

#include "macros.h"
#include "hidden_api_bypass.h"
#include "transact_native_hooker.h"

jboolean com_zack_monitor_binder_HiddenApiBypass_nativeExemptAll(JNIEnv *, jclass);
jboolean com_zack_monitor_binder_TransactNativeHooker_nativeHook(JNIEnv *, jclass);
jboolean com_zack_monitor_binder_TransactNativeHooker_nativeUnhook(JNIEnv *, jclass);

JavaVM *g_vm = nullptr;

static const JNINativeMethod g_hidden_api_bypass_methods[] = {
        {"nativeExemptAll", "()Z", (void *)com_zack_monitor_binder_HiddenApiBypass_nativeExemptAll}
};
static const JNINativeMethod g_transact_native_hooker_methods[] = {
        {"nativeHook", "()Z", (void *)com_zack_monitor_binder_TransactNativeHooker_nativeHook},
        {"nativeUnhook", "()Z", (void *)com_zack_monitor_binder_TransactNativeHooker_nativeUnhook}
};

jint JNI_OnLoad(JavaVM *vm, void *reserved) {
    JNIEnv *env = nullptr;
    if (vm->GetEnv((void **) &env, JNI_VERSION_1_4) != JNI_OK) {
        return JNI_ERR;
    }
    g_vm = vm;
    auto bypass_class = env->FindClass("com/zack/monitor/binder/HiddenApiBypass");
    env->RegisterNatives(bypass_class, g_hidden_api_bypass_methods, NELEM(g_hidden_api_bypass_methods));
    auto hooker_class = env->FindClass("com/zack/monitor/binder/TransactNativeHooker");
    env->RegisterNatives(hooker_class, g_transact_native_hooker_methods, NELEM(g_transact_native_hooker_methods));
    return JNI_VERSION_1_4;
}

jboolean com_zack_monitor_binder_HiddenApiBypass_nativeExemptAll(JNIEnv *, jclass) {
    return HiddenApiBypass::exemptAll(g_vm) ? JNI_TRUE : JNI_FALSE;
}

jboolean com_zack_monitor_binder_TransactNativeHooker_nativeHook(JNIEnv *env, jclass) {
    return TransactNativeHooker::hook(env) ? JNI_TRUE : JNI_FALSE;
}

jboolean com_zack_monitor_binder_TransactNativeHooker_nativeUnhook(JNIEnv *env, jclass) {
    return TransactNativeHooker::unhook(env) ? JNI_TRUE : JNI_FALSE;
}
