package com.melonsmasher.hivegamme;

import com.esotericsoftware.kryonet.Connection;
import com.esotericsoftware.kryonet.Listener;

/**
 * Created by melon on 3/26/16.
 */
class ServerNetworkListener extends Listener {

    ServerNetworkListener() {
    }

    @Override
    public void connected(Connection connection) {
        super.connected(connection);
        System.out.println("[QUEEN][INFO] >> A drone is joining the hive!");
    }

    @Override
    public void disconnected(Connection connection) {
        super.disconnected(connection);
        System.out.println("[QUEEN][INFO] >> A drone has left the hive.");
    }

    @Override
    public void received(Connection connection, Object o) {
        super.received(connection, o);
        if (o instanceof Packets.Packet00JoinRequest) {
            Packets.Packet01JoinResponse response = new Packets.Packet01JoinResponse();
            response.a = true;
            System.out.println("[" + ((Packets.Packet00JoinRequest) o).name + "][MSG] >> Has requested access.");
            connection.sendTCP(response);
        } else if (o instanceof Packets.Packet02Ping) {
            Packets.Packet03Pong response = new Packets.Packet03Pong();
            System.out.println("[" + ((Packets.Packet02Ping) o).name + "][MSG] >> " + ((Packets.Packet02Ping) o).m + ".");
            connection.sendTCP(response);
        } else if (o instanceof Packets.Packet04Message) {
            System.out.println("[" + ((Packets.Packet04Message) o).name + "][MSG] >> " + ((Packets.Packet04Message) o).text + ".");
        } else if (o instanceof Packets.Packet06PayloadRequest) {
            System.out.println("[" + ((Packets.Packet06PayloadRequest) o).name + "][MSG] >> Drone reporting for duty!");
        }
    }

    @Override
    public void idle(Connection connection) {
        super.idle(connection);
    }
}
