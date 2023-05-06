package com.zack.monitor.binder;

import androidx.annotation.Nullable;

import java.lang.reflect.Method;

public class MetaReflectHelper {
    @Nullable
    public static Method resolveMethodOfGetDeclaredMethod() {
        try {
            return Class.class.getDeclaredMethod("getDeclaredMethod", String.class, Class[].class);
        } catch (NoSuchMethodException e) {
            return null;
        }
    }
}
