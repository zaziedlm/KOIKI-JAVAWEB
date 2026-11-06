# Runtime-only recipe: build the application in its own project, then stage app.jar.
ARG JAVA_RUNTIME_IMAGE=eclipse-temurin:21-jre-noble@sha256:0eda5830ed7b669abc246a8dc520ea8a23cab06f9a8d4b6964109e2f2394e46f
FROM ${JAVA_RUNTIME_IMAGE} AS extractor
WORKDIR /builder
COPY app.jar app.jar
RUN java -Djarmode=tools -jar app.jar extract --layers --destination extracted

FROM ${JAVA_RUNTIME_IMAGE} AS runtime
WORKDIR /app
COPY --chown=10001:10001 probe.sh /app/probe.sh
COPY --from=extractor --chown=10001:10001 /builder/extracted/dependencies/ ./
COPY --from=extractor --chown=10001:10001 /builder/extracted/spring-boot-loader/ ./
COPY --from=extractor --chown=10001:10001 /builder/extracted/snapshot-dependencies/ ./
COPY --from=extractor --chown=10001:10001 /builder/extracted/application/ ./
USER 10001:10001
EXPOSE 8080
STOPSIGNAL SIGTERM
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
# Source identity, not a digest of the extracted runtime files. Keep after layer COPYs.
ARG D3_JAR_SHA256
LABEL org.koikifw.d3.source-jar-sha256=${D3_JAR_SHA256}
