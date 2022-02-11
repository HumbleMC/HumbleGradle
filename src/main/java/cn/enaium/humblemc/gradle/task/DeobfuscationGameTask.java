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
