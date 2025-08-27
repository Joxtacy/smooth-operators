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
    public String checkStatus(@RequestParam String status) {
        // Validate input
        if (!status.equals("Assigned") && !status.equals("Unassigned") && !status.equals("Unknown")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST);
        }

        // Validate input
        Status enumStatus = null;
        for (Status s : Status.values()) {
            if (s.getStatus().equalsIgnoreCase(status)) {
                enumStatus = s;
                break;
            }
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
