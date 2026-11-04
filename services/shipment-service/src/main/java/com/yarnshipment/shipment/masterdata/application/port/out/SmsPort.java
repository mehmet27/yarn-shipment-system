package com.yarnshipment.shipment.masterdata.application.port.out;

import com.yarnshipment.shipment.masterdata.domain.model.PhoneNumber;

/** Kısa mesaj sağlayıcısı. Gerçek sağlayıcı ortamdan seçilir; yerelde mesaj yalnızca loga yazılır. */
public interface SmsPort {

    /**
     * Mesajı gönderir. Sağlayıcı yapılandırılmamışsa ya da yanıt vermezse {@code SmsUnavailableException} fırlatır.
     * Uygulamalar mesaj metnini (kod içerir) üretimde loglamaz.
     */
    void send(PhoneNumber to, String text);
}
