package com.melonsmasher.hivegamme;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryonet.Client;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.Scanner;
import java.net.InetAddress;
import java.net.UnknownHostException;

/**
 * Created by melon on 3/26/16.
 */
public class Drone {

    private int mPortTCP = 25801, mPortUDP = 25802, mTimeOut = 5000;
    private String mServerHost = "localhost";
    private Client mClient;
    public String mName = "Drone";
    private JSONObject config;

    private Drone() {

        init();

        System.out.println("[DRONE][INFO] >> Initializing client instance...");
        mClient = new Client();
        ClientNetworkListener mListener = new ClientNetworkListener(mClient);
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

        sendPing();

        while (true) {

        }
    }

    private void registerPackets() {
        Kryo mKryo = mClient.getKryo();
        mKryo.register(Packets.Packet00JoinRequest.class);
        mKryo.register(Packets.Packet01JoinResponse.class);
        mKryo.register(Packets.Packet02Ping.class);
        mKryo.register(Packets.Packet03Pong.class);
        mKryo.register(Packets.Packet04Message.class);
    }

    private void sendPing() {
        Packets.Packet02Ping ping = new Packets.Packet02Ping();
        ping.name = mName;
        mClient.sendUDP(ping);
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

    private void init() {

        Scanner mReader = new Scanner(System.in);
        System.out.println("[DRONE][CONF] >> Configuration file path: ");
        String configFilePath = mReader.nextLine();
        String configStr = "";
        try {
            configStr = Util.readFile(configFilePath, Charset.defaultCharset());
        } catch (IOException e) {
            e.printStackTrace();
            System.out.println("[QUEEN][ERROR] >> Could not find configuration file: " + configFilePath);
            System.exit(3);
        }

        config = new JSONObject(configStr);

        mPortTCP = config.getJSONObject("drone").getInt("tcp_port");
        mPortUDP = config.getJSONObject("drone").getInt("udp_port");
        mTimeOut = config.getJSONObject("drone").getInt("timeout");
        mServerHost = config.getJSONObject("drone").getString("queen_address");

        File workDir = new File(config.getJSONObject("drone").getString("working_dir"));
        if (!workDir.exists()) {
            try {
                workDir.mkdir();
            } catch (SecurityException se) {
                se.printStackTrace();
            }
        }
    }

    public static void main(String[] args) {
        new Drone();
    }
}
