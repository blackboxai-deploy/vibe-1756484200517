package com.analyzer.service;

import com.analyzer.model.ControllerMethodInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Set;

/**
 * Analyzes controller methods to determine if they alter persistent state.
 * Uses multiple heuristics including HTTP methods, method names, called methods,
 * and transactional annotations.
 */
@Service
public class StateAlterationAnalyzer {
    
    private static final Logger logger = LoggerFactory.getLogger(StateAlterationAnalyzer.class);
    
    // HTTP methods that typically alter state
    private static final Set<String> STATE_ALTERING_HTTP_METHODS = Set.of(
        "POST", "PUT", "DELETE", "PATCH"
    );
    
    // Method name patterns that suggest state alteration
    private static final Set<String> STATE_ALTERING_METHOD_PATTERNS = Set.of(
        "create", "save", "update", "modify", "edit", "delete", "remove", 
        "insert", "add", "set", "put", "post", "patch", "persist", "merge",
        "store", "write", "commit", "submit", "process", "execute", "apply"
    );
    
    // JPA/Hibernate method calls that alter state
    private static final Set<String> JPA_STATE_ALTERING_METHODS = Set.of(
        "save", "saveAll", "saveAndFlush", "delete", "deleteAll", "deleteById",
        "persist", "merge", "remove", "update", "flush", "clear", "refresh",
        "createQuery", "createNativeQuery", "createNamedQuery"
    );
    
    // Repository method patterns that alter state
    private static final Set<String> REPOSITORY_STATE_METHODS = Set.of(
        "save", "update", "delete", "remove", "create", "insert", "modify", "edit"
    );
    
    // Service method patterns that might alter state
    private static final Set<String> SERVICE_STATE_PATTERNS = Set.of(
        "process", "handle", "execute", "perform", "apply", "commit", "submit"
    );
    
    /**
     * Analyzes if a controller method alters persistent state.
     * 
     * @param methodInfo Controller method information
     * @return true if the method likely alters state, false otherwise
     */
    public boolean analyzesStateAlteration(ControllerMethodInfo methodInfo) {
        logger.debug("Analyzing state alteration for method: {}.{}", 
                    methodInfo.getClassName(), methodInfo.getMethodName());
        
        // Check HTTP methods
        if (hasStateAlteringHttpMethod(methodInfo)) {
            logger.debug("Method {} has state-altering HTTP method", methodInfo.getMethodName());
            return true;
        }
        
        // Check method name patterns
        if (hasStateAlteringMethodName(methodInfo)) {
            logger.debug("Method {} has state-altering name pattern", methodInfo.getMethodName());
            return true;
        }
        
        // Check transactional annotations (non-read-only)
        if (hasStateAlteringTransaction(methodInfo)) {
            logger.debug("Method {} has state-altering transaction", methodInfo.getMethodName());
            return true;
        }
        
        // Check called methods for JPA/persistence operations
        if (hasStateAlteringMethodCalls(methodInfo)) {
            logger.debug("Method {} calls state-altering persistence methods", methodInfo.getMethodName());
            return true;
        }
        
        // Check for repository method calls
        if (hasRepositoryStateOperations(methodInfo)) {
            logger.debug("Method {} calls repository state operations", methodInfo.getMethodName());
            return true;
        }
        
        // Check for service method calls that might alter state
        if (hasServiceStateOperations(methodInfo)) {
            logger.debug("Method {} calls service methods that might alter state", methodInfo.getMethodName());
            return true;
        }
        
        logger.debug("Method {} appears to be read-only", methodInfo.getMethodName());
        return false;
    }
    
    /**
     * Checks if the method uses HTTP methods that typically alter state
     */
    private boolean hasStateAlteringHttpMethod(ControllerMethodInfo methodInfo) {
        List<String> httpMethods = methodInfo.getHttpMethods();
        return httpMethods.stream().anyMatch(STATE_ALTERING_HTTP_METHODS::contains);
    }
    
    /**
     * Checks if the method name suggests state alteration
     */
    private boolean hasStateAlteringMethodName(ControllerMethodInfo methodInfo) {
        String methodName = methodInfo.getMethodName().toLowerCase();
        
        return STATE_ALTERING_METHOD_PATTERNS.stream()
                .anyMatch(pattern -> methodName.contains(pattern));
    }
    
    /**
     * Checks if the method has transactional annotations that suggest state alteration
     */
    private boolean hasStateAlteringTransaction(ControllerMethodInfo methodInfo) {
        if (!methodInfo.isTransactional()) {
            return false;
        }
        
        // Check if transaction is marked as read-only
        Object transactionalAnnotation = methodInfo.getAnnotations().get("Lorg/springframework/transaction/annotation/Transactional;");
        if (transactionalAnnotation != null) {
            // If we have detailed annotation info, check readOnly attribute
            String annotationStr = transactionalAnnotation.toString();
            if (annotationStr.contains("readOnly=true") || annotationStr.contains("readOnly:true")) {
                return false; // Read-only transaction doesn't alter state
            }
        }
        
        // If transactional but not explicitly read-only, assume it might alter state
        return true;
    }
    
    /**
     * Checks for JPA/Hibernate method calls that alter state
     */
    private boolean hasStateAlteringMethodCalls(ControllerMethodInfo methodInfo) {
        List<String> calledMethods = methodInfo.getCalledMethods();
        
        for (String methodCall : calledMethods) {
            String lowerMethodCall = methodCall.toLowerCase();
            
            // Check for direct JPA EntityManager or Repository method calls
            if (JPA_STATE_ALTERING_METHODS.stream().anyMatch(lowerMethodCall::contains)) {
                return true;
            }
            
            // Check for Spring Data JPA repository method patterns
            if (lowerMethodCall.contains("repository") || lowerMethodCall.contains("dao")) {
                if (REPOSITORY_STATE_METHODS.stream().anyMatch(lowerMethodCall::contains)) {
                    return true;
                }
            }
            
            // Check for custom repository methods with state-altering names
            if (isCustomRepositoryStateMethod(methodCall)) {
                return true;
            }
        }
        
        return false;
    }
    
    /**
     * Checks for repository operations that alter state
     */
    private boolean hasRepositoryStateOperations(ControllerMethodInfo methodInfo) {
        List<String> calledMethods = methodInfo.getCalledMethods();
        
        for (String methodCall : calledMethods) {
            String lowerMethodCall = methodCall.toLowerCase();
            
            // Look for repository/dao patterns
            if (lowerMethodCall.contains("repository") || lowerMethodCall.contains("dao")) {
                // Check method names within repository calls
                for (String pattern : REPOSITORY_STATE_METHODS) {
                    if (lowerMethodCall.contains(pattern)) {
                        return true;
                    }
                }
                
                // Check for Spring Data JPA generated method patterns
                if (isSpringDataStateMethod(methodCall)) {
                    return true;
                }
            }
        }
        
        return false;
    }
    
    /**
     * Checks for service method calls that might alter state
     */
    private boolean hasServiceStateOperations(ControllerMethodInfo methodInfo) {
        List<String> calledMethods = methodInfo.getCalledMethods();
        
        for (String methodCall : calledMethods) {
            String lowerMethodCall = methodCall.toLowerCase();
            
            // Look for service layer method calls
            if (lowerMethodCall.contains("service")) {
                // Check method names for state alteration patterns
                for (String pattern : SERVICE_STATE_PATTERNS) {
                    if (lowerMethodCall.contains(pattern)) {
                        return true;
                    }
                }
                
                // Check for business operation patterns
                if (isBusinessOperationMethod(methodCall)) {
                    return true;
                }
            }
        }
        
        return false;
    }
    
    /**
     * Checks if a method call is a custom repository method that alters state
     */
    private boolean isCustomRepositoryStateMethod(String methodCall) {
        String lowerMethodCall = methodCall.toLowerCase();
        
        // Custom repository method patterns
        String[] statePatterns = {
            "updateby", "deleteby", "removeby", "saveby", "createby",
            "insertby", "modifyby", "persistby", "mergeby"
        };
        
        for (String pattern : statePatterns) {
            if (lowerMethodCall.contains(pattern)) {
                return true;
            }
        }
        
        return false;
    }
    
    /**
     * Checks if a method call is a Spring Data JPA generated method that alters state
     */
    private boolean isSpringDataStateMethod(String methodCall) {
        String lowerMethodCall = methodCall.toLowerCase();
        
        // Spring Data JPA method name patterns
        String[] jpaPatterns = {
            "deleteallby", "removeby", "deleteby", "updateby", "saveby"
        };
        
        for (String pattern : jpaPatterns) {
            if (lowerMethodCall.contains(pattern)) {
                return true;
            }
        }
        
        return false;
    }
    
    /**
     * Checks if a method call represents a business operation that might alter state
     */
    private boolean isBusinessOperationMethod(String methodCall) {
        String lowerMethodCall = methodCall.toLowerCase();
        
        // Business operation patterns
        String[] businessPatterns = {
            "approve", "reject", "cancel", "activate", "deactivate",
            "enable", "disable", "publish", "unpublish", "archive",
            "restore", "validate", "confirm", "complete", "finalize",
            "authorize", "authenticate", "register", "enroll", "subscribe",
            "unsubscribe", "transfer", "import", "export", "sync", "migrate"
        };
        
        for (String pattern : businessPatterns) {
            if (lowerMethodCall.contains(pattern)) {
                return true;
            }
        }
        
        return false;
    }
    
    /**
     * Provides a confidence score for state alteration analysis (0.0 to 1.0)
     */
    public double getStateAlterationConfidence(ControllerMethodInfo methodInfo) {
        double confidence = 0.0;
        
        // HTTP method contributes 30%
        if (hasStateAlteringHttpMethod(methodInfo)) {
            confidence += 0.3;
        }
        
        // Method name contributes 20%
        if (hasStateAlteringMethodName(methodInfo)) {
            confidence += 0.2;
        }
        
        // Transactional annotation contributes 25%
        if (hasStateAlteringTransaction(methodInfo)) {
            confidence += 0.25;
        }
        
        // JPA method calls contribute 20%
        if (hasStateAlteringMethodCalls(methodInfo)) {
            confidence += 0.2;
        }
        
        // Repository operations contribute 15%
        if (hasRepositoryStateOperations(methodInfo)) {
            confidence += 0.15;
        }
        
        // Service operations contribute 10%
        if (hasServiceStateOperations(methodInfo)) {
            confidence += 0.1;
        }
        
        // Cap at 1.0
        return Math.min(confidence, 1.0);
    }
    
    /**
     * Provides a detailed analysis report for debugging
     */
    public String getStateAlterationAnalysisDetails(ControllerMethodInfo methodInfo) {
        StringBuilder details = new StringBuilder();
        details.append("State Alteration Analysis for ")
               .append(methodInfo.getClassName())
               .append(".")
               .append(methodInfo.getMethodName())
               .append(":\n");
        
        details.append("- HTTP Methods: ").append(hasStateAlteringHttpMethod(methodInfo) ? "ALTERS STATE" : "READ-ONLY").append("\n");
        details.append("- Method Name: ").append(hasStateAlteringMethodName(methodInfo) ? "SUGGESTS STATE CHANGE" : "NEUTRAL").append("\n");
        details.append("- Transactional: ").append(hasStateAlteringTransaction(methodInfo) ? "NON-READ-ONLY" : "NONE/READ-ONLY").append("\n");
        details.append("- JPA Calls: ").append(hasStateAlteringMethodCalls(methodInfo) ? "FOUND" : "NONE").append("\n");
        details.append("- Repository Ops: ").append(hasRepositoryStateOperations(methodInfo) ? "FOUND" : "NONE").append("\n");
        details.append("- Service Ops: ").append(hasServiceStateOperations(methodInfo) ? "FOUND" : "NONE").append("\n");
        details.append("- Confidence: ").append(String.format("%.2f", getStateAlterationConfidence(methodInfo))).append("\n");
        
        return details.toString();
    }
}