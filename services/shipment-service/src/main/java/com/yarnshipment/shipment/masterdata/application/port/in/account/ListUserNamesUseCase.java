package com.yarnshipment.shipment.masterdata.application.port.in.account;

import java.util.Collection;
import java.util.Map;
import java.util.UUID;

/** Kullanıcı kimliklerini okunur adlara çevirir (ad soyad, yoksa giriş adı); bulunamayan kimlik haritada olmaz. */
public interface ListUserNamesUseCase {

    Map<UUID, String> listUserNames(Collection<UUID> userIds);
}
