package com.yarnshipment.bag.application.port.in;

import com.yarnshipment.common.security.Actor;

/** İsteyenin lokasyonlarındaki kalitesi kilitli partiler. */
public record ListLockedLotsQuery(Actor actor) {}
