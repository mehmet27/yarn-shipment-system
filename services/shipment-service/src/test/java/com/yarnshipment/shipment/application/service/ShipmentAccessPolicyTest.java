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
    @DisplayName("Görünüm role göre seçilir: dış roller kendi görünümünü, depo kör görünümü alır")
    void shouldChooseAudienceByRole() {
        // when / then
        assertThat(policy.audienceOf(TestTokens.actor(TestTokens.subcontractorToken())))
                .isEqualTo(ShipmentAudience.SUBCONTRACTOR);
        assertThat(policy.audienceOf(TestTokens.actor(TestTokens.driverToken())))
                .isEqualTo(ShipmentAudience.DRIVER);
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
    @DisplayName("Teslim bekleyen sevkiyatta fasoncu yalnızca teslim girer")
    void shouldAllowSubcontractorDeliveryWhenAwaiting() {
        // when / then
        assertThat(policy.allowedActions(
                        TestShipments.shipment(ShipmentStatus.AWAITING_DELIVERY),
                        TestTokens.actor(TestTokens.subcontractorToken())))
                .containsExactly(ShipmentAction.RECORD_DELIVERY);
    }

    @Test
    @DisplayName("Şoför yüklenmiş sevkiyatı teslim alır, yoldakinde varış bildirir")
    void shouldAllowDriverDeclarationsByStatus() {
        // given
        Actor driver = TestTokens.actor(TestTokens.driverToken());

        // when / then
        assertThat(policy.allowedActions(TestShipments.shipment(ShipmentStatus.LOADED), driver))
                .containsExactly(ShipmentAction.CONFIRM_PICKUP);
        assertThat(policy.allowedActions(TestShipments.shipment(ShipmentStatus.IN_TRANSIT), driver))
                .containsExactly(ShipmentAction.DECLARE_ARRIVAL);
    }

    @Test
    @DisplayName("Yönetici hiçbir işlem yapamaz; iptal edilmiş sevkiyatta kimse işlem yapamaz")
    void shouldAllowNoActionsForManagerOrCancelledShipment() {
        // when / then
        assertThat(policy.allowedActions(TestShipments.shipment(ShipmentStatus.DRAFT), TestTokens.actor("manager")))
                .isEmpty();
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
