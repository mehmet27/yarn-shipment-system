package com.yarnshipment.discrepancy.application.port.in.claim;

import com.yarnshipment.discrepancy.domain.claim.ClaimTarget;
import java.time.Instant;
import java.util.List;
import java.util.Objects;

/**
 * Bir sevkiyatın itiraz dosyası.
 *
 * @param target itiraz edilebilecek teslim
 * @param windowOpen itiraz süresi sürüyor mu
 * @param claims açılmış itirazlar
 */
public record ShipmentClaims(ClaimTarget target, boolean windowOpen, List<ClaimFile> claims) {

    public ShipmentClaims {
        Objects.requireNonNull(target, "target");
        claims = List.copyOf(claims);
    }

    public Instant deadline() {
        return target.deadline();
    }
}
