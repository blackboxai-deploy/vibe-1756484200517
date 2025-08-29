package com.analyzer.service;

import com.analyzer.model.ControllerMethodInfo;
import com.analyzer.model.ValidationInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Analyzes validation annotations and rules in controller methods and their call chains.
 * Detects Bean Validation (JSR-303/380) annotations and Spring validation patterns.
 */
@Service
public class ValidationAnalyzer {
    
    private static final Logger logger = LoggerFactory.getLogger(ValidationAnalyzer.class);
    
    // Bean Validation annotations (JSR-303/380)
    private static final Set<String> VALIDATION_ANNOTATIONS = Set.of(
        "Valid", "Validated", "NotNull", "NotEmpty", "NotBlank", "Size", "Min", "Max",
        "Pattern", "Email", "Positive", "Negative", "PositiveOrZero", "NegativeOrZero",
        "DecimalMin", "DecimalMax", "Digits", "Future", "Past", "FutureOrPresent",
        "PastOrPresent", "AssertTrue", "AssertFalse"
    );
    
    // Spring MVC binding annotations that can trigger validation
    private static final Set<String> BINDING_ANNOTATIONS = Set.of(
        "RequestBody", "ModelAttribute", "RequestPart"
    );
    
    // Parameter binding annotations
    private static final Set<String> PARAMETER_ANNOTATIONS = Set.of(
        "PathVariable", "RequestParam", "RequestHeader", "CookieValue"
    );
    
    // Spring validation specific annotations
    private static final Set<String> SPRING_VALIDATION_ANNOTATIONS = Set.of(
        "Validated", "Valid"
    );
    
    /**
     * Analyzes validation rules applied to a controller method.
     * 
     * @param methodInfo Controller method information
     * @return List of validation rule descriptions
     */
    public List<String> analyzeValidation(ControllerMethodInfo methodInfo) {
        logger.debug("Analyzing validation for method: {}.{}", 
                    methodInfo.getClassName(), methodInfo.getMethodName());
        
        List<String> validationRules = new ArrayList<>();
        
        // Analyze method-level validation
        List<String> methodValidation = analyzeMethodLevelValidation(methodInfo);
        validationRules.addAll(methodValidation);
        
        // Analyze parameter validation
        List<String> parameterValidation = analyzeParameterValidation(methodInfo);
        validationRules.addAll(parameterValidation);
        
        // Analyze service layer validation (from method calls)
        List<String> serviceLevelValidation = analyzeServiceLevelValidation(methodInfo);
        validationRules.addAll(serviceLevelValidation);
        
        // Remove duplicates and sort
        validationRules = validationRules.stream()
                .distinct()
                .sorted()
                .collect(Collectors.toList());
        
        logger.debug("Found {} validation rules for method {}", 
                    validationRules.size(), methodInfo.getMethodName());
        
        return validationRules;
    }
    
    /**
     * Analyzes method-level validation annotations
     */
    private List<String> analyzeMethodLevelValidation(ControllerMethodInfo methodInfo) {
        List<String> rules = new ArrayList<>();
        
        Map<String, Object> annotations = methodInfo.getAnnotations();
        
        for (Map.Entry<String, Object> entry : annotations.entrySet()) {
            String annotationDesc = entry.getKey();
            Object annotationValue = entry.getValue();
            
            // Check for Spring validation annotations
            if (annotationDesc.contains("Validated")) {
                rules.add("@Validated annotation on method - enables validation groups");
            }
            
            if (annotationDesc.contains("Valid")) {
                rules.add("@Valid annotation on method - enables bean validation");
            }
            
            // Check for custom validation annotations
            if (isCustomValidationAnnotation(annotationDesc)) {
                rules.add("Custom validation annotation: " + extractAnnotationName(annotationDesc));
            }
        }
        
        return rules;
    }
    
    /**
     * Analyzes parameter-level validation
     */
    private List<String> analyzeParameterValidation(ControllerMethodInfo methodInfo) {
        List<String> rules = new ArrayList<>();
        
        List<ControllerMethodInfo.ParameterInfo> parameters = methodInfo.getParameters();
        
        for (int i = 0; i < parameters.size(); i++) {
            ControllerMethodInfo.ParameterInfo paramInfo = parameters.get(i);
            
            for (String annotation : paramInfo.getAnnotations()) {
                String annotationName = extractAnnotationName(annotation);
                
                // Check for validation annotations
                if (VALIDATION_ANNOTATIONS.contains(annotationName)) {
                    rules.add(String.format("@%s on parameter '%s' (type: %s)", 
                             annotationName, paramInfo.getName(), paramInfo.getType()));
                }
                
                // Check for binding annotations that enable validation
                if (BINDING_ANNOTATIONS.contains(annotationName)) {
                    rules.add(String.format("@%s on parameter '%s' - enables request body validation", 
                             annotationName, paramInfo.getName()));
                }
                
                // Check for parameter annotations with validation potential
                if (PARAMETER_ANNOTATIONS.contains(annotationName)) {
                    rules.add(String.format("@%s on parameter '%s' - parameter binding with potential validation", 
                             annotationName, paramInfo.getName()));
                }
            }
            
            // Check if parameter is marked as validated
            if (paramInfo.isValidated()) {
                rules.add(String.format("Parameter '%s' (type: %s) has validation annotations", 
                         paramInfo.getName(), paramInfo.getType()));
            }
            
            // Analyze parameter type for validation potential
            String parameterTypeValidation = analyzeParameterType(paramInfo);
            if (parameterTypeValidation != null) {
                rules.add(parameterTypeValidation);
            }
        }
        
        return rules;
    }
    
    /**
     * Analyzes service layer validation based on method calls
     */
    private List<String> analyzeServiceLevelValidation(ControllerMethodInfo methodInfo) {
        List<String> rules = new ArrayList<>();
        
        List<String> calledMethods = methodInfo.getCalledMethods();
        
        for (String methodCall : calledMethods) {
            String lowerMethodCall = methodCall.toLowerCase();
            
            // Look for validation-related method calls
            if (lowerMethodCall.contains("validate")) {
                rules.add("Service layer validation: " + methodCall);
            }
            
            if (lowerMethodCall.contains("check")) {
                rules.add("Service layer check: " + methodCall);
            }
            
            if (lowerMethodCall.contains("verify")) {
                rules.add("Service layer verification: " + methodCall);
            }
            
            if (lowerMethodCall.contains("assert")) {
                rules.add("Service layer assertion: " + methodCall);
            }
            
            // Look for Spring Validator usage
            if (lowerMethodCall.contains("validator")) {
                rules.add("Spring Validator usage: " + methodCall);
            }
            
            // Look for Bean Validation API usage
            if (lowerMethodCall.contains("constraintviolation") || 
                lowerMethodCall.contains("validationfactory") ||
                lowerMethodCall.contains("validator/validate")) {
                rules.add("Bean Validation API usage: " + methodCall);
            }
        }
        
        return rules;
    }
    
    /**
     * Analyzes parameter type for validation characteristics
     */
    private String analyzeParameterType(ControllerMethodInfo.ParameterInfo paramInfo) {
        String type = paramInfo.getType().toLowerCase();
        
        // Check for common validated types
        if (type.contains("request") || type.contains("dto") || type.contains("form")) {
            return String.format("Parameter type '%s' likely contains validation annotations", paramInfo.getType());
        }
        
        if (type.contains("entity") || type.contains("model")) {
            return String.format("Entity/Model parameter '%s' may have JPA validation constraints", paramInfo.getType());
        }
        
        // Check for collection types that might contain validated objects
        if (type.contains("list") || type.contains("set") || type.contains("collection")) {
            return String.format("Collection parameter '%s' may contain validated objects", paramInfo.getType());
        }
        
        return null;
    }
    
    /**
     * Extracts annotation name from descriptor
     */
    private String extractAnnotationName(String annotationDesc) {
        // Extract from descriptors like "Lorg/springframework/validation/annotation/Validated;"
        if (annotationDesc.startsWith("L") && annotationDesc.endsWith(";")) {
            String path = annotationDesc.substring(1, annotationDesc.length() - 1);
            int lastSlash = path.lastIndexOf('/');
            if (lastSlash != -1) {
                return path.substring(lastSlash + 1);
            }
        }
        return annotationDesc;
    }
    
    /**
     * Checks if an annotation is a custom validation annotation
     */
    private boolean isCustomValidationAnnotation(String annotationDesc) {
        // Look for annotations that might be custom validation constraints
        return annotationDesc.contains("validation") || 
               annotationDesc.contains("constraint") ||
               annotationDesc.contains("validator");
    }
    
    /**
     * Creates detailed validation information for comprehensive analysis
     */
    public ValidationInfo createDetailedValidationInfo(ControllerMethodInfo methodInfo) {
        ValidationInfo validationInfo = new ValidationInfo("METHOD", "CONTROLLER");
        
        // Analyze method-level validation
        analyzeMethodAnnotationsForValidation(methodInfo, validationInfo);
        
        // Analyze parameter validation
        analyzeParametersForValidation(methodInfo, validationInfo);
        
        // Set description
        validationInfo.setDescription(String.format("Validation analysis for %s.%s", 
                                                    methodInfo.getClassName(), methodInfo.getMethodName()));
        
        return validationInfo;
    }
    
    /**
     * Analyzes method annotations for validation information
     */
    private void analyzeMethodAnnotationsForValidation(ControllerMethodInfo methodInfo, ValidationInfo validationInfo) {
        Map<String, Object> annotations = methodInfo.getAnnotations();
        
        for (Map.Entry<String, Object> entry : annotations.entrySet()) {
            String annotationDesc = entry.getKey();
            String annotationName = extractAnnotationName(annotationDesc);
            
            if (SPRING_VALIDATION_ANNOTATIONS.contains(annotationName)) {
                ValidationInfo.ValidationRule rule = new ValidationInfo.ValidationRule(annotationName);
                rule.setTargetParameter("method");
                rule.setConstraintClass(annotationDesc);
                validationInfo.addValidationRule(rule);
            }
        }
    }
    
    /**
     * Analyzes parameters for validation information
     */
    private void analyzeParametersForValidation(ControllerMethodInfo methodInfo, ValidationInfo validationInfo) {
        List<ControllerMethodInfo.ParameterInfo> parameters = methodInfo.getParameters();
        
        for (ControllerMethodInfo.ParameterInfo paramInfo : parameters) {
            for (String annotation : paramInfo.getAnnotations()) {
                String annotationName = extractAnnotationName(annotation);
                
                if (VALIDATION_ANNOTATIONS.contains(annotationName) || 
                    BINDING_ANNOTATIONS.contains(annotationName) ||
                    PARAMETER_ANNOTATIONS.contains(annotationName)) {
                    
                    ValidationInfo.ValidationRule rule = new ValidationInfo.ValidationRule(annotationName);
                    rule.setTargetParameter(paramInfo.getName());
                    rule.setConstraintClass(annotation);
                    validationInfo.addValidationRule(rule);
                }
            }
        }
    }
    
    /**
     * Gets validation coverage summary for reporting
     */
    public String getValidationCoverageSummary(List<ControllerMethodInfo> methods) {
        int totalMethods = methods.size();
        int validatedMethods = 0;
        int totalParameters = 0;
        int validatedParameters = 0;
        
        for (ControllerMethodInfo method : methods) {
            List<String> validationRules = analyzeValidation(method);
            if (!validationRules.isEmpty()) {
                validatedMethods++;
            }
            
            totalParameters += method.getParameters().size();
            validatedParameters += (int) method.getParameters().stream()
                .mapToLong(param -> param.isValidated() ? 1 : 0)
                .sum();
        }
        
        double methodCoverage = totalMethods > 0 ? (double) validatedMethods / totalMethods * 100 : 0;
        double paramCoverage = totalParameters > 0 ? (double) validatedParameters / totalParameters * 100 : 0;
        
        return String.format("Validation Coverage: %.1f%% methods (%d/%d), %.1f%% parameters (%d/%d)",
                           methodCoverage, validatedMethods, totalMethods,
                           paramCoverage, validatedParameters, totalParameters);
    }
}