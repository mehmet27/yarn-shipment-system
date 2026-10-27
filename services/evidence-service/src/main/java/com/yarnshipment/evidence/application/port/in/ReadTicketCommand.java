package com.yarnshipment.evidence.application.port.in;

import com.yarnshipment.common.security.Actor;
import java.util.UUID;

/** Kantar fişi kanıtını okuma komutu. */
public record ReadTicketCommand(Actor actor, UUID evidenceId) {}
