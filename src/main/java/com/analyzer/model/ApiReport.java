package com.analyzer.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Main report entity containing comprehensive API analysis results.
 */
public class ApiReport {
    
    @JsonProperty("war_file_name")
    private String warFileName;
    
    @JsonProperty("analysis_date")
    private LocalDateTime analysisDate;
    
    @JsonProperty("total_apis")
    private int totalApis;
    
    @JsonProperty("analysis_summary")
    private AnalysisSummary analysisSummary;
    
    @JsonProperty("apis")
    private List<ApiEndpoint> apis;
    
    public ApiReport() {
        this.analysisDate = LocalDateTime.now();
        this.apis = new ArrayList<>();
        this.analysisSummary = new AnalysisSummary();
    }
    
    public ApiReport(String warFileName) {
        this();
        this.warFileName = warFileName;
    }
    
    // Getters and Setters
    public String getWarFileName() {
        return warFileName;
    }
    
    public void setWarFileName(String warFileName) {
        this.warFileName = warFileName;
    }
    
    public LocalDateTime getAnalysisDate() {
        return analysisDate;
    }
    
    public void setAnalysisDate(LocalDateTime analysisDate) {
        this.analysisDate = analysisDate;
    }
    
    public int getTotalApis() {
        return totalApis;
    }
    
    public void setTotalApis(int totalApis) {
        this.totalApis = totalApis;
    }
    
    public AnalysisSummary getAnalysisSummary() {
        return analysisSummary;
    }
    
    public void setAnalysisSummary(AnalysisSummary analysisSummary) {
        this.analysisSummary = analysisSummary;
    }
    
    public List<ApiEndpoint> getApis() {
        return apis;
    }
    
    public void setApis(List<ApiEndpoint> apis) {
        this.apis = apis;
        this.totalApis = apis != null ? apis.size() : 0;
    }
    
    public void addApiEndpoint(ApiEndpoint endpoint) {
        if (this.apis == null) {
            this.apis = new ArrayList<>();
        }
        this.apis.add(endpoint);
        this.totalApis = this.apis.size();
    }
    
    /**
     * Summary statistics for the analysis
     */
    public static class AnalysisSummary {
        @JsonProperty("state_altering_apis")
        private int stateAlteringApis;
        
        @JsonProperty("read_only_apis")
        private int readOnlyApis;
        
        @JsonProperty("validated_apis")
        private int validatedApis;
        
        @JsonProperty("controller_classes")
        private int controllerClasses;
        
        @JsonProperty("http_methods_distribution")
        private HttpMethodDistribution httpMethodDistribution;
        
        public AnalysisSummary() {
            this.httpMethodDistribution = new HttpMethodDistribution();
        }
        
        // Getters and Setters
        public int getStateAlteringApis() {
            return stateAlteringApis;
        }
        
        public void setStateAlteringApis(int stateAlteringApis) {
            this.stateAlteringApis = stateAlteringApis;
        }
        
        public int getReadOnlyApis() {
            return readOnlyApis;
        }
        
        public void setReadOnlyApis(int readOnlyApis) {
            this.readOnlyApis = readOnlyApis;
        }
        
        public int getValidatedApis() {
            return validatedApis;
        }
        
        public void setValidatedApis(int validatedApis) {
            this.validatedApis = validatedApis;
        }
        
        public int getControllerClasses() {
            return controllerClasses;
        }
        
        public void setControllerClasses(int controllerClasses) {
            this.controllerClasses = controllerClasses;
        }
        
        public HttpMethodDistribution getHttpMethodDistribution() {
            return httpMethodDistribution;
        }
        
        public void setHttpMethodDistribution(HttpMethodDistribution httpMethodDistribution) {
            this.httpMethodDistribution = httpMethodDistribution;
        }
    }
    
    /**
     * HTTP method distribution statistics
     */
    public static class HttpMethodDistribution {
        @JsonProperty("GET")
        private int get;
        
        @JsonProperty("POST")
        private int post;
        
        @JsonProperty("PUT")
        private int put;
        
        @JsonProperty("DELETE")
        private int delete;
        
        @JsonProperty("PATCH")
        private int patch;
        
        @JsonProperty("OPTIONS")
        private int options;
        
        @JsonProperty("HEAD")
        private int head;
        
        // Getters and Setters
        public int getGet() {
            return get;
        }
        
        public void setGet(int get) {
            this.get = get;
        }
        
        public int getPost() {
            return post;
        }
        
        public void setPost(int post) {
            this.post = post;
        }
        
        public int getPut() {
            return put;
        }
        
        public void setPut(int put) {
            this.put = put;
        }
        
        public int getDelete() {
            return delete;
        }
        
        public void setDelete(int delete) {
            this.delete = delete;
        }
        
        public int getPatch() {
            return patch;
        }
        
        public void setPatch(int patch) {
            this.patch = patch;
        }
        
        public int getOptions() {
            return options;
        }
        
        public void setOptions(int options) {
            this.options = options;
        }
        
        public int getHead() {
            return head;
        }
        
        public void setHead(int head) {
            this.head = head;
        }
        
        public void incrementMethod(String method) {
            switch (method.toUpperCase()) {
                case "GET" -> get++;
                case "POST" -> post++;
                case "PUT" -> put++;
                case "DELETE" -> delete++;
                case "PATCH" -> patch++;
                case "OPTIONS" -> options++;
                case "HEAD" -> head++;
            }
        }
    }
}