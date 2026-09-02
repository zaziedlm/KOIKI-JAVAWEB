package com.example.koikiconsumer.security;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
class ConsumerController {

    @GetMapping("/consumer/public")
    String publicRoute() {
        return "consumer-public-ok";
    }

    @GetMapping("/consumer/private")
    String privateRoute() {
        return "consumer-private";
    }
}
