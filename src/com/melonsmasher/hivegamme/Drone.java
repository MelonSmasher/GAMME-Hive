package com.melonsmasher.hivegamme;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryonet.Client;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.net.InetAddress;
import java.net.UnknownHostException;

/**
 * Created by melon on 3/26/16.
 */
public class Drone {

    private int mPortTCP = 25801, mPortUDP = 25802, mTimeOut = 5000;
    private String mServerHost = "localhost", mWorkDirPAth;
    private Client mClient;
    private boolean busy = false, started = true;
    private String mName = "Drone";
    private JSONObject config;

    private Drone() {

        init();

        System.out.println("[DRONE][INFO] >> Initializing client instance...");
        mClient = new Client(81920, 20480);
        ClientNetworkListener mListener = new ClientNetworkListener(mClient, this);
        mClient.addListener(mListener);
        registerPackets();
        mName = setName();

        try {
            mClient.start();
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("[DRONE][ERROR] >> Failed to start client!");
        }

        try {

            mClient.connect(mTimeOut, mServerHost, mPortTCP, mPortUDP);
        } catch (IOException e) {
            e.printStackTrace();
        }

        if (mClient.isConnected()) {
            System.out.println("[DRONE][INFO] >> Telepathic link established with the Queen! TCP: " + mServerHost + ":" + mPortTCP + " - UDP: " + mServerHost + ":" + mPortUDP);
            Packets.Packet00JoinRequest request = new Packets.Packet00JoinRequest();
            request.name = mName;
            mClient.sendTCP(request);
        } else {
            System.out.println("[DRONE][ERROR] >> Failed to connect to Queen! TCP: " + mServerHost + ":" + mPortTCP + " - UDP: " + mServerHost + ":" + mPortUDP);
        }


        requestWork();

        while (true) {
            if (!started) {
                break;
            }
        }
    }

    private void registerPackets() {
        Kryo mKryo = mClient.getKryo();
        mKryo.register(Packets.Packet00JoinRequest.class);
        mKryo.register(Packets.Packet01JoinResponse.class);
        mKryo.register(Packets.Packet02Ping.class);
        mKryo.register(Packets.Packet03Pong.class);
        mKryo.register(Packets.Packet04Message.class);
        mKryo.register(Packets.Packet05GammeLog.class);
        mKryo.register(Packets.Packet06PayloadRequest.class);
        mKryo.register(Packets.Packet07PayloadResponse.class);
        mKryo.register(Packets.Packet08NoWorkAvailable.class);
        mKryo.register(Packets.Packet09NotifyBusy.class);
        mKryo.register(Packets.Packet10NotifyFree.class);
        mKryo.register(Packets.Packet11ProgressUpdate.class);
        mKryo.register(Packets.Packet12JobComplete.class);
    }

    private void requestWork() {
        // Work manager thread
        new Thread() {
            public void run() {
                while (true) {
                    if (isBusy()) {
                        System.out.println("[DRONE][INFO] >> Working....");
                        try {
                            Thread.sleep(1000);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    } else {
                        int threads = config.getJSONObject("gamme").getInt("threads");
                        Packets.Packet06PayloadRequest packet = new Packets.Packet06PayloadRequest();
                        packet.name = mName;
                        packet.threads = threads;
                        System.out.println("[DRONE][INFO] >> Requesting work.");
                        mClient.sendTCP(packet);
                    }
                    try {
                        Thread.sleep(1000);
                    } catch (Exception e) {
                        e.printStackTrace();
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
        new Thread() {
            public void run() {
                System.out.println("[DRONE][INFO] >> Work thread started job " + packet.job_name + ".");
               // System.out.println(packet.payload);

                try {
                    Thread.sleep(3000);
                } catch (Exception e) {
                    e.printStackTrace();
                }

                System.out.println("[DRONE][INFO] >> Job " + packet.job_name + " is complete!");

                // Notify the queen that the job is done
                Packets.Packet12JobComplete completionPacket = new Packets.Packet12JobComplete();
                completionPacket.name = mName;
                completionPacket.job_name = packet.job_name;
                completionPacket.server = packet.server;
                mClient.sendUDP(completionPacket);

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

    private void loadConfig() {
        String configFilePath = Util.defaultConfDir() + "conf.json";
        String configStr = "";
        try {
            configStr = Util.readFile(configFilePath, Charset.defaultCharset());
        } catch (IOException e) {
            e.printStackTrace();
            System.out.println("[DRONE][ERROR] >> Could not find configuration file: " + configFilePath);
            System.exit(3);
        }
        config = new JSONObject(configStr);
    }

    private void init() {

        System.out.println("[DRONE][INFO] >> Loading configuration.");

        loadConfig();

        mPortTCP = config.getJSONObject("drone").getInt("tcp_port");
        mPortUDP = config.getJSONObject("drone").getInt("udp_port");
        mTimeOut = config.getJSONObject("drone").getInt("timeout");
        mServerHost = config.getJSONObject("drone").getString("queen_address");
        mWorkDirPAth = config.getJSONObject("drone").getString("working_dir");

        File workDir = new File(mWorkDirPAth);
        if (!workDir.exists()) {
            try {
                workDir.mkdir();
            } catch (SecurityException se) {
                se.printStackTrace();
            }
        }
    }

    void setBusy(boolean status) {
        this.busy = status;
    }

    boolean isBusy() {
        return this.busy;
    }

    public static void main(String[] args) {
        new Drone();
    }
}
