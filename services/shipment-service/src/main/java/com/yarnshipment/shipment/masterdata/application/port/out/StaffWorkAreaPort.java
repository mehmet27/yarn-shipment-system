package com.yarnshipment.shipment.masterdata.application.port.out;

import com.yarnshipment.shipment.masterdata.domain.model.StaffWorkArea;
import java.util.UUID;

/** Kalite kullanıcısının çalışma alanını yerel veritabanına yazar. */
public interface StaffWorkAreaPort {

    /** Alanı yazar; kullanıcının önceki alanı varsa değiştirir. */
    void save(UUID userId, StaffWorkArea workArea);
}
