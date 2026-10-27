package com.yarnshipment.evidence.application.port.in;

import com.yarnshipment.common.security.Actor;
import java.util.UUID;

/** Yüklemeyi onaylama komutu. */
public record ConfirmUploadCommand(Actor actor, UUID evidenceId) {}
