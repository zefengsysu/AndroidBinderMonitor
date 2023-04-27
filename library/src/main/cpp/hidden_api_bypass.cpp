//
// Created by 王泽锋 on 2023/4/27.
//

#include "hidden_api_bypass.h"

#include <future>

#include "log.h"

#define TAG "AndroidBinderMonitor.HiddenApiBypass"

bool HiddenApiBypass::exemptAll(JavaVM *vm) {
    // async for use null class loader,
    // so we can call VMRuntime.getRuntime().setHiddenApiExemptions(new String[]{"L"}); use jni
    auto exempt_all_future = std::async([](JavaVM* vm) {
        JNIEnv *env;
        if (JNI_OK != vm->AttachCurrentThread(&env, nullptr)) {
            LOGE(TAG, "resolve env fail");
            return false;
        }
        auto runtime_class = env->FindClass("dalvik/system/VMRuntime");
        if (nullptr == runtime_class) {
            LOGE(TAG, "resolve VMRuntime class fail");
            return false;
        }
        auto get_runtime_method = env->GetStaticMethodID(runtime_class, "getRuntime", "()Ldalvik/system/VMRuntime;");
        if (nullptr == get_runtime_method) {
            LOGE(TAG, "resolve getRuntime method fail");
            return false;
        }
        auto exempt_method = env->GetMethodID(runtime_class, "setHiddenApiExemptions", "([Ljava/lang/String;)V");
        if (nullptr == exempt_method) {
            LOGE(TAG, "resolve setHiddenApiExemptions method fail");
            return false;
        }
        auto runtime = env->CallStaticObjectMethod(runtime_class, get_runtime_method);
        if (nullptr == runtime) {
            LOGE(TAG, "resolve runtime fail");
            return false;
        }
        auto string_class = env->FindClass("java/lang/String");
        auto prefixes = env->NewObjectArray(1, string_class, nullptr);
        auto generic_prefix = env->NewStringUTF("L");
        env->SetObjectArrayElement(prefixes, 0, generic_prefix);
        // pass nullptr runtime is also ok
        env->CallVoidMethod(runtime, exempt_method, prefixes);
        return true;
    }, vm);
    return exempt_all_future.get();
}
