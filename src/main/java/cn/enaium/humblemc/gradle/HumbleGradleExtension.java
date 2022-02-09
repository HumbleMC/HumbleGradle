package cn.enaium.humblemc.gradle;

import groovy.lang.Closure;
import groovy.lang.GroovyObjectSupport;
import org.gradle.api.NamedDomainObjectContainer;
import org.gradle.api.Project;

import java.io.File;
import java.util.Collections;
import java.util.Set;

/**
 * @author Enaium
 */
public class HumbleGradleExtension {
    /**
     * Minecraft configuration
     */
    public MinecraftConfiguration minecraft = new MinecraftConfiguration();

    /**
     * Mixin configuration
     */
    public MixinConfiguration mixin = new MixinConfiguration();

    private final Project project;

    public HumbleGradleExtension(Project project) {
        this.project = project;
    }

    public File getUserCache() {
        File userCache = new File(project.getGradle().getGradleUserHomeDir(), "caches" + File.separator + "humblemc");

        if (!userCache.exists()) {
            userCache.mkdirs();
        }

        return userCache;
    }

    public MinecraftConfiguration minecraft(Closure closure) {
        project.configure(minecraft, closure);
        return minecraft;
    }

    public MixinConfiguration mixin(Closure closure) {
        project.configure(mixin, closure);
        return mixin;
    }

    public static class MinecraftConfiguration extends GroovyObjectSupport {
        /**
         * Version manifest url
         */
        public String manifest = "https://launchermeta.mojang.com/mc/game/version_manifest.json";

        /**
         * Resources url
         */
        public String assets = "https://resources.download.minecraft.net";

        /**
         * Libraries manifest url
         */
        public String libraries = "https://libraries.minecraft.net/";

        /**
         * run dir
         */
        public String runDir = "run";

        /**
         * Game version
         */
        public String version = null;

        /**
         * Main class
         */
        public String mainClass = "net.minecraft.launchwrapper.Launch";

        /**
         * Tweak classes
         */
        public Set<String> tweakClasses = Collections.emptySet();
    }

    public static class MixinConfiguration {
        /**
         * Reference map
         */
        public String referenceMap = null;
    }
}
