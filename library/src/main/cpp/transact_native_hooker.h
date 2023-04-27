//
// Created by 王泽锋 on 2023/4/27.
//

#ifndef ANDROIDBINDERMONITOR_TRANSACT_NATIVE_HOOKER_H
#define ANDROIDBINDERMONITOR_TRANSACT_NATIVE_HOOKER_H

#include <jni.h>

namespace TransactNativeHooker {

bool hook(JNIEnv *env);
bool unhook(JNIEnv *env);

} // namespace TransactNativeHooker

#endif //ANDROIDBINDERMONITOR_TRANSACT_NATIVE_HOOKER_H
