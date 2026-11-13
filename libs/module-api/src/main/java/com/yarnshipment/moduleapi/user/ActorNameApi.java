package com.yarnshipment.moduleapi.user;

import java.util.Collection;
import java.util.Map;
import java.util.UUID;

/**
 * Kullanıcı kimliklerini okunur adlara çevirir (H-24). Geçmiş ekranları (sevkiyat, uyuşmazlık) kişi adını bu
 * arayüzden alır; adın kaynağı (kimlik sağlayıcı ya da uygulama veritabanı) çağıranı ilgilendirmez.
 */
public interface ActorNameApi {

    /**
     * Adı bulunanların haritası (ad soyad, yoksa giriş adı). Kaynağa ulaşılamazsa ya da kullanıcı yoksa o kimlik
     * haritada olmaz; okuma bu yüzden hiç bozulmaz.
     */
    Map<UUID, String> namesOf(Collection<UUID> userIds);
}
