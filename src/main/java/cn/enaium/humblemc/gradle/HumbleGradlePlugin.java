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

package cn.enaium.humblemc.gradle;

import cn.enaium.humblemc.gradle.task.*;
import cn.enaium.humblemc.gradle.util.GameUtil;
import org.gradle.api.Plugin;
import org.gradle.api.Project;

/**
 * @author Enaium
 */
public class HumbleGradlePlugin implements Plugin<Project> {
    @Override
    public void apply(Project project1) {

        //Create extension
        HumbleGradleExtension extension = project1.getExtensions().create("humble", HumbleGradleExtension.class, project1);

        project1.afterEvaluate(project -> {

            if (extension.minecraft.version == null) {
                throw new NullPointerException("Extension minecraft version is null");
            }

            //Minecraft maven repository
            project.getRepositories().maven(mavenArtifactRepository -> {
                mavenArtifactRepository.setName("minecraft");
                mavenArtifactRepository.setUrl(extension.minecraft.libraries);
            });

            //Enaium maven repository
            project.getRepositories().maven(mavenArtifactRepository -> {
                mavenArtifactRepository.setName("enaium");
                mavenArtifactRepository.setUrl("https://maven.enaium.cn/");
            });

            //SpongePowered maven repository
            project.getRepositories().maven(mavenArtifactRepository -> {
                mavenArtifactRepository.setName("sponge");
                mavenArtifactRepository.setUrl("https://repo.spongepowered.org/repository/maven-public/");
            });

            //maven central repository
            project.getRepositories().mavenCentral();

            //maven local repository
            project.getRepositories().mavenLocal();

            //implementation minecraft libraries
            GameUtil.getLibraries(extension).forEach(library -> project.getDependencies().add("implementation", library));

            //java plugin
            project.getPlugins().apply("java");

            //idea plugin
            project.getPlugins().apply("idea");

            /*
            Task
             */

            //download mapping
            project.getTasks().create("downloadMapping", DownloadMappingTask.class);

            //download game
            project.getTasks().create("downloadGame", DownloadGameTask.class);

            //download assets
            project.getTasks().create("downloadAssets", DownloadAssetsTask.class);

            //deobfuscation game
            project.getTasks().create("deobfuscationGame", DeobfuscationGameTask.class);

            //obfuscation game
            project.getTasks().create("obfuscation", ObfuscationTask.class);

            //generate idea run configuration
            project.getTasks().create("generateIdeaRun", GenerateIdeaRunTask.class);

            //finalizedBy
            project.getTasks().getByName("idea")
                    .finalizedBy(project.getTasks().getByName("downloadMapping")
                            , project.getTasks().getByName("downloadGame")
                            , project.getTasks().getByName("downloadAssets")
                            , project.getTasks().getByName("deobfuscationGame")
                            , project.getTasks().getByName("generateIdeaRun")
                    );

            project.getTasks().getByName("compileJava").finalizedBy(project.getTasks().getByName("obfuscation"));

            //dependency
            project.getDependencies().add("compileOnly", project.getDependencies().create(project.files(GameUtil.getClientCleanFile(extension).getAbsolutePath())));
            project.getDependencies().add("runtimeOnly", project.getDependencies().create(project.files(GameUtil.getClientFile(extension).getAbsolutePath())));
        });
    }
}
