//
// Created by 王泽锋 on 2023/4/28.
//

#include "bp_binder_transact_hooker.h"

#include <cstring>
#include <cstdint>

#include "bytehook.h"
#include "xdl.h"

#include "log.h"
#include "utils.h"

#define TAG "AndroidBinderMonitor.BpBinderTransactHooker"

#if INTPTR_MAX == INT32_MAX
#define BINDER_LIB_PATH "/system/lib/libbinder.so"
#elif INTPTR_MAX == INT64_MAX
#define BINDER_LIB_PATH "/system/lib64/libbinder.so"
#else
#error "Invalid environment."
#endif

// NOT THREAD SAFE

namespace {

// https://cs.android.com/android/platform/superproject/+/refs/heads/master:system/core/libutils/include/utils/Errors.h
typedef int32_t status_t;

// https://cs.android.com/android/platform/superproject/+/refs/heads/master:system/core/libutils/include/utils/String16.h
class String16 {
public:
    inline const char16_t *string() const;
private:
    const char16_t* mString;
};

inline const char16_t* String16::string() const {
    return mString;
}

// https://cs.android.com/android/platform/superproject/+/refs/heads/master:frameworks/native/libs/binder/include/binder/Parcel.h
class Parcel {};

// virtual const String16& getInterfaceDescriptor() const = 0;
typedef const String16&(*IBinderGetInterfaceDescriptor)(const /* IBinder* */void *);
// size_t size() const;
typedef size_t(*String16Size)(const String16 *);

// size_t dataSize() const;
typedef size_t(*ParcelDataSize)(const Parcel *);

bool g_has_try_init = false;
bool g_init_success = false;

JavaVM *g_vm = nullptr;

IBinderGetInterfaceDescriptor g_i_binder_get_interface_descriptor = nullptr;
String16Size g_string16_size = nullptr;
ParcelDataSize g_parcel_data_size = nullptr;

jclass g_hooker_class = nullptr;
jmethodID g_on_transact_start_method = nullptr;
jmethodID g_on_transact_end_method = nullptr;

bytehook_stub_t g_hook_stub = nullptr;

// virtual status_t transact(uint32_t code, const Parcel& data, Parcel* reply, uint32_t flags = 0) = 0;
status_t HijackedTransact(void *, uint32_t, const Parcel &, Parcel *, uint32_t);

void HookedCallback(bytehook_stub_t, int, const char *, const char *, void *, void *, void *);

bool InitIfNeed(JavaVM *vm, JNIEnv *env) {
    if (g_has_try_init) {
        return g_init_success;
    }
    g_has_try_init = true;

    g_vm = vm;

    XDLHandle binder_handle("libbinder.so");
    if (nullptr == *binder_handle) {
        LOGE(TAG, "dlopen binder fail");
        return g_init_success;
    }
    g_i_binder_get_interface_descriptor = (IBinderGetInterfaceDescriptor) xdl_sym(*binder_handle,
                                                                                   "_ZNK7android8BpBinder22getInterfaceDescriptorEv",
                                                                                   nullptr);
    if (nullptr == g_i_binder_get_interface_descriptor) {
        LOGE(TAG, "dlsym BpBinder::getInterfaceDescriptor fail");
        return g_init_success;
    }
    XDLHandle utils_handle("libutils.so");
    g_string16_size = (String16Size) xdl_sym(*utils_handle, "_ZNK7android8String164sizeEv",
                                             nullptr);
    if (nullptr == g_string16_size) {
        LOGE(TAG, "dlsym String16::size fail");
        return g_init_success;
    }
    g_parcel_data_size = (ParcelDataSize) xdl_sym(*binder_handle, "_ZNK7android6Parcel8dataSizeEv",
                                                   nullptr);
    if (nullptr == g_parcel_data_size) {
        LOGE(TAG, "dlsym Parcel::dataSize fail");
        return g_init_success;
    }

    g_hooker_class = env->FindClass("com/zack/monitor/binder/BpBinderTransactHooker");
    g_hooker_class = (jclass) env->NewGlobalRef(g_hooker_class);
    g_on_transact_start_method = env->GetStaticMethodID(g_hooker_class, "onTransactStart",
                                                        "(Ljava/lang/String;III)V");
    g_on_transact_end_method = env->GetStaticMethodID(g_hooker_class, "onTransactEnd", "()V");

    g_init_success = true;
    return g_init_success;
}

status_t HijackedTransact(
        void *thiz, // IBinder*
        uint32_t code,
        const Parcel &data,
        Parcel *reply,
        uint32_t flags
) {
    BYTEHOOK_STACK_SCOPE();
    JNIEnv *env = nullptr;
    if (JNI_OK != g_vm->AttachCurrentThread(&env, nullptr)) {
        LOGE(TAG, "HijackedTransact, jni env attach current thread fail");
    }
    if (nullptr != env) {
        const String16 *descriptor = &g_i_binder_get_interface_descriptor(thiz);
        auto descriptor_content = descriptor->string();
        auto descriptor_len = g_string16_size(descriptor);
        auto j_descriptor = env->NewString((const jchar *) descriptor_content, (jsize) descriptor_len);
        auto data_size = g_parcel_data_size(&data);
        env->CallStaticVoidMethod(g_hooker_class, g_on_transact_start_method, j_descriptor, code, data_size, flags);
    }
    auto transact_ret = BYTEHOOK_CALL_PREV(HijackedTransact, thiz, code, data, reply, flags);
    if (nullptr != env) {
        env->CallStaticVoidMethod(g_hooker_class, g_on_transact_end_method);
    }
    return transact_ret;
}

void HookedCallback(
        bytehook_stub_t, int status_code,
        const char *caller_path_name, const char *sym_name,
        void *, void *,
        void *
) {
    LOGI(TAG, "HookedCallback, status_code: %d, caller_path_name: %s, sym_name: %s", status_code, caller_path_name, sym_name);
}

} // namespace anon

bool BpBinderTransactHooker::Hook(JavaVM *vm, JNIEnv *env) {
    if (!InitIfNeed(vm, env)) {
        return false;
    }
    // virtual function, caller is himself
    g_hook_stub = bytehook_hook_single(
            BINDER_LIB_PATH,
            BINDER_LIB_PATH, "_ZN7android8BpBinder8transactEjRKNS_6ParcelEPS1_j",
            (void *)HijackedTransact,
            HookedCallback, nullptr
    );
    if (nullptr == g_hook_stub) {
        LOGE(TAG, "hook BpBinder::transact fail");
        return false;
    }
    return true;
}

bool BpBinderTransactHooker::Unhook() {
    if (!g_init_success || nullptr == g_hook_stub) {
        return true;
    }
    auto ret = bytehook_unhook(g_hook_stub);
    if (0 != ret) {
        LOGE(TAG, "unhook BpBinder::transact fail, ret: %d", ret);
        return false;
    }
    g_hook_stub = nullptr;
    return true;
}
