package com.example.koikiconsumer.c2;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/** Consumer-owned synthetic routes used only for packaged security composition verification. */
@RestController
class C2ConsumerController {

    @GetMapping("/consumer/c2/public")
    String publicRoute() {
        return "c2-consumer-public-ok";
    }

    @GetMapping("/consumer/c2/private")
    String privateRoute() {
        return "c2-consumer-private-ok";
    }
}
