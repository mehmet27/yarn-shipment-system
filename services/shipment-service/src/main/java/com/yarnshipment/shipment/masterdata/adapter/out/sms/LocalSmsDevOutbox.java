package com.yarnshipment.shipment.masterdata.adapter.out.sms;

import com.yarnshipment.shipment.masterdata.domain.model.PhoneNumber;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * Düz metin SMS'i {@code sms_dev_outbox} tablosuna yazar. İki koşul birlikte aranır: {@code local} profili ve
 * {@code yarnshipment.sms.provider=log}. Biri eksikse bean oluşmaz, dolayısıyla canlıda hiçbir şey yazılmaz.
 * Telefon maskeli saklanır.
 */
@Component
@Profile("local")
@ConditionalOnProperty(name = "yarnshipment.sms.provider", havingValue = "log")
final class LocalSmsDevOutbox implements SmsDevOutbox {

    private final JdbcTemplate jdbc;

    LocalSmsDevOutbox(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public void record(PhoneNumber to, String text) {
        jdbc.update("INSERT INTO sms_dev_outbox (phone, message) VALUES (?, ?)", to.masked(), text);
    }
}
