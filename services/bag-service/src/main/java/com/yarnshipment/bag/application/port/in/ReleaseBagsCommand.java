package com.yarnshipment.bag.application.port.in;

import com.yarnshipment.common.security.Actor;
import java.util.List;
import java.util.UUID;

/**
 * Bir sevkiyatın rezervasyonlarını bırakma isteği. {@code uniqueKeys} boşsa sevkiyatın tüm rezervasyonları, doluysa
 * yalnızca o çuvallar bırakılır.
 */
public record ReleaseBagsCommand(Actor actor, UUID shipmentId, List<String> uniqueKeys, String reason) {}
