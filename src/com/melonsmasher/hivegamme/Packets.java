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
}
