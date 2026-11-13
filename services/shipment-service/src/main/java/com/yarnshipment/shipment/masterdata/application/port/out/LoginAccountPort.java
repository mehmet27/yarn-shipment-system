package com.yarnshipment.shipment.masterdata.application.port.out;

import com.yarnshipment.shipment.masterdata.domain.model.LoginAccountSummary;
import java.util.List;

/** Giriş hesabı olan kullanıcıları (app_user) listeler. */
public interface LoginAccountPort {

    /** Tüm hesaplar, giriş adına göre sıralı. */
    List<LoginAccountSummary> findAll();
}
