package com.fleetmanagement.utils;

public class ThemeManager {
    private static boolean isDarkMode = false;

    public static boolean isDarkMode() {
        return isDarkMode;
    }

    public static void setDarkMode(boolean dark) {
        isDarkMode = dark;
    }
}