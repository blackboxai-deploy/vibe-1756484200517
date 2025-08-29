package com.analyzer.model;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.HashMap;

/**
 * Detailed information about a controller method gathered during analysis.
 */
public class ControllerMethodInfo {
    
    private String className;
    private String methodName;
    private String methodSignature;
    private List<String> httpMethods;
    private List<String> urlPatterns;
    private Map<String, Object> annotations;
    private List<ParameterInfo> parameters;
    private String returnType;
    private boolean isTransactional;
    private List<String> calledMethods;
    private List<String> validationRules;
    
    public ControllerMethodInfo() {
        this.httpMethods = new ArrayList<>();
        this.urlPatterns = new ArrayList<>();
        this.annotations = new HashMap<>();
        this.parameters = new ArrayList<>();
        this.calledMethods = new ArrayList<>();
        this.validationRules = new ArrayList<>();
    }
    
    public ControllerMethodInfo(String className, String methodName) {
        this();
        this.className = className;
        this.methodName = methodName;
    }
    
    // Getters and Setters
    public String getClassName() {
        return className;
    }
    
    public void setClassName(String className) {
        this.className = className;
    }
    
    public String getMethodName() {
        return methodName;
    }
    
    public void setMethodName(String methodName) {
        this.methodName = methodName;
    }
    
    public String getMethodSignature() {
        return methodSignature;
    }
    
    public void setMethodSignature(String methodSignature) {
        this.methodSignature = methodSignature;
    }
    
    public List<String> getHttpMethods() {
        return httpMethods;
    }
    
    public void setHttpMethods(List<String> httpMethods) {
        this.httpMethods = httpMethods;
    }
    
    public void addHttpMethod(String httpMethod) {
        if (this.httpMethods == null) {
            this.httpMethods = new ArrayList<>();
        }
        if (!this.httpMethods.contains(httpMethod)) {
            this.httpMethods.add(httpMethod);
        }
    }
    
    public List<String> getUrlPatterns() {
        return urlPatterns;
    }
    
    public void setUrlPatterns(List<String> urlPatterns) {
        this.urlPatterns = urlPatterns;
    }
    
    public void addUrlPattern(String urlPattern) {
        if (this.urlPatterns == null) {
            this.urlPatterns = new ArrayList<>();
        }
        if (!this.urlPatterns.contains(urlPattern)) {
            this.urlPatterns.add(urlPattern);
        }
    }
    
    public Map<String, Object> getAnnotations() {
        return annotations;
    }
    
    public void setAnnotations(Map<String, Object> annotations) {
        this.annotations = annotations;
    }
    
    public void addAnnotation(String annotationName, Object value) {
        if (this.annotations == null) {
            this.annotations = new HashMap<>();
        }
        this.annotations.put(annotationName, value);
    }
    
    public List<ParameterInfo> getParameters() {
        return parameters;
    }
    
    public void setParameters(List<ParameterInfo> parameters) {
        this.parameters = parameters;
    }
    
    public void addParameter(ParameterInfo parameter) {
        if (this.parameters == null) {
            this.parameters = new ArrayList<>();
        }
        this.parameters.add(parameter);
    }
    
    public String getReturnType() {
        return returnType;
    }
    
    public void setReturnType(String returnType) {
        this.returnType = returnType;
    }
    
    public boolean isTransactional() {
        return isTransactional;
    }
    
    public void setTransactional(boolean transactional) {
        isTransactional = transactional;
    }
    
    public List<String> getCalledMethods() {
        return calledMethods;
    }
    
    public void setCalledMethods(List<String> calledMethods) {
        this.calledMethods = calledMethods;
    }
    
    public void addCalledMethod(String methodCall) {
        if (this.calledMethods == null) {
            this.calledMethods = new ArrayList<>();
        }
        this.calledMethods.add(methodCall);
    }
    
    public List<String> getValidationRules() {
        return validationRules;
    }
    
    public void setValidationRules(List<String> validationRules) {
        this.validationRules = validationRules;
    }
    
    public void addValidationRule(String validationRule) {
        if (this.validationRules == null) {
            this.validationRules = new ArrayList<>();
        }
        this.validationRules.add(validationRule);
    }
    
    /**
     * Information about method parameters
     */
    public static class ParameterInfo {
        private String name;
        private String type;
        private List<String> annotations;
        private boolean isValidated;
        
        public ParameterInfo() {
            this.annotations = new ArrayList<>();
        }
        
        public ParameterInfo(String name, String type) {
            this();
            this.name = name;
            this.type = type;
        }
        
        // Getters and Setters
        public String getName() {
            return name;
        }
        
        public void setName(String name) {
            this.name = name;
        }
        
        public String getType() {
            return type;
        }
        
        public void setType(String type) {
            this.type = type;
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
        
        public boolean isValidated() {
            return isValidated;
        }
        
        public void setValidated(boolean validated) {
            isValidated = validated;
        }
    }
    
    /**
     * Utility method to check if this method alters state based on HTTP methods and called methods
     */
    public boolean probablyAltersState() {
        // Check HTTP methods that typically alter state
        if (httpMethods.contains("POST") || httpMethods.contains("PUT") || 
            httpMethods.contains("DELETE") || httpMethods.contains("PATCH")) {
            return true;
        }
        
        // Check for transactional methods that are not read-only
        if (isTransactional) {
            return true;
        }
        
        // Check method calls for persistence operations
        for (String methodCall : calledMethods) {
            if (methodCall.contains("save") || methodCall.contains("update") || 
                methodCall.contains("delete") || methodCall.contains("persist") ||
                methodCall.contains("merge") || methodCall.contains("remove")) {
                return true;
            }
        }
        
        return false;
    }
    
    /**
     * Utility method to check if this method has validation
     */
    public boolean hasValidation() {
        if (!validationRules.isEmpty()) {
            return true;
        }
        
        // Check parameters for validation annotations
        for (ParameterInfo param : parameters) {
            if (param.isValidated() || !param.getAnnotations().isEmpty()) {
                return true;
            }
        }
        
        return false;
    }
}