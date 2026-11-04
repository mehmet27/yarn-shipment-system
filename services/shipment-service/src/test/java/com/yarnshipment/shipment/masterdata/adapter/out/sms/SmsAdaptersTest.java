package com.yarnshipment.shipment.masterdata.adapter.out.sms;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.yarnshipment.shipment.masterdata.domain.model.PhoneNumber;
import com.yarnshipment.shipment.masterdata.domain.model.SmsUnavailableException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class SmsAdaptersTest {

    private static final PhoneNumber PHONE = new PhoneNumber("+905321234567");

    @Test
    @DisplayName("Sağlayıcı seçilmemişse gönderim SmsUnavailableException ile reddedilir")
    void shouldRefuseWhenUnconfigured() {
        assertThatThrownBy(() -> new UnconfiguredSmsAdapter().send(PHONE, "kod 123456"))
                .isInstanceOf(SmsUnavailableException.class);
    }

    @Test
    @DisplayName("Sahte gönderici hata vermeden loga yazar")
    void shouldLogOnly() {
        assertThatCode(() -> new LogSmsAdapter(new org.springframework.beans.factory.support.StaticListableBeanFactory()
                                .getBeanProvider(SmsDevOutbox.class))
                        .send(PHONE, "kod 123456"))
                .doesNotThrowAnyException();
    }
}
