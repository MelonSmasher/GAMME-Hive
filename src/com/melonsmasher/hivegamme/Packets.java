package com.melonsmasher.hivegamme;

/**
 * Created by melon on 3/26/16.
 */
class Packets {
    static class Packet00JoinRequest {
        String name = "Drone";
    }

    static class Packet01JoinResponse {
        String name = "Drone";
        boolean a;
    }

    static class Packet02Ping {
        String name = "Drone";
        String m = "PING";
    }

    static class Packet03Pong {
        String name = "Drone";
        String m = "PONG";
    }

    static class Packet04Message {
        String name = "Drone";
        String text;
    }

    static class Packet05GammeLog {
        String name = "Drone";
        String text;
    }

    static class Packet06PayloadRequest {
        String name = "Drone";
    }

    static class Packet07PayloadResponse {
        String job_name;
        String payload;
        String server;
        String google_apps_admin;
        int retry_count;
        boolean imap_security;
        int imap_port;
        String imap_password;
        String imap_server_type;
        String gmail_key;
        String google_domain;
        String exclude_top_level_folders;
        int threads;
    }

    static class Packet08NoWorkAvailable {
    }

    static class Packet09NotifyBusy {
        String name;
    }

    static class Packet10NotifyFree {
        String name;
    }

    static class Packet11ProgressUpdate {
        String name;
        String job_name;
        int progress;
    }

    static class Packet12JobComplete {
        String name;
        String server;
        String job_name;
    }

    static class Packet13GammeLogMsg {
        String name = "Drone";
        String text;
    }

    static class Packet14GammeLogErr {
        String name = "Drone";
        String text;
    }
}
