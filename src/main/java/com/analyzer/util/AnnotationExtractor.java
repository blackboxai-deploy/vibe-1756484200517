package com.analyzer.util;

import org.objectweb.asm.tree.AnnotationNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * Utility class for extracting and processing annotation information from bytecode.
 * Handles ASM AnnotationNode objects and converts them to readable formats.
 */
@Component
public class AnnotationExtractor {
    
    private static final Logger logger = LoggerFactory.getLogger(AnnotationExtractor.class);
    
    /**
     * Extracts annotation values from an ASM AnnotationNode.
     * 
     * @param annotation The ASM AnnotationNode to process
     * @return Map of annotation attribute names to their values
     */
    public Map<String, Object> extractAnnotationValues(AnnotationNode annotation) {
        Map<String, Object> values = new HashMap<>();
        
        if (annotation.values == null) {
            return values;
        }
        
        try {
            // Process annotation values in pairs (name, value)
            for (int i = 0; i < annotation.values.size(); i += 2) {
                String attributeName = (String) annotation.values.get(i);
                Object attributeValue = annotation.values.get(i + 1);
                
                Object processedValue = processAnnotationValue(attributeValue);
                values.put(attributeName, processedValue);
            }
        } catch (Exception e) {
            logger.warn("Error extracting annotation values from {}: {}", annotation.desc, e.getMessage());
        }
        
        return values;
    }
    
    /**
     * Processes individual annotation values, handling different ASM value types.
     * 
     * @param value Raw annotation value from ASM
     * @return Processed value in a readable format
     */
    private Object processAnnotationValue(Object value) {
        if (value == null) {
            return null;
        }
        
        // Handle different types of annotation values
        if (value instanceof String || value instanceof Number || value instanceof Boolean) {
            return value;
        }
        
        if (value instanceof List) {
            return processAnnotationArray((List<?>) value);
        }
        
        if (value instanceof String[]) {
            return Arrays.asList((String[]) value);
        }
        
        // Handle enum values
        if (value.getClass().isArray() && value.getClass().getComponentType() == String.class) {
            String[] enumInfo = (String[]) value;
            if (enumInfo.length == 2) {
                return processEnumValue(enumInfo[0], enumInfo[1]);
            }
        }
        
        // Handle nested annotations
        if (value instanceof AnnotationNode) {
            return extractAnnotationValues((AnnotationNode) value);
        }
        
        // Handle Type objects (class references)
        if (value.toString().startsWith("L") && value.toString().endsWith(";")) {
            return processClassReference(value.toString());
        }
        
        // Default: return string representation
        return value.toString();
    }
    
    /**
     * Processes annotation arrays (multiple values)
     */
    private List<Object> processAnnotationArray(List<?> arrayValues) {
        List<Object> processedValues = new ArrayList<>();
        
        for (Object item : arrayValues) {
            processedValues.add(processAnnotationValue(item));
        }
        
        return processedValues;
    }
    
    /**
     * Processes enum values from annotation attributes
     */
    private String processEnumValue(String enumType, String enumConstant) {
        // Convert enum descriptor to readable format
        String className = enumType.substring(1, enumType.length() - 1).replace('/', '.');
        return className + "." + enumConstant;
    }
    
    /**
     * Processes class references in annotations
     */
    private String processClassReference(String classDescriptor) {
        // Convert class descriptor (e.g., "Ljava/lang/String;") to class name
        String className = classDescriptor.substring(1, classDescriptor.length() - 1).replace('/', '.');
        return className + ".class";
    }
    
    /**
     * Extracts specific annotation attribute value with type casting
     */
    @SuppressWarnings("unchecked")
    public <T> T extractAttributeValue(AnnotationNode annotation, String attributeName, Class<T> type) {
        Map<String, Object> values = extractAnnotationValues(annotation);
        Object value = values.get(attributeName);
        
        if (value == null) {
            return null;
        }
        
        if (type.isInstance(value)) {
            return (T) value;
        }
        
        // Handle type conversions
        if (type == String.class) {
            return (T) value.toString();
        }
        
        if (type == Boolean.class && value instanceof String) {
            return (T) Boolean.valueOf((String) value);
        }
        
        if (type == Integer.class && value instanceof String) {
            try {
                return (T) Integer.valueOf((String) value);
            } catch (NumberFormatException e) {
                logger.warn("Cannot convert '{}' to Integer", value);
                return null;
            }
        }
        
        logger.warn("Cannot convert annotation value '{}' to type {}", value, type.getSimpleName());
        return null;
    }
    
    /**
     * Extracts string array from annotation attribute
     */
    public List<String> extractStringArray(AnnotationNode annotation, String attributeName) {
        Map<String, Object> values = extractAnnotationValues(annotation);
        Object value = values.get(attributeName);
        
        if (value == null) {
            return new ArrayList<>();
        }
        
        if (value instanceof List) {
            List<?> list = (List<?>) value;
            return list.stream()
                .map(Object::toString)
                .toList();
        }
        
        if (value instanceof String) {
            return List.of((String) value);
        }
        
        return List.of(value.toString());
    }
    
    /**
     * Checks if annotation has a specific attribute
     */
    public boolean hasAttribute(AnnotationNode annotation, String attributeName) {
        if (annotation.values == null) {
            return false;
        }
        
        for (int i = 0; i < annotation.values.size(); i += 2) {
            String name = (String) annotation.values.get(i);
            if (attributeName.equals(name)) {
                return true;
            }
        }
        
        return false;
    }
    
    /**
     * Extracts HTTP methods from RequestMethod enum values
     */
    public List<String> extractHttpMethods(AnnotationNode annotation) {
        Object methodValue = extractAttributeValue(annotation, "method", Object.class);
        
        if (methodValue == null) {
            return new ArrayList<>();
        }
        
        List<String> httpMethods = new ArrayList<>();
        
        if (methodValue instanceof List) {
            List<?> methods = (List<?>) methodValue;
            for (Object method : methods) {
                String httpMethod = extractHttpMethodFromEnum(method.toString());
                if (httpMethod != null) {
                    httpMethods.add(httpMethod);
                }
            }
        } else {
            String httpMethod = extractHttpMethodFromEnum(methodValue.toString());
            if (httpMethod != null) {
                httpMethods.add(httpMethod);
            }
        }
        
        return httpMethods;
    }
    
    /**
     * Extracts HTTP method name from RequestMethod enum
     */
    private String extractHttpMethodFromEnum(String enumValue) {
        // Handle RequestMethod enum values like "org.springframework.web.bind.annotation.RequestMethod.GET"
        if (enumValue.contains("RequestMethod.")) {
            String[] parts = enumValue.split("\\.");
            String methodName = parts[parts.length - 1];
            return methodName.toUpperCase();
        }
        
        // Handle simple enum constant names
        String upperValue = enumValue.toUpperCase();
        if (upperValue.matches("GET|POST|PUT|DELETE|PATCH|OPTIONS|HEAD")) {
            return upperValue;
        }
        
        return null;
    }
    
    /**
     * Extracts URL paths from mapping annotations
     */
    public List<String> extractUrlPaths(AnnotationNode annotation) {
        // Try "value" first, then "path"
        List<String> paths = extractStringArray(annotation, "value");
        if (paths.isEmpty()) {
            paths = extractStringArray(annotation, "path");
        }
        
        // Default to empty string if no paths specified
        if (paths.isEmpty()) {
            paths = List.of("");
        }
        
        return paths;
    }
    
    /**
     * Extracts media types (produces/consumes) from mapping annotations
     */
    public List<String> extractMediaTypes(AnnotationNode annotation, String attributeName) {
        return extractStringArray(annotation, attributeName);
    }
    
    /**
     * Creates a human-readable string representation of an annotation
     */
    public String annotationToString(AnnotationNode annotation) {
        StringBuilder sb = new StringBuilder();
        
        // Extract annotation name
        String annotationName = annotation.desc;
        if (annotationName.startsWith("L") && annotationName.endsWith(";")) {
            annotationName = annotationName.substring(1, annotationName.length() - 1);
            int lastSlash = annotationName.lastIndexOf('/');
            if (lastSlash != -1) {
                annotationName = annotationName.substring(lastSlash + 1);
            }
        }
        
        sb.append("@").append(annotationName);
        
        // Add attributes if present
        Map<String, Object> values = extractAnnotationValues(annotation);
        if (!values.isEmpty()) {
            sb.append("(");
            List<String> attributes = new ArrayList<>();
            
            values.forEach((key, value) -> {
                if (value instanceof List) {
                    List<?> list = (List<?>) value;
                    if (list.size() == 1) {
                        attributes.add(key + "=" + formatAttributeValue(list.get(0)));
                    } else {
                        attributes.add(key + "=" + list);
                    }
                } else {
                    attributes.add(key + "=" + formatAttributeValue(value));
                }
            });
            
            sb.append(String.join(", ", attributes));
            sb.append(")");
        }
        
        return sb.toString();
    }
    
    /**
     * Formats annotation attribute values for display
     */
    private String formatAttributeValue(Object value) {
        if (value instanceof String) {
            return "\"" + value + "\"";
        }
        return value.toString();
    }
}