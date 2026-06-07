package com.commercesuite.webhooks.controller.dto;

import java.util.UUID;

/** Only place the plaintext secret is ever returned to the client. */
public record RotateSecretResponse(UUID secretId, String secret) {}