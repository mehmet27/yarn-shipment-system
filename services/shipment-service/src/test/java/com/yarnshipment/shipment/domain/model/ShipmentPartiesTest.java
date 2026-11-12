package com.yarnshipment.shipment.domain.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class ShipmentPartiesTest {

    private static final UUID RECEIVER = UUID.fromString("0192f0a0-0000-7000-8000-000000000003");
    private static final UUID SOURCE = UUID.fromString("0192f0a0-0000-7000-8000-000000000013");

    @Test
    @DisplayName("Gönderen fasoncu verilmezse sevkiyat merkezdendir")
    void shouldDefaultToCentralSource() {
        // when
        ShipmentParties parties = new ShipmentParties(RECEIVER, "Örnek Tekstil", null, null, null);

        // then
        assertThat(parties.sourceSubcontractorId()).isNull();
        assertThat(parties.sourceSubcontractorTitle()).isNull();
    }

    @Test
    @DisplayName("Gönderen fasoncu kimliği ve unvanı birlikte verilir")
    void shouldRequireSourceIdAndTitleTogether() {
        // when / then
        assertThatThrownBy(() -> new ShipmentParties(RECEIVER, "Örnek Tekstil", null, null, null, SOURCE, null))
                .isInstanceOf(InvalidNewShipmentException.class);
        assertThat(new ShipmentParties(RECEIVER, "Örnek Tekstil", null, null, null, SOURCE, "Başka Dokuma")
                        .sourceSubcontractorTitle())
                .isEqualTo("Başka Dokuma");
    }

    @Test
    @DisplayName("Gönderen ve alıcı fasoncu aynı olamaz")
    void shouldRejectSameSourceAndReceiver() {
        // when / then
        assertThatThrownBy(() -> new ShipmentParties(RECEIVER, "Örnek Tekstil", null, null, null, RECEIVER, "Örnek"))
                .isInstanceOf(InvalidNewShipmentException.class);
    }

    @Test
    @DisplayName("Kapıdan aktarma tedarikçi ile işaretlenir; irsaliye tedarikçisiz olmaz")
    void shouldMarkDirectTransferBySupplier() {
        // when
        ShipmentParties direct =
                new ShipmentParties(RECEIVER, "Örnek Tekstil", null, null, null, null, null, "Esha İplik", "IRS-1");

        // then
        assertThat(direct.directTransfer()).isTrue();
        assertThat(new ShipmentParties(RECEIVER, "Örnek Tekstil", null, null, null).directTransfer())
                .isFalse();
        assertThatThrownBy(() ->
                        new ShipmentParties(RECEIVER, "Örnek Tekstil", null, null, null, null, null, null, "IRS-1"))
                .isInstanceOf(InvalidNewShipmentException.class);
    }

    @Test
    @DisplayName("Kapıdan aktarma gönderen fasoncu ile birlikte verilemez ve tedarikçi boş olamaz")
    void shouldRejectDirectTransferFromSubcontractorOrBlankSupplier() {
        // when / then
        assertThatThrownBy(() -> new ShipmentParties(
                        RECEIVER, "Örnek Tekstil", null, null, null, SOURCE, "Başka Dokuma", "Esha İplik", null))
                .isInstanceOf(InvalidNewShipmentException.class);
        assertThatThrownBy(
                        () -> new ShipmentParties(RECEIVER, "Örnek Tekstil", null, null, null, null, null, " ", null))
                .isInstanceOf(InvalidNewShipmentException.class);
    }
}
