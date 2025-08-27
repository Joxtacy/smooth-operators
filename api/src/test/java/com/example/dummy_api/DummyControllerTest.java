package com.example.dummy_api;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.hamcrest.Matchers.containsString;

@SpringBootTest
@AutoConfigureMockMvc
public class DummyControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    public void testHelloEndpoint() throws Exception {
        mockMvc.perform(get("/api/hello"))
                .andExpect(status().isOk())
                .andExpect(content().string("Hello, this is a dummy endpoint!"));
    }

    @Test
    public void testFibonacciWithValidInput() throws Exception {
        // Test small values
        mockMvc.perform(get("/api/fibonacci").param("n", "0"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Fibonacci of 0 is 0")));

        mockMvc.perform(get("/api/fibonacci").param("n", "1"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Fibonacci of 1 is 1")));

        mockMvc.perform(get("/api/fibonacci").param("n", "10"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Fibonacci of 10 is 55")));

        // Test larger values (should be fast now)
        mockMvc.perform(get("/api/fibonacci").param("n", "40"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Fibonacci of 40 is 102334155")));
    }

    @Test
    public void testFibonacciWithNegativeInput() throws Exception {
        mockMvc.perform(get("/api/fibonacci").param("n", "-1"))
                .andExpect(status().isBadRequest())
                .andExpect(content().string(containsString("Input must be non-negative")));
    }

    @Test
    public void testFibonacciWithTooLargeInput() throws Exception {
        mockMvc.perform(get("/api/fibonacci").param("n", "93"))
                .andExpect(status().isBadRequest())
                .andExpect(content().string(containsString("Input too large")));

        mockMvc.perform(get("/api/fibonacci").param("n", "100"))
                .andExpect(status().isBadRequest())
                .andExpect(content().string(containsString("Input too large")));
    }

    @Test
    public void testFibonacciWithMissingParameter() throws Exception {
        mockMvc.perform(get("/api/fibonacci"))
                .andExpect(status().isBadRequest())
                .andExpect(content().string(containsString("Missing parameter")));
    }

    @Test
    public void testFibonacciPerformance() throws Exception {
        // Test that even large values complete quickly
        long startTime = System.currentTimeMillis();
        
        mockMvc.perform(get("/api/fibonacci").param("n", "92"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Fibonacci of 92 is 7540113804746346429")));
        
        long endTime = System.currentTimeMillis();
        long duration = endTime - startTime;
        
        // Should complete in less than 1 second (was timing out at 5 seconds before)
        assert(duration < 1000) : "Fibonacci calculation took too long: " + duration + "ms";
    }

    @Test
    public void testFibonacciCaching() throws Exception {
        // First request
        mockMvc.perform(get("/api/fibonacci").param("n", "30"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Fibonacci of 30 is 832040")));

        // Second request should be cached
        mockMvc.perform(get("/api/fibonacci").param("n", "30"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("(cached)")));
    }
}
