package com.commercesuite.webhooks.controller.dto;

import com.commercesuite.webhooks.domain.WebhookEndpointStatus;

public record UpdateEndpointRequest(String name, String url, String description,
                                    WebhookEndpointStatus status) {}