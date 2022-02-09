package cn.enaium.humblemc.gradle.task;

import cn.enaium.humblemc.gradle.util.GameUtil;
import org.apache.commons.io.FileUtils;
import org.gradle.api.tasks.TaskAction;

import java.io.File;
import java.io.IOException;

/**
 * @author Enaium
 */
public class DownloadGameTask extends Task {
    @TaskAction
    public void download() {
        File clientJar = GameUtil.getClientFile(extension);
        try {
            if (!GameUtil.fileVerify(clientJar, GameUtil.getClientJarSha1(extension))) {
                File localJar = GameUtil.getLocalJar(extension.minecraft.version);
                if (GameUtil.fileVerify(localJar, GameUtil.getClientJarSha1(extension))) {
                    getLogger().info(String.format("Copy local client.jar form %s", clientJar.getAbsolutePath()));
                    FileUtils.copyFile(localJar, clientJar);
                } else {
                    getLogger().info("Download client.jar");
                    FileUtils.writeByteArrayToFile(clientJar, GameUtil.getClientJar(extension));
                }
            }
        } catch (IOException e) {
            getProject().getLogger().lifecycle(e.getMessage(), e);
        }

//        File serverJar = GameUtil.getServerFile(extension);
//        if (!GameUtil.fileVerify(serverJar, GameUtil.getServerJarSha1(extension))) {
//            getLogger().info("Download server.jar");
//            try {
//                FileUtils.writeByteArrayToFile(serverJar,GameUtil.getServerJar(extension));
//            } catch (IOException e) {
//                getProject().getLogger().lifecycle(e.getMessage(), e);
//            }
//        }
    }
}
