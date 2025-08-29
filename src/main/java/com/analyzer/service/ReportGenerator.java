package com.analyzer.service;

import com.analyzer.model.ApiReport;
import com.analyzer.model.ApiEndpoint;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Service for generating reports in various formats (JSON, CSV, HTML).
 * Formats the API analysis results into human-readable and machine-readable reports.
 */
@Service
public class ReportGenerator {
    
    private static final Logger logger = LoggerFactory.getLogger(ReportGenerator.class);
    
    private final ObjectMapper objectMapper;
    
    public ReportGenerator() {
        this.objectMapper = new ObjectMapper();
        this.objectMapper.registerModule(new JavaTimeModule());
        this.objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        this.objectMapper.enable(SerializationFeature.INDENT_OUTPUT);
    }
    
    /**
     * Generates a JSON report from the API analysis.
     * 
     * @param report The API analysis report
     * @return JSON string representation of the report
     */
    public String generateJsonReport(ApiReport report) {
        try {
            logger.debug("Generating JSON report for {} APIs", report.getTotalApis());
            return objectMapper.writeValueAsString(report);
        } catch (Exception e) {
            logger.error("Error generating JSON report: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to generate JSON report", e);
        }
    }
    
    /**
     * Generates a CSV report from the API analysis.
     * 
     * @param report The API analysis report
     * @return CSV string representation of the report
     */
    public String generateCsvReport(ApiReport report) {
        try {
            logger.debug("Generating CSV report for {} APIs", report.getTotalApis());
            
            StringWriter stringWriter = new StringWriter();
            
            CSVFormat csvFormat = CSVFormat.DEFAULT
                .withHeader("API_URL", "HTTP_METHOD", "Controller_Class", "Controller_Method", 
                           "Alters_State", "Validation", "Return_Type", "Parameter_Types", 
                           "Is_Transactional", "Produces", "Consumes")
                .withFirstRecordAsHeader();
            
            try (CSVPrinter csvPrinter = new CSVPrinter(stringWriter, csvFormat)) {
                
                for (ApiEndpoint endpoint : report.getApis()) {
                    csvPrinter.printRecord(
                        endpoint.getApiUrl(),
                        endpoint.getHttpMethod(),
                        endpoint.getControllerClass(),
                        endpoint.getControllerMethod(),
                        endpoint.isAltersState(),
                        String.join("; ", endpoint.getValidation()),
                        endpoint.getMethodDetails().getReturnType(),
                        String.join(", ", endpoint.getMethodDetails().getParameterTypes()),
                        endpoint.getMethodDetails().getTransactionInfo().isTransactional(),
                        String.join(", ", endpoint.getMethodDetails().getProduces()),
                        String.join(", ", endpoint.getMethodDetails().getConsumes())
                    );
                }
            }
            
            return stringWriter.toString();
            
        } catch (Exception e) {
            logger.error("Error generating CSV report: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to generate CSV report", e);
        }
    }
    
    /**
     * Generates an HTML report from the API analysis.
     * 
     * @param report The API analysis report
     * @return HTML string representation of the report
     */
    public String generateHtmlReport(ApiReport report) {
        try {
            logger.debug("Generating HTML report for {} APIs", report.getTotalApis());
            
            StringBuilder html = new StringBuilder();
            
            // HTML header
            html.append("<!DOCTYPE html>\n")
                .append("<html lang=\"en\">\n")
                .append("<head>\n")
                .append("    <meta charset=\"UTF-8\">\n")
                .append("    <meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">\n")
                .append("    <title>WAR File API Analysis Report</title>\n")
                .append("    <style>\n")
                .append(getHtmlStyles())
                .append("    </style>\n")
                .append("</head>\n")
                .append("<body>\n");
            
            // Report header
            html.append(generateHtmlHeader(report));
            
            // Summary section
            html.append(generateHtmlSummary(report));
            
            // API endpoints table
            html.append(generateHtmlApiTable(report));
            
            // Footer
            html.append("    <footer>\n")
                .append("        <p>Generated by WAR API Analyzer on ")
                .append(report.getAnalysisDate().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")))
                .append("</p>\n")
                .append("    </footer>\n")
                .append("</body>\n")
                .append("</html>");
            
            return html.toString();
            
        } catch (Exception e) {
            logger.error("Error generating HTML report: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to generate HTML report", e);
        }
    }
    
    /**
     * Generates HTML styles for the report
     */
    private String getHtmlStyles() {
        return """
            body {
                font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif;
                margin: 0;
                padding: 20px;
                background-color: #f5f5f5;
                color: #333;
            }
            .container {
                max-width: 1200px;
                margin: 0 auto;
                background-color: white;
                padding: 30px;
                border-radius: 8px;
                box-shadow: 0 2px 10px rgba(0,0,0,0.1);
            }
            h1 {
                color: #2c3e50;
                border-bottom: 3px solid #3498db;
                padding-bottom: 10px;
                margin-bottom: 30px;
            }
            h2 {
                color: #34495e;
                margin-top: 30px;
                margin-bottom: 15px;
            }
            .summary {
                display: grid;
                grid-template-columns: repeat(auto-fit, minmax(200px, 1fr));
                gap: 20px;
                margin-bottom: 30px;
            }
            .summary-card {
                background-color: #ecf0f1;
                padding: 20px;
                border-radius: 6px;
                text-align: center;
                border-left: 4px solid #3498db;
            }
            .summary-card h3 {
                margin: 0 0 10px 0;
                color: #2c3e50;
            }
            .summary-card .number {
                font-size: 2em;
                font-weight: bold;
                color: #3498db;
            }
            table {
                width: 100%;
                border-collapse: collapse;
                margin-top: 20px;
                font-size: 14px;
            }
            th, td {
                padding: 12px;
                text-align: left;
                border-bottom: 1px solid #ddd;
            }
            th {
                background-color: #34495e;
                color: white;
                font-weight: bold;
                position: sticky;
                top: 0;
            }
            tr:nth-child(even) {
                background-color: #f8f9fa;
            }
            tr:hover {
                background-color: #e8f4f8;
            }
            .http-method {
                display: inline-block;
                padding: 4px 8px;
                border-radius: 4px;
                font-weight: bold;
                font-size: 12px;
                color: white;
            }
            .method-GET { background-color: #27ae60; }
            .method-POST { background-color: #f39c12; }
            .method-PUT { background-color: #3498db; }
            .method-DELETE { background-color: #e74c3c; }
            .method-PATCH { background-color: #9b59b6; }
            .alters-state-true {
                color: #e74c3c;
                font-weight: bold;
            }
            .alters-state-false {
                color: #27ae60;
                font-weight: bold;
            }
            .validation-list {
                max-width: 300px;
                word-wrap: break-word;
            }
            footer {
                margin-top: 40px;
                text-align: center;
                color: #7f8c8d;
                font-size: 12px;
            }
            """;
    }
    
    /**
     * Generates HTML header section
     */
    private String generateHtmlHeader(ApiReport report) {
        return String.format("""
            <div class="container">
                <h1>WAR File API Analysis Report</h1>
                <div class="report-info">
                    <p><strong>WAR File:</strong> %s</p>
                    <p><strong>Analysis Date:</strong> %s</p>
                    <p><strong>Total APIs Found:</strong> %d</p>
                </div>
            """,
            report.getWarFileName(),
            report.getAnalysisDate().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")),
            report.getTotalApis()
        );
    }
    
    /**
     * Generates HTML summary section
     */
    private String generateHtmlSummary(ApiReport report) {
        ApiReport.AnalysisSummary summary = report.getAnalysisSummary();
        ApiReport.AnalysisSummary.HttpMethodDistribution httpDist = summary.getHttpMethodDistribution();
        
        return String.format("""
            <h2>Analysis Summary</h2>
            <div class="summary">
                <div class="summary-card">
                    <h3>Controller Classes</h3>
                    <div class="number">%d</div>
                </div>
                <div class="summary-card">
                    <h3>State Altering APIs</h3>
                    <div class="number">%d</div>
                </div>
                <div class="summary-card">
                    <h3>Read-Only APIs</h3>
                    <div class="number">%d</div>
                </div>
                <div class="summary-card">
                    <h3>Validated APIs</h3>
                    <div class="number">%d</div>
                </div>
            </div>
            
            <h2>HTTP Method Distribution</h2>
            <div class="summary">
                <div class="summary-card">
                    <h3>GET</h3>
                    <div class="number">%d</div>
                </div>
                <div class="summary-card">
                    <h3>POST</h3>
                    <div class="number">%d</div>
                </div>
                <div class="summary-card">
                    <h3>PUT</h3>
                    <div class="number">%d</div>
                </div>
                <div class="summary-card">
                    <h3>DELETE</h3>
                    <div class="number">%d</div>
                </div>
            </div>
            """,
            summary.getControllerClasses(),
            summary.getStateAlteringApis(),
            summary.getReadOnlyApis(),
            summary.getValidatedApis(),
            httpDist.getGet(),
            httpDist.getPost(),
            httpDist.getPut(),
            httpDist.getDelete()
        );
    }
    
    /**
     * Generates HTML API endpoints table
     */
    private String generateHtmlApiTable(ApiReport report) {
        StringBuilder table = new StringBuilder();
        
        table.append("<h2>API Endpoints</h2>\n")
             .append("<table>\n")
             .append("    <thead>\n")
             .append("        <tr>\n")
             .append("            <th>API URL</th>\n")
             .append("            <th>HTTP Method</th>\n")
             .append("            <th>Controller Class</th>\n")
             .append("            <th>Controller Method</th>\n")
             .append("            <th>Alters State</th>\n")
             .append("            <th>Validation</th>\n")
             .append("        </tr>\n")
             .append("    </thead>\n")
             .append("    <tbody>\n");
        
        for (ApiEndpoint endpoint : report.getApis()) {
            table.append("        <tr>\n")
                 .append("            <td>").append(escapeHtml(endpoint.getApiUrl())).append("</td>\n")
                 .append("            <td><span class=\"http-method method-")
                 .append(endpoint.getHttpMethod()).append("\">")
                 .append(endpoint.getHttpMethod()).append("</span></td>\n")
                 .append("            <td>").append(escapeHtml(endpoint.getControllerClass())).append("</td>\n")
                 .append("            <td>").append(escapeHtml(endpoint.getControllerMethod())).append("</td>\n")
                 .append("            <td><span class=\"alters-state-").append(endpoint.isAltersState()).append("\">")
                 .append(endpoint.isAltersState() ? "YES" : "NO").append("</span></td>\n")
                 .append("            <td><div class=\"validation-list\">")
                 .append(endpoint.getValidation().isEmpty() ? "None" : 
                        String.join("<br>", endpoint.getValidation().stream()
                            .map(this::escapeHtml).toList()))
                 .append("</div></td>\n")
                 .append("        </tr>\n");
        }
        
        table.append("    </tbody>\n")
             .append("</table>\n")
             .append("</div>\n");
        
        return table.toString();
    }
    
    /**
     * Escapes HTML special characters
     */
    private String escapeHtml(String text) {
        if (text == null) {
            return "";
        }
        return text.replace("&", "&amp;")
                  .replace("<", "&lt;")
                  .replace(">", "&gt;")
                  .replace("\"", "&quot;")
                  .replace("'", "&#x27;");
    }
    
    /**
     * Saves a report to a file
     * 
     * @param content Report content
     * @param outputPath Path where to save the file
     * @throws IOException If file cannot be written
     */
    public void saveReportToFile(String content, Path outputPath) throws IOException {
        logger.info("Saving report to: {}", outputPath);
        Files.writeString(outputPath, content);
        logger.info("Report saved successfully");
    }
    
    /**
     * Generates a summary report with key metrics
     * 
     * @param report The API analysis report
     * @return Summary string
     */
    public String generateSummaryReport(ApiReport report) {
        ApiReport.AnalysisSummary summary = report.getAnalysisSummary();
        
        StringBuilder summaryText = new StringBuilder();
        summaryText.append("=== WAR FILE API ANALYSIS SUMMARY ===\n");
        summaryText.append("WAR File: ").append(report.getWarFileName()).append("\n");
        summaryText.append("Analysis Date: ").append(
            report.getAnalysisDate().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))
        ).append("\n\n");
        
        summaryText.append("OVERVIEW:\n");
        summaryText.append("- Total API Endpoints: ").append(report.getTotalApis()).append("\n");
        summaryText.append("- Controller Classes: ").append(summary.getControllerClasses()).append("\n");
        summaryText.append("- State Altering APIs: ").append(summary.getStateAlteringApis()).append("\n");
        summaryText.append("- Read-Only APIs: ").append(summary.getReadOnlyApis()).append("\n");
        summaryText.append("- Validated APIs: ").append(summary.getValidatedApis()).append("\n\n");
        
        ApiReport.AnalysisSummary.HttpMethodDistribution httpDist = summary.getHttpMethodDistribution();
        summaryText.append("HTTP METHOD DISTRIBUTION:\n");
        summaryText.append("- GET: ").append(httpDist.getGet()).append("\n");
        summaryText.append("- POST: ").append(httpDist.getPost()).append("\n");
        summaryText.append("- PUT: ").append(httpDist.getPut()).append("\n");
        summaryText.append("- DELETE: ").append(httpDist.getDelete()).append("\n");
        summaryText.append("- PATCH: ").append(httpDist.getPatch()).append("\n");
        summaryText.append("- OPTIONS: ").append(httpDist.getOptions()).append("\n");
        summaryText.append("- HEAD: ").append(httpDist.getHead()).append("\n\n");
        
        // Calculate percentages
        if (report.getTotalApis() > 0) {
            double stateAlteringPercentage = (double) summary.getStateAlteringApis() / report.getTotalApis() * 100;
            double validatedPercentage = (double) summary.getValidatedApis() / report.getTotalApis() * 100;
            
            summaryText.append("ANALYSIS INSIGHTS:\n");
            summaryText.append(String.format("- %.1f%% of APIs alter persistent state\n", stateAlteringPercentage));
            summaryText.append(String.format("- %.1f%% of APIs have validation rules\n", validatedPercentage));
            
            if (stateAlteringPercentage > 50) {
                summaryText.append("- HIGH state mutation: Consider reviewing transaction boundaries\n");
            }
            
            if (validatedPercentage < 30) {
                summaryText.append("- LOW validation coverage: Consider adding more input validation\n");
            }
        }
        
        return summaryText.toString();
    }
}