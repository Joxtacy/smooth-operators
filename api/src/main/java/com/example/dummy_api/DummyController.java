package com.example.dummy_api;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestMapping;

@RestController
@RequestMapping("/api")
public class DummyController {
    @GetMapping("/hello")
    public String hello() {
        //push N^2 complexity to this endpoint
        return "Hello, this is a dummy endpoint!";
    }
}