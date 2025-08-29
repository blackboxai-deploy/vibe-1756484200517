package com.analyzer.service;

import com.analyzer.model.ApiReport;
import com.analyzer.model.ApiEndpoint;
import com.analyzer.model.ControllerMethodInfo;
import org.apache.commons.compress.archivers.zip.ZipArchiveEntry;
import org.apache.commons.compress.archivers.zip.ZipFile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Main service for analyzing WAR files and generating API reports.
 * Extracts WAR contents, identifies Spring controllers, and analyzes REST endpoints.
 */
@Service
public class WarFileAnalyzer {
    
    private static final Logger logger = LoggerFactory.getLogger(WarFileAnalyzer.class);
    
    @Value("${analyzer.temp-directory:${java.io.tmpdir}/war-analyzer}")
    private String tempDirectory;
    
    @Value("${analyzer.max-analysis-timeout:300000}")
    private long maxAnalysisTimeout;
    
    @Autowired
    private ControllerAnalyzer controllerAnalyzer;
    
    @Autowired
    private StateAlterationAnalyzer stateAlterationAnalyzer;
    
    @Autowired
    private ValidationAnalyzer validationAnalyzer;
    
    @Autowired
    private ReportGenerator reportGenerator;
    
    /**
     * Analyzes a WAR file and generates a comprehensive API report.
     * 
     * @param warFilePath Path to the WAR file to analyze
     * @return Complete API analysis report
     * @throws IOException If WAR file cannot be read or processed
     */
    public ApiReport analyzeWarFile(String warFilePath) throws IOException {
        logger.info("Starting analysis of WAR file: {}", warFilePath);
        
        Path warPath = Paths.get(warFilePath);
        if (!Files.exists(warPath)) {
            throw new FileNotFoundException("WAR file not found: " + warFilePath);
        }
        
        String warFileName = warPath.getFileName().toString();
        ApiReport report = new ApiReport(warFileName);
        
        // Create temporary extraction directory
        Path extractionDir = createTempDirectory(warFileName);
        
        try {
            // Extract WAR file contents
            extractWarFile(warPath, extractionDir);
            
            // Find and analyze controller classes
            List<Path> classFiles = findClassFiles(extractionDir);
            List<ControllerMethodInfo> controllerMethods = analyzeControllerClasses(classFiles, extractionDir);
            
            // Generate API endpoints from controller analysis
            List<ApiEndpoint> endpoints = generateApiEndpoints(controllerMethods);
            
            // Populate report
            report.setApis(endpoints);
            updateReportSummary(report);
            
            logger.info("Analysis completed. Found {} API endpoints in {} controller classes", 
                       endpoints.size(), 
                       controllerMethods.stream().map(ControllerMethodInfo::getClassName).distinct().count());
            
            return report;
            
        } catch (Exception e) {
            logger.error("Error analyzing WAR file: {}", e.getMessage(), e);
            throw new IOException("Failed to analyze WAR file: " + e.getMessage(), e);
        } finally {
            // Clean up temporary files
            cleanupTempDirectory(extractionDir);
        }
    }
    
    /**
     * Extracts WAR file contents to temporary directory
     */
    private void extractWarFile(Path warFile, Path extractionDir) throws IOException {
        logger.debug("Extracting WAR file to: {}", extractionDir);
        
        try (ZipFile zipFile = new ZipFile(warFile.toFile())) {
            Enumeration<ZipArchiveEntry> entries = zipFile.getEntries();
            
            while (entries.hasMoreElements()) {
                ZipArchiveEntry entry = entries.nextElement();
                Path targetPath = extractionDir.resolve(entry.getName());
                
                if (entry.isDirectory()) {
                    Files.createDirectories(targetPath);
                } else {
                    Files.createDirectories(targetPath.getParent());
                    
                    try (InputStream inputStream = zipFile.getInputStream(entry);
                         OutputStream outputStream = Files.newOutputStream(targetPath)) {
                        inputStream.transferTo(outputStream);
                    }
                }
            }
        }
        
        logger.debug("WAR file extracted successfully");
    }
    
    /**
     * Finds all .class files in the extracted WAR directory
     */
    private List<Path> findClassFiles(Path extractionDir) throws IOException {
        logger.debug("Scanning for class files in: {}", extractionDir);
        
        List<Path> classFiles = new ArrayList<>();
        
        // Look in WEB-INF/classes and WEB-INF/lib directories
        Path webInfClasses = extractionDir.resolve("WEB-INF/classes");
        Path webInfLib = extractionDir.resolve("WEB-INF/lib");
        
        // Scan WEB-INF/classes directory
        if (Files.exists(webInfClasses)) {
            try (var stream = Files.walk(webInfClasses)) {
                List<Path> webInfClassFiles = stream
                    .filter(path -> path.toString().endsWith(".class"))
                    .collect(Collectors.toList());
                classFiles.addAll(webInfClassFiles);
            }
        }
        
        // Scan JAR files in WEB-INF/lib
        if (Files.exists(webInfLib)) {
            try (var stream = Files.list(webInfLib)) {
                List<Path> jarFiles = stream
                    .filter(path -> path.toString().endsWith(".jar"))
                    .collect(Collectors.toList());
                
                for (Path jarFile : jarFiles) {
                    classFiles.addAll(findClassFilesInJar(jarFile));
                }
            }
        }
        
        logger.debug("Found {} class files", classFiles.size());
        return classFiles;
    }
    
    /**
     * Finds class files within a JAR file
     */
    private List<Path> findClassFilesInJar(Path jarFile) throws IOException {
        List<Path> classFiles = new ArrayList<>();
        
        try (ZipFile zipFile = new ZipFile(jarFile.toFile())) {
            Enumeration<ZipArchiveEntry> entries = zipFile.getEntries();
            
            while (entries.hasMoreElements()) {
                ZipArchiveEntry entry = entries.nextElement();
                if (!entry.isDirectory() && entry.getName().endsWith(".class")) {
                    // Create a virtual path for JAR-contained class
                    Path classPath = Paths.get(jarFile.toString() + "!" + entry.getName());
                    classFiles.add(classPath);
                }
            }
        }
        
        return classFiles;
    }
    
    /**
     * Analyzes controller classes to extract method information
     */
    private List<ControllerMethodInfo> analyzeControllerClasses(List<Path> classFiles, Path extractionDir) throws IOException {
        logger.debug("Analyzing controller classes");
        
        List<ControllerMethodInfo> controllerMethods = new ArrayList<>();
        
        for (Path classFile : classFiles) {
            try {
                // Check if this is likely a controller class
                if (isLikelyControllerClass(classFile)) {
                    List<ControllerMethodInfo> methods = controllerAnalyzer.analyzeControllerClass(classFile, extractionDir);
                    controllerMethods.addAll(methods);
                }
            } catch (Exception e) {
                logger.warn("Failed to analyze class file {}: {}", classFile, e.getMessage());
                // Continue processing other files
            }
        }
        
        logger.debug("Analyzed {} controller methods", controllerMethods.size());
        return controllerMethods;
    }
    
    /**
     * Heuristic to determine if a class file is likely a Spring controller
     */
    private boolean isLikelyControllerClass(Path classFile) {
        String fileName = classFile.getFileName().toString();
        String lowerFileName = fileName.toLowerCase();
        
        // Basic heuristics for controller classes
        return lowerFileName.contains("controller") || 
               lowerFileName.contains("resource") ||
               lowerFileName.contains("endpoint") ||
               lowerFileName.contains("api");
    }
    
    /**
     * Generates API endpoint objects from controller method analysis
     */
    private List<ApiEndpoint> generateApiEndpoints(List<ControllerMethodInfo> controllerMethods) {
        logger.debug("Generating API endpoints from {} controller methods", controllerMethods.size());
        
        List<ApiEndpoint> endpoints = new ArrayList<>();
        
        for (ControllerMethodInfo methodInfo : controllerMethods) {
            // Create endpoints for each HTTP method and URL pattern combination
            for (String httpMethod : methodInfo.getHttpMethods()) {
                for (String urlPattern : methodInfo.getUrlPatterns()) {
                    ApiEndpoint endpoint = createApiEndpoint(methodInfo, httpMethod, urlPattern);
                    endpoints.add(endpoint);
                }
            }
            
            // If no specific HTTP methods or URL patterns found, create a default endpoint
            if (methodInfo.getHttpMethods().isEmpty() || methodInfo.getUrlPatterns().isEmpty()) {
                ApiEndpoint endpoint = createApiEndpoint(methodInfo, 
                    methodInfo.getHttpMethods().isEmpty() ? "GET" : methodInfo.getHttpMethods().get(0),
                    methodInfo.getUrlPatterns().isEmpty() ? "/" + methodInfo.getMethodName() : methodInfo.getUrlPatterns().get(0));
                endpoints.add(endpoint);
            }
        }
        
        logger.debug("Generated {} API endpoints", endpoints.size());
        return endpoints;
    }
    
    /**
     * Creates an ApiEndpoint from controller method information
     */
    private ApiEndpoint createApiEndpoint(ControllerMethodInfo methodInfo, String httpMethod, String urlPattern) {
        ApiEndpoint endpoint = new ApiEndpoint(urlPattern, httpMethod, methodInfo.getClassName(), methodInfo.getMethodName());
        
        // Analyze state alteration
        boolean altersState = stateAlterationAnalyzer.analyzesStateAlteration(methodInfo);
        endpoint.setAltersState(altersState);
        
        // Analyze validation
        List<String> validationRules = validationAnalyzer.analyzeValidation(methodInfo);
        endpoint.setValidation(validationRules);
        
        // Set method details
        ApiEndpoint.MethodDetails methodDetails = endpoint.getMethodDetails();
        methodDetails.setReturnType(methodInfo.getReturnType());
        methodDetails.setParameterTypes(methodInfo.getParameters().stream()
            .map(param -> param.getType())
            .collect(Collectors.toList()));
        
        // Add annotations
        methodInfo.getAnnotations().forEach((key, value) -> 
            methodDetails.addAnnotation(key + "=" + value));
        
        // Set transaction info
        methodDetails.getTransactionInfo().setTransactional(methodInfo.isTransactional());
        
        return endpoint;
    }
    
    /**
     * Updates the report summary with analysis statistics
     */
    private void updateReportSummary(ApiReport report) {
        ApiReport.AnalysisSummary summary = report.getAnalysisSummary();
        List<ApiEndpoint> endpoints = report.getApis();
        
        // Count state altering vs read-only APIs
        long stateAlteringCount = endpoints.stream().mapToLong(ep -> ep.isAltersState() ? 1 : 0).sum();
        summary.setStateAlteringApis((int) stateAlteringCount);
        summary.setReadOnlyApis(endpoints.size() - (int) stateAlteringCount);
        
        // Count validated APIs
        long validatedCount = endpoints.stream().mapToLong(ep -> ep.getValidation().isEmpty() ? 0 : 1).sum();
        summary.setValidatedApis((int) validatedCount);
        
        // Count unique controller classes
        Set<String> uniqueControllers = endpoints.stream()
            .map(ApiEndpoint::getControllerClass)
            .collect(Collectors.toSet());
        summary.setControllerClasses(uniqueControllers.size());
        
        // Calculate HTTP method distribution
        ApiReport.AnalysisSummary.HttpMethodDistribution httpDist = summary.getHttpMethodDistribution();
        for (ApiEndpoint endpoint : endpoints) {
            httpDist.incrementMethod(endpoint.getHttpMethod());
        }
    }
    
    /**
     * Creates a temporary directory for WAR extraction
     */
    private Path createTempDirectory(String warFileName) throws IOException {
        Path tempDir = Paths.get(tempDirectory);
        Files.createDirectories(tempDir);
        
        String dirName = "war_analysis_" + warFileName.replaceAll("[^a-zA-Z0-9]", "_") + "_" + System.currentTimeMillis();
        Path extractionDir = tempDir.resolve(dirName);
        Files.createDirectories(extractionDir);
        
        logger.debug("Created temporary extraction directory: {}", extractionDir);
        return extractionDir;
    }
    
    /**
     * Cleans up temporary extraction directory
     */
    private void cleanupTempDirectory(Path extractionDir) {
        try {
            if (Files.exists(extractionDir)) {
                try (var stream = Files.walk(extractionDir)) {
                    stream.sorted(Comparator.reverseOrder())
                          .map(Path::toFile)
                          .forEach(File::delete);
                }
                logger.debug("Cleaned up temporary directory: {}", extractionDir);
            }
        } catch (IOException e) {
            logger.warn("Failed to cleanup temporary directory {}: {}", extractionDir, e.getMessage());
        }
    }
}