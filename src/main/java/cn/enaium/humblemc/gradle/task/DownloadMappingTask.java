package cn.enaium.humblemc.gradle.task;

import cn.enaium.humblemc.gradle.util.GameUtil;
import org.apache.commons.io.FileUtils;
import org.gradle.api.tasks.TaskAction;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * @author Enaium
 */
public class DownloadMappingTask extends Task {
    @TaskAction
    public void downloadMapping() {
        File clientMappingFile = GameUtil.getClientMappingFile(extension);
        try {
            if (!GameUtil.fileVerify(clientMappingFile, GameUtil.getClientMappingSha1(extension))) {
                getProject().getLogger().lifecycle("Download client.txt");
                FileUtils.write(clientMappingFile, GameUtil.getClientMapping(extension), StandardCharsets.UTF_8);
            }
        } catch (IOException e) {
            getProject().getLogger().lifecycle(e.getMessage(), e);
        }
    }
}
