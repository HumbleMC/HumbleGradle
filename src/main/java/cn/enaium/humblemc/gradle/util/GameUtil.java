package cn.enaium.humblemc.gradle.util;

import cn.enaium.humblemc.gradle.HumbleGradleExtension;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.util.*;

import static cn.enaium.humblemc.gradle.util.DownloadUtil.readFile;
import static cn.enaium.humblemc.gradle.util.DownloadUtil.readString;

/**
 * @author Enaium
 */
public class GameUtil {
    public static File getMinecraftDir() {
        File minecraftFolder;
        if (getOsName().contains("win")) {
            minecraftFolder = new File(System.getenv("APPDATA"), File.separator + ".minecraft");
        } else if (getOsName().contains("mac")) {
            minecraftFolder = new File(System.getProperty("user.home"), File.separator + "Library" + File.separator + "Application Support" + File.separator + "minecraft");
        } else {
            minecraftFolder = new File(System.getProperty("user.home"), File.separator + ".minecraft");
        }
        return minecraftFolder;
    }

    private static String getOsName() {
        return System.getProperty("os.name").toLowerCase(Locale.ROOT);
    }

    public static String getJson(HumbleGradleExtension extension) {
        String jsonUrl = "";
        for (JsonElement jsonElement : new Gson().fromJson(readString(extension.minecraft.manifest), JsonObject.class).get("versions").getAsJsonArray()) {
            if (jsonElement.getAsJsonObject().get("id").getAsString().equals(extension.minecraft.version)) {
                jsonUrl = jsonElement.getAsJsonObject().get("url").getAsString();
            }
        }
        return readString(jsonUrl);
    }


    public static List<String> getLibraries(HumbleGradleExtension extension) {
        LinkedHashMap<String, String> list = new LinkedHashMap<>();
        for (JsonElement jsonElement : new Gson().fromJson(getJson(extension), JsonObject.class).get("libraries").getAsJsonArray()) {
            if (jsonElement.getAsJsonObject().has("natives")) {
                continue;
            }
            String name = jsonElement.getAsJsonObject().get("name").getAsString();
            list.put(name.substring(0, name.lastIndexOf(":")), name.substring(name.lastIndexOf(":")));
        }
        List<String> libraries = new ArrayList<>();
        for (Map.Entry<String, String> entry : list.entrySet()) {
            libraries.add(entry.getKey() + entry.getValue());
        }
        return libraries;
    }

    public static List<String> getNatives(HumbleGradleExtension extension) {
        List<String> libraries = new ArrayList<>();

        for (JsonElement jsonElement : new Gson().fromJson(getJson(extension), JsonObject.class).get("libraries").getAsJsonArray()) {
            JsonObject downloads = jsonElement.getAsJsonObject().get("downloads").getAsJsonObject();
            if (downloads.has("classifiers")) {
                String name = "natives-linux";
                if (getOsName().contains("win")) {
                    name = "natives-windows";
                } else if (getOsName().contains("mac")) {
                    name = "natives-macos";
                }
                JsonObject classifiers = downloads.get("classifiers").getAsJsonObject();
                if (classifiers.has(name)) {
                    libraries.add(downloads.get("classifiers").getAsJsonObject().get(name).getAsJsonObject().get("url").getAsString());
                }
            }
        }
        return libraries;
    }

    public static File getLocalJar(String version) {
        return new File(getMinecraftDir(), "versions" + File.separator + version + File.separator + version + ".jar");
    }

    public static File getClientNativeDir(HumbleGradleExtension extension) {
        File file = new File(getGameDir(extension), extension.minecraft.version + File.separator + extension.minecraft.version + "-native");
        if (!file.exists()) {
            file.mkdir();
        }
        return file;
    }

    public static File getNativeJarDir(HumbleGradleExtension extension) {
        File nativeJarDir = new File(GameUtil.getClientNativeDir(extension), "jars");
        if (!nativeJarDir.exists()) {
            nativeJarDir.mkdir();
        }
        return nativeJarDir;
    }

    public static File getNativeFileDir(HumbleGradleExtension extension) {
        File nativeFileDir = new File(GameUtil.getClientNativeDir(extension), "natives");
        if (!nativeFileDir.exists()) {
            nativeFileDir.mkdir();
        }
        return nativeFileDir;
    }

    public static File getClientFile(HumbleGradleExtension extension) {
        return new File(getGameDir(extension), extension.minecraft.version + File.separator + extension.minecraft.version + "-client.jar");
    }

    public static File getServerFile(HumbleGradleExtension extension) {
        return new File(getGameDir(extension), extension.minecraft.version + File.separator + extension.minecraft.version + "-server.jar");
    }

    public static File getClientCleanFile(HumbleGradleExtension extension) {
        return new File(getGameDir(extension), extension.minecraft.version + File.separator + extension.minecraft.version + "-client-clean.jar");
    }

    public static File getServerCleanFile(HumbleGradleExtension extension) {
        return new File(getGameDir(extension), extension.minecraft.version + File.separator + extension.minecraft.version + "-server-clean.jar");
    }

    public static File getClientCleanSourceFile(HumbleGradleExtension extension) {
        return new File(getGameDir(extension), extension.minecraft.version + File.separator + extension.minecraft.version + "-client-clean-source.jar");
    }

    public static File getClientServerSourceFile(HumbleGradleExtension extension) {
        return new File(getGameDir(extension), extension.minecraft.version + File.separator + extension.minecraft.version + "-client-server-source.jar");
    }

    public static File getMappingDir(HumbleGradleExtension extension) {
        File mapping = new File(extension.getUserCache(), "mapping");
        if (!mapping.exists()) {
            mapping.mkdir();
        }
        return mapping;
    }

    public static File getClientMappingFile(HumbleGradleExtension extension) {
        File file = new File(getMappingDir(extension), extension.minecraft.version + "-client.txt");
        if (!file.exists()) {
            try {
                file.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return file;
    }

    public static File getServerMappingFile(HumbleGradleExtension extension) {
        File file = new File(getMappingDir(extension), extension.minecraft.version + "-server.txt");
        if (!file.exists()) {
            try {
                file.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return file;
    }

    public static File getGameDir(HumbleGradleExtension extension) {
        File game = new File(extension.getUserCache(), "game");
        if (!game.exists()) {
            game.mkdir();
        }
        return game;
    }

    public static JsonObject getDownloadsJson(HumbleGradleExtension extension) {
        return new Gson().fromJson(getJson(extension), JsonObject.class).get("downloads").getAsJsonObject();
    }

    public static String getClientJarSha1(HumbleGradleExtension extension) {
        return getDownloadsJson(extension).getAsJsonObject().get("client").getAsJsonObject().get("sha1").getAsString();
    }

    public static String getServerJarSha1(HumbleGradleExtension extension) {
        return getDownloadsJson(extension).getAsJsonObject().get("server").getAsJsonObject().get("sha1").getAsString();
    }

    public static byte[] getClientJar(HumbleGradleExtension extension) {
        return readFile(getDownloadsJson(extension).getAsJsonObject().get("client").getAsJsonObject().get("url").getAsString());
    }

    public static byte[] getServerJar(HumbleGradleExtension extension) {
        return readFile(getDownloadsJson(extension).getAsJsonObject().get("server").getAsJsonObject().get("url").getAsString());
    }

    public static String getClientMapping(HumbleGradleExtension extension) {
        return readString(getDownloadsJson(extension).getAsJsonObject().get("client_mappings").getAsJsonObject().get("url").getAsString());
    }

    public static String getServerMapping(HumbleGradleExtension extension) {
        return readString(getDownloadsJson(extension).getAsJsonObject().get("server_mappings").getAsJsonObject().get("url").getAsString());
    }

    public static String getClientMappingSha1(HumbleGradleExtension extension) {
        return getDownloadsJson(extension).getAsJsonObject().get("client_mappings").getAsJsonObject().get("sha1").getAsString();
    }

    public static String getServerMappingSha1(HumbleGradleExtension extension) {
        return getDownloadsJson(extension).getAsJsonObject().get("server_mappings").getAsJsonObject().get("sha1").getAsString();
    }

    public static File getClientAssetDir(HumbleGradleExtension extension) {
        File assets = new File(extension.getUserCache(), "assets");
        if (!assets.exists()) {
            assets.mkdir();
        }
        return assets;
    }

    public static File getClientIndexDir(HumbleGradleExtension extension) {
        File index = new File(getClientAssetDir(extension), "indexes");
        if (!index.exists()) {
            index.mkdir();
        }
        return index;
    }

    public static File getClientIndexFile(HumbleGradleExtension extension) {
        File file = new File(GameUtil.getClientIndexDir(extension), extension.minecraft.version + ".json");
        if (!file.exists()) {
            try {
                file.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return file;
    }

    public static File getClientObjectFile(HumbleGradleExtension extension, String name) {
        File file = new File(GameUtil.getClientObjectDir(extension), name.substring(0, 2));
        if (!file.exists()) {
            file.mkdir();
        }
        return new File(file, name);
    }

    public static File getLocalClientObjectFile(String name) {
        File file = new File(GameUtil.getLocalClientObjectDir(), name.substring(0, 2));
        if (!file.exists()) {
            file.mkdir();
        }
        return new File(file, name);
    }


    public static File getClientObjectDir(HumbleGradleExtension extension) {
        File index = new File(getClientAssetDir(extension), "objects");
        if (!index.exists()) {
            index.mkdir();
        }
        return index;
    }

    public static File getLocalClientObjectDir() {
        return new File(getMinecraftDir(), "assets" + File.separator + "objects");
    }

    public static File getClientSkinDir(HumbleGradleExtension extension) {
        File index = new File(getClientAssetDir(extension), "skins");
        if (!index.exists()) {
            index.mkdir();
        }
        return index;
    }

    public static String getClientAsset(HumbleGradleExtension extension) {
        return readString(new Gson().fromJson(getJson(extension), JsonObject.class).get("assetIndex").getAsJsonObject().get("url").getAsString());
    }

    public static String getClientAssetSha1(HumbleGradleExtension extension) {
        return new Gson().fromJson(getJson(extension), JsonObject.class).get("assetIndex").getAsJsonObject().get("sha1").getAsString();
    }

    public static boolean fileVerify(File file, String sha1) {
        if (!file.exists()) {
            return false;
        }
        try {
            return DigestUtils.sha1Hex(FileUtils.readFileToByteArray(file)).toLowerCase(Locale.ROOT).equals(sha1.toLowerCase(Locale.ROOT));
        } catch (IOException e) {
            e.printStackTrace();
        }
        return false;
    }
}
