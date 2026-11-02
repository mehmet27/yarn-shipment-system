package com.yarnshipment.bag.application.port.in;

import com.yarnshipment.common.security.Actor;
import java.util.UUID;

/** Bir partinin çuvalları. */
public record ListLotBagsQuery(Actor actor, UUID lotId) {}
