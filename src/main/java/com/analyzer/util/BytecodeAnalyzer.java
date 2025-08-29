package com.analyzer.util;

import org.apache.commons.compress.archivers.zip.ZipArchiveEntry;
import org.apache.commons.compress.archivers.zip.ZipFile;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.tree.ClassNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.zip.ZipEntry;

/**
 * Utility class for deep bytecode analysis using ASM.
 * Provides methods for reading class files from various sources and analyzing bytecode.
 */
@Component
public class BytecodeAnalyzer {
    
    private static final Logger logger = LoggerFactory.getLogger(BytecodeAnalyzer.class);
    
    // Cache for frequently accessed classes
    private final Map<String, ClassNode> classCache = new HashMap<>();
    private final Map<String, ZipFile> jarCache = new HashMap<>();
    
    /**
     * Reads a class file and returns the ASM ClassNode.
     * 
     * @param classPath Path to the class file
     * @return ClassNode representing the class
     * @throws IOException If the class file cannot be read
     */
    public ClassNode readClassFile(Path classPath) throws IOException {
        String pathKey = classPath.toString();
        
        // Check cache first
        if (classCache.containsKey(pathKey)) {
            return classCache.get(pathKey);
        }
        
        ClassNode classNode;
        
        try (InputStream inputStream = getInputStream(classPath)) {
            ClassReader classReader = new ClassReader(inputStream);
            classNode = new ClassNode();
            classReader.accept(classNode, ClassReader.EXPAND_FRAMES);
            
            // Cache the result
            classCache.put(pathKey, classNode);
        }
        
        return classNode;
    }
    
    /**
     * Gets InputStream for a class file, handling both regular files and JAR entries.
     * 
     * @param classPath Path to the class file
     * @return InputStream for reading the class file
     * @throws IOException If the class file cannot be accessed
     */
    public InputStream getInputStream(Path classPath) throws IOException {
        String pathStr = classPath.toString();
        
        if (pathStr.contains("!")) {
            // Handle JAR file entry (format: /path/to/jar.jar!/com/example/Class.class)
            String[] parts = pathStr.split("!", 2);
            String jarPath = parts[0];
            String entryPath = parts[1].startsWith("/") ? parts[1].substring(1) : parts[1];
            
            return getClassFromJar(jarPath, entryPath);
        } else {
            // Handle regular file
            return Files.newInputStream(classPath);
        }
    }
    
    /**
     * Extracts a class file from a JAR archive.
     * 
     * @param jarPath Path to the JAR file
     * @param entryPath Path within the JAR to the class file
     * @return InputStream for the class file
     * @throws IOException If the JAR or class file cannot be accessed
     */
    public InputStream getClassFromJar(String jarPath, String entryPath) throws IOException {
        ZipFile zipFile = getZipFile(jarPath);
        
        ZipArchiveEntry entry = zipFile.getEntry(entryPath);
        if (entry == null) {
            throw new FileNotFoundException("Class entry not found in JAR: " + entryPath);
        }
        
        return zipFile.getInputStream(entry);
    }
    
    /**
     * Gets a ZipFile instance, using cache for performance.
     * 
     * @param jarPath Path to the JAR file
     * @return ZipFile instance
     * @throws IOException If the JAR file cannot be opened
     */
    private ZipFile getZipFile(String jarPath) throws IOException {
        if (jarCache.containsKey(jarPath)) {
            return jarCache.get(jarPath);
        }
        
        ZipFile zipFile = new ZipFile(new File(jarPath));
        jarCache.put(jarPath, zipFile);
        return zipFile;
    }
    
    /**
     * Finds all classes within a JAR file that match a pattern.
     * 
     * @param jarPath Path to the JAR file
     * @param classNamePattern Pattern to match class names (can use wildcards)
     * @return List of class entry paths within the JAR
     * @throws IOException If the JAR file cannot be read
     */
    public List<String> findClassesInJar(String jarPath, String classNamePattern) throws IOException {
        List<String> classEntries = new ArrayList<>();
        ZipFile zipFile = getZipFile(jarPath);
        
        Enumeration<ZipArchiveEntry> entries = zipFile.getEntries();
        while (entries.hasMoreElements()) {
            ZipArchiveEntry entry = entries.nextElement();
            
            if (!entry.isDirectory() && entry.getName().endsWith(".class")) {
                String className = entry.getName().replace('/', '.').replace(".class", "");
                
                if (matchesPattern(className, classNamePattern)) {
                    classEntries.add(entry.getName());
                }
            }
        }
        
        return classEntries;
    }
    
    /**
     * Analyzes class hierarchy and finds all subclasses of a given class.
     * 
     * @param baseClassName Name of the base class
     * @param classFiles List of class file paths to search
     * @return List of subclass names
     */
    public List<String> findSubclasses(String baseClassName, List<Path> classFiles) {
        List<String> subclasses = new ArrayList<>();
        
        for (Path classFile : classFiles) {
            try {
                ClassNode classNode = readClassFile(classFile);
                
                if (isSubclassOf(classNode, baseClassName)) {
                    subclasses.add(classNode.name.replace('/', '.'));
                }
                
            } catch (IOException e) {
                logger.warn("Failed to analyze class file {}: {}", classFile, e.getMessage());
            }
        }
        
        return subclasses;
    }
    
    /**
     * Checks if a class is a subclass of another class.
     * 
     * @param classNode The class to check
     * @param baseClassName The potential base class name
     * @return true if classNode is a subclass of baseClassName
     */
    public boolean isSubclassOf(ClassNode classNode, String baseClassName) {
        String normalizedBaseName = baseClassName.replace('.', '/');
        
        // Check direct superclass
        if (normalizedBaseName.equals(classNode.superName)) {
            return true;
        }
        
        // Check implemented interfaces
        if (classNode.interfaces != null) {
            for (String interfaceName : classNode.interfaces) {
                if (normalizedBaseName.equals(interfaceName)) {
                    return true;
                }
            }
        }
        
        // Note: This doesn't recursively check the entire hierarchy
        // For complete hierarchy analysis, you would need to load parent classes too
        
        return false;
    }
    
    /**
     * Extracts all string constants from a class.
     * 
     * @param classNode The class to analyze
     * @return Set of string constants found in the class
     */
    public Set<String> extractStringConstants(ClassNode classNode) {
        Set<String> constants = new HashSet<>();
        
        // Extract from method bytecode
        classNode.methods.forEach(method -> {
            method.instructions.forEach(instruction -> {
                if (instruction.getType() == org.objectweb.asm.tree.AbstractInsnNode.LDC_INSN) {
                    org.objectweb.asm.tree.LdcInsnNode ldcInsn = (org.objectweb.asm.tree.LdcInsnNode) instruction;
                    if (ldcInsn.cst instanceof String) {
                        constants.add((String) ldcInsn.cst);
                    }
                }
            });
        });
        
        return constants;
    }
    
    /**
     * Analyzes method calls within a class.
     * 
     * @param classNode The class to analyze
     * @return Map of method names to lists of called methods
     */
    public Map<String, List<String>> analyzeMethodCalls(ClassNode classNode) {
        Map<String, List<String>> methodCalls = new HashMap<>();
        
        classNode.methods.forEach(method -> {
            List<String> calls = new ArrayList<>();
            
            method.instructions.forEach(instruction -> {
                if (instruction.getType() == org.objectweb.asm.tree.AbstractInsnNode.METHOD_INSN) {
                    org.objectweb.asm.tree.MethodInsnNode methodInsn = 
                        (org.objectweb.asm.tree.MethodInsnNode) instruction;
                    String methodCall = methodInsn.owner.replace('/', '.') + "." + methodInsn.name;
                    calls.add(methodCall);
                }
            });
            
            methodCalls.put(method.name, calls);
        });
        
        return methodCalls;
    }
    
    /**
     * Gets detailed information about a specific method.
     * 
     * @param classNode The class containing the method
     * @param methodName The name of the method
     * @return Detailed method information
     */
    public MethodInfo getMethodInfo(ClassNode classNode, String methodName) {
        return classNode.methods.stream()
            .filter(method -> method.name.equals(methodName))
            .findFirst()
            .map(method -> new MethodInfo(
                method.name,
                method.desc,
                method.access,
                method.signature,
                extractMethodAnnotations(method),
                analyzeMethodComplexity(method)
            ))
            .orElse(null);
    }
    
    /**
     * Extracts annotation information from a method.
     */
    private List<String> extractMethodAnnotations(org.objectweb.asm.tree.MethodNode method) {
        List<String> annotations = new ArrayList<>();
        
        if (method.visibleAnnotations != null) {
            method.visibleAnnotations.forEach(annotation -> 
                annotations.add(annotation.desc));
        }
        
        if (method.invisibleAnnotations != null) {
            method.invisibleAnnotations.forEach(annotation -> 
                annotations.add(annotation.desc));
        }
        
        return annotations;
    }
    
    /**
     * Analyzes method complexity (rough cyclomatic complexity).
     */
    private int analyzeMethodComplexity(org.objectweb.asm.tree.MethodNode method) {
        int complexity = 1; // Base complexity
        
        method.instructions.forEach(instruction -> {
            switch (instruction.getType()) {
                case org.objectweb.asm.tree.AbstractInsnNode.JUMP_INSN:
                case org.objectweb.asm.tree.AbstractInsnNode.LOOKUPSWITCH_INSN:
                case org.objectweb.asm.tree.AbstractInsnNode.TABLESWITCH_INSN:
                    complexity++;
                    break;
            }
        });
        
        return complexity;
    }
    
    /**
     * Simple pattern matching for class names (supports * wildcards).
     */
    private boolean matchesPattern(String className, String pattern) {
        if ("*".equals(pattern)) {
            return true;
        }
        
        if (!pattern.contains("*")) {
            return className.equals(pattern);
        }
        
        // Convert pattern to regex
        String regex = pattern.replace("*", ".*");
        return className.matches(regex);
    }
    
    /**
     * Cleans up cached resources.
     */
    public void cleanup() {
        classCache.clear();
        
        jarCache.values().forEach(zipFile -> {
            try {
                zipFile.close();
            } catch (IOException e) {
                logger.warn("Error closing ZipFile: {}", e.getMessage());
            }
        });
        jarCache.clear();
    }
    
    /**
     * Information about a specific method.
     */
    public static class MethodInfo {
        private final String name;
        private final String descriptor;
        private final int access;
        private final String signature;
        private final List<String> annotations;
        private final int complexity;
        
        public MethodInfo(String name, String descriptor, int access, String signature, 
                         List<String> annotations, int complexity) {
            this.name = name;
            this.descriptor = descriptor;
            this.access = access;
            this.signature = signature;
            this.annotations = annotations;
            this.complexity = complexity;
        }
        
        // Getters
        public String getName() { return name; }
        public String getDescriptor() { return descriptor; }
        public int getAccess() { return access; }
        public String getSignature() { return signature; }
        public List<String> getAnnotations() { return annotations; }
        public int getComplexity() { return complexity; }
        
        public boolean isPublic() { return (access & org.objectweb.asm.Opcodes.ACC_PUBLIC) != 0; }
        public boolean isStatic() { return (access & org.objectweb.asm.Opcodes.ACC_STATIC) != 0; }
        public boolean isFinal() { return (access & org.objectweb.asm.Opcodes.ACC_FINAL) != 0; }
    }
}