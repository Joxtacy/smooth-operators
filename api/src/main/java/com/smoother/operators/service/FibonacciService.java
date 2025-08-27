package com.smoother.operators.service;

import org.springframework.stereotype.Service;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

@Service
public class FibonacciService {
    
    // Memoization cache to prevent recalculation
    private final Map<Long, Long> cache = new ConcurrentHashMap<>();
    
    // Maximum allowed input to prevent excessive computation
    private static final long MAX_INPUT = 50;
    
    // Request timeout in seconds
    private static final int TIMEOUT_SECONDS = 5;
    
    static {
        // Pre-populate cache with base cases
    }
    
    public Long calculateFibonacci(Long n) throws IllegalArgumentException, TimeoutException {
        if (n == null) {
            throw new IllegalArgumentException("Input cannot be null");
        }
        
        if (n < 0) {
            throw new IllegalArgumentException("Input must be non-negative");
        }
        
        if (n > MAX_INPUT) {
            throw new IllegalArgumentException("Input too large. Maximum allowed: " + MAX_INPUT);
        }
        
        try {
            // Use CompletableFuture with timeout to prevent long-running calculations
            return CompletableFuture
                .supplyAsync(() -> fibonacciWithMemoization(n))
                .get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
        } catch (java.util.concurrent.ExecutionException e) {
            throw new RuntimeException("Error calculating fibonacci", e.getCause());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Calculation interrupted", e);
        } catch (java.util.concurrent.TimeoutException e) {
            throw new TimeoutException("Calculation timed out after " + TIMEOUT_SECONDS + " seconds");
        }
    }
    
    private Long fibonacciWithMemoization(Long n) {
        // Check cache first
        if (cache.containsKey(n)) {
            return cache.get(n);
        }
        
        Long result;
        if (n <= 1) {
            result = n;
        } else {
            // Use iterative approach instead of recursion to prevent stack overflow
            result = fibonacciIterative(n);
        }
        
        // Store in cache
        cache.put(n, result);
        return result;
    }
    
    private Long fibonacciIterative(Long n) {
        if (n <= 1) return n;
        
        long a = 0, b = 1;
        for (long i = 2; i <= n; i++) {
            long temp = a + b;
            a = b;
            b = temp;
        }
        return b;
    }
    
    // Method to clear cache if needed
    public void clearCache() {
        cache.clear();
    }
    
    // Method to get cache statistics
    public int getCacheSize() {
        return cache.size();
    }
}