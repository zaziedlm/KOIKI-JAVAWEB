package org.koikifw.reference.identity.adapter.inbound.web;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/** The SPA handler materializes the cookie in the CSRF filter, never in JSON. */
@RestController
public class ReferenceSessionCsrfController {
    @GetMapping("/spa-api/csrf")
    ResponseEntity<Void> csrf() {
        return ResponseEntity.noContent().build();
    }
}
