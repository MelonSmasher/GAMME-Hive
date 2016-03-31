package com.melonsmasher.hivegamme;

import com.esotericsoftware.kryonet.Server;

import org.json.*;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.Charset;
import java.sql.*;

/**
 * Created by melon on 3/26/16.
 */
public class Queen {

    private int mPortTCP = 25851, mPortUDP = 25852;
    private String mGmailKey, mName;
    private boolean started = true, mRemoteLoggingEnabled = false;
    private Connection mySQLConnection = null;
    private Statement mStatement = null;
    private JSONObject config = null;
    private ServerLogger mLogger;

    private Queen() {
        // Set the server's name
        setName(Util.getName());
        // Make sure that the log dir exists.
        Util.mkdir(Util.defaultLogDir());
        // Create a new ServerLogger instance
        mLogger = new ServerLogger(this);
        mLogger.logInfo("SYS", "Loading configuration...");
        // Load the configuration for the first time.
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
        // Initiate our connection to MySQL
        initSQL();
        mLogger.logInfo("SYS", "Reading emails and servers...");
        // Look for new addresses every 10 seconds
        new Thread() { // Create a new thread that reloads the auth token, server list, and email list periodically.
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
        mLogger.logInfo("SYS", "Initializing server instance...");
        // Instantiate new server object with overloaded packet sizes
        Server mServer = new Server(163840, 20480);
        // Create a new listener
        ServerNetworkListener mListener = new ServerNetworkListener(this);
        // Attach the server instance to our listener instance
        mServer.addListener(mListener);
        // Try to bind to our ports
        try {
            mServer.bind(mPortTCP, mPortUDP);
        } catch (Exception e) {
            e.printStackTrace();
            mLogger.logErr("SYS", "Failed to bind to TCP: " + mPortTCP + " and UDP: " + mPortUDP + "! - Exit Code: 1");
            started = false;
            System.exit(1);
        }
        // Register packets with Kryo
        Util.registerPackets(mServer.getKryo());
        // Try to start the server
        try {
            mServer.start();
        } catch (Exception e) {
            e.printStackTrace();
            mLogger.logErr("SYS", "Failed to start server! - Exit Code: 2");
            started = false;
            System.exit(2);
        }
        mLogger.logInfo("SYS", "Server started and listening on TCP: " + mPortTCP + " and UDP: " + mPortUDP);
    }


    /**
     * @param strict Should the system exit with code 3 if the config cannot be reloaded?
     */
    private void loadConfig(boolean strict) {
        String configFilePath = Util.defaultConfDir() + "conf.json";
        JSONObject old_config = config;
        boolean shouldReload = true;
        try {
            String configStr = Util.readFile(configFilePath, Charset.defaultCharset());
            config = new JSONObject(configStr);
            config = config.getJSONObject("queen");
        } catch (Exception e) {
            if (strict) {
                e.printStackTrace();
                mLogger.logErr("SYS", "Could not find configuration file: " + configFilePath + " - Error Code: 3");
                started = false;
                System.exit(3);
            } else {
                shouldReload = false;
                mLogger.logWarn("SYS", "Could not find configuration file: " + configFilePath + " Using last known configuration...");
            }
        }
        try {
            mRemoteLoggingEnabled = config.getBoolean("remote_logging");
        } catch (Exception e) {
            mRemoteLoggingEnabled = false;
            mLogger.logWarn("SYS", "Option: \"remote_logging\" not defined in config file. Assuming false.");
        }
        if (shouldReload) {
            // Load config into global vars
            mPortTCP = config.getInt("tcp_port");
            mPortUDP = config.getInt("udp_port");
        } else {
            config = old_config;
        }
        if (config == null) {
            mLogger.logErr("SYS", "Could load/reload the configuration. - Error Code: 4");
            started = false;
            System.exit(4);
        }
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
            mLogger.logInfo("SYS", "Connection to MySQL has been established" + version);
        } catch (SQLException ex) {
            ex.printStackTrace();
            mLogger.logErr("SYS", "Could not connect to MySQL. Please check config... Exiting! - Error Code: 4");
            started = false;
            System.exit(4);
        } catch (Exception e) {
            e.printStackTrace();
            mLogger.logErr("SYS", "[QUEEN][HIVE][ERROR] >> Could not load MySQL configuration. Please check config... Exiting! - Error Code: 4");
            started = false;
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
                mLogger.logInfo("SYS", "Found new " + count + " new email addresses.");
            }
        } catch (IOException e) {
            mLogger.logWarn("SYS", "No emails obtained. Looking for them here: " + mEmailFile);
        }
    }

    private void readInServers() {
        String mServersFile = config.getString("server_list");
        try (BufferedReader br = new BufferedReader(new FileReader(mServersFile))) {
            for (String line; (line = br.readLine()) != null; ) {
                try {
                    ResultSet res = mStatement.executeQuery("SELECT * FROM servers WHERE address='" + line + "'");
                    if (!res.next()) {
                        mLogger.logInfo("SYS", "Found new server(" + line + ")");
                        mStatement.executeUpdate("INSERT INTO servers(`id`, `address`) VALUES (null,'" + line + "')");
                    }
                } catch (SQLException ex) {
                    ex.printStackTrace();
                }
            }
        } catch (IOException e) {
            mLogger.logWarn("SYS", "No servers obtained. Looking for them here: " + mServersFile);
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
                    mLogger.logInfo("JOB", job_name + " Complete!");
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
                    mLogger.logInfo("JOB", job_name + " - Progress: " + percent + "%");
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
        mLogger.logInfo("JOB", "Constructing workload for: " + name);
        StringBuilder payload = new StringBuilder("");
        StringBuilder idString = new StringBuilder("");
        StringBuilder ids = new StringBuilder("");
        int droneThreads = config.getJSONObject("gamme").getInt("threads");
        try {
            Statement selectAddresses = mySQLConnection.createStatement();
            ResultSet res = selectAddresses.executeQuery("SELECT id,email FROM emails ORDER BY pass ASC LIMIT " + droneThreads + " FOR UPDATE");
            mLogger.logInfo("JOB", "Gathering target email addresses for: " + name);
            if (res.next()) {
                res.beforeFirst();
                mLogger.logInfo("JOB", "Building payload for: " + name);

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
            mLogger.logInfo("JOB", "Selecting server for: " + name);
            if (!mPayload.isEmpty()) {
                serverRes.first();
                mLogger.logInfo("JOB", "Server has been selected: " + serverRes.getString(2));
                Statement getDrone = mySQLConnection.createStatement();
                ResultSet droneInfo = getDrone.executeQuery("SELECT * from drones where `connection_id`=" + conn.getID());
                if (droneInfo.next()) {
                    droneInfo.first();
                    String droneID = droneInfo.getString(1);
                    jobName = droneInfo.getString(2) + "-" + String.valueOf(System.currentTimeMillis());
                    mLogger.logInfo("JOB", "New Job is being created: " + jobName);
                    Statement createJob = mySQLConnection.createStatement();
                    createJob.executeUpdate("INSERT INTO jobs(`id`,`job_name`,`drone_id`) VALUES (NULL, '" + jobName + "', " + droneID + ")", Statement.RETURN_GENERATED_KEYS);
                    ResultSet generatedKeys = createJob.getGeneratedKeys();
                    generatedKeys.first();
                    int jobID = generatedKeys.getInt(1);
                    mLogger.logInfo("JOB", "New Job created with ID: " + jobID);
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
                mLogger.logInfo("JOB", "Job: " + jobName + " has been dispatched to: " + name);
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

    public boolean isRemoteLoggingEnabled() {
        return this.mRemoteLoggingEnabled;
    }

    String getName() {
        return mName;
    }

    void setName(String mName) {
        this.mName = mName;
    }

    public ServerLogger getLogger() {
        return mLogger;
    }

    public static void main(String[] args) {
        new Queen();
    }
}
