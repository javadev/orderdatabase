package com.github.javadev.orderdatabase;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Log {
    private enum MessageType { DEBUG, INFO, WARN, ERROR };

    public static void debug(String param) {
        log(MessageType.DEBUG, param);
    }

    public static void error(String param) {
        log(MessageType.ERROR, param);
    }

    public static void error(Throwable aProblem, String param) {
        log(MessageType.ERROR, problem2String(param, aProblem));
     }

    public static void info(String param) {
        log(MessageType.INFO, param);
    }

    public static void warn(String param) {
        log(MessageType.WARN, param);
    }

    public static void warn(Throwable aProblem, String param) {
        warn(problem2String(param, aProblem));
    }

    private static void log(MessageType messageType, String localParam) {
        String aClassName = whoCalledMe();
        Logger log = LoggerFactory.getLogger(aClassName);
        String param = modifyString(localParam);
        if (MessageType.DEBUG.equals(messageType)) {
            log.debug(param);
        } else if (MessageType.INFO.equals(messageType)) {
            log.info(param);
        } else if (MessageType.WARN.equals(messageType)) {
            log.warn(param);
        } else {
            log.error(param);
        }
    }

    private static String whoCalledMe() {
        StackTraceElement[] stackTraceElements = Thread.currentThread().getStackTrace();
        StackTraceElement caller = stackTraceElements[4];
        String classname = caller.getClassName();
        return classname;
    }

    private static String problem2String(String aMsg, Throwable aProblem) {
        StringBuilder sb = new StringBuilder();
        if (aMsg != null) {
            sb.append(aMsg).append('\n');
        }
        sb.append("Error is: ").append(aProblem.getClass().getName()).
                append(" Message: ").append(aProblem.getMessage()).append('\n');
        makeGoodTrace(sb, aProblem.getStackTrace());
        Throwable cause = aProblem.getCause();
        while (cause != null) {
            sb.append("The cause is ").append(cause.getClass().getName()).
                    append(" Message: ").append(aProblem.getMessage()).append('\n');
            makeGoodTrace(sb, cause.getStackTrace());
            cause = cause.getCause();
        }
        return sb.toString();
    }

    public static String modifyString(String param) {
         if ("".equals(param)) {
             return "";
         }
         return param;
     }

    private static void makeGoodTrace(StringBuilder sb, StackTraceElement[] trace) {
        for (StackTraceElement entry : trace) {
            if (entry.getClassName().startsWith("com.github.javadev")) {
                sb.append("\t-->");
            } else {
                sb.append('\t');
            }
            sb.append(entry).append('\n');
        }
    }
}
