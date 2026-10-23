package org.koikifw.reference.externalcheck.adapter.inbound.sessionapi;

import jakarta.servlet.http.HttpServletRequest;
import java.time.LocalDate;
import java.util.Set;
import java.util.UUID;
import org.koikifw.reference.externalcheck.application.ExternalCheckFailure;
import org.koikifw.reference.externalcheck.application.ExternalCheckService;
import org.koikifw.reference.externalcheck.application.dto.ExternalCheckData.Query;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Read-only Session transport. No external receipt POST or UI is provided. */
@RestController
@ConditionalOnProperty(name = "koiki.reference.external-check.enabled", havingValue = "true")
public class ExternalCheckController {
    private final ExternalCheckService service;
    public ExternalCheckController(ExternalCheckService service) { this.service = service; }

    @GetMapping("/spa-api/v1/external-checks/rate")
    RateResponse lookup(@RequestParam String base, @RequestParam String quote,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            HttpServletRequest request) {
        String query = request.getQueryString();
        if ((query != null && query.length() > 4096)
                || !request.getParameterMap().keySet().equals(Set.of("base", "quote", "date"))
                || request.getParameterMap().values().stream().anyMatch(values -> values.length != 1)) {
            throw new ExternalCheckFailure(ExternalCheckFailure.Kind.INVALID_INPUT);
        }
        var rate = service.lookup(new Query(base, quote, date));
        return new RateResponse(rate.operationId(), rate.base(), rate.quote(), rate.date(), rate.rate());
    }

    record RateResponse(UUID operationId, String base, String quote, LocalDate date, String rate) { }
}
