# syntax=docker/dockerfile:1.6

ARG KEYCLOAK_VERSION=23.0.6

FROM quay.io/keycloak/keycloak:${KEYCLOAK_VERSION} AS builder

ARG RUSSIAN_PROVIDERS_VERSION=23.0.6.rsp-3
ARG JSON_PATH_VERSION=2.9.0
ARG JSON_SMART_VERSION=2.5.0
ARG ACCESSORS_SMART_VERSION=2.5.0

ADD --checksum=sha256:51270964e9a223ba8c53c6ebfa94eb00c9166cb06a9cdb166f869f4bce8f8b11 --chown=keycloak:root \
    https://repo1.maven.org/maven2/ru/playa/keycloak/keycloak-russian-providers/${RUSSIAN_PROVIDERS_VERSION}/keycloak-russian-providers-${RUSSIAN_PROVIDERS_VERSION}.jar \
    /opt/keycloak/providers/keycloak-russian-providers-${RUSSIAN_PROVIDERS_VERSION}.jar
ADD --checksum=sha256:11a9ee6f88bb31f1450108d1cf6441377dec84aca075eb6bb2343be157575bea --chown=keycloak:root \
    https://repo1.maven.org/maven2/com/jayway/jsonpath/json-path/${JSON_PATH_VERSION}/json-path-${JSON_PATH_VERSION}.jar \
    /opt/keycloak/providers/json-path-${JSON_PATH_VERSION}.jar
ADD --checksum=sha256:432b9e545848c4141b80717b26e367f83bf33f19250a228ce75da6e967da2bc7 --chown=keycloak:root \
    https://repo1.maven.org/maven2/net/minidev/json-smart/${JSON_SMART_VERSION}/json-smart-${JSON_SMART_VERSION}.jar \
    /opt/keycloak/providers/json-smart-${JSON_SMART_VERSION}.jar
ADD --checksum=sha256:12314fc6881d66a413fd66370787adba16e504fbf7e138690b0f3952e3fbd321 --chown=keycloak:root \
    https://repo1.maven.org/maven2/net/minidev/accessors-smart/${ACCESSORS_SMART_VERSION}/accessors-smart-${ACCESSORS_SMART_VERSION}.jar \
    /opt/keycloak/providers/accessors-smart-${ACCESSORS_SMART_VERSION}.jar

RUN /opt/keycloak/bin/kc.sh build

FROM quay.io/keycloak/keycloak:${KEYCLOAK_VERSION}

COPY --from=builder /opt/keycloak/ /opt/keycloak/

ENTRYPOINT ["/opt/keycloak/bin/kc.sh"]
