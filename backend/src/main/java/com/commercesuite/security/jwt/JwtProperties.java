package com.commercesuite.security.jwt;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.jwt")
public record JwtProperties(String secret, String issuer, long accessTtlMinutes, long refreshTtlDays) {}
