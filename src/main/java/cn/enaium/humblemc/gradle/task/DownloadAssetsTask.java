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

package cn.enaium.humblemc.gradle.task;

import cn.enaium.humblemc.gradle.util.DownloadUtil;
import cn.enaium.humblemc.gradle.util.GameUtil;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.gradle.api.tasks.TaskAction;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * @author Enaium
 */
public class DownloadAssetsTask extends Task {

    @TaskAction
    public void downloadAssets() {
        String asset = GameUtil.getClientAsset(extension);
        File index = GameUtil.getClientIndexFile(extension);

        //download index
        try {
            if (!GameUtil.fileVerify(index, GameUtil.getClientAssetSha1(extension))) {
                FileUtils.write(index, asset, StandardCharsets.UTF_8);
            }
        } catch (IOException e) {
            getProject().getLogger().lifecycle(e.getMessage(), e);
        }


        //get objects
        HashMap<String, AssetObject> objectHashMap = new HashMap<>();
        for (Map.Entry<String, JsonElement> objects : new Gson().fromJson(asset, JsonObject.class).get("objects").getAsJsonObject().entrySet()) {
            objectHashMap.put(objects.getKey(), new Gson().fromJson(objects.getValue(), AssetObject.class));
        }

        //download object
        objectHashMap.forEach((name, assetObject) -> {
            File local = GameUtil.getLocalClientObjectFile(assetObject.getHash());
            File objectFile = GameUtil.getClientObjectFile(extension, assetObject.getHash());

            try {
                if (!GameUtil.fileVerify(objectFile, assetObject.getHash())) {
                    if (GameUtil.fileVerify(local, assetObject.getHash())) {
                        try {
                            FileUtils.copyFile(local, objectFile);
                        } catch (IOException e) {
                            getProject().getLogger().lifecycle(e.getMessage(), e);
                        }
                    } else {
                        FileUtils.writeByteArrayToFile(objectFile, DownloadUtil.readFile(extension.minecraft.assets + "/" + assetObject.getHash().substring(0, 2) + "/" + assetObject.getHash()));
                    }
                }
            } catch (IOException e) {
                getProject().getLogger().lifecycle(e.getMessage(), e);
            }
        });


        //download native
        File nativeJarDir = GameUtil.getNativeJarDir(extension);
        File nativeFileDir = GameUtil.getNativeFileDir(extension);

        GameUtil.getNatives(extension).forEach(link -> {
            String name = link.substring(link.lastIndexOf("/") + 1);
            File nativeJarFile = new File(nativeJarDir, name);
            if (!nativeJarFile.exists()) {
                try {
                    FileUtils.writeByteArrayToFile(nativeJarFile, DownloadUtil.readFile(link));
                } catch (IOException e) {
                    getProject().getLogger().lifecycle(e.getMessage(), e);
                }
            }

            try {
                ZipFile zipFile = new ZipFile(nativeJarFile);

                Enumeration<? extends ZipEntry> entries = zipFile.entries();
                while (entries.hasMoreElements()) {
                    ZipEntry zipEntry = entries.nextElement();
                    if (zipEntry.isDirectory() || zipEntry.getName().contains("META-INF"))
                        continue;
                    FileUtils.writeByteArrayToFile(new File(nativeFileDir, zipEntry.getName()), IOUtils.toByteArray(zipFile.getInputStream(zipEntry)));
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
    }


    public static class AssetObject {
        private String hash;
        private long size;

        public String getHash() {
            return this.hash;
        }

        public long getSize() {
            return this.size;
        }
    }
}
