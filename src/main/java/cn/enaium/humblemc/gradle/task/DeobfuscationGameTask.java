package cn.enaium.humblemc.gradle.task;

import cn.enaium.humblemc.gradle.util.GameUtil;
import cn.enaium.humblemc.gradle.util.MappingUtil;
import cn.enaium.humblemc.gradle.util.RemappingUtil;
import org.gradle.api.tasks.TaskAction;

import java.io.File;
import java.io.IOException;

/**
 * @author Enaium
 */
public class DeobfuscationGameTask extends Task {
    @TaskAction
    public void deobfuscationGame() {
        File clientCleanFile = GameUtil.getClientCleanFile(extension);
        File clientFile = GameUtil.getClientFile(extension);

        if (clientFile.exists()) {
            try {
                MappingUtil clientMappingUtil = MappingUtil.getInstance(GameUtil.getClientMappingFile(extension));
                RemappingUtil clientRemappingUtil = RemappingUtil.getInstance("deobfuscation", clientMappingUtil.getMap(true));
                clientRemappingUtil.analyzeJar(clientFile);
                clientRemappingUtil.remappingJar(clientFile, clientCleanFile);
            } catch (IOException e) {
                getProject().getLogger().lifecycle(e.getMessage(), e);
            }
        }
    }
}
