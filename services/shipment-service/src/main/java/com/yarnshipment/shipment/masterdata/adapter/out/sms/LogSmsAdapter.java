package com.yarnshipment.shipment.masterdata.adapter.out.sms;

import com.yarnshipment.shipment.masterdata.application.port.out.SmsPort;
import com.yarnshipment.shipment.masterdata.domain.model.PhoneNumber;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * Yerel geliştirme için sahte SMS gönderici: mesajı gerçek bir telefona göndermez, loga yazar (kod dahil). Yalnızca
 * {@code yarnshipment.sms.provider=log} ile açılır; canlıda açılmamalıdır.
 */
@Component
@ConditionalOnProperty(name = "yarnshipment.sms.provider", havingValue = "log")
final class LogSmsAdapter implements SmsPort {

    private static final Logger LOG = LoggerFactory.getLogger(LogSmsAdapter.class);

    @Override
    public void send(PhoneNumber to, String text) {
        LOG.warn("SMS (sahte gönderici) to={} text={}", to.masked(), text);
    }
}
