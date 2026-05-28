package com.sems.apigateway.shared.utils;

public final class PathUtils {

    private PathUtils() {
    }

    public static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
