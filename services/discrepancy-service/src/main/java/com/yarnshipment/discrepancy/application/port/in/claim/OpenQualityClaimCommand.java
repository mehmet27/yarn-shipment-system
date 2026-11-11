package com.yarnshipment.discrepancy.application.port.in.claim;

import com.yarnshipment.common.security.Actor;
import com.yarnshipment.discrepancy.domain.claim.ClaimRequest;
import java.util.UUID;

/**
 * Kalite itirazı açma komutu.
 *
 * @param actor itirazı açan
 * @param shipmentId itiraz edilen sevkiyat
 * @param request açıklama, çuvallar ve fotoğraflar
 */
public record OpenQualityClaimCommand(Actor actor, UUID shipmentId, ClaimRequest request) {}
