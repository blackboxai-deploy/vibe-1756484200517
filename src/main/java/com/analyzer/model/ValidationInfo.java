package com.analyzer.model;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.HashMap;

/**
 * Comprehensive validation information for API endpoints.
 */
public class ValidationInfo {
    
    private String scope; // "PARAMETER", "METHOD", "CLASS", "SERVICE"
    private String level; // "CONTROLLER", "SERVICE", "REPOSITORY", "ENTITY"
    private List<ValidationRule> rules;
    private String description;
    
    public ValidationInfo() {
        this.rules = new ArrayList<>();
    }
    
    public ValidationInfo(String scope, String level) {
        this();
        this.scope = scope;
        this.level = level;
    }
    
    // Getters and Setters
    public String getScope() {
        return scope;
    }
    
    public void setScope(String scope) {
        this.scope = scope;
    }
    
    public String getLevel() {
        return level;
    }
    
    public void setLevel(String level) {
        this.level = level;
    }
    
    public List<ValidationRule> getRules() {
        return rules;
    }
    
    public void setRules(List<ValidationRule> rules) {
        this.rules = rules;
    }
    
    public void addValidationRule(ValidationRule rule) {
        if (this.rules == null) {
            this.rules = new ArrayList<>();
        }
        this.rules.add(rule);
    }
    
    public String getDescription() {
        return description;
    }
    
    public void setDescription(String description) {
        this.description = description;
    }
    
    /**
     * Individual validation rule details
     */
    public static class ValidationRule {
        private String annotationType;
        private String targetField;
        private String targetParameter;
        private Map<String, Object> attributes;
        private String message;
        private String constraintClass;
        
        public ValidationRule() {
            this.attributes = new HashMap<>();
        }
        
        public ValidationRule(String annotationType) {
            this();
            this.annotationType = annotationType;
        }
        
        // Getters and Setters
        public String getAnnotationType() {
            return annotationType;
        }
        
        public void setAnnotationType(String annotationType) {
            this.annotationType = annotationType;
        }
        
        public String getTargetField() {
            return targetField;
        }
        
        public void setTargetField(String targetField) {
            this.targetField = targetField;
        }
        
        public String getTargetParameter() {
            return targetParameter;
        }
        
        public void setTargetParameter(String targetParameter) {
            this.targetParameter = targetParameter;
        }
        
        public Map<String, Object> getAttributes() {
            return attributes;
        }
        
        public void setAttributes(Map<String, Object> attributes) {
            this.attributes = attributes;
        }
        
        public void addAttribute(String key, Object value) {
            if (this.attributes == null) {
                this.attributes = new HashMap<>();
            }
            this.attributes.put(key, value);
        }
        
        public String getMessage() {
            return message;
        }
        
        public void setMessage(String message) {
            this.message = message;
        }
        
        public String getConstraintClass() {
            return constraintClass;
        }
        
        public void setConstraintClass(String constraintClass) {
            this.constraintClass = constraintClass;
        }
        
        /**
         * Converts validation rule to human-readable string
         */
        public String toReadableString() {
            StringBuilder sb = new StringBuilder();
            
            if (targetParameter != null) {
                sb.append("@").append(annotationType).append(" on parameter '").append(targetParameter).append("'");
            } else if (targetField != null) {
                sb.append("@").append(annotationType).append(" on field '").append(targetField).append("'");
            } else {
                sb.append("@").append(annotationType);
            }
            
            if (!attributes.isEmpty()) {
                sb.append(" with attributes: ");
                attributes.forEach((key, value) -> {
                    if (value instanceof String) {
                        sb.append(key).append("='").append(value).append("' ");
                    } else {
                        sb.append(key).append("=").append(value).append(" ");
                    }
                });
            }
            
            if (message != null && !message.trim().isEmpty()) {
                sb.append(" (").append(message).append(")");
            }
            
            return sb.toString().trim();
        }
    }
    
    /**
     * Predefined validation annotation types commonly found in Spring applications
     */
    public static class ValidationTypes {
        public static final String VALID = "Valid";
        public static final String VALIDATED = "Validated";
        public static final String NOT_NULL = "NotNull";
        public static final String NOT_EMPTY = "NotEmpty";
        public static final String NOT_BLANK = "NotBlank";
        public static final String SIZE = "Size";
        public static final String MIN = "Min";
        public static final String MAX = "Max";
        public static final String PATTERN = "Pattern";
        public static final String EMAIL = "Email";
        public static final String POSITIVE = "Positive";
        public static final String NEGATIVE = "Negative";
        public static final String POSITIVE_OR_ZERO = "PositiveOrZero";
        public static final String NEGATIVE_OR_ZERO = "NegativeOrZero";
        public static final String DECIMAL_MIN = "DecimalMin";
        public static final String DECIMAL_MAX = "DecimalMax";
        public static final String DIGITS = "Digits";
        public static final String FUTURE = "Future";
        public static final String PAST = "Past";
        public static final String FUTURE_OR_PRESENT = "FutureOrPresent";
        public static final String PAST_OR_PRESENT = "PastOrPresent";
        public static final String ASSERT_TRUE = "AssertTrue";
        public static final String ASSERT_FALSE = "AssertFalse";
        
        /**
         * Spring-specific validation annotations
         */
        public static final String REQUEST_BODY = "RequestBody";
        public static final String PATH_VARIABLE = "PathVariable";
        public static final String REQUEST_PARAM = "RequestParam";
        public static final String REQUEST_HEADER = "RequestHeader";
        
        /**
         * Check if annotation type is a validation annotation
         */
        public static boolean isValidationAnnotation(String annotationType) {
            return annotationType.equals(VALID) || annotationType.equals(VALIDATED) ||
                   annotationType.equals(NOT_NULL) || annotationType.equals(NOT_EMPTY) ||
                   annotationType.equals(NOT_BLANK) || annotationType.equals(SIZE) ||
                   annotationType.equals(MIN) || annotationType.equals(MAX) ||
                   annotationType.equals(PATTERN) || annotationType.equals(EMAIL) ||
                   annotationType.equals(POSITIVE) || annotationType.equals(NEGATIVE) ||
                   annotationType.equals(POSITIVE_OR_ZERO) || annotationType.equals(NEGATIVE_OR_ZERO) ||
                   annotationType.equals(DECIMAL_MIN) || annotationType.equals(DECIMAL_MAX) ||
                   annotationType.equals(DIGITS) || annotationType.equals(FUTURE) ||
                   annotationType.equals(PAST) || annotationType.equals(FUTURE_OR_PRESENT) ||
                   annotationType.equals(PAST_OR_PRESENT) || annotationType.equals(ASSERT_TRUE) ||
                   annotationType.equals(ASSERT_FALSE);
        }
        
        /**
         * Check if annotation affects request binding and validation
         */
        public static boolean isRequestBindingAnnotation(String annotationType) {
            return annotationType.equals(REQUEST_BODY) || annotationType.equals(PATH_VARIABLE) ||
                   annotationType.equals(REQUEST_PARAM) || annotationType.equals(REQUEST_HEADER);
        }
    }
    
    /**
     * Creates a summary string of all validation rules
     */
    public String getValidationSummary() {
        if (rules.isEmpty()) {
            return "No validation rules";
        }
        
        StringBuilder summary = new StringBuilder();
        for (int i = 0; i < rules.size(); i++) {
            if (i > 0) {
                summary.append(", ");
            }
            summary.append(rules.get(i).toReadableString());
        }
        
        return summary.toString();
    }
    
    /**
     * Check if this validation info contains any actual validation rules
     */
    public boolean hasValidationRules() {
        return rules != null && !rules.isEmpty() && 
               rules.stream().anyMatch(rule -> ValidationTypes.isValidationAnnotation(rule.getAnnotationType()));
    }
}