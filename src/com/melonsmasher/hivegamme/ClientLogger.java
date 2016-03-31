package com.melonsmasher.hivegamme;

/**
 * Created by melon on 3/30/16.
 */
public class ClientLogger extends Logger {

    private Drone mDrone;

    /**
     * ClientLogger provides a way to log different kinds of log messages to the console in a standard format.
     * It will also send them to the Queen server if remote logging is enabled on both the Drone and the Queen.
     *
     * @param drone The Drone object that the ClientLogger was called from, passing in 'this' should do the trick.
     */
    ClientLogger(Drone drone) {
        this.mDrone = drone;
    }

    /**
     * Logs a formatted error message to the console.
     *
     * @param context The logging context, often 'SYS' or other phase of process.
     * @param message The log entry message.
     */
    void logErr(String context, String message) {
        String tag = logTag(mDrone.getName(), context, "ERROR");
        message = tag + message;
        logToStdOut(message);
        sendError(message);
        if (getCanLogToFile()) logToFile(Util.defaultLogDir() + mDrone.getName() + ".log", message);
    }

    /**
     * Logs a formatted warning message to the console.
     *
     * @param context The logging context, often 'SYS' or other phase of process.
     * @param message The log entry message.
     */
    void logWarn(String context, String message) {
        String tag = logTag(mDrone.getName(), context, "WARN");
        message = tag + message;
        logToStdOut(message);
        sendWarning(message);
        if (getCanLogToFile()) logToFile(Util.defaultLogDir() + mDrone.getName() + ".log", message);
    }

    /**
     * Logs a formatted info message to the console.
     *
     * @param context The logging context, often 'SYS' or other phase of process.
     * @param message The log entry message.
     */
    void logInfo(String context, String message) {
        String tag = logTag(mDrone.getName(), context, "INFO");
        message = tag + message;
        logToStdOut(message);
        sendInfo(message);
        if (getCanLogToFile()) logToFile(Util.defaultLogDir() + mDrone.getName() + ".log", message);
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
        logErr(context, message);
        return message;
    }

    /**
     * Sends an error log packet to the Queen server if remote logging is enabled.
     *
     * @param message The pre-formatted log entry.
     */
    private void sendError(String message) {
        if (mDrone.isRemoteLoggingEnabled() && mDrone.getClient() != null && mDrone.getClient().isConnected()) {
            Packets.Packet14LogError packet = new Packets.Packet14LogError();
            packet.name = mDrone.getName();
            packet.text = message;
            packet.job_name = mDrone.getmJobName();
            mDrone.getClient().sendUDP(packet);
        }
    }

    /**
     * Sends a warning log packet to the Queen server if remote logging is enabled.
     *
     * @param message The pre-formatted log entry.
     */
    private void sendWarning(String message) {
        if (mDrone.isRemoteLoggingEnabled() && mDrone.getClient() != null && mDrone.getClient().isConnected()) {
            Packets.Packet15LogWarning packet = new Packets.Packet15LogWarning();
            packet.name = mDrone.getName();
            packet.text = message;
            packet.job_name = mDrone.getmJobName();
            mDrone.getClient().sendUDP(packet);
        }
    }

    /**
     * Sends an info log packet to the Queen server if remote logging is enabled.
     *
     * @param message The pre-formatted log entry.
     */
    private void sendInfo(String message) {
        if (mDrone.isRemoteLoggingEnabled() && mDrone.getClient() != null && mDrone.getClient().isConnected()) {
            Packets.Packet13LogInfo packet = new Packets.Packet13LogInfo();
            packet.name = mDrone.getName();
            packet.text = message;
            packet.job_name = mDrone.getmJobName();
            mDrone.getClient().sendUDP(packet);
        }
    }
}
