package com.yarnshipment.bag.application.port.in;

import com.yarnshipment.common.security.Actor;
import java.util.UUID;

/** Bir sevkiyatın tüm rezervasyonlarını bırakma isteği. */
public record ReleaseBagsCommand(Actor actor, UUID shipmentId, String reason) {}
