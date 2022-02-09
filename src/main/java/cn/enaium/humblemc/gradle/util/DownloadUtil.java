package cn.enaium.humblemc.gradle.util;

import org.apache.commons.io.IOUtils;

import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.StandardCharsets;

/**
 * @author Enaium
 */
public class DownloadUtil {
    public static String readString(String link) {
        return new String(readFile(link), StandardCharsets.UTF_8);
    }

    public static byte[] readFile(String link) {
        byte[] bytes = null;
        try {
            URL url = new URL(link);
            URLConnection urlConnection = url.openConnection();
            HttpURLConnection connection = null;
            if (urlConnection instanceof HttpURLConnection) {
                connection = (HttpURLConnection) urlConnection;
            }

            if (connection == null) {
                throw new NullPointerException(String.format("Link: '%s' fail", link));
            }

            bytes = IOUtils.toByteArray(connection.getInputStream());
        } catch (Exception e) {
            e.printStackTrace();
        }
        return bytes;
    }
}
