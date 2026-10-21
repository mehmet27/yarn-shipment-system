package com.yarnshipment.gateway.web;

import java.net.URI;
import java.util.Optional;
import org.jspecify.annotations.Nullable;

/**
 * Girişten sonra dönülecek adres. Yalnızca SPA ile aynı kökene (şema, ana makine, port) ya da köke göreli yola izin
 * verilir; aksi halde açık yönlendirme açığı olurdu.
 */
final class ReturnTarget {

    private final URI origin;

    ReturnTarget(String appUrl) {
        this.origin = URI.create(appUrl).resolve("/");
    }

    /** İstenen adres güvenliyse mutlak adresi, değilse boş döner. */
    Optional<String> resolve(@Nullable String requested) {
        if (requested == null || requested.isBlank() || requested.startsWith("//") || requested.contains("\\")) {
            return Optional.empty();
        }
        try {
            URI target = origin.resolve(URI.create(requested));
            return sameOrigin(target) ? Optional.of(target.toString()) : Optional.empty();
        } catch (IllegalArgumentException e) {
            return Optional.empty();
        }
    }

    private boolean sameOrigin(URI target) {
        return origin.getScheme().equalsIgnoreCase(target.getScheme())
                && origin.getAuthority().equalsIgnoreCase(target.getAuthority());
    }
}
