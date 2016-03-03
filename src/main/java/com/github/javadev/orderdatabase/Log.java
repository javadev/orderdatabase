package com.github.javadev.orderdatabase;

import java.io.IOException;
import java.util.logging.*;

public final class Log  {
    private enum MessageType { DEBUG, INFO, WARN, ERROR };

    private static class LogFormatter extends Formatter {
        public String format(LogRecord record) {
            StringBuilder builder = new StringBuilder(1000);
            builder.append(new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS").format(
                new java.util.Date(record.getMillis()))).append(" ");
            builder.append(String.format("%-5s", record.getLevel())).append(" ");
            builder.append(record.getSourceClassName()).append(" - ");
            
            builder.append(formatMessage(record));
            builder.append("\n");
            return builder.toString();
        }
    }
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
        Logger logger = Logger.getLogger(aClassName);
        if (logger.getHandlers().length == 0) {
            logger.setUseParentHandlers(false);
            ConsoleHandler handler = new ConsoleHandler();
            handler.setFormatter(new LogFormatter());
            logger.addHandler(handler);
            try {
                FileHandler fileHandler = new FileHandler("orderdatabase.log", 100000, 10, true);
                fileHandler.setFormatter(new LogFormatter());
                logger.addHandler(fileHandler);
            } catch (IOException | SecurityException ex) {
            }
        }
        switch (messageType) {
        case DEBUG:
            logger.logp(Level.CONFIG, aClassName, "", localParam);
            break;
        case INFO:
            logger.logp(Level.INFO, aClassName, "", localParam);
            break;
        case WARN:
            logger.logp(Level.WARNING, aClassName, "", localParam);
            break;
        default:
            logger.logp(Level.SEVERE, aClassName, "",  localParam);
            break;
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

    private static void makeGoodTrace(StringBuilder sb, StackTraceElement[] trace) {
        for (StackTraceElement entry : trace) {
            if (entry.getClassName().startsWith("com.github")) {
                sb.append("\t-->");
            } else {
                sb.append('\t');
            }
            sb.append(entry).append('\n');
        }
    }
}
