/**
 * Copyright (C) 2022 Enaium
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

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
