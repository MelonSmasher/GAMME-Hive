package com.melonsmasher.hivegamme;

import com.esotericsoftware.kryo.Kryo;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;

/**
 * Created by melon on 3/27/16.
 */
class Util {

    Util() {

    }

    static String readFile(String path, Charset encoding)
            throws IOException {
        byte[] encoded = Files.readAllBytes(Paths.get(path));
        return new String(encoded, encoding);
    }


    static String defaultConfDir() {
        if (isWindows()) {
            return "C:\\Program Files\\GAMME Hive\\";
        } else if (isMac()) {
            return "/usr/local/etc/gamme_hive/";
        } else if (isUnix()) {
            return "/etc/gamme_hive/";
        } else {
            return null;
        }
    }

    static void registerPackets(Kryo lKryo) {
        lKryo.register(Packets.Packet00JoinRequest.class);
        lKryo.register(Packets.Packet01JoinResponse.class);
        lKryo.register(Packets.Packet02Ping.class);
        lKryo.register(Packets.Packet03Pong.class);
        lKryo.register(Packets.Packet04Message.class);
        lKryo.register(Packets.Packet05GammeLog.class);
        lKryo.register(Packets.Packet06PayloadRequest.class);
        lKryo.register(Packets.Packet07PayloadResponse.class);
        lKryo.register(Packets.Packet08NoWorkAvailable.class);
        lKryo.register(Packets.Packet09NotifyBusy.class);
        lKryo.register(Packets.Packet10NotifyFree.class);
        lKryo.register(Packets.Packet11ProgressUpdate.class);
        lKryo.register(Packets.Packet12JobComplete.class);
        lKryo.register(Packets.Packet13GammeLogMsg.class);
        lKryo.register(Packets.Packet14GammeLogErr.class);
    }

    static boolean isWindows() {
        String OS = System.getProperty("os.name").toLowerCase();
        return (OS.contains("win"));
    }

    static boolean isMac() {
        String OS = System.getProperty("os.name").toLowerCase();
        return (OS.contains("mac"));
    }

    static boolean isUnix() {
        String OS = System.getProperty("os.name").toLowerCase();
        return (OS.contains("nix") || OS.contains("nux") || OS.indexOf("aix") > 0);
    }
}