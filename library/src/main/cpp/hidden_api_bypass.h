//
// Created by 王泽锋 on 2023/4/27.
//

#ifndef ANDROIDBINDERMONITOR_HIDDEN_API_BYPASS_H
#define ANDROIDBINDERMONITOR_HIDDEN_API_BYPASS_H

#include <jni.h>

namespace HiddenApiBypass {

bool exemptAll(JavaVM *vm);

} // namespace HiddenApiBypass

#endif //ANDROIDBINDERMONITOR_HIDDEN_API_BYPASS_H
