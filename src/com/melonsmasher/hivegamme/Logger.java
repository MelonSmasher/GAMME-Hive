package com.melonsmasher.hivegamme;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Created by melon on 3/30/16.
 */
class Logger {

    private boolean canLogToFile = true;

    /**
     * Simple logger class
     */
    Logger() {
    }

    /**
     * Creates a formatted log tag.
     *
     * @param name    The machine's name.
     * @param context The logging context, often 'SYS' or other phase of process.
     * @param kind    Kind of message, usually 'ERROR', 'WARN', or 'INFO'.
     * @return A formatted log entry tag, that should prefix a log message.
     */
    String logTag(String name, String context, String kind) {
        String timeStamp = new SimpleDateFormat("MM/dd/yy hh:mm:ss:a").format(new Date());
        return "[" + timeStamp + "]" + "[" + name + "]" + "[" + context + "]" + "[" + kind + "] >> ";
    }

    /**
     * Logs an unformatted log message to the console, it formats the message using the logTag() method.
     *
     * @param name    The machine's name.
     * @param context The logging context, often 'SYS' or other phase of process.
     * @param kind    Kind of message, usually 'ERROR', 'WARN', or 'INFO'.
     * @param message The log entry message.
     */
    void logUntaggedToStdOut(String name, String context, String kind, String message) {
        logToStdOut(logTag(name, context, kind) + message);
    }

    /**
     * Logs a pre-formatted message to the console.
     *
     * @param message The log entry message.
     */
    void logToStdOut(String message) {
        System.out.println(message);
    }

    boolean getCanLogToFile() {
        return this.canLogToFile;
    }

    void setCanLogToFile(boolean canLogToFile) {
        this.canLogToFile = canLogToFile;
    }

    void logToFile(String logFilePath, String line) {
        if (canLogToFile) {
            if (line != null) line = line + "\n";

            try {
                Files.createFile(Paths.get(logFilePath));
            } catch (Exception e) {

            }
            try {
                Files.write(Paths.get(logFilePath), line.getBytes(), StandardOpenOption.APPEND);
            } catch (Exception e) {
                e.printStackTrace();
                setCanLogToFile(false);
            }
        }
    }

}
