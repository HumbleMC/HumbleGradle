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

import cn.enaium.humblemc.gradle.util.GameUtil;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.gradle.api.Project;
import org.gradle.api.tasks.TaskAction;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Objects;

/**
 * @author Enaium
 */
public class GenerateIdeaRunTask extends Task {
    @TaskAction
    public void generateIdeaRun() {
        StringBuilder vmArgs = new StringBuilder("-Djava.library.path=" + GameUtil.getNativeFileDir(extension).getAbsolutePath());
        StringBuilder programArgs = new StringBuilder();

        if (extension.minecraft.tweakClasses != null && !extension.minecraft.tweakClasses.isEmpty()) {
            for (String tweakClass : extension.minecraft.tweakClasses) {
                programArgs.append("--tweakClass").append(" ").append(tweakClass).append(" ");
            }
        }

        File runDir = getProject().getRootProject().file(extension.minecraft.runDir);
        if (!runDir.exists()) {
            runDir.mkdir();
        }

        programArgs.append("--gameDir").append(" ").append(runDir.getAbsolutePath()).append(" ");
        programArgs.append("--assetsDir").append(" ").append(GameUtil.getClientAssetDir(extension)).append(" ");
        programArgs.append("--assetIndex").append(" ").append(extension.minecraft.version).append(" ");
        programArgs.append("--version").append(" ").append("humble").append(" ");
        programArgs.append("--accessToken").append(" ").append("0").append(" ");

        try {
            String idea = IOUtils.toString(Objects.requireNonNull(GenerateIdeaRunTask.class.getResourceAsStream("/IDEA_RUN_CONFIGURATION.xml")), StandardCharsets.UTF_8);
            idea = idea.replace("%NAME%", "Humble Client Run");
            idea = idea.replace("%MAIN_CLASS%", extension.minecraft.mainClass);
            idea = idea.replace("%IDEA_MODULE%", getModule());
            idea = idea.replace("%PROGRAM_ARGS%", programArgs.toString().replaceAll("\"", "&quot;"));
            idea = idea.replace("%VM_ARGS%", vmArgs.toString().replaceAll("\"", "&quot;"));
            idea = idea.replace("%RUN_DIR%", extension.minecraft.runDir);

            String projectPath = getProject() == getProject().getRootProject() ? "" : getProject().getPath().replace(':', '_');
            File projectDir = getProject().getRootProject().file(".idea");
            File runConfigsDir = new File(projectDir, "runConfigurations");
            File runConfigs = new File(runConfigsDir, "Humble_Client_Run" + projectPath + ".xml");
            if (!runConfigsDir.exists()) {
                runConfigsDir.mkdirs();
            }
            FileUtils.write(runConfigs, idea, StandardCharsets.UTF_8);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private String getModule() {
        Project project = getProject();
        StringBuilder module = new StringBuilder(project.getName() + ".main");

        while ((project = project.getParent()) != null) {
            module.insert(0, project.getName() + ".");
        }
        return module.toString();
    }
}
