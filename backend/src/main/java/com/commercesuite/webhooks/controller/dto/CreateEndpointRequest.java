package com.commercesuite.webhooks.controller.dto;

import java.util.UUID;

public record CreateEndpointRequest(String ownerType, UUID ownerId,
                                    String name, String url, String description) {}