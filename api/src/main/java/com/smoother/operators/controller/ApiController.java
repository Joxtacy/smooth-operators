package com.smoother.operators.controller;

import com.smoother.operators.service.FibonacciService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.context.request.async.DeferredResult;

import java.util.concurrent.TimeoutException;
import java.util.Map;
import java.util.HashMap;
import java.util.concurrent.atomic.AtomicLong;

@RestController
@RequestMapping("/api")
public class ApiController {

    @Autowired
    private FibonacciService fibonacciService;
    
    // Simple health metrics
    private final AtomicLong requestCount = new AtomicLong(0);
    private final AtomicLong errorCount = new AtomicLong(0);
    private volatile long lastRequestTime = System.currentTimeMillis();
    
    @GetMapping("/hello")
    public ResponseEntity<String> hello() {
        try {
            requestCount.incrementAndGet();
            lastRequestTime = System.currentTimeMillis();
            return ResponseEntity.ok("Hello from Smoother Operators!");
        } catch (Exception e) {
            errorCount.incrementAndGet();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body("Error: " + e.getMessage());
        }
    }

    @GetMapping("/fibonacci")
    public DeferredResult<ResponseEntity<Map<String, Object>>> fibonacci(
            @RequestParam(defaultValue = "10") Long number) {
        
        DeferredResult<ResponseEntity<Map<String, Object>>> deferredResult = 
            new DeferredResult<>(5000L); // 5 second timeout
        
        try {
            requestCount.incrementAndGet();
            lastRequestTime = System.currentTimeMillis();
            
            // Handle the request asynchronously
            new Thread(() -> {
                try {
                    Long result = fibonacciService.calculateFibonacci(number);
                    
                    Map<String, Object> response = new HashMap<>();
                    response.put("input", number);
                    response.put("result", result);
                    response.put("cacheSize", fibonacciService.getCacheSize());
                    
                    deferredResult.setResult(ResponseEntity.ok(response));
                    
                } catch (IllegalArgumentException e) {
                    errorCount.incrementAndGet();
                    Map<String, Object> errorResponse = new HashMap<>();
                    errorResponse.put("error", "Invalid input");
                    errorResponse.put("message", e.getMessage());
                    errorResponse.put("input", number);
                    
                    deferredResult.setResult(ResponseEntity.badRequest().body(errorResponse));
                    
                } catch (TimeoutException e) {
                    errorCount.incrementAndGet();
                    Map<String, Object> errorResponse = new HashMap<>();
                    errorResponse.put("error", "Request timeout");
                    errorResponse.put("message", "Calculation took too long");
                    errorResponse.put("input", number);
                    
                    deferredResult.setResult(ResponseEntity.status(HttpStatus.REQUEST_TIMEOUT).body(errorResponse));
                    
                } catch (Exception e) {
                    errorCount.incrementAndGet();
                    Map<String, Object> errorResponse = new HashMap<>();
                    errorResponse.put("error", "Internal server error");
                    errorResponse.put("message", e.getMessage());
                    errorResponse.put("input", number);
                    
                    deferredResult.setResult(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse));
                }
            }).start();
            
        } catch (Exception e) {
            errorCount.incrementAndGet();
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", "Failed to process request");
            errorResponse.put("message", e.getMessage());
            
            deferredResult.setResult(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse));
        }
        
        // Handle timeout at controller level
        deferredResult.onTimeout(() -> {
            errorCount.incrementAndGet();
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", "Request timeout");
            errorResponse.put("message", "Request exceeded 5 second limit");
            
            deferredResult.setResult(ResponseEntity.status(HttpStatus.REQUEST_TIMEOUT).body(errorResponse));
        });
        
        return deferredResult;
    }

    @GetMapping("/check-status")
    public ResponseEntity<Map<String, Object>> checkStatus() {
        try {
            requestCount.incrementAndGet();
            lastRequestTime = System.currentTimeMillis();
            
            Map<String, Object> status = new HashMap<>();
            status.put("status", "UP");
            status.put("timestamp", System.currentTimeMillis());
            status.put("requests", requestCount.get());
            status.put("errors", errorCount.get());
            status.put("errorRate", errorCount.get() / (double) requestCount.get() * 100);
            status.put("cacheSize", fibonacciService.getCacheSize());
            status.put("lastRequestTime", lastRequestTime);
            
            return ResponseEntity.ok(status);
        } catch (Exception e) {
            errorCount.incrementAndGet();
            Map<String, Object> errorStatus = new HashMap<>();
            errorStatus.put("status", "ERROR");
            errorStatus.put("error", e.getMessage());
            errorStatus.put("timestamp", System.currentTimeMillis());
            
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorStatus);
        }
    }
    
    // New endpoint for health checks
    @GetMapping("/health")
    public ResponseEntity<Map<String, String>> health() {
        Map<String, String> health = new HashMap<>();
        health.put("status", "UP");
        health.put("service", "smooth-operators-api");
        return ResponseEntity.ok(health);
    }
    
    // Cache management endpoint
    @PostMapping("/cache/clear")
    public ResponseEntity<Map<String, Object>> clearCache() {
        try {
            int oldSize = fibonacciService.getCacheSize();
            fibonacciService.clearCache();
            
            Map<String, Object> response = new HashMap<>();
            response.put("message", "Cache cleared successfully");
            response.put("oldCacheSize", oldSize);
            response.put("newCacheSize", fibonacciService.getCacheSize());
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", "Failed to clear cache");
            errorResponse.put("message", e.getMessage());
            
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }
}