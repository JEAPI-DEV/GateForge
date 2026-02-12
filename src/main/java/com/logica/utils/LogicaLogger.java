package com.logica.utils;

import com.hypixel.hytale.logger.HytaleLogger;

public class LogicaLogger {
    private static final HytaleLogger LOGGER = HytaleLogger.getLogger();
    private static boolean DEBUG = false;

    public static void setDebug(boolean debug) {
        DEBUG = debug;
        if (DEBUG) {
            info("Debug logging enabled");
        } else {
            info("Debug logging disabled");
        }
    }

    public static boolean isDebug() {
        return DEBUG;
    }

    public static void info(String message) {
        LOGGER.atInfo().log(message);
    }

    public static void info(String message, Object... args) {
        try {
            LOGGER.atInfo().log(String.format(message, args));
        } catch (Exception e) {
            LOGGER.atInfo().log(message + " [LogicaLogger Formatting Error: " + e.getMessage() + "]");
        }
    }

    public static void warn(String message) {
        LOGGER.atWarning().log(message);
    }

    public static void warn(String message, Object... args) {
        try {
            LOGGER.atWarning().log(String.format(message, args));
        } catch (Exception e) {
            LOGGER.atWarning().log(message + " [LogicaLogger Formatting Error: " + e.getMessage() + "]");
        }
    }

    public static void error(String message) {
        LOGGER.atSevere().log(message);
    }

    public static void error(String message, Object... args) {
        try {
            LOGGER.atSevere().log(String.format(message, args));
        } catch (Exception e) {
            LOGGER.atSevere().log(message + " [LogicaLogger Formatting Error: " + e.getMessage() + "]");
        }
    }

    public static void debug(String message) {
        if (DEBUG) {
            LOGGER.atInfo().log("[DEBUG] " + message);
        }
    }

    public static void debug(String message, Object... args) {
        if (DEBUG) {
            try {
                LOGGER.atInfo().log("[DEBUG] " + String.format(message, args));
            } catch (Exception e) {
                LOGGER.atInfo().log("[DEBUG] " + message + " [LogicaLogger Formatting Error: " + e.getMessage() + "]");
            }
        }
    }
}
