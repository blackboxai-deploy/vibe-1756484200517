package com.analyzer.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Utility class for scanning class paths and finding classes within JAR/WAR files.
 * Provides methods for recursive directory scanning and class discovery.
 */
@Component
public class ClassPathScanner {
    
    private static final Logger logger = LoggerFactory.getLogger(ClassPathScanner.class);
    
    /**
     * Scans a directory recursively for class files.
     * 
     * @param rootPath The root directory to scan
     * @return List of paths to .class files
     * @throws IOException If directory cannot be scanned
     */
    public List<Path> scanForClassFiles(Path rootPath) throws IOException {
        logger.debug("Scanning for class files in: {}", rootPath);
        
        List<Path> classFiles = new ArrayList<>();
        
        if (!Files.exists(rootPath)) {
            logger.warn("Path does not exist: {}", rootPath);
            return classFiles;
        }
        
        Files.walkFileTree(rootPath, new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                if (file.toString().endsWith(".class")) {
                    classFiles.add(file);
                }
                return FileVisitResult.CONTINUE;
            }
            
            @Override
            public FileVisitResult visitFileFailed(Path file, IOException exc) throws IOException {
                logger.warn("Failed to visit file {}: {}", file, exc.getMessage());
                return FileVisitResult.CONTINUE;
            }
        });
        
        logger.debug("Found {} class files", classFiles.size());
        return classFiles;
    }
    
    /**
     * Scans for JAR files within a directory.
     * 
     * @param rootPath The root directory to scan
     * @return List of paths to .jar files
     * @throws IOException If directory cannot be scanned
     */
    public List<Path> scanForJarFiles(Path rootPath) throws IOException {
        logger.debug("Scanning for JAR files in: {}", rootPath);
        
        List<Path> jarFiles = new ArrayList<>();
        
        if (!Files.exists(rootPath)) {
            logger.warn("Path does not exist: {}", rootPath);
            return jarFiles;
        }
        
        try (var stream = Files.walk(rootPath)) {
            jarFiles = stream
                .filter(path -> path.toString().endsWith(".jar"))
                .collect(Collectors.toList());
        }
        
        logger.debug("Found {} JAR files", jarFiles.size());
        return jarFiles;
    }
    
    /**
     * Finds classes matching specific patterns within a directory.
     * 
     * @param rootPath The root directory to scan
     * @param patterns Class name patterns to match (supports wildcards)
     * @return Set of matching class names
     * @throws IOException If directory cannot be scanned
     */
    public Set<String> findClassesByPattern(Path rootPath, String... patterns) throws IOException {
        logger.debug("Finding classes by patterns: {} in {}", Arrays.toString(patterns), rootPath);
        
        Set<String> matchingClasses = new HashSet<>();
        List<Path> classFiles = scanForClassFiles(rootPath);
        
        for (Path classFile : classFiles) {
            String className = extractClassName(classFile, rootPath);
            
            for (String pattern : patterns) {
                if (matchesPattern(className, pattern)) {
                    matchingClasses.add(className);
                    break;
                }
            }
        }
        
        logger.debug("Found {} matching classes", matchingClasses.size());
        return matchingClasses;
    }
    
    /**
     * Extracts the fully qualified class name from a class file path.
     * 
     * @param classFile Path to the .class file
     * @param basePath Base path to calculate relative path from
     * @return Fully qualified class name
     */
    public String extractClassName(Path classFile, Path basePath) {
        try {
            Path relativePath = basePath.relativize(classFile);
            String className = relativePath.toString()
                .replace(File.separatorChar, '.')
                .replace('/', '.')
                .replace('\\', '.');
            
            // Remove .class extension
            if (className.endsWith(".class")) {
                className = className.substring(0, className.length() - 6);
            }
            
            return className;
        } catch (Exception e) {
            logger.warn("Failed to extract class name from {}: {}", classFile, e.getMessage());
            return classFile.getFileName().toString().replace(".class", "");
        }
    }
    
    /**
     * Scans a WAR file structure for Spring-related classes.
     * 
     * @param warExtractionPath Path where WAR file has been extracted
     * @return Map of class types to their file paths
     * @throws IOException If scanning fails
     */
    public Map<String, List<Path>> scanWarForSpringClasses(Path warExtractionPath) throws IOException {
        logger.debug("Scanning WAR structure for Spring classes: {}", warExtractionPath);
        
        Map<String, List<Path>> springClasses = new HashMap<>();
        springClasses.put("controllers", new ArrayList<>());
        springClasses.put("services", new ArrayList<>());
        springClasses.put("repositories", new ArrayList<>());
        springClasses.put("entities", new ArrayList<>());
        springClasses.put("configurations", new ArrayList<>());
        
        // Scan WEB-INF/classes directory
        Path webInfClasses = warExtractionPath.resolve("WEB-INF/classes");
        if (Files.exists(webInfClasses)) {
            scanDirectoryForSpringClasses(webInfClasses, springClasses);
        }
        
        // Scan JAR files in WEB-INF/lib
        Path webInfLib = warExtractionPath.resolve("WEB-INF/lib");
        if (Files.exists(webInfLib)) {
            List<Path> jarFiles = scanForJarFiles(webInfLib);
            for (Path jarFile : jarFiles) {
                // For JAR files, we'll identify them but not extract for performance
                // The actual analysis will happen when the analyzer processes them
                logger.debug("Found JAR file for analysis: {}", jarFile);
            }
        }
        
        return springClasses;
    }
    
    /**
     * Scans a directory for Spring-related classes and categorizes them.
     * 
     * @param directory Directory to scan
     * @param springClasses Map to populate with categorized classes
     * @throws IOException If scanning fails
     */
    private void scanDirectoryForSpringClasses(Path directory, Map<String, List<Path>> springClasses) throws IOException {
        List<Path> classFiles = scanForClassFiles(directory);
        
        for (Path classFile : classFiles) {
            String fileName = classFile.getFileName().toString().toLowerCase();
            String className = extractClassName(classFile, directory);
            
            // Categorize based on naming conventions
            if (fileName.contains("controller")) {
                springClasses.get("controllers").add(classFile);
            } else if (fileName.contains("service")) {
                springClasses.get("services").add(classFile);
            } else if (fileName.contains("repository") || fileName.contains("dao")) {
                springClasses.get("repositories").add(classFile);
            } else if (fileName.contains("entity") || fileName.contains("model")) {
                springClasses.get("entities").add(classFile);
            } else if (fileName.contains("config")) {
                springClasses.get("configurations").add(classFile);
            }
        }
        
        logger.debug("Categorized Spring classes: Controllers={}, Services={}, Repositories={}, Entities={}, Configs={}",
                    springClasses.get("controllers").size(),
                    springClasses.get("services").size(),
                    springClasses.get("repositories").size(),
                    springClasses.get("entities").size(),
                    springClasses.get("configurations").size());
    }
    
    /**
     * Finds all packages within a scanned directory.
     * 
     * @param rootPath Root directory to scan
     * @return Set of package names found
     * @throws IOException If scanning fails
     */
    public Set<String> findPackages(Path rootPath) throws IOException {
        logger.debug("Finding packages in: {}", rootPath);
        
        Set<String> packages = new HashSet<>();
        
        if (!Files.exists(rootPath)) {
            return packages;
        }
        
        Files.walkFileTree(rootPath, new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
                if (!dir.equals(rootPath)) {
                    Path relativePath = rootPath.relativize(dir);
                    String packageName = relativePath.toString()
                        .replace(File.separatorChar, '.')
                        .replace('/', '.')
                        .replace('\\', '.');
                    packages.add(packageName);
                }
                return FileVisitResult.CONTINUE;
            }
        });
        
        logger.debug("Found {} packages", packages.size());
        return packages;
    }
    
    /**
     * Checks if a class name matches a pattern (supports * wildcards).
     * 
     * @param className Class name to check
     * @param pattern Pattern to match against
     * @return true if the class name matches the pattern
     */
    private boolean matchesPattern(String className, String pattern) {
        if ("*".equals(pattern)) {
            return true;
        }
        
        if (!pattern.contains("*")) {
            return className.equals(pattern);
        }
        
        // Convert pattern to regex
        String regex = pattern.replace("*", ".*").replace("?", ".");
        return className.matches(regex);
    }
    
    /**
     * Gets statistics about the scanned class files.
     * 
     * @param rootPath Root directory that was scanned
     * @return Statistics map
     * @throws IOException If scanning fails
     */
    public Map<String, Object> getClassScanStatistics(Path rootPath) throws IOException {
        Map<String, Object> stats = new HashMap<>();
        
        List<Path> classFiles = scanForClassFiles(rootPath);
        List<Path> jarFiles = scanForJarFiles(rootPath);
        Set<String> packages = findPackages(rootPath);
        
        stats.put("totalClassFiles", classFiles.size());
        stats.put("totalJarFiles", jarFiles.size());
        stats.put("totalPackages", packages.size());
        stats.put("scanPath", rootPath.toString());
        
        // Categorize by likely types
        long controllerCount = classFiles.stream()
            .mapToLong(path -> path.toString().toLowerCase().contains("controller") ? 1 : 0)
            .sum();
        long serviceCount = classFiles.stream()
            .mapToLong(path -> path.toString().toLowerCase().contains("service") ? 1 : 0)
            .sum();
        long repositoryCount = classFiles.stream()
            .mapToLong(path -> path.toString().toLowerCase().contains("repository") || 
                              path.toString().toLowerCase().contains("dao") ? 1 : 0)
            .sum();
        
        stats.put("likelyControllers", controllerCount);
        stats.put("likelyServices", serviceCount);
        stats.put("likelyRepositories", repositoryCount);
        
        return stats;
    }
}