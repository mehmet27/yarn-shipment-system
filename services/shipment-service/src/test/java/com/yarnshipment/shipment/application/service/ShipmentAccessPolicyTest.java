package com.yarnshipment.shipment.application.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.yarnshipment.common.security.Actor;
import com.yarnshipment.shipment.TestShipments;
import com.yarnshipment.shipment.TestTokens;
import com.yarnshipment.shipment.application.port.in.ShipmentAction;
import com.yarnshipment.shipment.application.port.in.ShipmentAudience;
import com.yarnshipment.shipment.domain.model.ShipmentStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class ShipmentAccessPolicyTest {

    private final ShipmentAccessPolicy policy = new ShipmentAccessPolicy();

    @Test
    @DisplayName("Görünüm role göre seçilir: fasoncu kendi görünümünü, depo depo görünümünü alır")
    void shouldChooseAudienceByRole() {
        // when / then
        assertThat(policy.audienceOf(TestTokens.actor(TestTokens.subcontractorToken())))
                .isEqualTo(ShipmentAudience.SUBCONTRACTOR);
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
    @DisplayName("Teslim bekleyen sevkiyatta fasoncu hiçbir işlem yapamaz")
    void shouldAllowSubcontractorNothingWhenAwaiting() {
        // when / then
        assertThat(policy.allowedActions(
                        TestShipments.shipment(ShipmentStatus.AWAITING_DELIVERY),
                        TestTokens.actor(TestTokens.subcontractorToken())))
                .isEmpty();
    }

    @Test
    @DisplayName("Müdür taslağa çuval ekler ya da iptal eder; teslim bekleyeni tamamlar ya da iptal eder")
    void shouldAllowManagerLifecycleActions() {
        // given
        Actor manager = TestTokens.actor("manager");

        // when / then
        assertThat(policy.allowedActions(TestShipments.shipment(ShipmentStatus.DRAFT), manager))
                .containsExactlyInAnyOrder(ShipmentAction.ASSIGN_BAGS, ShipmentAction.CANCEL);
        assertThat(policy.allowedActions(TestShipments.shipment(ShipmentStatus.AWAITING_DELIVERY), manager))
                .containsExactlyInAnyOrder(ShipmentAction.COMPLETE, ShipmentAction.CANCEL);
    }

    @Test
    @DisplayName("İptal edilmiş sevkiyatta kimse işlem yapamaz")
    void shouldAllowNoActionsForCancelledShipment() {
        // when / then
        assertThat(policy.allowedActions(
                        TestShipments.shipment(ShipmentStatus.CANCELLED), TestTokens.mfaActor("admin")))
                .isEmpty();
    }

    @Test
    @DisplayName("Admin tamamlanmış sevkiyatta itiraz açar ve düzeltme kaydı oluşturur")
    void shouldAllowAdminClaimAndCorrectionWhenCompleted() {
        // when / then
        assertThat(policy.allowedActions(
                        TestShipments.shipment(ShipmentStatus.COMPLETED), TestTokens.mfaActor("admin")))
                .containsExactlyInAnyOrder(ShipmentAction.OPEN_QUALITY_CLAIM, ShipmentAction.CREATE_CORRECTION);
    }
}
