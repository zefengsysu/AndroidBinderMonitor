#include <android/log.h>

#include <jni.h>

#include <future>

#define TAG "AndroidBinderMonitor.HiddenApiBypass.Native"

extern "C"
JNIEXPORT jboolean JNICALL
Java_com_zack_monitor_binder_HiddenApiBypass_nativeExemptAll(JNIEnv *env, jobject thiz) {
    JavaVM* vm;
    if (JNI_OK != env->GetJavaVM(&vm)) {
        __android_log_print(ANDROID_LOG_ERROR, TAG, "resolve vm fail");
        return JNI_FALSE;
    }
    auto exempt_all_future = std::async([](JavaVM* vm) {
        JNIEnv *env;
        if (JNI_OK != vm->AttachCurrentThread(&env, nullptr)) {
            __android_log_print(ANDROID_LOG_ERROR, TAG, "resolve env fail");
            return false;
        }
        auto runtime_class = env->FindClass("dalvik/system/VMRuntime");
        if (nullptr == runtime_class) {
            __android_log_print(ANDROID_LOG_ERROR, TAG, "resolve VMRuntime class fail");
            return false;
        }
        auto get_runtime_method = env->GetStaticMethodID(runtime_class, "getRuntime", "()Ldalvik/system/VMRuntime;");
        if (nullptr == get_runtime_method) {
            __android_log_print(ANDROID_LOG_INFO, TAG, "resolve getRuntime method fail");
            return false;
        }
        auto exempt_method = env->GetMethodID(runtime_class, "setHiddenApiExemptions", "([Ljava/lang/String;)V");
        if (nullptr == exempt_method) {
            __android_log_print(ANDROID_LOG_ERROR, TAG, "resolve setHiddenApiExemptions method fail");
            return false;
        }
        auto runtime = env->CallStaticObjectMethod(runtime_class, get_runtime_method);
        if (nullptr == runtime) {
            __android_log_print(ANDROID_LOG_INFO, TAG, "resolve runtime fail");
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
    return exempt_all_future.get() ? JNI_TRUE : JNI_FALSE;
}
