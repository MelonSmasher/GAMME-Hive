package com.melonsmasher.hivegamme;

import com.esotericsoftware.kryonet.Client;
import com.esotericsoftware.kryonet.Connection;
import com.esotericsoftware.kryonet.Listener;

/**
 * Created by melon on 3/26/16.
 */
class ClientNetworkListener extends Listener {

    private Client mClient;
    private Drone mDrone;

    ClientNetworkListener(Client client, Drone drone) {
        this.mClient = client;
        this.mDrone = drone;
    }

    @Override
    public void connected(Connection connection) {
        super.connected(connection);
        System.out.println("[DRONE][INFO] >> Joining the hive!");
    }

    @Override
    public void disconnected(Connection connection) {
        super.disconnected(connection);
        System.out.println("[DRONE][INFO] >> Left the hive!");
    }

    @Override
    public void received(Connection connection, Object o) {
        super.received(connection, o);
        if (o instanceof Packets.Packet01JoinResponse) {
            Packets.Packet01JoinResponse response = (Packets.Packet01JoinResponse) o;
            if (response.a) {
                System.out.println("[QUEEN][INFO] >> Welcome!");
            } else {
                System.out.println("[QUEEN][INFO] >> Your join request has been denied!");
                connection.close();
                System.exit(2);
            }
        } else if (o instanceof Packets.Packet03Pong) {
            System.out.println("[QUEEN][MSG] >> " + ((Packets.Packet03Pong) o).m);
        } else if (o instanceof Packets.Packet07PayloadResponse) {
            System.out.println("[DRONE][INFO] >> Obtained workload from the Queen.");
            mDrone.setBusy(true);
            Packets.Packet07PayloadResponse packet = (Packets.Packet07PayloadResponse) o;
            mDrone.beginWork(packet);
        } else if (o instanceof Packets.Packet08NoWorkAvailable) {
            System.out.println("[QUEEN][MSG] >> No work, sit tight.");
            mDrone.setBusy(false);
        }
    }

    @Override
    public void idle(Connection connection) {
        super.idle(connection);
    }
}
