//
// Created by 王泽锋 on 2023/4/28.
//

#ifndef ANDROIDBINDERMONITOR_BP_BINDER_TRANSACT_HOOKER_H
#define ANDROIDBINDERMONITOR_BP_BINDER_TRANSACT_HOOKER_H

#include <jni.h>

namespace BpBinderTransactHooker {

    bool Hook(JavaVM *vm, JNIEnv *env);

    bool Unhook();

} // namespace TransactHooker

#endif //ANDROIDBINDERMONITOR_BP_BINDER_TRANSACT_HOOKER_H
