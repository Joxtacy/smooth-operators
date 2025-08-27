package com.example.dummy_api;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api")
public class DummyController {
    @GetMapping("/hello")
    public String hello() {
        //push N^2 complexity to this endpoint
        return "Hello, this is a dummy endpoint!";
    }

    @GetMapping("/fibonacci")
    public String fibonacciEndpoint(@RequestParam int n) {
        // Exponential slowdown due to naive recursion
        long result = fibonacci(n);
        return "Hello! Fibonacci of " + n + " is " + result;
    }

    // Naive recursive Fibonacci (O(2^n))
    private long fibonacci(int n) {
        if (n <= 1) return n;
        return fibonacci(n - 1) + fibonacci(n - 2);
    }

    @GetMapping("/check-status")
    public String checkStatus(@RequestParam(required = false) String status) {
        // Check if status parameter is missing
        if (status == null || status.trim().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, 
                "Missing required parameter 'status'. Please provide a status parameter. " +
                "Example: /api/check-status?status=Assigned. " +
                "Allowed values: Assigned, Unassigned, Unknown");
        }

        // Find matching enum status
        Status enumStatus = null;
        for (Status s : Status.values()) {
            if (s.getStatus().equalsIgnoreCase(status.trim())) {
                enumStatus = s;
                break;
            }
        }

        // If no matching status found, provide helpful error message
        if (enumStatus == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, 
                "Invalid status value '" + status + "'. " +
                "Allowed values are: Assigned, Unassigned, Unknown. " +
                "Values are case-insensitive.");
        }

        return "Received status: " + enumStatus.getStatus();
    }
}

enum Status {
    ASSIGNED("Assigned"),
    UNASSIGNED("Unassigned"),
    EMPTY("Unknown");

    private final String status;

    Status(String status) {
        this.status = status;
    }

    public String getStatus() {
        return status;
    }
}
