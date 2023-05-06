//
// Created by 王泽锋 on 2023/4/28.
//

#ifndef ANDROIDBINDERMONITOR_BP_BINDER_TRANSACT_HOOKER_H
#define ANDROIDBINDERMONITOR_BP_BINDER_TRANSACT_HOOKER_H

#include <jni.h>

namespace BpBinderTransactHooker {

    bool Hook(
        JavaVM *vm, JNIEnv *env,
        bool monitor_block_on_main_thread, long block_time_threshold_ms,
        bool monitor_data_too_large, float data_too_large_factor
    );

    bool Unhook();

} // namespace TransactHooker

#endif //ANDROIDBINDERMONITOR_BP_BINDER_TRANSACT_HOOKER_H
