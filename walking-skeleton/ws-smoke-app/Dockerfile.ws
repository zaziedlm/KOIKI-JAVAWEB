# Walking Skeleton only.
# The final KOIKI reference Dockerfile is intentionally not defined here.

ARG RUNTIME_IMAGE=eclipse-temurin:21-jre-jammy

FROM ${RUNTIME_IMAGE} AS builder
WORKDIR /builder

ARG JAR_FILE=walking-skeleton/ws-smoke-app/target/ws-smoke-app-0.0.1-SNAPSHOT.jar
COPY ${JAR_FILE} application.jar

RUN java -Djarmode=tools -jar application.jar extract --layers --destination extracted


FROM ${RUNTIME_IMAGE}
WORKDIR /application

RUN groupadd --system koiki \
    && useradd --system --gid koiki --home-dir /nonexistent --shell /usr/sbin/nologin koiki

COPY --from=builder /builder/extracted/dependencies/ ./
COPY --from=builder /builder/extracted/spring-boot-loader/ ./
COPY --from=builder /builder/extracted/snapshot-dependencies/ ./
COPY --from=builder /builder/extracted/application/ ./

USER koiki

ENTRYPOINT ["java", "-jar", "application.jar"]
