const http = require('http');
const fs = require('fs');
const path = require('path');

// Mock API endpoints that simulate the WAR analyzer functionality
const routes = {
  '/api/health': {
    method: 'GET',
    handler: (req, res) => {
      res.writeHead(200, { 'Content-Type': 'application/json' });
      res.end(JSON.stringify({
        status: 'UP',
        service: 'WAR API Analyzer',
        version: '1.0.0',
        timestamp: Date.now()
      }));
    }
  },
  
  '/api/docs': {
    method: 'GET',
    handler: (req, res) => {
      res.writeHead(200, { 'Content-Type': 'text/plain' });
      res.end(`=== WAR API ANALYZER - API DOCUMENTATION ===

This service analyzes Spring Boot WAR files to generate comprehensive API reports.

ENDPOINTS:

1. POST /api/analyze
   - Description: Analyzes a WAR file and returns detailed JSON report
   - Content-Type: application/json
   - Request Body: {"warFilePath": "/path/to/your/app.war"}
   - Response: Complete API analysis in JSON format

2. POST /api/analyze/csv
   - Description: Analyzes a WAR file and returns CSV report
   - Content-Type: application/json
   - Request Body: {"warFilePath": "/path/to/your/app.war"}
   - Response: CSV formatted report (downloadable)

3. POST /api/analyze/html
   - Description: Analyzes a WAR file and returns HTML report
   - Content-Type: application/json
   - Request Body: {"warFilePath": "/path/to/your/app.war"}
   - Response: HTML formatted report with styling

4. GET /api/health
   - Description: Service health check
   - Response: Service status information

5. GET /api/docs
   - Description: This documentation
   - Response: API usage information

EXAMPLE USAGE:
curl -X POST http://localhost:8090/api/analyze \\
     -H "Content-Type: application/json" \\
     -d '{"warFilePath": "/path/to/your/application.war"}'

NOTE: This is a demo server. In production, use the Spring Boot application.`);
    }
  },
  
  '/api/analyze': {
    method: 'POST',
    handler: (req, res) => {
      let body = '';
      req.on('data', chunk => {
        body += chunk.toString();
      });
      
      req.on('end', () => {
        try {
          const request = JSON.parse(body);
          
          if (!request.warFilePath) {
            res.writeHead(400, { 'Content-Type': 'application/json' });
            res.end(JSON.stringify({
              error: true,
              message: 'WAR file path is required',
              timestamp: Date.now()
            }));
            return;
          }
          
          // Mock analysis result
          const mockReport = {
            war_file_name: path.basename(request.warFilePath),
            analysis_date: new Date().toISOString(),
            total_apis: 12,
            analysis_summary: {
              state_altering_apis: 7,
              read_only_apis: 5,
              validated_apis: 9,
              controller_classes: 3,
              http_methods_distribution: {
                GET: 5,
                POST: 4,
                PUT: 2,
                DELETE: 1,
                PATCH: 0,
                OPTIONS: 0,
                HEAD: 0
              }
            },
            apis: [
              {
                api_url: "/api/users",
                http_method: "GET",
                controller_class: "com.example.controller.UserController",
                controller_method: "getAllUsers",
                alters_state: false,
                validation: ["@Valid on parameter 'pageable'"],
                method_details: {
                  return_type: "org.springframework.data.domain.Page",
                  parameter_types: ["org.springframework.data.domain.Pageable"],
                  annotations: ["@GetMapping", "@PreAuthorize"],
                  transaction_attributes: {
                    is_transactional: false,
                    read_only: true
                  }
                }
              },
              {
                api_url: "/api/users",
                http_method: "POST",
                controller_class: "com.example.controller.UserController",
                controller_method: "createUser",
                alters_state: true,
                validation: ["@Valid on parameter 'userDto'", "@NotNull on field 'email'", "@Size on field 'username'"],
                method_details: {
                  return_type: "com.example.dto.UserDto",
                  parameter_types: ["com.example.dto.CreateUserDto"],
                  annotations: ["@PostMapping", "@Validated"],
                  transaction_attributes: {
                    is_transactional: true,
                    read_only: false
                  }
                }
              },
              {
                api_url: "/api/users/{id}",
                http_method: "PUT",
                controller_class: "com.example.controller.UserController",
                controller_method: "updateUser",
                alters_state: true,
                validation: ["@PathVariable Long id", "@Valid on parameter 'userDto'"],
                method_details: {
                  return_type: "com.example.dto.UserDto",
                  parameter_types: ["java.lang.Long", "com.example.dto.UpdateUserDto"],
                  annotations: ["@PutMapping", "@PreAuthorize"],
                  transaction_attributes: {
                    is_transactional: true,
                    read_only: false
                  }
                }
              },
              {
                api_url: "/api/users/{id}",
                http_method: "DELETE",
                controller_class: "com.example.controller.UserController",
                controller_method: "deleteUser",
                alters_state: true,
                validation: ["@PathVariable Long id"],
                method_details: {
                  return_type: "void",
                  parameter_types: ["java.lang.Long"],
                  annotations: ["@DeleteMapping", "@PreAuthorize"],
                  transaction_attributes: {
                    is_transactional: true,
                    read_only: false
                  }
                }
              },
              {
                api_url: "/api/orders",
                http_method: "GET",
                controller_class: "com.example.controller.OrderController",
                controller_method: "getOrders",
                alters_state: false,
                validation: ["@RequestParam filters"],
                method_details: {
                  return_type: "java.util.List",
                  parameter_types: ["com.example.dto.OrderFilters"],
                  annotations: ["@GetMapping"],
                  transaction_attributes: {
                    is_transactional: false,
                    read_only: true
                  }
                }
              },
              {
                api_url: "/api/orders",
                http_method: "POST",
                controller_class: "com.example.controller.OrderController",
                controller_method: "createOrder",
                alters_state: true,
                validation: ["@Valid on parameter 'orderDto'", "@NotEmpty on field 'items'"],
                method_details: {
                  return_type: "com.example.dto.OrderDto",
                  parameter_types: ["com.example.dto.CreateOrderDto"],
                  annotations: ["@PostMapping", "@Transactional"],
                  transaction_attributes: {
                    is_transactional: true,
                    read_only: false
                  }
                }
              }
            ]
          };
          
          res.writeHead(200, { 'Content-Type': 'application/json' });
          res.end(JSON.stringify(mockReport, null, 2));
          
        } catch (error) {
          res.writeHead(400, { 'Content-Type': 'application/json' });
          res.end(JSON.stringify({
            error: true,
            message: 'Invalid JSON in request body',
            timestamp: Date.now()
          }));
        }
      });
    }
  },
  
  '/api/analyze/csv': {
    method: 'POST',
    handler: (req, res) => {
      let body = '';
      req.on('data', chunk => {
        body += chunk.toString();
      });
      
      req.on('end', () => {
        try {
          const request = JSON.parse(body);
          
          if (!request.warFilePath) {
            res.writeHead(400, { 'Content-Type': 'text/plain' });
            res.end('WAR file path is required');
            return;
          }
          
          // Mock CSV report
          const csvData = `API_URL,HTTP_METHOD,Controller_Class,Controller_Method,Alters_State,Validation,Return_Type,Parameter_Types,Is_Transactional,Produces,Consumes
/api/users,GET,com.example.controller.UserController,getAllUsers,false,"@Valid on parameter 'pageable'",org.springframework.data.domain.Page,org.springframework.data.domain.Pageable,false,application/json,
/api/users,POST,com.example.controller.UserController,createUser,true,"@Valid on parameter 'userDto'; @NotNull on field 'email'; @Size on field 'username'",com.example.dto.UserDto,com.example.dto.CreateUserDto,true,application/json,application/json
/api/users/{id},PUT,com.example.controller.UserController,updateUser,true,"@PathVariable Long id; @Valid on parameter 'userDto'",com.example.dto.UserDto,"java.lang.Long, com.example.dto.UpdateUserDto",true,application/json,application/json
/api/users/{id},DELETE,com.example.controller.UserController,deleteUser,true,"@PathVariable Long id",void,java.lang.Long,true,,
/api/orders,GET,com.example.controller.OrderController,getOrders,false,"@RequestParam filters",java.util.List,com.example.dto.OrderFilters,false,application/json,
/api/orders,POST,com.example.controller.OrderController,createOrder,true,"@Valid on parameter 'orderDto'; @NotEmpty on field 'items'",com.example.dto.OrderDto,com.example.dto.CreateOrderDto,true,application/json,application/json`;
          
          res.writeHead(200, { 
            'Content-Type': 'text/csv',
            'Content-Disposition': 'attachment; filename="api-report.csv"'
          });
          res.end(csvData);
          
        } catch (error) {
          res.writeHead(400, { 'Content-Type': 'text/plain' });
          res.end('Invalid JSON in request body');
        }
      });
    }
  }
};

const server = http.createServer((req, res) => {
  // Enable CORS
  res.setHeader('Access-Control-Allow-Origin', '*');
  res.setHeader('Access-Control-Allow-Methods', 'GET, POST, PUT, DELETE, OPTIONS');
  res.setHeader('Access-Control-Allow-Headers', 'Content-Type, Authorization');
  
  if (req.method === 'OPTIONS') {
    res.writeHead(200);
    res.end();
    return;
  }
  
  const url = req.url;
  const method = req.method;
  
  const route = routes[url];
  if (route && route.method === method) {
    route.handler(req, res);
  } else {
    res.writeHead(404, { 'Content-Type': 'text/plain' });
    res.end('Not Found');
  }
});

const PORT = 8090;
server.listen(PORT, () => {
  console.log(`WAR API Analyzer Demo Server running on http://localhost:${PORT}`);
  console.log('Available endpoints:');
  console.log('  GET  /api/health  - Health check');
  console.log('  GET  /api/docs    - API documentation');
  console.log('  POST /api/analyze - Analyze WAR file (JSON response)');
  console.log('  POST /api/analyze/csv - Analyze WAR file (CSV response)');
});