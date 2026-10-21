package com.yarnshipment.shipment.adapter.in.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.yarnshipment.shipment.TestShipments;
import com.yarnshipment.shipment.TestTokens;
import com.yarnshipment.shipment.application.port.in.GetShipmentUseCase;
import com.yarnshipment.shipment.application.port.in.ListShipmentsUseCase;
import com.yarnshipment.shipment.application.port.in.ShipmentAction;
import com.yarnshipment.shipment.application.port.in.ShipmentAudience;
import com.yarnshipment.shipment.application.port.in.ShipmentDetails;
import com.yarnshipment.shipment.application.port.in.ShipmentListItem;
import com.yarnshipment.shipment.application.port.in.ShipmentNotFoundException;
import com.yarnshipment.shipment.application.port.in.ShipmentPage;
import com.yarnshipment.shipment.domain.model.ShipmentStatus;
import java.util.List;
import java.util.Set;
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

/** Sözleşme ve kör giriş testi (SEC-07): her görünümde yasaklı alanlar JSON'da hiç bulunmaz. */
@WebMvcTest(controllers = {ShipmentController.class, CurrentUserController.class})
class ShipmentControllerTest {

    private static final String DETAIL_URI = "/api/v1/shipments/" + TestShipments.SHIPMENT_ID;
    private static final List<String> DISPATCH_FIELDS =
            List.of("dispatchWeighing", "plannedNetKg", "plannedBagCount", "randomCheckSelected", "dispatchBagCount");

    private static final int PAGE_SIZE = 20;

    private final MockMvcTester mvc;

    @MockitoBean
    private ListShipmentsUseCase listShipmentsUseCase;

    @MockitoBean
    private GetShipmentUseCase getShipmentUseCase;

    @MockitoBean
    private JwtDecoder jwtDecoder;

    @Autowired
    ShipmentControllerTest(MockMvcTester mvc) {
        this.mvc = mvc;
    }

    @Test
    @DisplayName("Fasoncu görünümünde çıkış tartısı, beklenen yük ve rastgele kontrol alanları hiç yoktur")
    void shouldHideDispatchFieldsFromSubcontractor() {
        // given
        givenDetails(ShipmentAudience.SUBCONTRACTOR);

        // when
        MvcTestResult result = get(DETAIL_URI, TestTokens.subcontractorToken());

        // then
        assertThat(result).hasStatusOk();
        assertThat(result).bodyJson().extractingPath("$.audience").isEqualTo("SUBCONTRACTOR");
        assertThat(result).bodyJson().extractingPath("$.deliveryWeighing.netKg").isEqualTo("1977.250");
        DISPATCH_FIELDS.forEach(field -> assertThat(result).bodyJson().doesNotHavePath("$." + field));
        assertThat(result).bodyJson().doesNotHavePath("$.dispatchSealNo");
    }

    @Test
    @DisplayName("Şoför görünümünde kilo yoktur, yalnızca çuval sayısı ve mühür numarası vardır")
    void shouldShowOnlyBagCountAndSealToDriver() {
        // given
        givenDetails(ShipmentAudience.DRIVER);

        // when
        MvcTestResult result = get(DETAIL_URI, TestTokens.driverToken());

        // then
        assertThat(result).bodyJson().extractingPath("$.dispatchBagCount").isEqualTo(TestShipments.BAG_COUNT);
        assertThat(result).bodyJson().extractingPath("$.dispatchSealNo").isEqualTo("M-0001");
        List.of("dispatchWeighing", "deliveryWeighing", "plannedNetKg", "plannedBagCount", "randomCheckSelected")
                .forEach(field -> assertThat(result).bodyJson().doesNotHavePath("$." + field));
    }

    @Test
    @DisplayName("Depo görünümünde teslim tarafının alanları hiç yoktur")
    void shouldHideDeliveryFieldsFromWarehouse() {
        // given
        givenDetails(ShipmentAudience.WAREHOUSE);

        // when
        MvcTestResult result = get(DETAIL_URI, TestTokens.token("warehouse").build());

        // then
        assertThat(result).bodyJson().extractingPath("$.dispatchWeighing.netKg").isEqualTo("1982.500");
        assertThat(result).bodyJson().doesNotHavePath("$.deliveryWeighing");
        assertThat(result).bodyJson().doesNotHavePath("$.deliverySealCondition");
    }

    @Test
    @DisplayName("İç görünüm iki tarafı da içerir, kilolar üç ondalıklı dizedir")
    void shouldShowBothSidesToInternalAudience() {
        // given
        givenDetails(ShipmentAudience.INTERNAL);

        // when
        MvcTestResult result = get(DETAIL_URI, TestTokens.token("manager").build());

        // then
        assertThat(result).bodyJson().extractingPath("$.plannedNetKg").isEqualTo("1982.500");
        assertThat(result)
                .bodyJson()
                .extractingPath("$.dispatchWeighing.grossKg")
                .isEqualTo("2000.500");
        assertThat(result).bodyJson().extractingPath("$.deliverySealCondition").isEqualTo("INTACT");
        assertThat(result).bodyJson().extractingPath("$.createdAt").isEqualTo("2026-10-21T06:00:00Z");
    }

    @Test
    @DisplayName("Liste sayfa bilgisi ve izin verilen işlemlerle döner")
    void shouldListShipmentsWithPageMetadata() {
        // given
        ShipmentListItem item = new ShipmentListItem(
                TestShipments.shipment(ShipmentStatus.DRAFT), Set.of(ShipmentAction.RECORD_DISPATCH));
        when(listShipmentsUseCase.listShipments(any())).thenReturn(new ShipmentPage(List.of(item), 0, PAGE_SIZE, 1));

        // when
        MvcTestResult result = get(
                "/api/v1/shipments?status=DRAFT", TestTokens.token("warehouse").build());

        // then
        assertThat(result).hasStatusOk();
        assertThat(result).bodyJson().extractingPath("$.page.totalPages").isEqualTo(1);
        assertThat(result)
                .bodyJson()
                .extractingPath("$.items[0].allowedActions[0]")
                .isEqualTo("RECORD_DISPATCH");
        assertThat(result).bodyJson().doesNotHavePath("$.items[0].plannedNetKg");
    }

    @Test
    @DisplayName("Sayfa boyu 100'ü geçerse 400 döner")
    void shouldRejectTooLargePageSize() {
        // when
        MvcTestResult result =
                get("/api/v1/shipments?size=101", TestTokens.token("warehouse").build());

        // then
        assertThat(result).hasStatus(HttpStatus.BAD_REQUEST);
        assertThat(result).bodyJson().extractingPath("$.code").isEqualTo("VALIDATION_FAILED");
        assertThat(result).bodyJson().extractingPath("$.errors[0].field").isEqualTo("size");
    }

    @Test
    @DisplayName("Olmayan sevkiyat SHIPMENT_NOT_FOUND koduyla 404 döner")
    void shouldReturnProblemForMissingShipment() {
        // given
        when(getShipmentUseCase.getShipment(any())).thenThrow(new ShipmentNotFoundException(UUID.randomUUID()));

        // when
        MvcTestResult result = get(DETAIL_URI, TestTokens.token("warehouse").build());

        // then
        assertThat(result).hasStatus(HttpStatus.NOT_FOUND);
        assertThat(result).bodyJson().extractingPath("$.code").isEqualTo("SHIPMENT_NOT_FOUND");
        assertThat(result).bodyJson().extractingPath("$.title").isEqualTo("Sevkiyat bulunamadı");
    }

    @Test
    @DisplayName("SHIPMENT_VIEW izni olmayan kullanıcı 403 alır")
    void shouldForbidUserWithoutShipmentView() {
        // given: tanınmayan rol izin vermez
        Jwt withoutRole = TestTokens.token("offline_access").build();

        // when
        MvcTestResult result = get(DETAIL_URI, withoutRole);

        // then
        assertThat(result).hasStatus(HttpStatus.FORBIDDEN);
        assertThat(result).bodyJson().extractingPath("$.code").isEqualTo("ACCESS_DENIED");
    }

    @Test
    @DisplayName("Oturumdaki kullanıcı rol, izin ve kapsamıyla döner")
    void shouldReturnCurrentUser() {
        // when
        MvcTestResult result = get("/api/v1/me", TestTokens.subcontractorToken());

        // then
        assertThat(result).hasStatusOk();
        assertThat(result).bodyJson().extractingPath("$.fullName").isEqualTo("Test Kullanıcı");
        assertThat(result).bodyJson().extractingPath("$.roles[0]").isEqualTo("SUBCONTRACTOR");
        assertThat(result)
                .bodyJson()
                .extractingPath("$.subcontractorId")
                .isEqualTo(TestShipments.SUBCONTRACTOR_ID.toString());
        assertThat(result).bodyJson().extractingPath("$.permissions").asArray().contains("DELIVERY_RECORD");
    }

    @Test
    @DisplayName("Token'sız istek 401 alır")
    void shouldRequireAuthentication() {
        // when / then
        assertThat(mvc.get().uri("/api/v1/me").exchange()).hasStatus(HttpStatus.UNAUTHORIZED);
    }

    private void givenDetails(ShipmentAudience audience) {
        ShipmentDetails details =
                new ShipmentDetails(TestShipments.shipment(ShipmentStatus.AWAITING_DELIVERY), audience, Set.of());
        when(getShipmentUseCase.getShipment(any())).thenReturn(details);
    }

    private MvcTestResult get(String uri, Jwt jwt) {
        return mvc.get()
                .uri(uri)
                .with(SecurityMockMvcRequestPostProcessors.authentication(TestTokens.CONVERTER.convert(jwt)))
                .exchange();
    }
}
