//
// Created by 王泽锋 on 2023/4/27.
//

#ifndef ANDROIDBINDERMONITOR_LOG_H
#define ANDROIDBINDERMONITOR_LOG_H

#include <android/log.h>

#define LOGD(TAG, ...) __android_log_print(ANDROID_LOG_DEBUG, TAG, __VA_ARGS__)
#define LOGI(TAG, ...) __android_log_print(ANDROID_LOG_INFO, TAG, __VA_ARGS__)
#define LOGE(TAG, ...) __android_log_print(ANDROID_LOG_ERROR, TAG, __VA_ARGS__)

#endif //ANDROIDBINDERMONITOR_LOG_H
