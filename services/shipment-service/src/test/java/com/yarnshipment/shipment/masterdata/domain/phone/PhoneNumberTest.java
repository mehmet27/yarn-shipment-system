package com.yarnshipment.shipment.masterdata.domain.phone;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.yarnshipment.shipment.masterdata.domain.model.InvalidMasterDataException;
import com.yarnshipment.shipment.masterdata.domain.model.PhoneNumber;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class PhoneNumberTest {

    @ParameterizedTest
    @ValueSource(strings = {"0532 123 45 67", "5321234567", "+90 532 123 45 67", "905321234567", "0 (532) 123-45-67"})
    @DisplayName("Türkiye cep numarası hangi biçimde yazılırsa yazılsın E.164 olur")
    void shouldNormalizeTurkishMobile(String raw) {
        assertThat(PhoneNumber.parse(raw).e164()).isEqualTo("+905321234567");
    }

    @ParameterizedTest
    @ValueSource(strings = {"+4915112345678", "+1 415 555 0100"})
    @DisplayName("Yurt dışı numarası + ile başlıyorsa olduğu gibi kabul edilir")
    void shouldKeepInternationalNumber(String raw) {
        assertThat(PhoneNumber.parse(raw).e164()).startsWith("+").doesNotContain(" ");
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "123", "0212 123 45 67", "abc", "+90", "05321234"})
    @DisplayName("Cep telefonu olmayan ya da eksik numara InvalidMasterDataException verir")
    void shouldRejectInvalid(String raw) {
        assertThatThrownBy(() -> PhoneNumber.parse(raw))
                .isInstanceOf(InvalidMasterDataException.class)
                .extracting(exception -> ((InvalidMasterDataException) exception).field())
                .isEqualTo("phone");
    }

    @Test
    @DisplayName("Numara loglarda ve listede yalnızca son dört haneyle görünür")
    void shouldMask() {
        PhoneNumber phone = PhoneNumber.parse("05321234567");

        assertThat(phone.masked()).isEqualTo("***4567");
        assertThat(phone.toString()).doesNotContain("532");
    }
}
