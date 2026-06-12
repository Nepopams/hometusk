package com.hometusk.auth.keycloak;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(KeycloakAuthProperties.class)
public class KeycloakAuthConfiguration {}
