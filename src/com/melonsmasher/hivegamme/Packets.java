package com.melonsmasher.hivegamme;

/**
 * Created by melon on 3/26/16.
 */

public class Packets {
    public static class Packet00JoinRequest {
        public String name = "Drone";
    }

    public static class Packet01JoinResponse {
        public String name = "Drone";
        public boolean a;
    }

    public static class Packet02Ping {
        public String name = "Drone";
        public String m = "PING";
    }

    public static class Packet03Pong {
        public String name = "Drone";
        public String m = "PONG";
    }

    public static class Packet04Message {
        public String name = "Drone";
        public String text;
    }

    public static class Packet05GammeLog {
        public String name = "Drone";
        public String text;
    }

    public static class Packet06PayloadRequest {
        public String name = "Drone";
    }

    public static class Packet07PayloadResponse {
        public String job_name;
        public String payload;
        public String server;
        public String google_apps_admin;
        public int retry_count;
        public boolean imap_security;
        public int imap_port;
        public String imap_password;
        public String imap_server_type;
        public String gmail_key;
        public String google_domain;
        public String exclude_top_level_folders;
        public int threads;
    }

    public static class Packet08NoWorkAvailable {
    }

    public static class Packet09NotifyBusy {
        public String name;
    }

    public static class Packet10NotifyFree {
        public String name;
    }

    public static class Packet11ProgressUpdate {
        public String name;
        public String job_name;
        public int progress;
    }

    public static class Packet12JobComplete {
        public String name;
        public String server;
        public String job_name;
    }

    public static class Packet13LogInfo {
        public String name = "Drone";
        public String job_name;
        public String text;
    }

    public static class Packet14LogError {
        public String name = "Drone";
        public String job_name;
        public String text;
    }

    public static class Packet15LogWarning {
        public String name = "Drone";
        public String job_name;
        public String text;
    }
}
