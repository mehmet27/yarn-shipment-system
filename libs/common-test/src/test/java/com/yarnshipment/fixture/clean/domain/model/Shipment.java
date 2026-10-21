package com.yarnshipment.fixture.clean.domain.model;

import java.time.Instant;

public record Shipment(String shipmentNo, Instant createdAt) {}
