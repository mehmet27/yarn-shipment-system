package com.yarnshipment.evidence.application.port.in;

import com.yarnshipment.common.security.Actor;
import com.yarnshipment.evidence.domain.ticket.UnverifiedReason;
import java.util.UUID;

/** Okunamayan fişle gerekçe seçerek devam etme komutu. */
public record DeclareTicketUnverifiedCommand(Actor actor, UUID evidenceId, UnverifiedReason reason) {}
