package com.melonsmasher.hivegamme.server;

import com.esotericsoftware.kryonet.Connection;
import com.esotericsoftware.kryonet.Listener;
import com.melonsmasher.hivegamme.Packets;
import com.melonsmasher.hivegamme.Util;

/**
 * Created by melon on 3/26/16.
 */
class ServerNetworkListener extends Listener {

    private Queen mQueen;

    /**
     * Listens for packet events from clients
     *
     * @param queen The main Queen instance
     */
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

            Packets.Packet13LogInfo packet = (Packets.Packet13LogInfo) o;

            if (mQueen.getLogger().getCanLogToFile() && (packet.job_name != null)) {
                new Thread() {
                    public void run() {
                        String logFile = Util.defaultLogDir() + packet.job_name + "-info.log";
                        mQueen.getLogger().logToFile(logFile, packet.text);
                    }
                }.start();
            }

        } else if (o instanceof Packets.Packet14LogError && mQueen.isRemoteLoggingEnabled()) {

            Packets.Packet14LogError packet = (Packets.Packet14LogError) o;

            if (mQueen.getLogger().getCanLogToFile() && packet.job_name != null) {
                new Thread() {
                    public void run() {
                        String logFile = Util.defaultLogDir() + packet.job_name + "-error.log";
                        mQueen.getLogger().logToFile(logFile, packet.text);
                    }
                }.start();
            }

        } else if (o instanceof Packets.Packet15LogWarning && mQueen.isRemoteLoggingEnabled()) {

            Packets.Packet15LogWarning packet = (Packets.Packet15LogWarning) o;

            if (mQueen.getLogger().getCanLogToFile() && packet.job_name != null) {
                new Thread() {
                    public void run() {
                        String logFile = Util.defaultLogDir() + packet.job_name + "-warn.log";
                        mQueen.getLogger().logToFile(logFile, packet.text);
                    }
                }.start();
            }

        }
    }

    @Override
    public void idle(Connection connection) {
        super.idle(connection);
    }
}
