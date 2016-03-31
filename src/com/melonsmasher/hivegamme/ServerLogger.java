package com.melonsmasher.hivegamme;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.PrintWriter;

/**
 * Created by melon on 3/30/16.
 */

public class ServerLogger extends Logger {

    private Queen mQueen;

    /**
     * ServerLogger provides a way to log different kinds of log messages to the console in a standard format.
     *
     * @param queen The Queen object that the ServerLogger was called from, passing in 'this' should do the trick.
     */
    public ServerLogger(Queen queen) {
        this.mQueen = queen;
    }

    /**
     * Logs a formatted error message to the console.
     *
     * @param context The logging context, often 'SYS' or other phase of process.
     * @param message The log entry message.
     */
    void logErr(String context, String message) {
        String tag = logTag(mQueen.getName(), context, "ERROR");
        message = tag + message;
        logToStdOut(message);
        if (getCanLogToFile()) logToFile(Util.defaultLogDir() + mQueen.getName() + ".log", message);
    }

    /**
     * Logs a formatted warning message to the console.
     *
     * @param context The logging context, often 'SYS' or other phase of process.
     * @param message The log entry message.
     */
    void logWarn(String context, String message) {
        String tag = logTag(mQueen.getName(), context, "WARN");
        message = tag + message;
        logToStdOut(message);
        if (getCanLogToFile()) logToFile(Util.defaultLogDir() + mQueen.getName() + ".log", message);
    }

    /**
     * Logs a formatted info message to the console.
     *
     * @param context The logging context, often 'SYS' or other phase of process.
     * @param message The log entry message.
     */
    void logInfo(String context, String message) {
        String tag = logTag(mQueen.getName(), context, "INFO");
        message = tag + message;
        logToStdOut(message);
        if (getCanLogToFile()) logToFile(Util.defaultLogDir() + mQueen.getName() + ".log", message);
    }

    /**
     * Logs a formatted error message to the console and returns the formatted message as a String.
     *
     * @param context The logging context, often 'SYS' or other phase of process.
     * @param message The log entry message.
     * @return Formatted log entry.
     */
    String logErrForResult(String context, String message) {
        logErr(context, message);
        return message;
    }

    /**
     * Logs a formatted warning message to the console and returns the formatted message as a String.
     *
     * @param context The logging context, often 'SYS' or other phase of process.
     * @param message The log entry message.
     * @return Formatted log entry.
     */
    String logWarnForResult(String context, String message) {
        logWarn(context, message);
        return message;
    }

    /**
     * Logs a formatted info message to the console and returns the formatted message as a String.
     *
     * @param context The logging context, often 'SYS' or other phase of process.
     * @param message The log entry message.
     * @return Formatted log entry.
     */
    String logInfoForResult(String context, String message) {
        logInfo(context, message);
        return message;
    }

    @Override
    void logToStdOut(String message) {
        super.logToStdOut(message);
        if (getCanLogToFile()) logToFile(Util.defaultLogDir() + mQueen.getName() + ".log", message);
    }

    /**
     * Logs a pre-formatted log entry to the console and returns the log entry string.
     *
     * @param message The log entry message.
     * @return The log entry message.
     */
    String logToStdOutForResult(String message) {
        logToStdOut(message);
        return message;
    }
}
