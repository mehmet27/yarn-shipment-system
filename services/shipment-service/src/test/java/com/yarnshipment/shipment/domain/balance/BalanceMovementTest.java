package com.yarnshipment.shipment.domain.balance;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.yarnshipment.shipment.domain.balance.BalanceMovement.Consumption;
import com.yarnshipment.shipment.domain.balance.BalanceMovement.DispatchContext;
import com.yarnshipment.shipment.domain.balance.BalanceMovement.DispatchLine;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** Defter kuralları: işaret türe göre belirlenir, düzeltme gerekçe ister (KR-13). */
class BalanceMovementTest {

    private static final UUID SUBCONTRACTOR = UUID.randomUUID();
    private static final UUID SHIPMENT = UUID.randomUUID();
    private static final UUID USER = UUID.randomUUID();
    private static final Instant AT = Instant.parse("2026-11-12T09:00:00Z");

    private static BalanceMovement movement(MovementKind kind, String netKg, @Nullable String reason) {
        return new BalanceMovement(
                UUID.randomUUID(),
                SUBCONTRACTOR,
                new YarnRef("30/1 penye", null),
                SHIPMENT,
                kind,
                new BigDecimal(netKg),
                AT,
                USER,
                reason,
                null);
    }

    @Test
    @DisplayName("Sevkiyat hareketi artıdır ve satır başına kaynak anahtarı taşır")
    void shouldCreateDispatchMovementAsPositive() {
        // when
        BalanceMovement result = BalanceMovement.dispatch(
                new DispatchLine(2, "30/1 penye", null, new BigDecimal("1982.5")),
                new DispatchContext(SUBCONTRACTOR, SHIPMENT, AT, USER));

        // then
        assertThat(result.kind()).isEqualTo(MovementKind.DISPATCH);
        assertThat(result.netKg().toPlainString()).isEqualTo("1982.500");
        assertThat(result.sourceRef()).isEqualTo("DISPATCH:" + SHIPMENT + ":2");
    }

    @Test
    @DisplayName("Kumaş, dönen iplik ve fasoncudan gelen iplik bakiyeden düşer: kilo eksi yazılır")
    void shouldWriteConsumptionAsNegative() {
        for (MovementKind kind : new MovementKind[] {
            MovementKind.FABRIC_RECEIPT, MovementKind.YARN_RETURN, MovementKind.YARN_FROM_SUBCONTRACTOR
        }) {
            // when
            BalanceMovement result = BalanceMovement.consumption(new Consumption(
                    kind, SUBCONTRACTOR, SHIPMENT, "30/1 penye", null, new BigDecimal("100"), AT, USER, "ref-" + kind));

            // then
            assertThat(result.netKg().toPlainString()).isEqualTo("-100.000");
        }
    }

    @Test
    @DisplayName("Türe aykırı işaret ve sıfır kilo reddedilir")
    void shouldRejectWrongSignAndZero() {
        assertThatThrownBy(() -> movement(MovementKind.DISPATCH, "-1", null))
                .isInstanceOf(InvalidLedgerInputException.class);
        assertThatThrownBy(() -> movement(MovementKind.FABRIC_RECEIPT, "5", null))
                .isInstanceOf(InvalidLedgerInputException.class);
        assertThatThrownBy(() -> movement(MovementKind.OPENING, "0", null))
                .isInstanceOf(InvalidLedgerInputException.class);
    }

    @Test
    @DisplayName("İtiraz düzeltmesi ve açılış her iki işaretle yazılabilir; haklı itiraz türü tanımlıdır")
    void shouldAllowEitherSignForAdjustments() {
        assertThat(movement(MovementKind.CLAIM_ADJUSTMENT, "-40", null).netKg()).isEqualByComparingTo("-40");
        assertThat(movement(MovementKind.CLAIM_ADJUSTMENT, "40", null).netKg()).isEqualByComparingTo("40");
        assertThat(movement(MovementKind.OPENING, "12.3456", null).netKg().toPlainString())
                .isEqualTo("12.346");
    }

    @Test
    @DisplayName("Gerekçesiz düzeltme reddedilir")
    void shouldRequireReasonForCorrection() {
        assertThatThrownBy(() -> movement(MovementKind.CORRECTION, "-5", " "))
                .isInstanceOf(InvalidLedgerInputException.class);
        assertThat(movement(MovementKind.CORRECTION, "-5", "Yanlış sevkiyata girilmişti")
                        .reason())
                .isNotBlank();
    }

    @Test
    @DisplayName("Kilo metni en çok 3 ondalıklı ve sıfırdan büyük olmalıdır")
    void shouldParseOnlyPositiveThreeDecimalKilograms() {
        assertThat(Kilograms.parsePositive("1982.5", "netKg").toPlainString()).isEqualTo("1982.500");
        for (String bad : new String[] {"0", "-1", "1.2345", "abc", "", "1e3x"}) {
            assertThatThrownBy(() -> Kilograms.parsePositive(bad, "netKg"))
                    .isInstanceOf(InvalidLedgerInputException.class);
        }
    }
}
