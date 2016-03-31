package com.melonsmasher.hivegamme.client;

import com.esotericsoftware.kryonet.Client;
import com.melonsmasher.hivegamme.Packets;
import com.melonsmasher.hivegamme.Util;
import org.json.JSONObject;

import java.io.*;
import java.nio.charset.Charset;

/**
 * Created by melon on 3/26/16.
 */
public class Drone {

    private int mPortTCP = 25801, mPortUDP = 25802, mTimeOut = 5000;
    private String mServerHost, mJobDirPath, mJobName;
    private Client mClient = null;
    private boolean busy = false, started = true, waitingForWorkResponse = false, mRemoteLoggingEnabled = false;
    private String mName;
    private JSONObject config = null;
    private ClientLogger mLogger;

    /**
     * The Drone Client class.
     */
    private Drone() {

        // Set our drone name
        setName(Util.getName());
        // Make sure that the log dir exists.
        Util.mkdir(Util.defaultLogDir());
        // Initialize our custom logger
        mLogger = new ClientLogger(this);
        mLogger.logInfo("SYS", "Loading configuration...");
        // Load the configuration
        loadConfig(true);
        // Create job dir
        if (!Util.mkdir(mJobDirPath)) {
            mLogger.logErr("SYS", "Could not ensure that \"" + mJobDirPath + "\" exists! Error Code: 4");
            started = false;
            System.exit(4);
        }
        // Start thread that reloads the configuration every minute
        new Thread() {
            public void run() {
                while (true) {
                    try {
                        Thread.sleep(60000);
                        loadConfig(false);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    if (!started) {
                        break;
                    }
                }
            }
        }.start();

        mLogger.logInfo("SYS", "Initializing client instance...");
        mClient = new Client(81920, 20480);
        ClientNetworkListener mListener = new ClientNetworkListener(this);
        mClient.addListener(mListener);
        Util.registerPackets(mClient.getKryo());

        try {
            mClient.start();
        } catch (Exception e) {
            e.printStackTrace();
            mLogger.logErr("SYS", "Failed to start client! Exit Code: 5");
            started = false;
            System.exit(5);
        }

        try {
            mClient.connect(mTimeOut, mServerHost, mPortTCP, mPortUDP);
        } catch (IOException e) {
            e.printStackTrace();
            mLogger.logErr("SYS", "Failed to connect! Exit Code: 6");
            started = false;
            System.exit(6);
        }

        if (mClient.isConnected()) {
            mLogger.logInfo("SYS", "Link established with the hive! TCP: " + mServerHost + ":" + mPortTCP + " - UDP: " + mServerHost + ":" + mPortUDP);
            // Send a join authorization request, not needed at the moment, but will be useful when hive is password protected.
            Packets.Packet00JoinRequest request = new Packets.Packet00JoinRequest();
            request.name = mName;
            mClient.sendTCP(request);
        } else {
            mLogger.logErr("SYS", "Failed to connect to the hive! TCP: " + mServerHost + ":" + mPortTCP + " - UDP: " + mServerHost + ":" + mPortUDP + " - Error Code: 6");
            started = false;
            System.exit(6);
        }

        // Start requesting work
        requestWork();

        // Simple while loop to keep main thread open
        while (true) {
            if (!started) {
                break;
            }
        }
    }

    /**
     * This function spawns a background thread that determines if the Drone is actively working or just idle.
     * If the Drone is working this thread sleeps for 30 seconds.
     * If the drone is not working it sends a work request packet and then cools down for 5 seconds to avoid flooding the queen.
     */
    private void requestWork() {
        // Work manager thread
        new Thread() {
            public void run() {
                while (true) {
                    if (isBusy()) {
                        Packets.Packet04Message msg = new Packets.Packet04Message();
                        msg.name = mName;
                        msg.text = "Working... Buzz Buzz";
                        mClient.sendUDP(msg);
                        try {
                            Thread.sleep(30000);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    } else {
                        if (!isWaitingForWorkResponse() && !isBusy()) {
                            setWaitingForWorkResponse(true);
                            Packets.Packet06PayloadRequest packet = new Packets.Packet06PayloadRequest();
                            packet.name = mName;
                            mLogger.logInfo("SYS", "Requesting work.");
                            mClient.sendTCP(packet);
                        }
                        try {
                            Thread.sleep(5000);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                    if (!started) {
                        break;
                    }
                }
            }
        }.start();
    }

    /**
     * This method spawns a worker thread, and ensures that the job environment is up to snuff.
     * It then runs the GAMME tool with parameters obtained from the payload packet.
     *
     * @param packet This packet contains all of the information needed to begin working.
     */
    void beginWork(Packets.Packet07PayloadResponse packet) {
        setBusy(true);
        setWaitingForWorkResponse(false);
        setmJobName(packet.job_name);
        mLogger.logInfo(mJobName, "Obtained workload from the Queen!");
        new Thread() {
            public void run() {

                mLogger.logInfo(mJobName, "Work thread started!");

                // Work happens here
                String jobWorkPath = mJobDirPath + "\\" + mJobName;
                String emailListFile = jobWorkPath + "\\" + mJobName + "-addresses.list";
                String privateKeyFile = jobWorkPath + "\\" + mJobName + "-key.json";

                // Ensure that job paths exists.
                if (!Util.mkdir(mJobDirPath)) {
                    mLogger.logWarn("SYS", "Could not ensure that this \"" + mJobDirPath + "\" exists!");
                }

                // Ensure that job work paths exists.
                if (!Util.mkdir(jobWorkPath)) {
                    mLogger.logWarn("SYS", "Could not ensure that this \"" + jobWorkPath + "\" exists!");
                }

                // Write emails to file with imap password
                try {
                    mLogger.logInfo(mJobName, "Writing emails to file: " + emailListFile);
                    String[] email_addresses = packet.payload.split("\\r?\\n");
                    PrintWriter address_writer = new PrintWriter(emailListFile, "UTF-8");
                    for (String address : email_addresses) {
                        address_writer.println(address + "#" + packet.imap_password);
                    }
                    address_writer.close();
                } catch (Exception e) {
                    e.printStackTrace();
                    mLogger.logErr(mJobName, "Failed to write emails to file: " + emailListFile);
                }

                // Write google admin token to file
                try (Writer writer = new BufferedWriter(new OutputStreamWriter(
                        new FileOutputStream(privateKeyFile), "utf-8"))) {
                    mLogger.logInfo(mJobName, "Writing google admin token to file: " + privateKeyFile);
                    writer.write(packet.gmail_key);
                } catch (Exception e) {
                    e.printStackTrace();
                    mLogger.logErr(mJobName, "Failed to write google admin token to file: " + privateKeyFile);
                }

                StringBuilder gammeOptions = new StringBuilder();

                int imap_security = (packet.imap_security) ? 1 : 0;

                // Build gamme options
                gammeOptions.append("--google_apps_admin=\"").append(packet.google_apps_admin).append("\" ")
                        .append("--enable_imap ")
                        .append("--nouse_gui ")
                        .append("--filename=\"").append(emailListFile).append("\" ")
                        .append("--source_server=\"").append(packet.server).append("\" ")
                        .append("--google_domain=\"").append(packet.google_domain).append("\" ")
                        .append("--service_account_json_path=\"").append(privateKeyFile).append("\" ")
                        .append("--retry_count=").append(packet.retry_count).append(" ")
                        .append("--num_threads=").append(packet.threads).append(" ")
                        .append("--imap_security=").append(imap_security).append(" ")
                        .append("--imap_port=").append(packet.imap_port).append(" ")
                        .append("--imap_server_type=\"").append(packet.imap_server_type).append("\" ");

                // If we are not excluding any folders do not add the option
                if (!packet.exclude_top_level_folders.isEmpty()) {
                    gammeOptions.append("--exclude_top_level_folders=\"").append(packet.exclude_top_level_folders.replaceAll("\"", "")).append("\" ");
                }

                // Work happens here
                execGamme(gammeOptions.toString());

                mLogger.logInfo(mJobName, "Job is complete!");
                // Notify the queen that the job is done
                Packets.Packet12JobComplete completionPacket = new Packets.Packet12JobComplete();
                completionPacket.name = mName;
                completionPacket.job_name = mJobName;
                completionPacket.server = packet.server;
                mClient.sendUDP(completionPacket);
                mLogger.logInfo(mJobName, "Cooling down for 10 seconds... this is your chance to exit the hive.");
                try {
                    Thread.sleep(5000);
                } catch (Exception e) {
                    e.printStackTrace();
                }
                setmJobName(null);
                // Ensure that other threads realize that we are idle
                setBusy(false);
            }
        }.start();
    }

    /**
     * This method wraps around the GAMME tool and is called in the worker thread.
     *
     * @param options GAMME command arguments
     */
    private void execGamme(String options) {
        Runtime rt = Runtime.getRuntime();
        String command = config.getString("gamme_location") + " " + options.trim();
        mLogger.logInfo(mJobName, command);

        try {
            Process proc = rt.exec(command);

            BufferedReader stdInput = new BufferedReader(new InputStreamReader(proc.getInputStream()));

            BufferedReader stdError = new BufferedReader(new InputStreamReader(proc.getErrorStream()));

            // read the output from the command
            String s = null;
            while ((s = stdInput.readLine()) != null) {
                if (!s.isEmpty()) {
                    if (s.startsWith("Failure:") || s.startsWith("Error:")) {
                        mLogger.logErr(mJobName + "-GAMME", s);
                        mLogger.logToFile(mJobDirPath + "\\" + mJobName + "\\error-" + mJobName + ".log", s);
                        mLogger.logToFile(mJobDirPath + "\\" + mJobName + "\\" + mJobName + ".log", s);
                    } else {
                        mLogger.logInfo(mJobName + "-GAMME", s);
                        mLogger.logToFile(mJobDirPath + "\\" + mJobName + "\\info-" + mJobName + ".log", s);
                        mLogger.logToFile(mJobDirPath + "\\" + mJobName + "\\" + mJobName + ".log", s);

                    }
                    if (s.equals("Press 'Enter' to exit...")) {
                        proc.destroy();
                    }
                    if (!started) {
                        break;
                    }
                }
            }

            // read any errors from the attempted command
            while ((s = stdError.readLine()) != null) {
                if (!s.isEmpty()) {
                    mLogger.logErr(mJobName + "-GAMME", s);
                    mLogger.logToFile(mJobDirPath + "\\" + mJobName + "\\error-" + mJobName + ".log", s);
                    mLogger.logToFile(mJobDirPath + "\\" + mJobName + "\\" + mJobName + ".log", s);
                }
                if (!started) {
                    break;
                }
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * This method loads the configuration file into memory
     *
     * @param strict If we are strict the Drone will halt if it cannot load it's configuration. This is needed when it is first run.
     *               Otherwise it can remain off, as the old configuration will be used.
     */
    private void loadConfig(boolean strict) {
        String configFilePath = Util.defaultConfDir() + "conf.json";
        JSONObject old_config = config;
        boolean shouldReload = true;
        try {
            // Load config from file and store in variable
            String configStr = Util.readFile(configFilePath, Charset.defaultCharset());
            config = new JSONObject(configStr);
            config = config.getJSONObject("drone");
        } catch (IOException e) {
            e.printStackTrace();
            if (strict) {
                shouldReload = false;
                mLogger.logErr("SYS", "Could not find configuration file: " + configFilePath + " Error Code: 3");
                started = false;
                System.exit(3);
            } else {
                shouldReload = false;
                mLogger.logWarn("SYS", "Could not find configuration file: " + configFilePath + " Using last known configuration...");
            }
        }
        // Load config into global vars
        if (shouldReload) {
            mPortTCP = config.getInt("tcp_port");
            mPortUDP = config.getInt("udp_port");
            mTimeOut = config.getInt("timeout");
            mServerHost = config.getString("queen_address");
            mJobDirPath = config.getString("job_dir");
            try {
                mRemoteLoggingEnabled = config.getBoolean("remote_logging");
            } catch (Exception e) {
                mRemoteLoggingEnabled = false;
                mLogger.logWarn("SYS", "Option: \"remote_logging\" not defined in config file. Assuming false.");
            }
        } else {
            config = old_config;
        }
        if (config == null) {
            mLogger.logErr("SYS", "Could load/reload the configuration. - Error Code: 7");
            System.exit(7);
        }
    }

    String getmJobName() {
        return mJobName;
    }

    private void setmJobName(String mJobName) {
        this.mJobName = mJobName;
    }

    private void setName(String name) {
        this.mName = name;
    }

    String getName() {
        return this.mName;
    }

    Client getClient() {
        return this.mClient;
    }

    ClientLogger getLogger() {
        return mLogger;
    }

    void setStarted(boolean started) {
        this.started = started;
    }

    void setBusy(boolean status) {
        this.busy = status;
    }

    void setWaitingForWorkResponse(boolean status) {
        this.waitingForWorkResponse = status;
    }

    private boolean isBusy() {
        return this.busy;
    }

    private boolean isWaitingForWorkResponse() {
        return this.waitingForWorkResponse;
    }

    boolean isRemoteLoggingEnabled() {
        return this.mRemoteLoggingEnabled;
    }

    public static void main(String[] args) {
        new Drone();
    }
}
