package com.melonsmasher.hivegamme;

import com.esotericsoftware.kryo.Kryo;

import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;

/**
 * Created by melon on 3/27/16.
 */
class Util {

    /**
     * A class full of various helper methods.
     */
    Util() {

    }

    /**
     * Reads a file into a string.
     *
     * @param path     The path of the target file
     * @param encoding The file encoding.
     * @return The contents of the target file in a String.
     * @throws IOException
     */
    static String readFile(String path, Charset encoding)
            throws IOException {
        byte[] encoded = Files.readAllBytes(Paths.get(path));
        return new String(encoded, encoding);
    }

    /**
     * Registers all known packet types with Kryo
     *
     * @param lKryo The current instance of Kryo. Usually mClient.getKryo() or mServer.getKryo().
     */
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
        lKryo.register(Packets.Packet13LogInfo.class);
        lKryo.register(Packets.Packet14LogError.class);
        lKryo.register(Packets.Packet15LogWarning.class);
    }


    /**
     * The path that where your config directory should be based on the underlying OS.
     *
     * @return null || Config directory path.
     */
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

    static String defaultLogDir() {
        if (isWindows()) {
            return "C:\\Program Files\\GAMME Hive\\log\\";
        } else if (isMac()) {
            return "/var/log/hive/";
        } else if (isUnix()) {
            return "/var/log/hive/";
        } else {
            return null;
        }
    }

    /**
     * A simple method that creates a directory if it does not exists.
     *
     * @param dir_str The path of the directory.
     * @return A boolean value that should signify the directory's existence. If FALSE, you should assume that there was an issue while creating it.
     */
    static boolean mkdir(String dir_str) {
        File dir = new File(dir_str);
        if (!dir.exists()) {
            try {
                dir.mkdir();
                return true;
            } catch (SecurityException se) {
                se.printStackTrace();
                return false;
            }
        } else {
            return true;
        }
    }

    /**
     * A method that is used to generate the hive member's name.
     *
     * @return The machines name or the epoch if the name could not be determined.
     */
    static String getName() {
        String name;
        try {
            InetAddress addr;
            addr = InetAddress.getLocalHost();
            name = addr.getHostName();
        } catch (UnknownHostException e) {
            name = String.valueOf(System.currentTimeMillis());
            e.printStackTrace();
        }
        return name;
    }

    /**
     * A simple wrapper method that tells us if the OS is Windows.
     *
     * @return True if the underlying OS is Windows, otherwise false.
     */
    private static boolean isWindows() {
        String OS = System.getProperty("os.name").toLowerCase();
        return (OS.contains("win"));
    }

    /**
     * A simple wrapper method that tells us if the OS is Mac OSX.
     *
     * @return True if the underlying OS is Mac OSX, otherwise false.
     */
    private static boolean isMac() {
        String OS = System.getProperty("os.name").toLowerCase();
        return (OS.contains("mac"));
    }

    /**
     * A simple wrapper method that tells us if the OS is Unix Like.
     *
     * @return True if the underlying OS is Unix Like, otherwise false.
     */
    private static boolean isUnix() {
        String OS = System.getProperty("os.name").toLowerCase();
        return (OS.contains("nix") || OS.contains("nux") || OS.indexOf("aix") > 0);
    }
}