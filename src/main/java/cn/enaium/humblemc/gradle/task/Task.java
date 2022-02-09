package cn.enaium.humblemc.gradle.task;

import cn.enaium.humblemc.gradle.HumbleGradleExtension;
import org.gradle.api.DefaultTask;

/**
 * @author Enaium
 */
public class Task extends DefaultTask {
    public final HumbleGradleExtension extension;

    public Task() {
        //get extension
        extension = getProject().getExtensions().getByType(HumbleGradleExtension.class);
        setGroup("humble");
    }
}
