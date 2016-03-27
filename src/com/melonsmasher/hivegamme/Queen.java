package com.melonsmasher.hivegamme;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryonet.Server;

/**
 * Created by melon on 3/26/16.
 */
public class Queen {

    private int mPortTCP = 25851, mPortUDP = 25852;
    private Server mServer;
    private ServerNetworkListener mListener;

    private Queen() {

        System.out.println("[QUEEN][INFO] >> Initializing server instance...");

        mServer = new Server();
        mListener = new ServerNetworkListener();
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
    }

    public static void main(String[] args) {
        new Queen();
    }
}
