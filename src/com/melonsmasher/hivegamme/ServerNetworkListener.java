package com.melonsmasher.hivegamme;

import com.esotericsoftware.kryonet.Connection;
import com.esotericsoftware.kryonet.Listener;

/**
 * Created by melon on 3/26/16.
 */
class ServerNetworkListener extends Listener {

    private Queen mQueen;

    ServerNetworkListener(Queen queen) {
        this.mQueen = queen;
    }

    @Override
    public void connected(Connection connection) {
        super.connected(connection);
        mQueen.getLogger().logInfo("HIVE", "A drone is joining the hive!");
    }

    @Override
    public void disconnected(Connection connection) {
        super.disconnected(connection);
        mQueen.getLogger().logInfo("HIVE", "A drone has left the hive.");
    }

    @Override
    public void received(Connection connection, Object o) {
        super.received(connection, o);
        if (o instanceof Packets.Packet00JoinRequest) {
            Packets.Packet01JoinResponse response = new Packets.Packet01JoinResponse();
            response.a = true;
            mQueen.getLogger().logInfo("MSG", ((Packets.Packet00JoinRequest) o).name + " Reporting in for duty!");
            mQueen.storeDroneInfo(((Packets.Packet00JoinRequest) o).name, connection.getID());
            connection.sendTCP(response);
        } else if (o instanceof Packets.Packet02Ping) {
            Packets.Packet03Pong response = new Packets.Packet03Pong();
            mQueen.getLogger().logInfo("MSG", ((Packets.Packet02Ping) o).m);
            connection.sendTCP(response);
        } else if (o instanceof Packets.Packet04Message) {
            mQueen.getLogger().logInfo("MSG", ((Packets.Packet04Message) o).name + ": " + ((Packets.Packet04Message) o).text);
        } else if (o instanceof Packets.Packet06PayloadRequest) {
            new Thread() {
                public void run() {
                    mQueen.sendWorkLoad(connection, ((Packets.Packet06PayloadRequest) o).name);
                }
            }.start();
        } else if (o instanceof Packets.Packet09NotifyBusy) {

        } else if (o instanceof Packets.Packet10NotifyFree) {

        } else if (o instanceof Packets.Packet11ProgressUpdate) {
            Packets.Packet11ProgressUpdate packet = (Packets.Packet11ProgressUpdate) o;
            mQueen.updateJobProgress(packet.job_name, packet.progress);
        } else if (o instanceof Packets.Packet12JobComplete) {
            Packets.Packet12JobComplete packet = (Packets.Packet12JobComplete) o;
            mQueen.completeJob(packet.job_name, packet.server);
        } else if (o instanceof Packets.Packet13LogInfo && mQueen.isRemoteLoggingEnabled()) {
            if (mQueen.getLogger().getCanLogToFile() && ((Packets.Packet13LogInfo) o).job_name != null) {
                mQueen.getLogger().logToFile(Util.defaultLogDir() + ((Packets.Packet13LogInfo) o).job_name + "-info.log", ((Packets.Packet13LogInfo) o).text);
            }
        } else if (o instanceof Packets.Packet14LogError && mQueen.isRemoteLoggingEnabled()) {
            if (mQueen.getLogger().getCanLogToFile() && ((Packets.Packet14LogError) o).job_name != null) {
                mQueen.getLogger().logToFile(Util.defaultLogDir() + ((Packets.Packet14LogError) o).job_name + "-error.log", ((Packets.Packet14LogError) o).text);
            }
        } else if (o instanceof Packets.Packet15LogWarning && mQueen.isRemoteLoggingEnabled()) {
            if (mQueen.getLogger().getCanLogToFile() && ((Packets.Packet15LogWarning) o).job_name != null) {
                mQueen.getLogger().logToFile(Util.defaultLogDir() + ((Packets.Packet15LogWarning) o).job_name + "-warn.log", ((Packets.Packet15LogWarning) o).text);
            }
        }
    }

    @Override
    public void idle(Connection connection) {
        super.idle(connection);
    }
}
