package org.koikifw.performance.koiki;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/** KOIKI Starter comparison variant using the same fixture binary as bare. */
@SpringBootApplication(scanBasePackages = "org.koikifw.performance")
public class PerformanceKoikiApplication {

    public static void main(String[] args) {
        SpringApplication.run(PerformanceKoikiApplication.class, args);
    }
}
