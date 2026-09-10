package org.koikifw.buildsupport.sessionfixture;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/** Non-distributed executable fixture for the Phase 2 B3-4 process boundary. */
@SpringBootApplication
public class SessionTwoProcessFixtureApplication {

    public static void main(String[] args) {
        SpringApplication.run(SessionTwoProcessFixtureApplication.class, args);
    }
}
