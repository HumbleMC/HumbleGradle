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
