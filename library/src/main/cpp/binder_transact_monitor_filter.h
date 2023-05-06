//
// Created by 王泽锋 on 2023/5/6.
//

#ifndef ANDROIDBINDERMONITOR_BINDER_TRANSACT_MONITOR_FILTER_H
#define ANDROIDBINDERMONITOR_BINDER_TRANSACT_MONITOR_FILTER_H

#include <unistd.h>
#include <ctime>
#include <stack>

#include "log.h"

#define FILTER_TAG "AndroidBinderMonitor.BinderTransactMonitorFilter"

constexpr int kFlagOneWay = 0x00000001;

constexpr long kBlockTimeThresholdSyncAsBlock = 0L;
constexpr long kSyncIPCDataSizeThreshold = 1L * 1024 * 1024;
constexpr long kAsyncIPCDataSizeThreshold = kSyncIPCDataSizeThreshold / 2;

class BinderTransactCallInfo {
public:
    BinderTransactCallInfo() : create_total_time_ms_(GetCurrentTimeMills()) {}

public:
    virtual int Flags() const = 0;
    virtual int DataSize() const = 0;

public:
    long TotalTimeSinceCreate() const {
        return GetCurrentTimeMills() - create_total_time_ms_;
    }

private:
    long GetCurrentTimeMills() const {
        timeval tv;
        gettimeofday(&tv, nullptr);
        return tv.tv_sec * 1000 + tv.tv_usec / 1000;
    }

private:
    long create_total_time_ms_;
};

template <class CallInfo>
class BinderTransactMonitorFilter {
public:
    typedef void(*OnTransactDataTooLarge)(const CallInfo &);
    typedef void(*OnTransactBlock)(const CallInfo &, long);

public:
    BinderTransactMonitorFilter(
        bool monitor_block_on_main_thread, long block_time_threshold_ms,
        bool monitor_data_too_large, float data_too_large_factor,
        OnTransactDataTooLarge on_transact_data_too_large = nullptr,
        OnTransactBlock on_transact_block = nullptr
    ) : config_{ monitor_block_on_main_thread, block_time_threshold_ms, monitor_data_too_large, data_too_large_factor },
        on_transact_data_too_large_(on_transact_data_too_large),
        on_transact_block_(on_transact_block) {}

public:
    void OnTransactStart(CallInfo call_info) {
        if (nullptr != on_transact_data_too_large_ && config_.monitor_data_too_large_) {
            long threshold = 0 != (call_info.Flags() & kFlagOneWay) // isAsyncIPC
                             ? kAsyncIPCDataSizeThreshold
                             : kSyncIPCDataSizeThreshold;
            if (call_info.DataSize() >= config_.data_too_large_factor_ * threshold) {
                on_transact_data_too_large_(call_info);
            }
        }
        transact_call_stack_.push(call_info);
    }
    void OnTransactEnd() {
        if (transact_call_stack_.empty()) {
            LOGE(FILTER_TAG, "OnTransactEnd, transact call stack is empty");
            return;
        }
        auto call_info = transact_call_stack_.top();
        transact_call_stack_.pop();
        if (nullptr != on_transact_block_ &&
            config_.monitor_block_on_main_thread_ && IsMainThread()) {
            auto cost_total_time_ms = call_info.TotalTimeSinceCreate();
            if (kBlockTimeThresholdSyncAsBlock == config_.block_time_threshold_ms_) {
                if (0 == (call_info.Flags() & kFlagOneWay)) { // isSyncIPC
                    on_transact_block_(call_info, cost_total_time_ms);
                    return;
                }
            } else if (cost_total_time_ms >= config_.block_time_threshold_ms_) {
                on_transact_block_(call_info, cost_total_time_ms);
                return;
            }
        }
    }

private:
    bool IsMainThread() {
        return getpid() == gettid();
    }

private:
    struct Config {
        bool monitor_block_on_main_thread_;
        long block_time_threshold_ms_;
        bool monitor_data_too_large_;
        float data_too_large_factor_;
    };

private:
    Config config_;
    OnTransactDataTooLarge on_transact_data_too_large_;
    OnTransactBlock on_transact_block_;

private:
    static thread_local std::stack<CallInfo> transact_call_stack_;
};

template <class CallInfo>
thread_local std::stack<CallInfo> BinderTransactMonitorFilter<CallInfo>::transact_call_stack_;

#endif //ANDROIDBINDERMONITOR_BINDER_TRANSACT_MONITOR_FILTER_H
