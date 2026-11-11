package com.yarnshipment.shipment.domain.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class ShipmentSettingsTest {

    @Test
    @DisplayName("Varsayılanlar %4 fire ve 0 kg tolerans")
    void shouldDefaultToFourPercentAndZeroTolerance() {
        assertThat(ShipmentSettings.DEFAULTS.defaultFireRate()).isEqualByComparingTo("0.04");
        assertThat(ShipmentSettings.DEFAULTS.weightToleranceKg()).isEqualByComparingTo("0");
    }

    @Test
    @DisplayName("Fire oranı 0 ile 0.20 arasında olmalı; tolerans negatif olamaz")
    void shouldRejectOutOfRangeValues() {
        assertThat(new ShipmentSettings(new BigDecimal("0.2"), BigDecimal.TEN).defaultFireRate())
                .isEqualByComparingTo("0.2000");
        assertThatThrownBy(() -> new ShipmentSettings(new BigDecimal("0.2001"), BigDecimal.ZERO))
                .isInstanceOf(InvalidNewShipmentException.class);
        assertThatThrownBy(() -> new ShipmentSettings(new BigDecimal("-0.01"), BigDecimal.ZERO))
                .isInstanceOf(InvalidNewShipmentException.class);
        assertThatThrownBy(() -> new ShipmentSettings(new BigDecimal("0.04"), new BigDecimal("-1")))
                .isInstanceOf(InvalidNewShipmentException.class);
    }
}
