package com.mall;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class MallServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(MallServiceApplication.class, args);
    }
}
