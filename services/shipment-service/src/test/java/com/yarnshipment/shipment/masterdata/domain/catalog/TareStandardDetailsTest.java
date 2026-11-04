package com.yarnshipment.shipment.masterdata.domain.catalog;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.yarnshipment.shipment.masterdata.domain.model.InvalidMasterDataException;
import java.math.BigDecimal;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class TareStandardDetailsTest {

    @Test
    @DisplayName("Sıfır dara ve sıfır ya da yüzde yüz sapma geçerlidir")
    void shouldAcceptBounds() {
        assertThat(new TareStandardDetails(BigDecimal.ZERO, BigDecimal.ZERO)).isNotNull();
        assertThat(new TareStandardDetails(new BigDecimal("0.450"), new BigDecimal("100")))
                .isNotNull();
    }

    @Test
    @DisplayName("Negatif dara ve aralık dışı sapma alan adıyla reddedilir")
    void shouldRejectOutOfRange() {
        assertThatThrownBy(() -> new TareStandardDetails(new BigDecimal("-0.1"), BigDecimal.ONE))
                .isInstanceOfSatisfying(
                        InvalidMasterDataException.class,
                        e -> assertThat(e.field()).isEqualTo("standardTareKg"));
        assertThatThrownBy(() -> new TareStandardDetails(BigDecimal.ONE, new BigDecimal("100.01")))
                .isInstanceOfSatisfying(
                        InvalidMasterDataException.class,
                        e -> assertThat(e.field()).isEqualTo("maxDeviationPct"));
        assertThatThrownBy(() -> new TareStandardDetails(BigDecimal.ONE, new BigDecimal("-1")))
                .isInstanceOf(InvalidMasterDataException.class);
    }
}
