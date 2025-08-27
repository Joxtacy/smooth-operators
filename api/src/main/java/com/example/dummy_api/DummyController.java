package com.example.dummy_api;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class DummyController {
    
    // Cache for frequently requested Fibonacci numbers
    private final Map<Integer, Long> fibonacciCache = new HashMap<>();
    
    @GetMapping("/hello")
    public String hello() {
        return "Hello, this is a dummy endpoint!";
    }

    @GetMapping("/fibonacci")
    public ResponseEntity<?> fibonacciEndpoint(@RequestParam(required = false) Integer n) {
        // Validate input
        if (n == null) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body("Error: Missing parameter 'n'. Please provide a number.");
        }
        
        if (n < 0) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body("Error: Input must be non-negative. Received: " + n);
        }
        
        // Limit to prevent overflow (fibonacci(93) exceeds Long.MAX_VALUE)
        if (n > 92) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body("Error: Input too large (maximum: 92). Received: " + n);
        }
        
        try {
            // Check cache first
            if (fibonacciCache.containsKey(n)) {
                long result = fibonacciCache.get(n);
                return ResponseEntity.ok("Hello! Fibonacci of " + n + " is " + result + " (cached)");
            }
            
            // Calculate and cache result
            long result = fibonacciIterative(n);
            fibonacciCache.put(n, result);
            
            return ResponseEntity.ok("Hello! Fibonacci of " + n + " is " + result);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body("Error calculating Fibonacci: " + e.getMessage());
        }
    }

    /**
     * Efficient iterative Fibonacci implementation
     * Time complexity: O(n)
     * Space complexity: O(1)
     */
    private long fibonacciIterative(int n) {
        if (n <= 1) {
            return n;
        }
        
        long prev = 0;
        long curr = 1;
        
        for (int i = 2; i <= n; i++) {
            long next = prev + curr;
            prev = curr;
            curr = next;
        }
        
        return curr;
    }
    
    /**
     * Alternative: Memoized recursive implementation (kept for reference)
     * Time complexity: O(n)
     * Space complexity: O(n)
     */
    @SuppressWarnings("unused")
    private long fibonacciMemoized(int n, Map<Integer, Long> memo) {
        if (n <= 1) {
            return n;
        }
        
        if (memo.containsKey(n)) {
            return memo.get(n);
        }
        
        long result = fibonacciMemoized(n - 1, memo) + fibonacciMemoized(n - 2, memo);
        memo.put(n, result);
        return result;
    }
}
