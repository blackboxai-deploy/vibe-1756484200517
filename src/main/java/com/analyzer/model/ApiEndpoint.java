package com.analyzer.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.ArrayList;
import java.util.List;

/**
 * Represents a single API endpoint with all its characteristics.
 */
public class ApiEndpoint {
    
    @JsonProperty("api_url")
    private String apiUrl;
    
    @JsonProperty("http_method")
    private String httpMethod;
    
    @JsonProperty("controller_class")
    private String controllerClass;
    
    @JsonProperty("controller_method")
    private String controllerMethod;
    
    @JsonProperty("alters_state")
    private boolean altersState;
    
    @JsonProperty("validation")
    private List<String> validation;
    
    @JsonProperty("method_details")
    private MethodDetails methodDetails;
    
    public ApiEndpoint() {
        this.validation = new ArrayList<>();
        this.methodDetails = new MethodDetails();
    }
    
    public ApiEndpoint(String apiUrl, String httpMethod, String controllerClass, String controllerMethod) {
        this();
        this.apiUrl = apiUrl;
        this.httpMethod = httpMethod;
        this.controllerClass = controllerClass;
        this.controllerMethod = controllerMethod;
    }
    
    // Getters and Setters
    public String getApiUrl() {
        return apiUrl;
    }
    
    public void setApiUrl(String apiUrl) {
        this.apiUrl = apiUrl;
    }
    
    public String getHttpMethod() {
        return httpMethod;
    }
    
    public void setHttpMethod(String httpMethod) {
        this.httpMethod = httpMethod;
    }
    
    public String getControllerClass() {
        return controllerClass;
    }
    
    public void setControllerClass(String controllerClass) {
        this.controllerClass = controllerClass;
    }
    
    public String getControllerMethod() {
        return controllerMethod;
    }
    
    public void setControllerMethod(String controllerMethod) {
        this.controllerMethod = controllerMethod;
    }
    
    public boolean isAltersState() {
        return altersState;
    }
    
    public void setAltersState(boolean altersState) {
        this.altersState = altersState;
    }
    
    public List<String> getValidation() {
        return validation;
    }
    
    public void setValidation(List<String> validation) {
        this.validation = validation;
    }
    
    public void addValidation(String validationRule) {
        if (this.validation == null) {
            this.validation = new ArrayList<>();
        }
        this.validation.add(validationRule);
    }
    
    public MethodDetails getMethodDetails() {
        return methodDetails;
    }
    
    public void setMethodDetails(MethodDetails methodDetails) {
        this.methodDetails = methodDetails;
    }
    
    /**
     * Additional method details for comprehensive analysis
     */
    public static class MethodDetails {
        @JsonProperty("return_type")
        private String returnType;
        
        @JsonProperty("parameter_types")
        private List<String> parameterTypes;
        
        @JsonProperty("annotations")
        private List<String> annotations;
        
        @JsonProperty("transaction_attributes")
        private TransactionInfo transactionInfo;
        
        @JsonProperty("security_annotations")
        private List<String> securityAnnotations;
        
        @JsonProperty("produces")
        private List<String> produces;
        
        @JsonProperty("consumes")
        private List<String> consumes;
        
        public MethodDetails() {
            this.parameterTypes = new ArrayList<>();
            this.annotations = new ArrayList<>();
            this.securityAnnotations = new ArrayList<>();
            this.produces = new ArrayList<>();
            this.consumes = new ArrayList<>();
            this.transactionInfo = new TransactionInfo();
        }
        
        // Getters and Setters
        public String getReturnType() {
            return returnType;
        }
        
        public void setReturnType(String returnType) {
            this.returnType = returnType;
        }
        
        public List<String> getParameterTypes() {
            return parameterTypes;
        }
        
        public void setParameterTypes(List<String> parameterTypes) {
            this.parameterTypes = parameterTypes;
        }
        
        public void addParameterType(String parameterType) {
            if (this.parameterTypes == null) {
                this.parameterTypes = new ArrayList<>();
            }
            this.parameterTypes.add(parameterType);
        }
        
        public List<String> getAnnotations() {
            return annotations;
        }
        
        public void setAnnotations(List<String> annotations) {
            this.annotations = annotations;
        }
        
        public void addAnnotation(String annotation) {
            if (this.annotations == null) {
                this.annotations = new ArrayList<>();
            }
            this.annotations.add(annotation);
        }
        
        public TransactionInfo getTransactionInfo() {
            return transactionInfo;
        }
        
        public void setTransactionInfo(TransactionInfo transactionInfo) {
            this.transactionInfo = transactionInfo;
        }
        
        public List<String> getSecurityAnnotations() {
            return securityAnnotations;
        }
        
        public void setSecurityAnnotations(List<String> securityAnnotations) {
            this.securityAnnotations = securityAnnotations;
        }
        
        public void addSecurityAnnotation(String securityAnnotation) {
            if (this.securityAnnotations == null) {
                this.securityAnnotations = new ArrayList<>();
            }
            this.securityAnnotations.add(securityAnnotation);
        }
        
        public List<String> getProduces() {
            return produces;
        }
        
        public void setProduces(List<String> produces) {
            this.produces = produces;
        }
        
        public void addProduces(String mediaType) {
            if (this.produces == null) {
                this.produces = new ArrayList<>();
            }
            this.produces.add(mediaType);
        }
        
        public List<String> getConsumes() {
            return consumes;
        }
        
        public void setConsumes(List<String> consumes) {
            this.consumes = consumes;
        }
        
        public void addConsumes(String mediaType) {
            if (this.consumes == null) {
                this.consumes = new ArrayList<>();
            }
            this.consumes.add(mediaType);
        }
    }
    
    /**
     * Transaction-related information
     */
    public static class TransactionInfo {
        @JsonProperty("is_transactional")
        private boolean isTransactional;
        
        @JsonProperty("propagation")
        private String propagation;
        
        @JsonProperty("isolation")
        private String isolation;
        
        @JsonProperty("read_only")
        private boolean readOnly;
        
        // Getters and Setters
        public boolean isTransactional() {
            return isTransactional;
        }
        
        public void setTransactional(boolean transactional) {
            isTransactional = transactional;
        }
        
        public String getPropagation() {
            return propagation;
        }
        
        public void setPropagation(String propagation) {
            this.propagation = propagation;
        }
        
        public String getIsolation() {
            return isolation;
        }
        
        public void setIsolation(String isolation) {
            this.isolation = isolation;
        }
        
        public boolean isReadOnly() {
            return readOnly;
        }
        
        public void setReadOnly(boolean readOnly) {
            this.readOnly = readOnly;
        }
    }
}