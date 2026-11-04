package com.yarnshipment.shipment.masterdata.adapter.out.sms;

import com.yarnshipment.shipment.masterdata.application.port.out.SmsPort;
import com.yarnshipment.shipment.masterdata.domain.model.PhoneNumber;
import com.yarnshipment.shipment.masterdata.domain.model.SmsUnavailableException;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * SMS sağlayıcısı seçilmemişse (varsayılan) gönderim reddedilir; kod hiçbir yere yazılmaz. Gerçek sağlayıcı
 * adaptörü eklenene kadar canlıda SMS gerektiren uçlar 502 {@code SMS_UNAVAILABLE} döner.
 */
@Component
@ConditionalOnProperty(name = "yarnshipment.sms.provider", havingValue = "none", matchIfMissing = true)
final class UnconfiguredSmsAdapter implements SmsPort {

    @Override
    public void send(PhoneNumber to, String text) {
        throw new SmsUnavailableException();
    }
}
