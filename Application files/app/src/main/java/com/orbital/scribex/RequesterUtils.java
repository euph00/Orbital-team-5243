package com.orbital.scribex;

import android.util.Log;

import java.io.File;
import java.io.FileNotFoundException;
import java.net.URL;
import java.net.URLConnection;
import java.util.Scanner;

public class RequesterUtils {
    /**
     * sends request to backend to begin transcription
     * @param req   request id, currently implemented as user id
     */
    static boolean sendRequest(String req) {
        try {
            URL url = new URL("http://34.143.147.223/app/" + req);
            URLConnection conn = url.openConnection();
            conn.connect();
            Object content = conn.getContent();

            return true;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    /**
     * Reads txt file into a String
     * @param file  File object, the txt file to be read
     * @return  String of the file contents
     */
    static String readFile(File file) {
        try {
            Scanner sc = new Scanner(file);
            StringBuffer sb = new StringBuffer();
            while (sc.hasNextLine()) sb.append(sc.nextLine());
            return sb.toString();
        } catch (FileNotFoundException e) {
            return null;
        }
    }
}
