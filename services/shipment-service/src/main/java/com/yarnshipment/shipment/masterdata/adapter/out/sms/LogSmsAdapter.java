package com.yarnshipment.shipment.masterdata.adapter.out.sms;

import com.yarnshipment.shipment.masterdata.application.port.out.SmsPort;
import com.yarnshipment.shipment.masterdata.domain.model.PhoneNumber;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * Yerel geliştirme için sahte SMS gönderici: mesajı gerçek bir telefona göndermez, loga yazar (kod dahil). Yalnızca
 * {@code yarnshipment.sms.provider=log} ile açılır; canlıda açılmamalıdır.
 * Ek olarak {@code local} profilinde mesaj {@code sms_dev_outbox} tablosuna da yazılır ({@link LocalSmsDevOutbox}).
 */
@Component
@ConditionalOnProperty(name = "yarnshipment.sms.provider", havingValue = "log")
final class LogSmsAdapter implements SmsPort {

    private static final Logger LOG = LoggerFactory.getLogger(LogSmsAdapter.class);

    private final ObjectProvider<SmsDevOutbox> devOutbox;

    LogSmsAdapter(ObjectProvider<SmsDevOutbox> devOutbox) {
        this.devOutbox = devOutbox;
    }

    @Override
    public void send(PhoneNumber to, String text) {
        LOG.warn("SMS (sahte gönderici) to={} text={}", to.masked(), text);
        devOutbox.ifAvailable(outbox -> outbox.record(to, text));
    }
}
