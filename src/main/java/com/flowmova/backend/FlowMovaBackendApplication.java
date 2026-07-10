package com.flowmova.backend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class FlowMovaBackendApplication {

    public static void main(String[] args) {
        SpringApplication.run(FlowMovaBackendApplication.class, args);
    }
}
