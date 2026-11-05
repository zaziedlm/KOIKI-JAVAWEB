# Runtime-only recipe: build the application in its own project, then stage app.jar.
ARG JAVA_RUNTIME_IMAGE=eclipse-temurin:21-jre-noble@sha256:0eda5830ed7b669abc246a8dc520ea8a23cab06f9a8d4b6964109e2f2394e46f
FROM ${JAVA_RUNTIME_IMAGE}
WORKDIR /app
COPY --chown=10001:10001 app.jar /app/app.jar
COPY --chown=10001:10001 probe.sh /app/probe.sh
USER 10001:10001
EXPOSE 8080
STOPSIGNAL SIGTERM
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
