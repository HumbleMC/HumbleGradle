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
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import org.apache.commons.io.FileUtils;
import org.gradle.api.plugins.JavaPluginExtension;
import org.gradle.api.tasks.SourceSet;
import org.gradle.api.tasks.TaskAction;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AnnotationNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodNode;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.*;

/**
 * @author Enaium
 */
public class ObfuscationTask extends Task {
    @TaskAction
    public void obfuscation() {
        File classes = new File(getProject().getBuildDir(), "classes");

        MappingUtil mappingUtil = MappingUtil.getInstance(GameUtil.getClientMappingFile(extension));
        RemappingUtil remappingUtil = RemappingUtil.getInstance("obfuscation", mappingUtil.getMap(false));
        remappingUtil.analyzeJar(GameUtil.getClientCleanFile(extension));

        JsonObject mixinReferenceMap = new JsonObject();
        JsonObject mixinMappings = new JsonObject();

        if (extension.mixin.referenceMap != null) {
            getProject().getLogger().lifecycle("Generate mixin reference map");

            try {
                Files.walkFileTree(classes.toPath(), new SimpleFileVisitor<Path>() {
                    @Override
                    public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                        byte[] bytes = FileUtils.readFileToByteArray(file.toFile());
                        MixinMapping mixinMapping = new MixinMapping();
                        mixinMapping.accept(bytes);
                        remappingUtil.superHashMap.put(mixinMapping.className, new HashSet<>(mixinMapping.mixins));
                        for (String mixin : mixinMapping.mixins) {
                            JsonObject mapping = new JsonObject();

                            mixinMapping.methods.forEach((descriptor, methods) -> {
                                for (String method : methods) {
                                    if (method.contains("(")) {
                                        mapping.addProperty(method, getMethodObf(mappingUtil.methodCleanToObfMap, mixin, method, false));
                                    } else {
                                        mapping.addProperty(method, getMethodObf(mappingUtil.methodCleanToObfMap, mixin, method + descriptor.replace("Lorg/spongepowered/asm/mixin/injection/callback/CallbackInfo;", ""), false));
                                    }
                                }
                            });

                            for (String mixinTarget : mixinMapping.targets) {
                                if (!mixinTarget.contains("field:")) {
                                    String targetClass = mixinTarget.substring(1, mixinTarget.indexOf(";"));

                                    String targetMethod = getMethodObf(mappingUtil.methodCleanToObfMap, targetClass, mixinTarget.substring(mixinTarget.indexOf(";") + 1), false);
                                    if (targetMethod == null) {
                                        continue;
                                    }
                                    mapping.addProperty(mixinTarget, targetMethod);
                                } else {
                                    String left = mixinTarget.split("field:")[0];
                                    String right = mixinTarget.split("field:")[1];
                                    String targetClass = mappingUtil.classCleanToObfMap.get(left.substring(1, left.indexOf(";")));
                                    String targetField = mappingUtil.classCleanToObfMap.get(right.substring(1, right.indexOf(";")));

                                    if (targetClass == null || targetField == null) {
                                        continue;
                                    }

                                    mapping.addProperty(mixinTarget, "L" + targetClass + ";field:L" + targetField + ";");
                                }
                            }

                            for (Map.Entry<String, String> entry : mixinMapping.accessors.entrySet()) {

                                String fieldName = mappingUtil.fieldCleanToObfMap.get(mixin + "/" + entry.getValue());

                                if (fieldName == null) {
                                    continue;
                                }

                                if (entry.getKey().contains(";")) {
                                    String arg;
                                    if (!entry.getKey().contains(")V")) {
                                        arg = entry.getKey().substring(entry.getKey().lastIndexOf(")") + 1);
                                    } else {
                                        arg = entry.getKey().substring(entry.getKey().indexOf("(") + 1, entry.getKey().lastIndexOf(")"));
                                    }

                                    arg = arg.substring(1, arg.lastIndexOf(";"));
                                    arg = mappingUtil.classCleanToObfMap.get(arg);
                                    if (arg == null) {
                                        continue;
                                    }
                                    mapping.addProperty(entry.getValue(), fieldName.split("/")[1] + ":L" + arg + ";");
                                } else {
                                    mapping.addProperty(entry.getValue(), entry.getKey());
                                }
                            }

                            for (Map.Entry<String, String> entry : mixinMapping.invokes.entrySet()) {
                                mapping.addProperty(entry.getValue(), getMethodObf(mappingUtil.methodCleanToObfMap, mixin, entry.getValue() + entry.getKey(), false));
                            }

                            mixinMappings.add(mixinMapping.className, mapping);
                        }
                        return super.visitFile(file, attrs);
                    }
                });
            } catch (IOException e) {
                getProject().getLogger().lifecycle(e.getMessage(), e);
            }
            mixinReferenceMap.add("mappings", mixinMappings);
        }

        JavaPluginExtension java = getProject().getExtensions().getByType(JavaPluginExtension.class);
        File resourceDir = new File(getProject().getBuildDir(), "resources");
        for (SourceSet sourceSet : java.getSourceSets()) {
            if (!resourceDir.exists()) {
                resourceDir.mkdir();
            }
            File dir = new File(resourceDir, sourceSet.getName());
            if (!dir.exists()) {
                dir.mkdir();
            }

            if (extension.mixin.referenceMap != null) {
                try {
                    FileUtils.write(new File(dir, extension.mixin.referenceMap), new GsonBuilder().setPrettyPrinting().create().toJson(mixinReferenceMap), StandardCharsets.UTF_8);
                } catch (IOException e) {
                    getProject().getLogger().lifecycle(e.getMessage(), e);
                }
            }
        }

        getProject().getLogger().lifecycle("Obfuscation");

        try {
            Files.walkFileTree(classes.toPath(), new SimpleFileVisitor<Path>() {
                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                    remappingUtil.analyze(FileUtils.readFileToByteArray(file.toFile()));
                    return super.visitFile(file, attrs);
                }
            });

            Files.walkFileTree(classes.toPath(), new SimpleFileVisitor<Path>() {
                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                    Files.write(file, remappingUtil.remapping(FileUtils.readFileToByteArray(file.toFile())));
                    return super.visitFile(file, attrs);
                }
            });
        } catch (IOException e) {
            getProject().getLogger().lifecycle(e.getMessage(), e);
        }
    }

    private String getMethodObf(Map<String, String> methodCleanToObfMap, String klass, String method, boolean only) {
        String methodName = method.substring(0, method.indexOf("("));
        String methodDescriptor = method.substring(method.indexOf("("));
        String methodObf = methodCleanToObfMap.get(klass + "." + methodName + " " + methodDescriptor);
        if (methodObf == null) {
            return null;
        }

        if (!only) {
            methodObf = "L" + methodObf.split(" ")[0].replace(".", ";") + methodObf.split(" ")[1];
        } else {
            methodObf = methodObf.split(" ")[0];
            methodObf = methodObf.substring(methodObf.lastIndexOf(".") + 1);
        }
        return methodObf;
    }

    public static class MixinMapping {
        public final List<String> mixins = new ArrayList<>();
        public final List<String> targets = new ArrayList<>();
        public final HashMap<String, String> invokes = new HashMap<>();
        public final HashMap<String, String> accessors = new HashMap<>();
        public final HashMap<String, List<String>> methods = new HashMap<>();
        public String className = null;

        public void accept(byte[] basic) {
            ClassReader classReader = new ClassReader(basic);
            ClassNode classNode = new ClassNode();
            classReader.accept(classNode, 0);

            if (classNode.invisibleAnnotations == null) {
                return;
            }

            for (AnnotationNode invisibleAnnotation : classNode.invisibleAnnotations) {
                if (invisibleAnnotation.desc.equals("Lorg/spongepowered/asm/mixin/Mixin;")) {
                    className = classNode.name;
                    List<Type> values = getAnnotationValue(invisibleAnnotation, "value");
                    if (values != null) {
                        for (Type type : values) {
                            mixins.add(type.getClassName().replace(".", "/"));
                        }
                    }
                    List<String> targets = getAnnotationValue(invisibleAnnotation, "targets");
                    if (targets != null) {
                        for (String target : targets) {
                            mixins.add(target.replace(".", "/"));
                        }
                    }
                }
            }

            if (className == null) {
                return;
            }

            for (MethodNode methodNode : classNode.methods) {

                if (methodNode.visibleAnnotations == null) {
                    continue;
                }

                for (AnnotationNode visibleAnnotation : methodNode.visibleAnnotations) {
                    if (visibleAnnotation.desc.equals("Lorg/spongepowered/asm/mixin/injection/Inject;")
                            || visibleAnnotation.desc.equals("Lorg/spongepowered/asm/mixin/injection/Redirect;")) {

                        List<String> method = getAnnotationValue(visibleAnnotation, "method");
                        if (method != null) {
                            methods.put(methodNode.desc, method);
                        }

                        List<AnnotationNode> at = getAnnotationValue(visibleAnnotation, "at");
                        if (at != null) {
                            String target = getAnnotationValue(visibleAnnotation, "target");
                            if (target != null) {
                                targets.add(target);
                            }
                        }
                    }

                    if (visibleAnnotation.desc.equals("Lorg/spongepowered/asm/mixin/gen/Invoker;")) {
                        String value = getAnnotationValue(visibleAnnotation, "value");
                        if (value != null) {
                            invokes.put(methodNode.desc, value);
                        }
                    }

                    if (visibleAnnotation.desc.equals("Lorg/spongepowered/asm/mixin/gen/Accessor;")) {
                        String value = getAnnotationValue(visibleAnnotation, "value");
                        if (value != null) {
                            accessors.put(methodNode.desc, value);
                        }
                    }
                }
            }
        }

        @SuppressWarnings("unchecked")
        public static <T> T getAnnotationValue(AnnotationNode annotationNode, String key) {
            boolean getNextValue = false;

            if (annotationNode.values == null) {
                return null;
            }

            for (Object value : annotationNode.values) {
                if (getNextValue) {
                    return (T) value;
                }
                if (value.equals(key)) {
                    getNextValue = true;
                }
            }

            return null;
        }
    }
}
