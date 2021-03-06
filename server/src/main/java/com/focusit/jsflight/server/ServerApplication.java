package com.focusit.jsflight.server;

import com.focusit.jsflight.server.service.SettingsService;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories;

import javax.inject.Inject;

/**
 * Generic Spring Boot Application entry point
 */
@Configuration
@EnableAutoConfiguration
@ComponentScan
@SpringBootApplication
@EnableMongoRepositories(basePackages = "com.focusit.jsflight.server.repository")
@PropertySource(value = "classpath:application.properties", ignoreResourceNotFound = true)
public class ServerApplication implements CommandLineRunner
{
    @Inject
    SettingsService settingsService;

    public static void main(String[] args)
    {
        SpringApplication.run(ServerApplication.class, args);
    }

    @Override
    public void run(String... strings) throws Exception
    {
        settingsService.getSettings();
    }
}
