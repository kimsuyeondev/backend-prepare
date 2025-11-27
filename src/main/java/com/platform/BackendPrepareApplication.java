package com.platform;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@SpringBootApplication
@ComponentScan(basePackages = {"com.platform", "com.api"})
@EnableJpaRepositories(basePackages = {"com.platform", "com.api"})
@EntityScan(basePackages = {"com.platform", "com.api"})
public class BackendPrepareApplication {

    public static void main(String[] args) {
        SpringApplication.run(BackendPrepareApplication.class, args);
    }
}
