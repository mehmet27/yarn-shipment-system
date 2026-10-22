package com.yarnshipment.bag.application.port.in;

import com.yarnshipment.common.security.Actor;
import java.util.List;
import java.util.UUID;

/** Çuvalları bir sevkiyata ayırma isteği. */
public record ReserveBagsCommand(Actor actor, UUID shipmentId, List<String> uniqueKeys) {}
