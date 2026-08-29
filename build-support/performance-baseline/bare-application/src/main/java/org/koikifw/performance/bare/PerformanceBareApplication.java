package org.koikifw.performance.bare;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/** Bare Spring Boot comparison variant with no KOIKI runtime artifact. */
@SpringBootApplication(scanBasePackages = "org.koikifw.performance")
public class PerformanceBareApplication {

    public static void main(String[] args) {
        SpringApplication.run(PerformanceBareApplication.class, args);
    }
}
