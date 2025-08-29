package com.analyzer.controller;

import com.analyzer.model.ApiReport;
import com.analyzer.service.ReportGenerator;
import com.analyzer.service.WarFileAnalyzer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * REST controller for WAR file analysis operations.
 * Provides endpoints for analyzing WAR files and generating reports.
 */
@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "*", maxAge = 3600)
public class ApiAnalysisController {
    
    private static final Logger logger = LoggerFactory.getLogger(ApiAnalysisController.class);
    
    @Autowired
    private WarFileAnalyzer warFileAnalyzer;
    
    @Autowired
    private ReportGenerator reportGenerator;
    
    /**
     * Analyzes a WAR file and returns the API report in JSON format.
     * 
     * @param request Analysis request containing the WAR file path
     * @return ApiReport with comprehensive analysis results
     */
    @PostMapping(value = "/analyze", 
                 consumes = MediaType.APPLICATION_JSON_VALUE,
                 produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> analyzeWarFile(@RequestBody AnalysisRequest request) {
        logger.info("Received WAR file analysis request for: {}", request.getWarFilePath());
        
        try {
            // Validate request
            if (request.getWarFilePath() == null || request.getWarFilePath().trim().isEmpty()) {
                return ResponseEntity.badRequest()
                    .body(createErrorResponse("WAR file path is required"));
            }
            
            // Perform analysis
            ApiReport report = warFileAnalyzer.analyzeWarFile(request.getWarFilePath().trim());
            
            logger.info("Analysis completed successfully. Found {} API endpoints", report.getTotalApis());
            return ResponseEntity.ok(report);
            
        } catch (IOException e) {
            logger.error("IO error analyzing WAR file: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(createErrorResponse("Failed to read WAR file: " + e.getMessage()));
                
        } catch (Exception e) {
            logger.error("Unexpected error analyzing WAR file: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(createErrorResponse("Analysis failed: " + e.getMessage()));
        }
    }
    
    /**
     * Analyzes a WAR file and returns the report in CSV format.
     * 
     * @param request Analysis request containing the WAR file path
     * @return CSV report as plain text
     */
    @PostMapping(value = "/analyze/csv",
                 consumes = MediaType.APPLICATION_JSON_VALUE,
                 produces = "text/csv")
    public ResponseEntity<?> analyzeWarFileCsv(@RequestBody AnalysisRequest request) {
        logger.info("Received WAR file analysis request (CSV) for: {}", request.getWarFilePath());
        
        try {
            // Validate request
            if (request.getWarFilePath() == null || request.getWarFilePath().trim().isEmpty()) {
                return ResponseEntity.badRequest()
                    .body("WAR file path is required");
            }
            
            // Perform analysis
            ApiReport report = warFileAnalyzer.analyzeWarFile(request.getWarFilePath().trim());
            String csvReport = reportGenerator.generateCsvReport(report);
            
            logger.info("CSV analysis completed successfully. Found {} API endpoints", report.getTotalApis());
            return ResponseEntity.ok()
                .header("Content-Disposition", "attachment; filename=\"api-report.csv\"")
                .body(csvReport);
                
        } catch (IOException e) {
            logger.error("IO error analyzing WAR file for CSV: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body("Failed to read WAR file: " + e.getMessage());
                
        } catch (Exception e) {
            logger.error("Unexpected error analyzing WAR file for CSV: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body("Analysis failed: " + e.getMessage());
        }
    }
    
    /**
     * Analyzes a WAR file and returns the report in HTML format.
     * 
     * @param request Analysis request containing the WAR file path
     * @return HTML report
     */
    @PostMapping(value = "/analyze/html",
                 consumes = MediaType.APPLICATION_JSON_VALUE,
                 produces = MediaType.TEXT_HTML_VALUE)
    public ResponseEntity<?> analyzeWarFileHtml(@RequestBody AnalysisRequest request) {
        logger.info("Received WAR file analysis request (HTML) for: {}", request.getWarFilePath());
        
        try {
            // Validate request
            if (request.getWarFilePath() == null || request.getWarFilePath().trim().isEmpty()) {
                return ResponseEntity.badRequest()
                    .body("<html><body><h1>Error</h1><p>WAR file path is required</p></body></html>");
            }
            
            // Perform analysis
            ApiReport report = warFileAnalyzer.analyzeWarFile(request.getWarFilePath().trim());
            String htmlReport = reportGenerator.generateHtmlReport(report);
            
            logger.info("HTML analysis completed successfully. Found {} API endpoints", report.getTotalApis());
            return ResponseEntity.ok(htmlReport);
                
        } catch (IOException e) {
            logger.error("IO error analyzing WAR file for HTML: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body("<html><body><h1>Error</h1><p>Failed to read WAR file: " + e.getMessage() + "</p></body></html>");
                
        } catch (Exception e) {
            logger.error("Unexpected error analyzing WAR file for HTML: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body("<html><body><h1>Error</h1><p>Analysis failed: " + e.getMessage() + "</p></body></html>");
        }
    }
    
    /**
     * Gets a summary of the analysis for a WAR file.
     * 
     * @param request Analysis request containing the WAR file path
     * @return Analysis summary
     */
    @PostMapping(value = "/analyze/summary",
                 consumes = MediaType.APPLICATION_JSON_VALUE,
                 produces = MediaType.TEXT_PLAIN_VALUE)
    public ResponseEntity<?> analyzeWarFileSummary(@RequestBody AnalysisRequest request) {
        logger.info("Received WAR file analysis summary request for: {}", request.getWarFilePath());
        
        try {
            // Validate request
            if (request.getWarFilePath() == null || request.getWarFilePath().trim().isEmpty()) {
                return ResponseEntity.badRequest()
                    .body("WAR file path is required");
            }
            
            // Perform analysis
            ApiReport report = warFileAnalyzer.analyzeWarFile(request.getWarFilePath().trim());
            String summaryReport = reportGenerator.generateSummaryReport(report);
            
            logger.info("Summary analysis completed successfully. Found {} API endpoints", report.getTotalApis());
            return ResponseEntity.ok(summaryReport);
                
        } catch (IOException e) {
            logger.error("IO error analyzing WAR file for summary: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body("Failed to read WAR file: " + e.getMessage());
                
        } catch (Exception e) {
            logger.error("Unexpected error analyzing WAR file for summary: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body("Analysis failed: " + e.getMessage());
        }
    }
    
    /**
     * Health check endpoint.
     * 
     * @return Service status
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> healthCheck() {
        Map<String, Object> status = new HashMap<>();
        status.put("status", "UP");
        status.put("service", "WAR API Analyzer");
        status.put("version", "1.0.0");
        status.put("timestamp", System.currentTimeMillis());
        
        return ResponseEntity.ok(status);
    }
    
    /**
     * Gets API documentation and usage information.
     * 
     * @return API documentation
     */
    @GetMapping(value = "/docs", produces = MediaType.TEXT_PLAIN_VALUE)
    public ResponseEntity<String> getApiDocumentation() {
        String docs = """
            === WAR API ANALYZER - API DOCUMENTATION ===
            
            This service analyzes Spring Boot WAR files to generate comprehensive API reports.
            
            ENDPOINTS:
            
            1. POST /api/analyze
               - Description: Analyzes a WAR file and returns detailed JSON report
               - Content-Type: application/json
               - Request Body: {"warFilePath": "/path/to/your/app.war"}
               - Response: Complete API analysis in JSON format
               
            2. POST /api/analyze/csv
               - Description: Analyzes a WAR file and returns CSV report
               - Content-Type: application/json
               - Request Body: {"warFilePath": "/path/to/your/app.war"}
               - Response: CSV formatted report (downloadable)
               
            3. POST /api/analyze/html
               - Description: Analyzes a WAR file and returns HTML report
               - Content-Type: application/json
               - Request Body: {"warFilePath": "/path/to/your/app.war"}
               - Response: HTML formatted report with styling
               
            4. POST /api/analyze/summary
               - Description: Analyzes a WAR file and returns text summary
               - Content-Type: application/json
               - Request Body: {"warFilePath": "/path/to/your/app.war"}
               - Response: Plain text summary of key metrics
               
            5. GET /api/health
               - Description: Service health check
               - Response: Service status information
               
            6. GET /api/docs
               - Description: This documentation
               - Response: API usage information
            
            REPORT FIELDS:
            - API_URL: The URL pattern for the API endpoint
            - HTTP_METHOD: HTTP method (GET, POST, PUT, DELETE, etc.)
            - Controller_Class: Full class name of the controller
            - Controller_Method: Method name handling the request
            - Alters_State: Boolean indicating if API modifies persistent state
            - Validation: List of validation rules applied to the endpoint
            
            EXAMPLE USAGE:
            curl -X POST http://localhost:8090/api/analyze \\
                 -H "Content-Type: application/json" \\
                 -d '{"warFilePath": "/path/to/your/application.war"}'
            
            NOTE: WAR file must be accessible to the analyzer service.
            """;
        
        return ResponseEntity.ok(docs);
    }
    
    /**
     * Creates standardized error response
     */
    private Map<String, Object> createErrorResponse(String message) {
        Map<String, Object> error = new HashMap<>();
        error.put("error", true);
        error.put("message", message);
        error.put("timestamp", System.currentTimeMillis());
        return error;
    }
    
    /**
     * Request object for analysis operations
     */
    public static class AnalysisRequest {
        private String warFilePath;
        private String outputFormat = "json";
        private boolean includeDetails = true;
        private boolean includeValidationDetails = true;
        
        // Default constructor
        public AnalysisRequest() {}
        
        public AnalysisRequest(String warFilePath) {
            this.warFilePath = warFilePath;
        }
        
        // Getters and Setters
        public String getWarFilePath() {
            return warFilePath;
        }
        
        public void setWarFilePath(String warFilePath) {
            this.warFilePath = warFilePath;
        }
        
        public String getOutputFormat() {
            return outputFormat;
        }
        
        public void setOutputFormat(String outputFormat) {
            this.outputFormat = outputFormat;
        }
        
        public boolean isIncludeDetails() {
            return includeDetails;
        }
        
        public void setIncludeDetails(boolean includeDetails) {
            this.includeDetails = includeDetails;
        }
        
        public boolean isIncludeValidationDetails() {
            return includeValidationDetails;
        }
        
        public void setIncludeValidationDetails(boolean includeValidationDetails) {
            this.includeValidationDetails = includeValidationDetails;
        }
    }
}