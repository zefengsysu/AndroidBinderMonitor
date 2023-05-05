//
// Created by 王泽锋 on 2023/4/27.
//

#ifndef ANDROIDBINDERMONITOR_BINDER_PROXY_TRANSACT_NATIVE_HOOKER_H
#define ANDROIDBINDERMONITOR_BINDER_PROXY_TRANSACT_NATIVE_HOOKER_H

#include <jni.h>

namespace BinderProxyTransactNativeHooker {

    bool Hook(JNIEnv *env);

    bool Unhook(JNIEnv *env);

} // namespace TransactNativeHooker

#endif //ANDROIDBINDERMONITOR_BINDER_PROXY_TRANSACT_NATIVE_HOOKER_H
