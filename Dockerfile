FROM --platform=$BUILDPLATFORM eclipse-temurin:21-jre-jammy AS builder

ARG BUILD_NUMBER
ENV BUILD_NUMBER ${BUILD_NUMBER:-1_0_0}

WORKDIR /app
ADD . .
RUN ./gradlew --no-daemon assemble

FROM eclipse-temurin:21-jre-jammy
LABEL maintainer="HMPPS Digital Studio <info@digital.justice.gov.uk>"

ARG BUILD_NUMBER
ENV BUILD_NUMBER ${BUILD_NUMBER:-1_0_0}

RUN apt-get update && \
    apt-get -y upgrade && \
    apt-get install -y curl && \
    rm -rf /var/lib/apt/lists/*

ENV TZ=Europe/London
RUN ln -snf "/usr/share/zoneinfo/$TZ" /etc/localtime && echo "$TZ" > /etc/timezone

RUN addgroup --gid 2000 --system appgroup && \
    adduser --uid 2000 --system appuser --gid 2000

WORKDIR /app
COPY --from=builder --chown=appuser:appgroup /app/build/libs/approved-premises-api*.jar /app/app.jar
COPY --from=builder --chown=appuser:appgroup /app/build/libs/applicationinsights-agent*.jar /app/agent.jar
COPY --from=builder --chown=appuser:appgroup /app/applicationinsights.json /app
COPY --from=builder --chown=appuser:appgroup /app/applicationinsights.nonprod.json /app

COPY --from=builder --chown=appuser:appgroup /app/script/run_seed_job /app
COPY --from=builder --chown=appuser:appgroup /app/script/run_seed_from_excel_job /app
COPY --from=builder --chown=appuser:appgroup /app/script/run_migration_job /app
COPY --from=builder --chown=appuser:appgroup /app/script/hard_delete /app
COPY --from=builder --chown=appuser:appgroup /app/script/clear_cache /app
RUN mkdir /tmp/seed && chown appuser:appgroup /tmp/seed && chmod +x /app/run_seed_job && chmod +x /app/run_seed_from_excel_job && chmod +x /app/run_migration_job && chmod +x /app/hard_delete && chmod +x /app/clear_cache

USER 2000

ENTRYPOINT ["java", "-XX:+AlwaysActAsServerClassMachine", "-javaagent:/app/agent.jar", "-jar", "/app/app.jar"]
