# WAR API Analyzer - Spring Boot Application

A comprehensive Spring Boot application that analyzes Spring REST WAR files to generate detailed API reports including URL patterns, HTTP methods, controller mappings, state alteration capabilities, and validation rules.

## Features

- **Comprehensive Analysis**: Extracts REST API information from compiled WAR files
- **State Alteration Detection**: Identifies APIs that modify persistent state using JPA/Hibernate operations
- **Validation Analysis**: Detects Bean Validation (JSR-303/380) and Spring validation annotations
- **Multiple Report Formats**: JSON, CSV, HTML, and text summary reports
- **Bytecode Inspection**: Uses ASM library for deep bytecode analysis without loading classes
- **Enterprise Ready**: Handles complex Spring applications with multiple layers

## Architecture

### Core Components

- **WarFileAnalyzer**: Main service for extracting and analyzing WAR contents
- **ControllerAnalyzer**: Analyzes Spring controller classes for REST mappings
- **StateAlterationAnalyzer**: Determines if methods alter persistent state
- **ValidationAnalyzer**: Detects validation annotations across layers
- **BytecodeAnalyzer**: Performs deep bytecode analysis using ASM
- **ReportGenerator**: Formats analysis results into various report formats

### Analysis Capabilities

#### State Alteration Detection
- HTTP method analysis (POST, PUT, DELETE typically alter state)
- JPA/Hibernate method call detection (`save()`, `update()`, `delete()`, etc.)
- Transaction annotation analysis (`@Transactional`)
- Method name pattern matching for CRUD operations

#### Validation Detection
- Controller level: `@Valid`, `@Validated` on parameters
- Entity level: `@NotNull`, `@Size`, `@Pattern`, `@Email`, etc.
- Service layer validation through method call analysis
- Custom constraint annotations

## API Endpoints

### 1. Analyze WAR File (JSON)
```bash
POST /api/analyze
Content-Type: application/json

{
  "warFilePath": "/path/to/your/application.war"
}
```

### 2. Analyze WAR File (CSV)
```bash
POST /api/analyze/csv
Content-Type: application/json

{
  "warFilePath": "/path/to/your/application.war"
}
```

### 3. Analyze WAR File (HTML)
```bash
POST /api/analyze/html
Content-Type: application/json

{
  "warFilePath": "/path/to/your/application.war"
}
```

### 4. Health Check
```bash
GET /api/health
```

### 5. API Documentation
```bash
GET /api/docs
```

## Report Format

### JSON Report Structure
```json
{
  "war_file_name": "example.war",
  "analysis_date": "2024-01-15T10:30:00",
  "total_apis": 25,
  "analysis_summary": {
    "state_altering_apis": 12,
    "read_only_apis": 13,
    "validated_apis": 18,
    "controller_classes": 5,
    "http_methods_distribution": {
      "GET": 13,
      "POST": 6,
      "PUT": 4,
      "DELETE": 2
    }
  },
  "apis": [
    {
      "api_url": "/api/users/{id}",
      "http_method": "GET",
      "controller_class": "com.example.UserController",
      "controller_method": "getUserById",
      "alters_state": false,
      "validation": ["@PathVariable Long id", "@Valid in service layer"],
      "method_details": {
        "return_type": "com.example.dto.UserDto",
        "parameter_types": ["java.lang.Long"],
        "transaction_attributes": {
          "is_transactional": false,
          "read_only": true
        }
      }
    }
  ]
}
```

## Building and Running

### Prerequisites
- Java 17 or higher
- Maven 3.8 or higher

### Build
```bash
mvn clean compile
mvn package
```

### Run
```bash
java -jar target/war-api-analyzer-1.0.0.jar
```

The application will start on port 8090 by default.

## Configuration

### Application Properties
```properties
# Server configuration
server.port=8090

# Analysis configuration
analyzer.temp-directory=${java.io.tmpdir}/war-analyzer
analyzer.max-analysis-timeout=300000

# Report configuration
analyzer.report.include-method-details=true
analyzer.report.include-validation-details=true
```

## Usage Examples

### Basic Analysis
```bash
curl -X POST http://localhost:8090/api/analyze \
     -H "Content-Type: application/json" \
     -d '{"warFilePath": "/path/to/your/app.war"}'
```

### Download CSV Report
```bash
curl -X POST http://localhost:8090/api/analyze/csv \
     -H "Content-Type: application/json" \
     -d '{"warFilePath": "/path/to/your/app.war"}' \
     --output api-report.csv
```

### View HTML Report
```bash
curl -X POST http://localhost:8090/api/analyze/html \
     -H "Content-Type: application/json" \
     -d '{"warFilePath": "/path/to/your/app.war"}' \
     --output report.html
```

## Technical Details

### Bytecode Analysis
- Uses ASM 9.6 for bytecode inspection
- Analyzes compiled classes without loading them into JVM
- Extracts annotation metadata and method call graphs
- Handles both loose classes and JAR-packaged dependencies

### State Alteration Heuristics
- **HTTP Methods**: POST, PUT, DELETE, PATCH typically alter state
- **Method Names**: Contains patterns like "create", "save", "update", "delete"
- **JPA Operations**: Calls to EntityManager or Repository methods
- **Transactions**: Non-read-only `@Transactional` methods

### Validation Detection
- **Bean Validation**: JSR-303/380 annotations (`@NotNull`, `@Size`, etc.)
- **Spring Validation**: `@Valid`, `@Validated` annotations
- **Parameter Binding**: `@RequestBody`, `@PathVariable` with validation
- **Service Layer**: Method calls containing "validate", "check", "verify"

## Limitations

- Requires WAR file to be accessible on the file system
- Analysis is based on static bytecode inspection (no runtime analysis)
- Custom validation logic may not be fully detected
- Complex dynamic proxies may not be fully analyzed

## Dependencies

### Core Dependencies
- Spring Boot 3.2.1
- ASM 9.6 (bytecode analysis)
- Apache Commons Compress (WAR extraction)
- Jackson (JSON processing)
- Apache Commons CSV (CSV reports)

### Analysis Libraries
- Spring Web MVC (annotation processing)
- Spring Data JPA (persistence annotation detection)
- Jakarta Validation API (Bean Validation support)

## Error Handling

The application provides comprehensive error handling for:
- Invalid WAR file formats
- Missing or inaccessible files
- Bytecode analysis failures
- Out of memory conditions during large WAR processing

## Performance Considerations

- Large WAR files may require increased heap memory
- Analysis timeout can be configured via `analyzer.max-analysis-timeout`
- Temporary directory cleanup is automatic
- Class caching improves performance for repeated analysis

## Contributing

The application is modular and extensible:
- Add new analyzers by implementing analysis services
- Extend report formats by modifying `ReportGenerator`
- Add new annotation types to validation detection
- Enhance state alteration heuristics in `StateAlterationAnalyzer`

## Demo Server

For demonstration purposes, a Node.js demo server is included (`demo-server.js`) that provides mock responses showing the expected output format.