//
// Created by 王泽锋 on 2023/5/1.
//

#ifndef ANDROIDBINDERMONITOR_UTILS_H
#define ANDROIDBINDERMONITOR_UTILS_H

#include "xdl.h"

class XDLHandle {
public:
    XDLHandle(const char *filename) : lib_handle_(xdl_open(filename, XDL_DEFAULT)) {}

    ~XDLHandle() {
        if (nullptr != lib_handle_) {
            xdl_close(lib_handle_);
        }
    }

public:
    void *operator*() {
        return lib_handle_;
    }

private:
    void *lib_handle_;
};

#endif //ANDROIDBINDERMONITOR_UTILS_H
