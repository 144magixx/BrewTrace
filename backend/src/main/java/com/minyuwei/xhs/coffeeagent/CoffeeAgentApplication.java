package com.minyuwei.xhs.coffeeagent;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class CoffeeAgentApplication {
    public static void main(String[] args) {
        SpringApplication.run(CoffeeAgentApplication.class, args);
    }
}
