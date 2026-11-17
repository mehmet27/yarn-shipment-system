package com.yarnshipment.shipment.application.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.yarnshipment.common.security.Actor;
import com.yarnshipment.shipment.TestShipments;
import com.yarnshipment.shipment.TestTokens;
import com.yarnshipment.shipment.application.port.in.ShipmentAction;
import com.yarnshipment.shipment.application.port.in.ShipmentAudience;
import com.yarnshipment.shipment.domain.model.ShipmentStatus;
import java.time.LocalDate;
import java.time.ZoneOffset;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class ShipmentAccessPolicyTest {

    private final ShipmentAccessPolicy policy = new ShipmentAccessPolicy(TestShipments.CLOCK);

    @Test
    @DisplayName("Görünüm role göre seçilir: depo depo görünümünü, iç roller iç görünümü alır")
    void shouldChooseAudienceByRole() {
        // when / then
        assertThat(policy.audienceOf(TestTokens.actor("warehouse"))).isEqualTo(ShipmentAudience.WAREHOUSE);
        assertThat(policy.audienceOf(TestTokens.actor("manager"))).isEqualTo(ShipmentAudience.INTERNAL);
        assertThat(policy.audienceOf(TestTokens.actor("warehouse", "quality"))).isEqualTo(ShipmentAudience.INTERNAL);
    }

    @Test
    @DisplayName("Taslak sevkiyatta depo çuval atar ve çıkış tartısı girer, iptal edemez")
    void shouldAllowWarehouseActionsOnDraft() {
        // when / then
        assertThat(policy.allowedActions(TestShipments.shipment(ShipmentStatus.DRAFT), TestTokens.actor("warehouse")))
                .containsExactlyInAnyOrder(ShipmentAction.ASSIGN_BAGS, ShipmentAction.RECORD_DISPATCH);
    }

    @Test
    @DisplayName("Müdür taslağı iptal eder, tartı uyuşmazlığına karar verir, teslim bekleyeni tamamlar, düzeltme açar")
    void shouldAllowManagerLifecycleActions() {
        // given
        Actor manager = TestTokens.actor("manager");

        // when / then
        assertThat(policy.allowedActions(TestShipments.shipment(ShipmentStatus.DRAFT), manager))
                .containsExactlyInAnyOrder(
                        ShipmentAction.CANCEL,
                        ShipmentAction.CREATE_CORRECTION,
                        ShipmentAction.UPDATE_SHIPMENT_DATE,
                        ShipmentAction.UPDATE_TRANSPORT);
        assertThat(policy.allowedActions(TestShipments.shipment(ShipmentStatus.WEIGHT_MISMATCH), manager))
                .containsExactlyInAnyOrder(
                        ShipmentAction.CANCEL,
                        ShipmentAction.RESOLVE_WEIGHT_MISMATCH,
                        ShipmentAction.CREATE_CORRECTION);
        assertThat(policy.allowedActions(TestShipments.shipment(ShipmentStatus.AWAITING_DELIVERY), manager))
                .containsExactlyInAnyOrder(
                        ShipmentAction.COMPLETE, ShipmentAction.CANCEL, ShipmentAction.CREATE_CORRECTION);
    }

    @Test
    @DisplayName("Depo, sevkiyat günü gelmeden çuval atayamaz ve çıkış tartımı giremez; gün gelince yapar")
    void shouldHideWarehouseActionsBeforeShipmentDay() {
        Actor warehouse = TestTokens.actor("warehouse");
        LocalDate today = LocalDate.ofInstant(TestShipments.CLOCK.instant(), ZoneOffset.UTC);

        assertThat(policy.allowedActions(TestShipments.scheduledFor(today.plusDays(1)), warehouse))
                .isEmpty();
        assertThat(policy.allowedActions(TestShipments.scheduledFor(today), warehouse))
                .containsExactlyInAnyOrder(ShipmentAction.ASSIGN_BAGS, ShipmentAction.RECORD_DISPATCH);
    }

    @Test
    @DisplayName("İptal edilmiş sevkiyatta kimse işlem yapamaz")
    void shouldAllowNoActionsForCancelledShipment() {
        // when / then
        assertThat(policy.allowedActions(
                        TestShipments.shipment(ShipmentStatus.CANCELLED), TestTokens.mfaActor("manager")))
                .isEmpty();
    }

    @Test
    @DisplayName("Müdür tamamlanmış sevkiyatta itiraz açar ve düzeltme kaydı oluşturur")
    void shouldAllowManagerClaimAndCorrectionWhenCompleted() {
        // when / then
        assertThat(policy.allowedActions(
                        TestShipments.shipment(ShipmentStatus.COMPLETED), TestTokens.mfaActor("manager")))
                .containsExactlyInAnyOrder(ShipmentAction.OPEN_QUALITY_CLAIM, ShipmentAction.CREATE_CORRECTION);
    }
}
