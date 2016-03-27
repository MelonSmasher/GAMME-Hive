package com.melonsmasher.hivegamme;

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