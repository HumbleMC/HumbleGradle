package cn.enaium.humblemc.gradle.util;

import org.apache.commons.io.IOUtils;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.commons.ClassRemapper;
import org.objectweb.asm.commons.SimpleRemapper;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.zip.ZipOutputStream;

import static org.objectweb.asm.Opcodes.ASM9;

/**
 * @author Enaium
 */
public class RemappingUtil {

    private static final ConcurrentHashMap<String, RemappingUtil> instanceMap = new ConcurrentHashMap<>();

    public static RemappingUtil getInstance(String name, Map<String, String> map) {
        if (!instanceMap.containsKey(name)) {
            instanceMap.put(name, new RemappingUtil(map));
        }
        return instanceMap.get(name);
    }

    private final Map<String, String> map;

    public final Map<String, Set<String>> superHashMap = new HashMap<>();

    private RemappingUtil(Map<String, String> map) {
        this.map = map;
    }

    public void analyzeJar(File inputFile) {
        try {
            JarFile jarFile = new JarFile(inputFile);
            Enumeration<JarEntry> entries = jarFile.entries();
            while (entries.hasMoreElements()) {
                JarEntry jarEntry = entries.nextElement();
                if (jarEntry.isDirectory())
                    continue;
                if (!jarEntry.getName().endsWith(".class"))
                    continue;

                analyze(IOUtils.toByteArray(jarFile.getInputStream(jarEntry)));

            }
            jarFile.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void analyze(byte[] bytes) {
        ClassReader classReader = new ClassReader(bytes);
        classReader.accept(new ClassVisitor(ASM9) {
            @Override
            public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
                Set<String> strings = new HashSet<>();
                if (superHashMap.containsKey(name)) {
                    if (superName != null) {
                        if (!superHashMap.get(name).contains(superName)) {
                            strings.add(superName);
                        }
                    }

                    if (interfaces != null) {
                        for (String inter : interfaces) {
                            if (!superHashMap.get(name).contains(inter)) {
                                strings.add(inter);
                            }
                        }
                    }
                    superHashMap.get(name).addAll(strings);
                } else {
                    if (superName != null) {
                        strings.add(superName);
                    }

                    if (interfaces != null) {
                        Collections.addAll(strings, interfaces);
                    }
                    superHashMap.put(name, strings);
                }
                super.visit(version, access, name, signature, superName, interfaces);
            }
        }, 0);
    }

    public void remappingJar(File inputFile, File outFile) throws IOException {
        analyzeJar(inputFile);
        ZipOutputStream jarOutputStream = new ZipOutputStream(new BufferedOutputStream(new FileOutputStream(outFile)), StandardCharsets.UTF_8);
        JarFile jarFile = new JarFile(inputFile);
        Enumeration<JarEntry> entries = jarFile.entries();
        while (entries.hasMoreElements()) {
            JarEntry jarEntry = entries.nextElement();
            if (jarEntry.isDirectory())
                continue;
            if (jarEntry.getName().endsWith(".class")) {
                String name = map.get(jarEntry.getName().replace(".class", ""));
                if (name != null) {
                    name += ".class";
                } else {
                    name = jarEntry.getName();
                }
                jarOutputStream.putNextEntry(new JarEntry(name));
                remappingClass(jarFile.getInputStream(jarEntry), jarOutputStream);
            } else {
                if (jarEntry.getName().endsWith("MANIFEST.MF"))
                    continue;

                jarOutputStream.putNextEntry(new JarEntry(jarEntry.getName()));
                IOUtils.copy(jarFile.getInputStream(jarEntry), jarOutputStream);
            }
            jarOutputStream.closeEntry();
        }
        jarFile.close();
        jarOutputStream.close();
    }

    private void remappingClass(InputStream input, OutputStream output) throws IOException {
        output.write(remapping(IOUtils.toByteArray(input)));
        output.flush();
    }

    public byte[] remapping(byte[] bytes) {
        ClassReader classReader = new ClassReader(bytes);
        ClassWriter classWriter = new ClassWriter(0);
        ClassRemapper classRemapper = new ClassRemapper(new ClassVisitor(ASM9, classWriter) {
        }, new SimpleRemapper(map) {
            @Override
            public String mapFieldName(String owner, String name, String descriptor) {
                String remappedName = map(owner + '.' + name);
                if (remappedName == null) {
                    if (superHashMap.containsKey(owner)) {
                        for (String s : superHashMap.get(owner)) {
                            String rn = mapFieldName(s, name, descriptor);
                            if (rn != null) {
                                return rn;
                            }
                        }
                    }
                }


                return remappedName == null ? name : remappedName;
            }

            @Override
            public String mapMethodName(String owner, String name, String descriptor) {
                String remappedName = map(owner + '.' + name + descriptor);
                if (remappedName == null) {
                    if (superHashMap.containsKey(owner)) {
                        for (String s : superHashMap.get(owner)) {
                            String rn = mapMethodName(s, name, descriptor);
                            if (rn != null) {
                                return rn;
                            }
                        }
                    }
                }
                return remappedName == null ? name : remappedName;
            }
        });
        classReader.accept(classRemapper, 0);
        return classWriter.toByteArray();
    }
}
