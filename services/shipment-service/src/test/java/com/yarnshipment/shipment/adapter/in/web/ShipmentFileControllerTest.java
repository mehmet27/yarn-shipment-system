package com.yarnshipment.shipment.adapter.in.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.yarnshipment.shipment.TestShipments;
import com.yarnshipment.shipment.TestTokens;
import com.yarnshipment.shipment.application.port.in.GetShipmentBagsUseCase;
import com.yarnshipment.shipment.application.port.in.GetShipmentHistoryUseCase;
import com.yarnshipment.shipment.application.port.in.ShipmentAudience;
import com.yarnshipment.shipment.application.port.in.ShipmentBags;
import com.yarnshipment.shipment.application.port.in.ShipmentNotFoundException;
import com.yarnshipment.shipment.domain.model.DeliveryScan;
import com.yarnshipment.shipment.domain.model.DispatchBag;
import com.yarnshipment.shipment.domain.model.ScanMethod;
import com.yarnshipment.shipment.domain.model.ShipmentStatus;
import com.yarnshipment.shipment.domain.model.StatusChange;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.HttpStatus;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.test.web.servlet.assertj.MvcTestResult;

/** Geçmiş ve çuval uçlarının sözleşmesi; kör girişte yasaklı alanlar JSON'da hiç bulunmaz (SEC-05). */
@WebMvcTest(controllers = ShipmentFileController.class)
class ShipmentFileControllerTest {

    private static final String BASE = "/api/v1/shipments/" + TestShipments.SHIPMENT_ID;
    private static final Instant AT = Instant.parse("2026-10-21T09:00:00Z");

    private final MockMvcTester mvc;

    @MockitoBean
    private GetShipmentHistoryUseCase getShipmentHistoryUseCase;

    @MockitoBean
    private GetShipmentBagsUseCase getShipmentBagsUseCase;

    @MockitoBean
    private JwtDecoder jwtDecoder;

    @Autowired
    ShipmentFileControllerTest(MockMvcTester mvc) {
        this.mvc = mvc;
    }

    private MvcTestResult get(String uri, Jwt jwt) {
        return mvc.get()
                .uri(uri)
                .with(SecurityMockMvcRequestPostProcessors.authentication(TestTokens.CONVERTER.convert(jwt)))
                .exchange();
    }

    private void givenBags(ShipmentAudience audience, boolean dispatch, boolean delivery) {
        List<DispatchBag> bags =
                dispatch ? List.of(new DispatchBag(UUID.randomUUID(), "K-1", "LOT-1", "A", ScanMethod.SCAN, AT)) : null;
        List<DeliveryScan> scans = delivery ? List.of(new DeliveryScan("K-1", true, ScanMethod.LIST, AT)) : null;
        when(getShipmentBagsUseCase.getBags(any())).thenReturn(new ShipmentBags(audience, bags, scans));
    }

    @Test
    @DisplayName("Geçmiş satırları sırayla döner; ilk satırda önceki durum yoktur")
    void shouldReturnHistory() {
        UUID user = UUID.randomUUID();
        when(getShipmentHistoryUseCase.getHistory(any()))
                .thenReturn(List.of(
                        new StatusChange(null, ShipmentStatus.DRAFT, AT, user, null),
                        new StatusChange(ShipmentStatus.DRAFT, ShipmentStatus.LOADED, AT.plusSeconds(1), user, null)));

        MvcTestResult result =
                get(BASE + "/history", TestTokens.token("warehouse").build());

        assertThat(result).hasStatusOk();
        assertThat(result).bodyJson().doesNotHavePath("$.items[0].fromStatus");
        assertThat(result).bodyJson().extractingPath("$.items[1].fromStatus").isEqualTo("DRAFT");
        assertThat(result).bodyJson().extractingPath("$.items[1].changedBy").isEqualTo(user.toString());
    }

    @Test
    @DisplayName("Olmayan sevkiyatın geçmişi 404 döner")
    void shouldMapMissingShipment() {
        when(getShipmentHistoryUseCase.getHistory(any())).thenThrow(new ShipmentNotFoundException(UUID.randomUUID()));

        MvcTestResult result =
                get(BASE + "/history", TestTokens.token("warehouse").build());

        assertThat(result).hasStatus(HttpStatus.NOT_FOUND);
        assertThat(result).bodyJson().extractingPath("$.code").isEqualTo("SHIPMENT_NOT_FOUND");
    }

    @Test
    @DisplayName("Depo görünümünde teslim okutmaları JSON'da hiç yoktur")
    void shouldHideDeliveryScansFromWarehouse() {
        givenBags(ShipmentAudience.WAREHOUSE, true, false);

        MvcTestResult result = get(BASE + "/bags", TestTokens.token("warehouse").build());

        assertThat(result)
                .bodyJson()
                .extractingPath("$.dispatchBags[0].uniqueKey")
                .isEqualTo("K-1");
        assertThat(result).bodyJson().doesNotHavePath("$.deliveryScans");
    }

    @Test
    @DisplayName("Fasoncu görünümünde çıkış çuvalları ve matched alanı JSON'da hiç yoktur")
    void shouldHideDispatchBagsAndMatchFromSubcontractor() {
        givenBags(ShipmentAudience.SUBCONTRACTOR, false, true);

        MvcTestResult result = get(BASE + "/bags", TestTokens.subcontractorToken());

        assertThat(result).bodyJson().doesNotHavePath("$.dispatchBags");
        assertThat(result)
                .bodyJson()
                .extractingPath("$.deliveryScans[0].scannedKey")
                .isEqualTo("K-1");
        assertThat(result).bodyJson().doesNotHavePath("$.deliveryScans[0].matched");
    }

    @Test
    @DisplayName("İç görünüm ikisini ve matched alanını içerir; şoför görünümünde ikisi de yoktur")
    void shouldShowEverythingInternallyAndNothingToDriver() {
        givenBags(ShipmentAudience.INTERNAL, true, true);
        MvcTestResult internal = get(BASE + "/bags", TestTokens.token("manager").build());
        givenBags(ShipmentAudience.DRIVER, false, false);
        MvcTestResult driver = get(BASE + "/bags", TestTokens.driverToken());

        assertThat(internal)
                .bodyJson()
                .extractingPath("$.deliveryScans[0].matched")
                .isEqualTo(true);
        assertThat(internal).bodyJson().hasPath("$.dispatchBags");
        assertThat(driver).bodyJson().doesNotHavePath("$.dispatchBags");
        assertThat(driver).bodyJson().doesNotHavePath("$.deliveryScans");
    }

    @Test
    @DisplayName("SHIPMENT_VIEW izni olmayan kullanıcı 403 alır")
    void shouldForbidWithoutPermission() {
        MvcTestResult result =
                get(BASE + "/bags", TestTokens.token("offline_access").build());

        assertThat(result).hasStatus(HttpStatus.FORBIDDEN);
    }
}
