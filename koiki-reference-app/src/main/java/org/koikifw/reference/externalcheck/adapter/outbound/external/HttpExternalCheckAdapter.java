package org.koikifw.reference.externalcheck.adapter.outbound.external;

import java.math.BigDecimal;
import java.net.http.HttpClient;
import java.time.Duration;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.koikifw.reference.externalcheck.application.ExternalCheckFailure;
import org.koikifw.reference.externalcheck.application.ExternalCheckFailure.Kind;
import org.koikifw.reference.externalcheck.application.dto.ExternalCheckData.Query;
import org.koikifw.reference.externalcheck.application.dto.ExternalCheckData.Rate;
import org.koikifw.reference.externalcheck.configuration.ExternalCheckProperties;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.ResourceAccessException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

/** Owns one finite synchronous transport; status bodies and redirects are never trusted. */
public final class HttpExternalCheckAdapter implements AutoCloseable {
    private final ExecutorService executor = Executors.newFixedThreadPool(3);
    private final HttpClient transport;
    private final RestClient client;
    private final ObjectMapper mapper;

    public HttpExternalCheckAdapter(ExternalCheckProperties properties, ObjectMapper mapper) {
        this.mapper = mapper;
        transport = HttpClient.newBuilder().connectTimeout(properties.connectTimeout())
                .executor(executor).followRedirects(HttpClient.Redirect.NEVER)
                .version(HttpClient.Version.HTTP_1_1).build();
        var factory = new JdkClientHttpRequestFactory(transport, executor);
        factory.setReadTimeout(properties.readTimeout());
        client = RestClient.builder().baseUrl(properties.providerBaseUri().toString())
                .requestFactory(factory).build();
    }

    public Rate lookup(UUID operationId, Query query) {
        if (Thread.currentThread().isInterrupted()) { throw new ExternalCheckFailure(Kind.INTERRUPTED); }
        try {
            return Objects.requireNonNull(client.get().uri(builder -> builder.path("/rates")
                            .queryParam("base", query.base()).queryParam("quote", query.quote())
                            .queryParam("date", query.date()).build())
                    .header("X-Operation-Id", operationId.toString())
                    .exchange((request, response) -> {
                        int status = response.getStatusCode().value();
                        if (status == 503) { throw new ExternalCheckFailure.TransientRateFailure(); }
                        if (status == 429) { throw new ExternalCheckFailure(Kind.PROVIDER_RATE_LIMITED); }
                        if (status == 400 || status == 404) { throw new ExternalCheckFailure(Kind.PROVIDER_REJECTED); }
                        if (status != 200) { throw new ExternalCheckFailure(Kind.PROVIDER_UNAVAILABLE); }
                        byte[] body = response.getBody().readNBytes(4097);
                        if (body.length > 4096) { throw new ExternalCheckFailure(Kind.INVALID_PROVIDER_RESPONSE); }
                        return decode(operationId, query, body);
                    }));
        } catch (ResourceAccessException failure) {
            throw new ExternalCheckFailure(Thread.currentThread().isInterrupted()
                    ? Kind.INTERRUPTED : Kind.PROVIDER_TIMEOUT);
        }
    }

    private Rate decode(UUID operationId, Query query, byte[] body) {
        try {
            JsonNode root = mapper.readTree(body);
            if (root == null || !root.isObject() || root.size() != 4
                    || !text(root, "base").equals(query.base()) || !text(root, "quote").equals(query.quote())
                    || !text(root, "date").equals(query.date().toString())) {
                throw new ExternalCheckFailure(Kind.INVALID_PROVIDER_RESPONSE);
            }
            String rate = text(root, "rate");
            if (rate.length() > 32 || !rate.matches("[0-9]+(?:\\.[0-9]{1,8})?")
                    || new BigDecimal(rate).signum() <= 0) {
                throw new ExternalCheckFailure(Kind.INVALID_PROVIDER_RESPONSE);
            }
            return new Rate(operationId, query.base(), query.quote(), query.date(), rate);
        } catch (tools.jackson.core.JacksonException | IllegalArgumentException failure) {
            throw new ExternalCheckFailure(Kind.INVALID_PROVIDER_RESPONSE);
        }
    }

    private static String text(JsonNode node, String field) {
        JsonNode value = node.get(field);
        if (value == null || !value.isString()) { throw new ExternalCheckFailure(Kind.INVALID_PROVIDER_RESPONSE); }
        return value.stringValue();
    }

    @Override
    public void close() {
        transport.shutdownNow();
        executor.shutdownNow();
        try {
            if (!transport.awaitTermination(Duration.ofSeconds(2)) || !executor.awaitTermination(2, TimeUnit.SECONDS)) {
                throw new IllegalStateException("External check transport did not terminate");
            }
        } catch (InterruptedException failure) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("External check cleanup interrupted");
        }
    }
}
