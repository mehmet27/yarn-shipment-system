package com.yarnshipment.shipment.masterdata.application.port.out;

import com.yarnshipment.shipment.masterdata.domain.model.StaffWorkArea;
import java.util.Map;
import java.util.UUID;

/** Kalite kullanıcısının çalışma alanını yerel veritabanına yazar, siler ve okur. */
public interface StaffWorkAreaPort {

    /** Alanı yazar; kullanıcının önceki alanı varsa değiştirir. */
    void save(UUID userId, StaffWorkArea workArea);

    /** Kullanıcının alanını siler; kullanıcı kalite rolünden çıkınca çağrılır. */
    void delete(UUID userId);

    /** Kayıtlı bütün alanlar, kullanıcı kimliğine göre. */
    Map<UUID, StaffWorkArea> findAll();
}
