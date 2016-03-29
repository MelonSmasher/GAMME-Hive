package com.melonsmasher.hivegamme;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryonet.Client;
import org.json.JSONObject;

import java.io.*;
import java.nio.charset.Charset;
import java.net.InetAddress;
import java.net.UnknownHostException;

/**
 * Created by melon on 3/26/16.
 */
public class Drone {

    private int mPortTCP = 25801, mPortUDP = 25802, mTimeOut = 5000;
    private String mServerHost = "localhost", mJobDirPath;
    private Client mClient;
    private boolean busy = false, started = true, waitingForWorkResponse = false;
    private String mName = "Drone";
    private JSONObject config;

    private Drone() {

        System.out.println("[DRONE][INFO] >> Loading configuration.");
        // Load the configuration
        loadConfig(true);
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
                }
            }
        }.start();

        mName = setName();

        System.out.println("[DRONE][INFO] >> Initializing client instance...");
        mClient = new Client(81920, 20480);
        ClientNetworkListener mListener = new ClientNetworkListener(this);
        mClient.addListener(mListener);
        Util.registerPackets(mClient.getKryo());

        try {
            mClient.start();
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("[DRONE][ERROR] >> Failed to start client!");
            System.exit(5);
        }

        try {
            mClient.connect(mTimeOut, mServerHost, mPortTCP, mPortUDP);
        } catch (IOException e) {
            System.out.println("[DRONE][ERROR] >> Failed to connect!");
            e.printStackTrace();
            System.exit(6);
        }

        if (mClient.isConnected()) {
            System.out.println("[DRONE][INFO] >> Telepathic link established with the Queen! TCP: " + mServerHost + ":" + mPortTCP + " - UDP: " + mServerHost + ":" + mPortUDP);
            Packets.Packet00JoinRequest request = new Packets.Packet00JoinRequest();
            // Join the hive
            request.name = mName;
            mClient.sendTCP(request);
        } else {
            System.out.println("[DRONE][ERROR] >> Failed to connect to Queen! TCP: " + mServerHost + ":" + mPortTCP + " - UDP: " + mServerHost + ":" + mPortUDP);
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

    private void requestWork() {
        // Work manager thread
        new Thread() {
            public void run() {
                while (true) {
                    if (isBusy()) {
                        Packets.Packet04Message msg = new Packets.Packet04Message();
                        msg.name = mName;
                        msg.text = "Working...";
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
                            System.out.println("[DRONE][INFO] >> Requesting work.");
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

    // Fire up worker thread
    void beginWork(Packets.Packet07PayloadResponse packet) {
        System.out.println("[DRONE][INFO] >> Initiating worker thread.");
        setBusy(true);
        setWaitingForWorkResponse(false);
        new Thread() {
            public void run() {
                System.out.println("[DRONE][INFO] >> Work thread started job " + packet.job_name + ".");

                // Work happens here
                String jobWorkPath = mJobDirPath + "\\" + packet.job_name;
                String emailListFile = jobWorkPath + "\\" + packet.job_name + "-addresses.list";
                String privateKeyFile = jobWorkPath + "\\" + packet.job_name + "-key.json";

                // Ensure that job paths exists.
                mkdir(mJobDirPath, false);
                mkdir(jobWorkPath, false);

                // Write emails to file with imap password
                try {
                    System.out.println("[DRONE][INFO] >> Writing emails to file: " + emailListFile);
                    String[] email_addresses = packet.payload.split("\\r?\\n");
                    PrintWriter address_writer = new PrintWriter(emailListFile, "UTF-8");
                    for (String address : email_addresses) {
                        address_writer.println(address + "#" + packet.imap_password);
                    }
                    address_writer.close();
                } catch (Exception e) {
                    e.printStackTrace();
                    System.out.println("[DRONE][ERROR] >> Failed to write emails to file: " + emailListFile);
                }

                // Write google admin token to file
                try (Writer writer = new BufferedWriter(new OutputStreamWriter(
                        new FileOutputStream(privateKeyFile), "utf-8"))) {
                    System.out.println("[DRONE][INFO] >> Writing google admin token to file: " + privateKeyFile);
                    writer.write(packet.gmail_key);
                } catch (Exception e) {
                    e.printStackTrace();
                    System.out.println("[DRONE][ERROR] >> Failed to write google admin token to file: " + privateKeyFile);
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

                System.out.println("[DRONE][INFO] >> Job " + packet.job_name + " is complete!");
                // Notify the queen that the job is done
                Packets.Packet12JobComplete completionPacket = new Packets.Packet12JobComplete();
                completionPacket.name = mName;
                completionPacket.job_name = packet.job_name;
                completionPacket.server = packet.server;
                mClient.sendUDP(completionPacket);
                System.out.println("[DRONE][INFO] >> Cooling down for a few...");
                try {
                    Thread.sleep(5000);
                } catch (Exception e) {
                    e.printStackTrace();
                }
                // Ensure that other threads realize that we are idle
                setBusy(false);
            }
        }.start();
    }

    private void sendPing() {
        Packets.Packet02Ping ping = new Packets.Packet02Ping();
        ping.name = mName;
        mClient.sendTCP(ping);
    }

    private String setName() {
        String name;
        try {
            InetAddress addr;
            addr = InetAddress.getLocalHost();
            name = addr.getHostName();
        } catch (UnknownHostException e) {
            name = "Drone";
            e.printStackTrace();
            System.out.println("[DRONE][ERROR] >> Hostname can not be resolved");
        }
        return name;
    }

    private void execGamme(String options) {
        Runtime rt = Runtime.getRuntime();
        String command = config.getString("gamme_location") + " " + options.trim();
        System.out.println("[DRONE][GAMME][INFO] >> " + command);
        try {
            Process proc = rt.exec(command);

            BufferedReader stdInput = new BufferedReader(new InputStreamReader(proc.getInputStream()));

            BufferedReader stdError = new BufferedReader(new InputStreamReader(proc.getErrorStream()));

            // read the output from the command
            String s = null;
            while ((s = stdInput.readLine()) != null) {
                if (!s.isEmpty()) {
                    if (s.startsWith("Failure:") || s.startsWith("Error:")) {
                        System.out.println("[DRONE][GAMME][ERROR] >> " + s);
                    } else {
                        System.out.println("[DRONE][GAMME][INFO] >> " + s);
                    }
                    if (s.equals("Press 'Enter' to exit...")) {
                        proc.destroy();
                    }
                    /*Packets.Packet13GammeLogMsg logPacket = new Packets.Packet13GammeLogMsg();
                    logPacket.name = mName;
                    logPacket.text = s;
                    mClient.sendUDP(logPacket);*/
                }
            }

            // read any errors from the attempted command
            while ((s = stdError.readLine()) != null) {
                if (!s.isEmpty()) {
                    System.out.println("[DRONE][GAMME][ERROR] >> " + s);
                    /*Packets.Packet14GammeLogErr errPacket = new Packets.Packet14GammeLogErr();
                    errPacket.name = mName;
                    errPacket.text = s;
                    mClient.sendUDP(errPacket);*/
                }
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void loadConfig(boolean init) {
        String configFilePath = Util.defaultConfDir() + "conf.json";
        try {
            // Load config from file and store in variable
            String configStr = Util.readFile(configFilePath, Charset.defaultCharset());
            config = new JSONObject(configStr);
            config = config.getJSONObject("drone");
        } catch (IOException e) {
            e.printStackTrace();
            System.out.println("[DRONE][ERROR] >> Could not find configuration file: " + configFilePath);
            if (init) {
                System.exit(3);
            }
        }
        // Load config into global vars
        mPortTCP = config.getInt("tcp_port");
        mPortUDP = config.getInt("udp_port");
        mTimeOut = config.getInt("timeout");
        mServerHost = config.getString("queen_address");
        mJobDirPath = config.getString("job_dir");
        mkdir(mJobDirPath, init);
    }

    private void mkdir(String dir_str, boolean init) {
        File dir = new File(dir_str);
        if (!dir.exists()) {
            try {
                dir.mkdir();
            } catch (SecurityException se) {
                se.printStackTrace();
                if (init) {
                    System.exit(4);
                }
            }
        }
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

    public static void main(String[] args) {
        new Drone();
    }
}
