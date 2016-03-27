package com.melonsmasher.hivegamme;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryonet.Client;

import java.io.IOException;
import java.util.Scanner;
import java.net.InetAddress;
import java.net.UnknownHostException;

/**
 * Created by melon on 3/26/16.
 */
public class Drone {

    private int mPortTCP = 25851, mPortUDP = 25852, mTimeOut = 5000;
    private String mServerHost = "localhost";
    private Client mClient;
    public Scanner scanner;
    public String mName = "Drone";
    private ClientNetworkListener mListener;

    private Drone() {
        System.out.println("[DRONE][INFO] >> Initializing client instance...");
        mClient = new Client();
        mListener = new ClientNetworkListener(mClient);
        scanner = new Scanner(System.in);
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
            System.out.println("Hostname can not be resolved");
        }
        return name;
    }

    public static void main(String[] args) {
        new Drone();
    }
}
