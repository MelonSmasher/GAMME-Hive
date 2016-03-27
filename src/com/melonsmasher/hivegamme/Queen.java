package com.melonsmasher.hivegamme;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryonet.Server;

import org.json.*;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.Scanner;

/**
 * Created by melon on 3/26/16.
 */
public class Queen {

    private int mPortTCP = 25851, mPortUDP = 25852, mSQLPort, mSQLDB;
    private String mSQLHost, mSQLPass, mSQLUser;
    private Server mServer;
    private JSONObject config;

    private Queen() {

        init();
        initSQL();
        initEmailCache();

        System.out.println("[QUEEN][INFO] >> Initializing server instance...");

        mServer = new Server();
        ServerNetworkListener mListener = new ServerNetworkListener();
        mServer.addListener(mListener);

        try {
            mServer.bind(mPortTCP, mPortUDP);
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("[QUEEN][ERROR] >> Failed to bind to TCP: " + mPortTCP + " and UDP: " + mPortUDP);
        }

        registerPackets();

        try {
            mServer.start();
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("[QUEEN][ERROR] >> Failed to start server!");
        }

        System.out.println("[QUEEN][INFO] >> Server started and listening on TCP: " + mPortTCP + " and UDP: " + mPortUDP);
    }

    private void registerPackets() {
        Kryo mKryo = mServer.getKryo();
        mKryo.register(Packets.Packet00JoinRequest.class);
        mKryo.register(Packets.Packet01JoinResponse.class);
        mKryo.register(Packets.Packet02Ping.class);
        mKryo.register(Packets.Packet03Pong.class);
        mKryo.register(Packets.Packet04Message.class);
        mKryo.register(Packets.Packet05GammeLog.class);
        mKryo.register(Packets.Packet06PayloadRequest.class);
        mKryo.register(Packets.Packet07PayloadResponse.class);
        mKryo.register(Packets.Packet08ThreadCountRequest.class);
        mKryo.register(Packets.Packet09ThreadCountResponse.class);
        mKryo.register(Packets.Packet10ProgressUpdate.class);
    }

    private void init() {

        Scanner mReader = new Scanner(System.in);
        System.out.println("[QUEEN][CONF] >> Configuration file path: ");
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

        mPortTCP = config.getJSONObject("queen").getInt("tcp_port");
        mPortUDP = config.getJSONObject("queen").getInt("udp_port");
    }

    private void initSQL() {

        System.out.println("[QUEEN][INFO] >> Connection to MySQL has been established.");

    }

    private void initEmailCache() {
        String mEmailFile = config.getJSONObject("queen").getString("email_list");

        System.out.println("[QUEEN][INFO] >> Reading emails into DB...");

        try (BufferedReader br = new BufferedReader(new FileReader(mEmailFile))) {
            for (String line; (line = br.readLine()) != null; ) {

            }
        } catch (IOException e) {
            e.printStackTrace();
            System.out.println("[QUEEN][ERROR] >> Could not find email file: " + mEmailFile);
        }

    }

    public static void main(String[] args) {
        new Queen();
    }
}
