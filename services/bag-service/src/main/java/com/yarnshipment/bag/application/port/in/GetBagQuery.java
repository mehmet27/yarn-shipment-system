package com.yarnshipment.bag.application.port.in;

import com.yarnshipment.common.security.Actor;

/** Unique Key ile çuval sorgusu. */
public record GetBagQuery(Actor actor, String uniqueKey) {}
