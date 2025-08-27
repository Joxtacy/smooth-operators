package se.telavox.mcp;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class ApiController {

    @GetMapping("/check-status")
    public ResponseEntity<?> checkStatus() {
        try {
            return ResponseEntity.ok(Map.of(
                "status", "healthy",
                "timestamp", LocalDateTime.now().toString(),
                "message", "Service is running properly"
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                .body(Map.of(
                    "error", "Service health check failed",
                    "message", "Unable to determine service status. Please check service configuration and dependencies.",
                    "timestamp", LocalDateTime.now().toString(),
                    "suggestion", "Verify that all required services are running and accessible"
                ));
        }
    }

    @GetMapping("/hello")
    public ResponseEntity<?> hello(@RequestParam(value = "name", required = false) String name) {
        try {
            String greeting = name != null ? "Hello, " + name + "!" : "Hello, World!";
            return ResponseEntity.ok(Map.of(
                "message", greeting,
                "timestamp", LocalDateTime.now().toString()
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                .body(Map.of(
                    "error", "Failed to generate greeting",
                    "message", "An error occurred while processing your request. Please ensure all parameters are valid.",
                    "timestamp", LocalDateTime.now().toString()
                ));
        }
    }

    @GetMapping("/")
    public ResponseEntity<?> root() {
        return ResponseEntity.ok(Map.of(
            "service", "smooth-operator-api",
            "status", "running",
            "message", "Welcome to the Smooth Operators API",
            "availableEndpoints", Map.of(
                "/api/check-status", "GET - Health check endpoint",
                "/api/hello", "GET - Greeting endpoint (accepts optional 'name' parameter)"
            ),
            "timestamp", LocalDateTime.now().toString()
        ));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<?> handleException(Exception e) {
        return ResponseEntity.badRequest()
            .body(Map.of(
                "error", "Request processing failed",
                "message", "Your request could not be processed. Please check the request format and try again.",
                "details", e.getMessage(),
                "timestamp", LocalDateTime.now().toString(),
                "help", "Ensure you're using the correct HTTP method and endpoint path"
            ));
    }
}