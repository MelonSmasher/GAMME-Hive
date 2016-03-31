package com.melonsmasher.hivegamme.client;

import com.esotericsoftware.kryonet.Connection;
import com.esotericsoftware.kryonet.Listener;
import com.melonsmasher.hivegamme.Packets;

/**
 * Created by melon on 3/26/16.
 */
class ClientNetworkListener extends Listener {

    private Drone mDrone;

    ClientNetworkListener(Drone drone) {
        this.mDrone = drone;
    }

    @Override
    public void connected(Connection connection) {
        super.connected(connection);
        mDrone.getLogger().logInfo("SYS", "Joining the hive!");
    }

    @Override
    public void disconnected(Connection connection) {
        mDrone.getLogger().logInfo("SYS", "Left the hive!");
        super.disconnected(connection);
    }

    @Override
    public void received(Connection connection, Object o) {
        super.received(connection, o);
        if (o instanceof Packets.Packet01JoinResponse) {
            Packets.Packet01JoinResponse response = (Packets.Packet01JoinResponse) o;
            if (response.a) {
                mDrone.getLogger().logInfo("HIVE", "Welcome!");
            } else {
                mDrone.getLogger().logErr("HIVE", "Your join request has been denied! Exit Code: 2");
                connection.close();
                mDrone.setStarted(false);
                System.exit(2);
            }
        } else if (o instanceof Packets.Packet03Pong) {
            mDrone.getLogger().logInfo("HIVE", ((Packets.Packet03Pong) o).m);
        } else if (o instanceof Packets.Packet07PayloadResponse) {
            Packets.Packet07PayloadResponse packet = (Packets.Packet07PayloadResponse) o;
            mDrone.beginWork(packet);
        } else if (o instanceof Packets.Packet08NoWorkAvailable) {
            mDrone.getLogger().logInfo("HIVE", "No work, sit tight.");
            mDrone.setBusy(false);
            mDrone.setWaitingForWorkResponse(false);
        }
    }

    @Override
    public void idle(Connection connection) {
        super.idle(connection);
    }
}
