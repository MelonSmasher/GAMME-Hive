package com.melonsmasher.hivegamme;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryonet.Server;

import org.json.*;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.nio.charset.Charset;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.sql.SQLException;

/**
 * Created by melon on 3/26/16.
 */
public class Queen {

    private int mPortTCP = 25851, mPortUDP = 25852;
    private Server mServer;
    private Connection mySQLConnection = null;
    private Statement mStatement = null;
    private JSONObject config;

    private Queen() {

        init();
        initSQL();

        System.out.println("[QUEEN][INFO] >> Reading new emails into DB...");
        new Thread() {
            public void run() {
                while (true) {
                    try {
                        readInEmails();
                        // Look for new addresses every 10 seconds
                        Thread.sleep(10000);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
        }.start();

        System.out.println("[QUEEN][INFO] >> Initializing server instance...");

        mServer = new Server();
        ServerNetworkListener mListener = new ServerNetworkListener(this);
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
        mKryo.register(Packets.Packet08NoWorkAvailable.class);
    }

    private void init() {
        System.out.println("[QUEEN][INFO] >> Loading configuration.");
        String configFilePath = Util.defaultConfDir() + "conf.json";
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
        try {
            String mSQLHost = config.getJSONObject("sql").getString("host");
            int mSQLPort = config.getJSONObject("sql").getInt("port");
            String mSQLUser = config.getJSONObject("sql").getString("user");
            String mSQLPass = config.getJSONObject("sql").getString("password");
            String mSQLDB = config.getJSONObject("sql").getString("db");
            String url = "jdbc:mysql://" + mSQLHost + ":" + String.valueOf(mSQLPort) + "/" + mSQLDB;
            mySQLConnection = DriverManager.getConnection(url, mSQLUser, mSQLPass);
            mStatement = mySQLConnection.createStatement();
            ResultSet res = mStatement.executeQuery("SELECT VERSION()");
            String version = "";
            if (res.next()) {
                version = ": " + res.getString(1);
            }
            System.out.println("[QUEEN][INFO] >> Connection to MySQL has been established" + version);
        } catch (SQLException ex) {
            ex.printStackTrace();
            System.out.println("[QUEEN][ERROR] >> Could not connect to MySQL. Please check config... Exiting!");
            System.exit(4);
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("[QUEEN][ERROR] >> Could not load MySQL configuration. Please check config... Exiting!");
            System.exit(4);
        }
    }

    void readInEmails() {
        String mEmailFile = config.getJSONObject("queen").getString("email_list");
        try (BufferedReader br = new BufferedReader(new FileReader(mEmailFile))) {
            for (String line; (line = br.readLine()) != null; ) {
                try {
                    ResultSet res = mStatement.executeQuery("SELECT * FROM emails WHERE email='" + line + "'");
                    if (!res.next()) {
                        System.out.println("[QUEEN][INFO] >> Found new address(" + line + "), adding to queue...");
                        mStatement.executeUpdate("INSERT INTO emails(`id`, `email`) VALUES (null,'" + line + "')");
                    }
                } catch (SQLException ex) {
                    ex.printStackTrace();
                }
            }
        } catch (IOException e) {
            System.out.println("[QUEEN][INFO] >> No emails obtained. Looking for them here: " + mEmailFile);
        }
    }

    void storeDroneInfo(String name, int connection_id) {
        new Thread() {
            public void run() {
                try {
                    Statement tmpStatement1 = mySQLConnection.createStatement();
                    ResultSet res1 = tmpStatement1.executeQuery("SELECT id FROM drones WHERE name = '" + name + "'");
                    if (res1.next()) {
                        Statement tmpStatement2 = mySQLConnection.createStatement();
                        tmpStatement2.executeUpdate("UPDATE drones SET connection_id=" + connection_id + " WHERE name='" + name + "'");
                        tmpStatement2.close();
                    } else {
                        Statement tmpStatement3 = mySQLConnection.createStatement();
                        tmpStatement3.executeUpdate("INSERT INTO drones(`id`, `name`, `connection_id`) VALUES (null,'" + name + "'," + connection_id + ")");
                        tmpStatement3.close();
                    }
                    tmpStatement1.close();
                    res1.close();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }.start();
    }

    void sendWorkLoad(int droneThreads, com.esotericsoftware.kryonet.Connection conn) {
        new Thread() {
            public void run() {
                StringBuilder payload = new StringBuilder("");
                StringBuilder idString = new StringBuilder("");
                try {
                    ResultSet res = mStatement.executeQuery("SELECT id,email FROM emails WHERE queue = TRUE LIMIT " + droneThreads + " FOR UPDATE");
                    String imapPass = config.getJSONObject("queen").getString("imap_password");
                    while (res.next()) {
                        payload.append(res.getString(2) + imapPass + "\n");
                        if (res.isFirst()) {
                            idString.append(" id=" + res.getString(1));
                        } else {
                            idString.append(" OR id=" + res.getString(1));
                        }
                    }
                    Statement tmpStatement = mySQLConnection.createStatement();
                    tmpStatement.executeUpdate("UPDATE emails SET queue=FALSE, processing=TRUE WHERE" + idString.toString());
                    tmpStatement.close();
                    res.close();
                    String mPayload = payload.toString();
                    if (!mPayload.isEmpty()) {
                        Packets.Packet07PayloadResponse packet = new Packets.Packet07PayloadResponse();
                        packet.payload = mPayload;
                        conn.sendTCP(packet);
                    } else {
                        Packets.Packet08NoWorkAvailable packet = new Packets.Packet08NoWorkAvailable();
                        conn.sendUDP(packet);
                    }
                } catch (SQLException e) {
                    e.printStackTrace();
                    Packets.Packet08NoWorkAvailable packet = new Packets.Packet08NoWorkAvailable();
                    conn.sendUDP(packet);
                }
            }
        }.start();
    }

    public static void main(String[] args) {
        new Queen();
    }
}
