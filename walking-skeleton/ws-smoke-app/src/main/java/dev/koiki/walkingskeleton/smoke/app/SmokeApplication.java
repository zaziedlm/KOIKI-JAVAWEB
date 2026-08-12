package dev.koiki.walkingskeleton.smoke.app;

import dev.koiki.walkingskeleton.smoke.lib.GreetingService;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

/**
 * Minimal Spring Boot application for Walking Skeleton build/runtime verification.
 */
@SpringBootApplication
public class SmokeApplication {

    public static void main(String[] args) {
        SpringApplication.run(SmokeApplication.class, args);
    }

    @Bean
    GreetingService greetingService() {
        return new GreetingService();
    }

    @Bean
    CommandLineRunner smokeRunner(GreetingService greetingService) {
        return args -> System.out.println(greetingService.greeting("Spring Boot"));
    }
}
