package com.ucd.urbanflow.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.stream.Collectors;

/**
 * A temporary diagnostic controller to inspect runtime security configurations.
 */
@RestController
@RequestMapping("/diag")
public class DiagnosticController {

    @Autowired
    private ApplicationContext applicationContext;

    /**
     * This endpoint inspects all SecurityFilterChain beans present in the application context
     * and returns their details. This helps diagnose configuration issues.
     * @return A list of strings, each describing a security filter chain.
     */
    @GetMapping("/security-rules")
    public List<String> getSecurityFilterChainInfo() {
        // Find all beans of type SecurityFilterChain
        return applicationContext.getBeansOfType(SecurityFilterChain.class)
                .entrySet().stream()
                .map(entry -> "Bean Name: " + entry.getKey() + " | Object: " + entry.getValue().toString())
                .collect(Collectors.toList());
    }
}