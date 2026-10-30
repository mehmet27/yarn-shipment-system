package com.yarnshipment.bag.application.port.in;

import com.yarnshipment.bag.domain.model.PackageType;
import com.yarnshipment.common.security.Actor;
import java.util.UUID;

/** Bir partiye {@code count} adet çuval üretme isteği. */
public record CreateBagsCommand(Actor actor, UUID lotId, PackageType packageType, int count) {}
