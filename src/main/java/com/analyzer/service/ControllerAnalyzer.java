package com.analyzer.service;

import com.analyzer.model.ControllerMethodInfo;
import com.analyzer.util.AnnotationExtractor;
import com.analyzer.util.BytecodeAnalyzer;
import org.objectweb.asm.*;
import org.objectweb.asm.tree.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Analyzes Spring controller classes to extract REST mapping information.
 * Uses ASM bytecode analysis to inspect compiled classes without loading them.
 */
@Service
public class ControllerAnalyzer {
    
    private static final Logger logger = LoggerFactory.getLogger(ControllerAnalyzer.class);
    
    @Autowired
    private AnnotationExtractor annotationExtractor;
    
    @Autowired
    private BytecodeAnalyzer bytecodeAnalyzer;
    
    // Spring controller and mapping annotation types
    private static final Set<String> CONTROLLER_ANNOTATIONS = Set.of(
        "Lorg/springframework/stereotype/Controller;",
        "Lorg/springframework/web/bind/annotation/RestController;",
        "Lorg/springframework/web/bind/annotation/ControllerAdvice;"
    );
    
    private static final Set<String> MAPPING_ANNOTATIONS = Set.of(
        "Lorg/springframework/web/bind/annotation/RequestMapping;",
        "Lorg/springframework/web/bind/annotation/GetMapping;",
        "Lorg/springframework/web/bind/annotation/PostMapping;",
        "Lorg/springframework/web/bind/annotation/PutMapping;",
        "Lorg/springframework/web/bind/annotation/DeleteMapping;",
        "Lorg/springframework/web/bind/annotation/PatchMapping;"
    );
    
    /**
     * Analyzes a controller class file and extracts method information.
     * 
     * @param classFile Path to the .class file
     * @param baseDir Base directory for resolving relative paths
     * @return List of controller method information
     * @throws IOException If class file cannot be read
     */
    public List<ControllerMethodInfo> analyzeControllerClass(Path classFile, Path baseDir) throws IOException {
        logger.debug("Analyzing controller class: {}", classFile);
        
        List<ControllerMethodInfo> methods = new ArrayList<>();
        
        try (InputStream inputStream = getClassInputStream(classFile)) {
            ClassReader classReader = new ClassReader(inputStream);
            ClassNode classNode = new ClassNode();
            classReader.accept(classNode, ClassReader.EXPAND_FRAMES);
            
            // Check if this is actually a controller class
            if (!isControllerClass(classNode)) {
                logger.debug("Class {} is not a Spring controller", classNode.name);
                return methods;
            }
            
            // Extract class-level request mapping information
            ClassMappingInfo classMappingInfo = extractClassMappingInfo(classNode);
            
            // Analyze each method in the class
            for (MethodNode methodNode : classNode.methods) {
                if (isRequestMappingMethod(methodNode)) {
                    ControllerMethodInfo methodInfo = analyzeControllerMethod(classNode, methodNode, classMappingInfo);
                    if (methodInfo != null) {
                        methods.add(methodInfo);
                    }
                }
            }
            
        } catch (Exception e) {
            logger.error("Error analyzing controller class {}: {}", classFile, e.getMessage(), e);
            throw new IOException("Failed to analyze controller class: " + e.getMessage(), e);
        }
        
        logger.debug("Found {} controller methods in {}", methods.size(), classFile);
        return methods;
    }
    
    /**
     * Gets InputStream for a class file (handles both regular files and JAR entries)
     */
    private InputStream getClassInputStream(Path classFile) throws IOException {
        String pathStr = classFile.toString();
        
        if (pathStr.contains("!")) {
            // Handle JAR file entry
            String[] parts = pathStr.split("!", 2);
            String jarPath = parts[0];
            String entryPath = parts[1].substring(1); // Remove leading slash
            
            return bytecodeAnalyzer.getClassFromJar(jarPath, entryPath);
        } else {
            // Handle regular file
            return Files.newInputStream(classFile);
        }
    }
    
    /**
     * Checks if a class is a Spring controller based on annotations
     */
    private boolean isControllerClass(ClassNode classNode) {
        if (classNode.visibleAnnotations == null) {
            return false;
        }
        
        return classNode.visibleAnnotations.stream()
            .anyMatch(annotation -> CONTROLLER_ANNOTATIONS.contains(annotation.desc));
    }
    
    /**
     * Extracts class-level request mapping information
     */
    private ClassMappingInfo extractClassMappingInfo(ClassNode classNode) {
        ClassMappingInfo classMappingInfo = new ClassMappingInfo();
        
        if (classNode.visibleAnnotations != null) {
            for (AnnotationNode annotation : classNode.visibleAnnotations) {
                if ("Lorg/springframework/web/bind/annotation/RequestMapping;".equals(annotation.desc)) {
                    Map<String, Object> values = annotationExtractor.extractAnnotationValues(annotation);
                    
                    // Extract path/value
                    Object pathValue = values.get("value");
                    if (pathValue == null) {
                        pathValue = values.get("path");
                    }
                    if (pathValue != null) {
                        classMappingInfo.basePaths = extractStringArrayValue(pathValue);
                    }
                    
                    // Extract method
                    Object methodValue = values.get("method");
                    if (methodValue != null) {
                        classMappingInfo.httpMethods = extractHttpMethods(methodValue);
                    }
                    
                    // Extract produces
                    Object producesValue = values.get("produces");
                    if (producesValue != null) {
                        classMappingInfo.produces = extractStringArrayValue(producesValue);
                    }
                    
                    // Extract consumes
                    Object consumesValue = values.get("consumes");
                    if (consumesValue != null) {
                        classMappingInfo.consumes = extractStringArrayValue(consumesValue);
                    }
                }
            }
        }
        
        return classMappingInfo;
    }
    
    /**
     * Checks if a method has request mapping annotations
     */
    private boolean isRequestMappingMethod(MethodNode methodNode) {
        if (methodNode.visibleAnnotations == null) {
            return false;
        }
        
        return methodNode.visibleAnnotations.stream()
            .anyMatch(annotation -> MAPPING_ANNOTATIONS.contains(annotation.desc));
    }
    
    /**
     * Analyzes a specific controller method to extract mapping information
     */
    private ControllerMethodInfo analyzeControllerMethod(ClassNode classNode, MethodNode methodNode, ClassMappingInfo classMappingInfo) {
        String className = classNode.name.replace('/', '.');
        String methodName = methodNode.name;
        
        ControllerMethodInfo methodInfo = new ControllerMethodInfo(className, methodName);
        methodInfo.setMethodSignature(methodNode.desc);
        
        // Extract method-level mapping information
        MethodMappingInfo methodMappingInfo = extractMethodMappingInfo(methodNode);
        
        // Combine class and method level mappings
        List<String> finalPaths = combinePaths(classMappingInfo.basePaths, methodMappingInfo.paths);
        List<String> finalMethods = methodMappingInfo.httpMethods.isEmpty() ? 
            classMappingInfo.httpMethods : methodMappingInfo.httpMethods;
        
        // If no HTTP methods specified, default based on annotation type
        if (finalMethods.isEmpty()) {
            finalMethods = deriveHttpMethodsFromAnnotation(methodNode);
        }
        
        methodInfo.setUrlPatterns(finalPaths);
        methodInfo.setHttpMethods(finalMethods);
        
        // Extract return type
        Type returnType = Type.getReturnType(methodNode.desc);
        methodInfo.setReturnType(returnType.getClassName());
        
        // Extract parameter information
        extractParameterInfo(methodNode, methodInfo);
        
        // Extract all annotations
        extractMethodAnnotations(methodNode, methodInfo);
        
        // Analyze method calls for state alteration hints
        analyzeMethodCalls(methodNode, methodInfo);
        
        return methodInfo;
    }
    
    /**
     * Extracts method-level request mapping information
     */
    private MethodMappingInfo extractMethodMappingInfo(MethodNode methodNode) {
        MethodMappingInfo methodMappingInfo = new MethodMappingInfo();
        
        if (methodNode.visibleAnnotations != null) {
            for (AnnotationNode annotation : methodNode.visibleAnnotations) {
                if (MAPPING_ANNOTATIONS.contains(annotation.desc)) {
                    Map<String, Object> values = annotationExtractor.extractAnnotationValues(annotation);
                    
                    // Extract path/value
                    Object pathValue = values.get("value");
                    if (pathValue == null) {
                        pathValue = values.get("path");
                    }
                    if (pathValue != null) {
                        methodMappingInfo.paths = extractStringArrayValue(pathValue);
                    }
                    
                    // Extract method (for @RequestMapping)
                    Object methodValue = values.get("method");
                    if (methodValue != null) {
                        methodMappingInfo.httpMethods = extractHttpMethods(methodValue);
                    }
                    
                    // For specific mapping annotations, derive HTTP method
                    if (methodMappingInfo.httpMethods.isEmpty()) {
                        String httpMethod = deriveHttpMethodFromAnnotation(annotation.desc);
                        if (httpMethod != null) {
                            methodMappingInfo.httpMethods = List.of(httpMethod);
                        }
                    }
                    
                    // Extract produces
                    Object producesValue = values.get("produces");
                    if (producesValue != null) {
                        methodMappingInfo.produces = extractStringArrayValue(producesValue);
                    }
                    
                    // Extract consumes
                    Object consumesValue = values.get("consumes");
                    if (consumesValue != null) {
                        methodMappingInfo.consumes = extractStringArrayValue(consumesValue);
                    }
                }
            }
        }
        
        return methodMappingInfo;
    }
    
    /**
     * Combines class-level and method-level URL paths
     */
    private List<String> combinePaths(List<String> basePaths, List<String> methodPaths) {
        if (basePaths.isEmpty() && methodPaths.isEmpty()) {
            return List.of("");
        }
        
        if (basePaths.isEmpty()) {
            return methodPaths;
        }
        
        if (methodPaths.isEmpty()) {
            return basePaths;
        }
        
        List<String> combinedPaths = new ArrayList<>();
        for (String basePath : basePaths) {
            for (String methodPath : methodPaths) {
                String combinedPath = combinePath(basePath, methodPath);
                combinedPaths.add(combinedPath);
            }
        }
        
        return combinedPaths;
    }
    
    /**
     * Combines two URL path segments
     */
    private String combinePath(String basePath, String methodPath) {
        if (basePath == null || basePath.isEmpty()) {
            return methodPath != null ? methodPath : "";
        }
        
        if (methodPath == null || methodPath.isEmpty()) {
            return basePath;
        }
        
        // Ensure proper path combination
        String base = basePath.endsWith("/") ? basePath.substring(0, basePath.length() - 1) : basePath;
        String method = methodPath.startsWith("/") ? methodPath : "/" + methodPath;
        
        return base + method;
    }
    
    /**
     * Derives HTTP methods from specific mapping annotations
     */
    private List<String> deriveHttpMethodsFromAnnotation(MethodNode methodNode) {
        if (methodNode.visibleAnnotations == null) {
            return List.of("GET"); // Default
        }
        
        for (AnnotationNode annotation : methodNode.visibleAnnotations) {
            String httpMethod = deriveHttpMethodFromAnnotation(annotation.desc);
            if (httpMethod != null) {
                return List.of(httpMethod);
            }
        }
        
        return List.of("GET"); // Default
    }
    
    /**
     * Maps annotation descriptor to HTTP method
     */
    private String deriveHttpMethodFromAnnotation(String annotationDesc) {
        return switch (annotationDesc) {
            case "Lorg/springframework/web/bind/annotation/GetMapping;" -> "GET";
            case "Lorg/springframework/web/bind/annotation/PostMapping;" -> "POST";
            case "Lorg/springframework/web/bind/annotation/PutMapping;" -> "PUT";
            case "Lorg/springframework/web/bind/annotation/DeleteMapping;" -> "DELETE";
            case "Lorg/springframework/web/bind/annotation/PatchMapping;" -> "PATCH";
            default -> null;
        };
    }
    
    /**
     * Extracts parameter information from method signature and annotations
     */
    private void extractParameterInfo(MethodNode methodNode, ControllerMethodInfo methodInfo) {
        Type[] argumentTypes = Type.getArgumentTypes(methodNode.desc);
        
        for (int i = 0; i < argumentTypes.length; i++) {
            ControllerMethodInfo.ParameterInfo paramInfo = new ControllerMethodInfo.ParameterInfo();
            paramInfo.setType(argumentTypes[i].getClassName());
            paramInfo.setName("param" + i); // Default name
            
            // Extract parameter annotations if available
            if (methodNode.visibleParameterAnnotations != null && 
                i < methodNode.visibleParameterAnnotations.length &&
                methodNode.visibleParameterAnnotations[i] != null) {
                
                for (AnnotationNode annotation : methodNode.visibleParameterAnnotations[i]) {
                    paramInfo.addAnnotation(annotation.desc);
                    
                    // Check for validation annotations
                    if (isValidationAnnotation(annotation.desc)) {
                        paramInfo.setValidated(true);
                    }
                }
            }
            
            methodInfo.addParameter(paramInfo);
        }
    }
    
    /**
     * Extracts all method annotations
     */
    private void extractMethodAnnotations(MethodNode methodNode, ControllerMethodInfo methodInfo) {
        if (methodNode.visibleAnnotations != null) {
            for (AnnotationNode annotation : methodNode.visibleAnnotations) {
                Map<String, Object> values = annotationExtractor.extractAnnotationValues(annotation);
                methodInfo.addAnnotation(annotation.desc, values);
                
                // Check for transactional annotations
                if (annotation.desc.contains("Transactional")) {
                    methodInfo.setTransactional(true);
                }
            }
        }
    }
    
    /**
     * Analyzes method bytecode for method calls that might indicate state alteration
     */
    private void analyzeMethodCalls(MethodNode methodNode, ControllerMethodInfo methodInfo) {
        for (AbstractInsnNode instruction : methodNode.instructions) {
            if (instruction.getType() == AbstractInsnNode.METHOD_INSN) {
                MethodInsnNode methodInsn = (MethodInsnNode) instruction;
                String methodCall = methodInsn.owner + "." + methodInsn.name;
                methodInfo.addCalledMethod(methodCall);
            }
        }
    }
    
    /**
     * Utility methods for extracting annotation values
     */
    private List<String> extractStringArrayValue(Object value) {
        if (value instanceof List) {
            return ((List<?>) value).stream()
                .map(Object::toString)
                .collect(Collectors.toList());
        } else if (value instanceof String) {
            return List.of((String) value);
        }
        return new ArrayList<>();
    }
    
    private List<String> extractHttpMethods(Object value) {
        List<String> methods = new ArrayList<>();
        if (value instanceof List) {
            for (Object item : (List<?>) value) {
                if (item.toString().contains("RequestMethod")) {
                    String method = extractRequestMethodValue(item.toString());
                    if (method != null) {
                        methods.add(method);
                    }
                }
            }
        }
        return methods;
    }
    
    private String extractRequestMethodValue(String enumValue) {
        // Extract HTTP method from RequestMethod enum value
        if (enumValue.contains("GET")) return "GET";
        if (enumValue.contains("POST")) return "POST";
        if (enumValue.contains("PUT")) return "PUT";
        if (enumValue.contains("DELETE")) return "DELETE";
        if (enumValue.contains("PATCH")) return "PATCH";
        if (enumValue.contains("OPTIONS")) return "OPTIONS";
        if (enumValue.contains("HEAD")) return "HEAD";
        return null;
    }
    
    private boolean isValidationAnnotation(String annotationDesc) {
        return annotationDesc.contains("Valid") || 
               annotationDesc.contains("NotNull") ||
               annotationDesc.contains("NotEmpty") ||
               annotationDesc.contains("Size") ||
               annotationDesc.contains("Pattern") ||
               annotationDesc.contains("Email");
    }
    
    /**
     * Helper classes for organizing mapping information
     */
    private static class ClassMappingInfo {
        List<String> basePaths = new ArrayList<>();
        List<String> httpMethods = new ArrayList<>();
        List<String> produces = new ArrayList<>();
        List<String> consumes = new ArrayList<>();
    }
    
    private static class MethodMappingInfo {
        List<String> paths = new ArrayList<>();
        List<String> httpMethods = new ArrayList<>();
        List<String> produces = new ArrayList<>();
        List<String> consumes = new ArrayList<>();
    }
}