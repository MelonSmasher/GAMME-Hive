package com.melonsmasher.hivegamme;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryonet.Server;

import org.json.*;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.nio.charset.Charset;
import java.sql.*;

/**
 * Created by melon on 3/26/16.
 */
public class Queen {

    private int mPortTCP = 25851, mPortUDP = 25852;
    private String mGmailKey;
    private boolean started = true;
    private Connection mySQLConnection = null;
    private Statement mStatement = null;
    private JSONObject config;

    private Queen() {

        System.out.println("[QUEEN][HIVE][INFO] >> Loading configuration.");
        loadConfig(true);
        // Start thread that reloads the configuration every minute
        new Thread() {
            public void run() {
                while (true) {
                    try {
                        Thread.sleep(60000);
                        loadConfig(false);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    if (!started) {
                        break;
                    }
                }
            }
        }.start();

        initSQL();

        System.out.println("[QUEEN][HIVE][INFO] >> Reading emails and servers...");
        // Look for new addresses every 10 seconds
        new Thread() {
            public void run() {
                while (true) {
                    try {
                        mGmailKey = readGmailKey();
                        readInServers();
                        readInEmails();
                        Thread.sleep(30000);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    if (!started) {
                        break;
                    }
                }
            }
        }.start();

        System.out.println("[QUEEN][HIVE][INFO] >> Initializing server instance...");

        Server mServer = new Server(163840, 20480);
        ServerNetworkListener mListener = new ServerNetworkListener(this);
        mServer.addListener(mListener);

        try {
            mServer.bind(mPortTCP, mPortUDP);
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("[QUEEN][HIVE][ERROR] >> Failed to bind to TCP: " + mPortTCP + " and UDP: " + mPortUDP);
        }

        Util.registerPackets(mServer.getKryo());

        try {
            mServer.start();
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("[QUEEN][HIVE][ERROR] >> Failed to start server!");
        }

        System.out.println("[QUEEN][HIVE][INFO] >> Server started and listening on TCP: " + mPortTCP + " and UDP: " + mPortUDP);
    }

    private void loadConfig(boolean init) {
        String configFilePath = Util.defaultConfDir() + "conf.json";
        try {
            String configStr = Util.readFile(configFilePath, Charset.defaultCharset());
            config = new JSONObject(configStr);
            config = config.getJSONObject("queen");
        } catch (IOException e) {
            e.printStackTrace();
            System.out.println("[QUEEN][HIVE][ERROR] >> Could not find configuration file: " + configFilePath);
            if (init) {
                System.exit(3);
            }
        }
        // Load config into global vars
        mPortTCP = config.getInt("tcp_port");
        mPortUDP = config.getInt("udp_port");

    }

    private void initSQL() {
        try {
            String mSQLHost = config.getJSONObject("mysql").getString("host");
            int mSQLPort = config.getJSONObject("mysql").getInt("port");
            String mSQLUser = config.getJSONObject("mysql").getString("user");
            String mSQLPass = config.getJSONObject("mysql").getString("password");
            String mSQLDB = config.getJSONObject("mysql").getString("db");

            String url = "jdbc:mysql://" + mSQLHost + ":" + String.valueOf(mSQLPort) + "/" + mSQLDB;
            mySQLConnection = DriverManager.getConnection(url, mSQLUser, mSQLPass);
            mStatement = mySQLConnection.createStatement();
            ResultSet res = mStatement.executeQuery("SELECT VERSION()");
            String version = "";
            if (res.next()) {
                version = ": " + res.getString(1);
            }
            System.out.println("[QUEEN][HIVE][INFO] >> Connection to MySQL has been established" + version);
        } catch (SQLException ex) {
            ex.printStackTrace();
            System.out.println("[QUEEN][HIVE][ERROR] >> Could not connect to MySQL. Please check config... Exiting!");
            System.exit(4);
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("[QUEEN][HIVE][ERROR] >> Could not load MySQL configuration. Please check config... Exiting!");
            System.exit(4);
        }
    }

    private void readInEmails() {
        String mEmailFile = config.getString("email_list");
        int count = 0;
        try (BufferedReader br = new BufferedReader(new FileReader(mEmailFile))) {
            for (String line; (line = br.readLine()) != null; ) {
                try {
                    ResultSet res = mStatement.executeQuery("SELECT * FROM emails WHERE email='" + line + "'");
                    if (!res.next()) {
                        mStatement.executeUpdate("INSERT INTO emails(`id`, `email`) VALUES (null,'" + line + "')");
                        count++;
                    }
                } catch (SQLException ex) {
                    ex.printStackTrace();
                }
            }
            if (count > 0) {
                System.out.println("[QUEEN][HIVE][INFO] >> Found new " + count + " new email addresses.");
            }
        } catch (IOException e) {
            System.out.println("[QUEEN][HIVE][INFO] >> No emails obtained. Looking for them here: " + mEmailFile);
        }
    }

    private void readInServers() {
        String mServersFile = config.getString("server_list");
        try (BufferedReader br = new BufferedReader(new FileReader(mServersFile))) {
            for (String line; (line = br.readLine()) != null; ) {
                try {
                    ResultSet res = mStatement.executeQuery("SELECT * FROM servers WHERE address='" + line + "'");
                    if (!res.next()) {
                        System.out.println("[QUEEN][HIVE][INFO] >> Found new server(" + line + ")");
                        mStatement.executeUpdate("INSERT INTO servers(`id`, `address`) VALUES (null,'" + line + "')");
                    }
                } catch (SQLException ex) {
                    ex.printStackTrace();
                }
            }
        } catch (IOException e) {
            System.out.println("[QUEEN][HIVE][INFO] >> No servers obtained. Looking for them here: " + mServersFile);
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

    void completeJob(String job_name, String server) {
        new Thread() {
            public void run() {
                try {
                    System.out.println("[QUEEN][JOB][" + job_name + "] >> Progress: 100%! Complete!");
                    Statement statement = mySQLConnection.createStatement();
                    statement.executeUpdate("UPDATE jobs SET `completed`=TRUE, `progress`=100 WHERE `job_name`='" + job_name + "'");
                    statement.closeOnCompletion();
                    statement.executeUpdate("UPDATE servers SET `current_jobs`=current_jobs - 1 WHERE `address`='" + server + "'");
                    statement.closeOnCompletion();
                } catch (Exception e) {
                    e.printStackTrace();
                }

            }
        }.start();
    }

    void updateJobProgress(String job_name, int percent) {
        new Thread() {
            public void run() {
                try {
                    System.out.println("[QUEEN][JOB][" + job_name + "] >> Progress: " + percent + "%");
                    Statement statement = mySQLConnection.createStatement();
                    statement.executeUpdate("UPDATE jobs SET `progress`=" + percent + " WHERE `job_name`=" + job_name);
                    statement.closeOnCompletion();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }.start();
    }

    private String readGmailKey() {
        String mJsonKey = config.getString("service_account_key_path");
        String key = null;
        try {
            key = Util.readFile(mJsonKey, Charset.defaultCharset());
        } catch (IOException e) {
            e.printStackTrace();
        }
        return key;
    }

    void sendWorkLoad(com.esotericsoftware.kryonet.Connection conn, String name) {
        System.out.println("[QUEEN][DRONE][" + name + "] >> Constructing workload.");
        StringBuilder payload = new StringBuilder("");
        StringBuilder idString = new StringBuilder("");
        StringBuilder ids = new StringBuilder("");
        int droneThreads = config.getJSONObject("gamme").getInt("threads");
        try {
            Statement selectAddresses = mySQLConnection.createStatement();
            ResultSet res = selectAddresses.executeQuery("SELECT id,email FROM emails ORDER BY pass ASC LIMIT " + droneThreads + " FOR UPDATE");
            System.out.println("[QUEEN][DRONE][" + name + "] >> Gathering target email addresses.");
            if (res.next()) {
                res.beforeFirst();
                System.out.println("[QUEEN][DRONE][" + name + "] >> Building payload.");

                while (res.next()) {
                    payload.append(res.getString(2) + "\n");
                    if (res.isFirst()) {
                        idString.append(" id=" + res.getString(1));
                        ids.append(res.getString(1));
                    } else {
                        idString.append(" OR id=" + res.getString(1));
                        ids.append(", " + res.getString(1));
                    }
                }
                Statement tmpStatement = mySQLConnection.createStatement();
                tmpStatement.executeUpdate("UPDATE emails SET `queue`=FALSE, `processing`=TRUE, `pass`=pass + 1 WHERE " + idString.toString().trim());
            }
            String mPayload = payload.toString();
            Statement getServer = mySQLConnection.createStatement();
            ResultSet serverRes = getServer.executeQuery("SELECT * FROM servers ORDER BY current_jobs ASC ");
            String jobName = "";
            System.out.println("[QUEEN][DRONE][" + name + "] >> Selecting server.");
            if (!mPayload.isEmpty()) {
                serverRes.first();
                System.out.println("[QUEEN][DRONE][" + name + "] >> Server has been selected: " + serverRes.getString(2));
                Statement getDrone = mySQLConnection.createStatement();
                ResultSet droneInfo = getDrone.executeQuery("SELECT * from drones where `connection_id`=" + conn.getID());
                if (droneInfo.next()) {
                    droneInfo.first();
                    String droneID = droneInfo.getString(1);
                    jobName = droneInfo.getString(2) + "-" + String.valueOf(System.currentTimeMillis());
                    System.out.println("[QUEEN][DRONE][" + name + "] >> New Job is being created: " + jobName);
                    Statement createJob = mySQLConnection.createStatement();
                    createJob.executeUpdate("INSERT INTO jobs(`id`,`job_name`,`drone_id`) VALUES (NULL, '" + jobName + "', " + droneID + ")", Statement.RETURN_GENERATED_KEYS);
                    ResultSet generatedKeys = createJob.getGeneratedKeys();
                    generatedKeys.first();
                    int jobID = generatedKeys.getInt(1);
                    System.out.println("[QUEEN][DRONE][" + name + "] >> New Job created with ID: " + jobID);
                    Statement createEmailJob = mySQLConnection.createStatement();
                    for (String id : ids.toString().split(",")) {
                        id = id.trim();
                        createEmailJob.executeUpdate("INSERT INTO job_emails(`id`,`job_id`,`email_id`) VALUES (NULL, " + jobID + ", " + id + ")");
                    }
                    Statement updateServerJobs = mySQLConnection.createStatement();
                    int jobCount = serverRes.getInt(3) + 1;
                    updateServerJobs.executeUpdate("UPDATE servers SET `current_jobs`=" + jobCount + " WHERE id=" + serverRes.getInt(1));
                    createEmailJob.close();
                    updateServerJobs.close();
                }
                Packets.Packet07PayloadResponse packet = new Packets.Packet07PayloadResponse();

                packet.gmail_key = mGmailKey;
                packet.payload = mPayload.trim();
                packet.job_name = jobName;
                packet.threads = droneThreads;
                packet.google_apps_admin = config.getJSONObject("gamme").getString("google_apps_admin");
                packet.retry_count = config.getJSONObject("gamme").getInt("retry_count");
                packet.imap_password = config.getJSONObject("gamme").getString("imap_password");
                packet.imap_server_type = config.getJSONObject("gamme").getString("imap_server_type");
                packet.imap_port = config.getJSONObject("gamme").getInt("imap_port");
                packet.imap_security = config.getJSONObject("gamme").getBoolean("imap_security");
                packet.google_domain = config.getJSONObject("gamme").getString("google_domain");
                packet.exclude_top_level_folders = config.getJSONObject("gamme").getJSONArray("exclude_top_level_folders").join(",");
                packet.server = serverRes.getString(2);

                conn.sendTCP(packet);
                System.out.println("[QUEEN][DRONE][" + name + "] >> Workload has shipped!");
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

    public static void main(String[] args) {
        new Queen();
    }
}
