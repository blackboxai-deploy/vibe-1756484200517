package com.analyzer;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;

/**
 * Main Spring Boot application class for WAR File API Analyzer.
 * This application performs static analysis on Spring REST WAR files
 * to generate comprehensive API reports.
 */
@SpringBootApplication(exclude = {DataSourceAutoConfiguration.class})
public class WarApiAnalyzerApplication {

    public static void main(String[] args) {
        SpringApplication.run(WarApiAnalyzerApplication.class, args);
        System.out.println("WAR API Analyzer started successfully!");
        System.out.println("Access the application at: http://localhost:8090");
        System.out.println("API Endpoint: POST /api/analyze - Analyze WAR file");
    }
}