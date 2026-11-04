package com.yarnshipment.shipment.application.port.out;

import java.util.Collection;
import java.util.Map;
import java.util.UUID;

/** Kullanıcı kimliklerini okunur adlara çevirir (H-24). */
public interface ActorNamePort {

    /** Adı bulunanların haritası; kimlik sağlayıcıya ulaşılamazsa ya da kullanıcı yoksa o kimlik haritada olmaz. */
    Map<UUID, String> namesOf(Collection<UUID> userIds);
}
