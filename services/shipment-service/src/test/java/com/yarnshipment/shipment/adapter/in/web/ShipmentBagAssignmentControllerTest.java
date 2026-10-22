package com.yarnshipment.shipment.adapter.in.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.yarnshipment.shipment.TestShipments;
import com.yarnshipment.shipment.TestTokens;
import com.yarnshipment.shipment.application.port.in.AssignBagsCommand;
import com.yarnshipment.shipment.application.port.in.AssignBagsUseCase;
import com.yarnshipment.shipment.domain.bag.BagReservationException;
import com.yarnshipment.shipment.domain.bag.BagReservationException.Reason;
import com.yarnshipment.shipment.domain.bag.BagServiceUnavailableException;
import com.yarnshipment.shipment.domain.model.ScanMethod;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.test.web.servlet.assertj.MvcTestResult;

/** Çuval atama ucunun sözleşmesi, hata kodları ve izin denetimi. */
@WebMvcTest(controllers = ShipmentBagAssignmentController.class)
class ShipmentBagAssignmentControllerTest {

    private static final String URI = "/api/v1/shipments/" + TestShipments.SHIPMENT_ID + "/bags";
    private static final String BODY = "{\"uniqueKeys\":[\"BAG-0001\",\"BAG-0002\"],\"scanMethod\":\"SCAN\"}";

    private final MockMvcTester mvc;

    @MockitoBean
    private AssignBagsUseCase assignBagsUseCase;

    @MockitoBean
    private JwtDecoder jwtDecoder;

    @Autowired
    ShipmentBagAssignmentControllerTest(MockMvcTester mvc) {
        this.mvc = mvc;
    }

    @Test
    @DisplayName("Atanan çuval sayısı döner ve komut sevkiyat, anahtarlar ve yöntemi taşır")
    void shouldAssignBags() {
        when(assignBagsUseCase.assignBags(any())).thenReturn(2);

        MvcTestResult result = post(BODY, TestTokens.token("warehouse").build());

        assertThat(result).hasStatusOk();
        assertThat(result).bodyJson().extractingPath("$.assignedCount").isEqualTo(2);
        ArgumentCaptor<AssignBagsCommand> command = ArgumentCaptor.forClass(AssignBagsCommand.class);
        verify(assignBagsUseCase).assignBags(command.capture());
        assertThat(command.getValue().shipmentId()).isEqualTo(TestShipments.SHIPMENT_ID);
        assertThat(command.getValue().uniqueKeys()).containsExactlyInAnyOrder("BAG-0001", "BAG-0002");
        assertThat(command.getValue().scanMethod()).isEqualTo(ScanMethod.SCAN);
    }

    @Test
    @DisplayName("Çuval servisinin reddi BAG_* koduyla 404 ve 409 döner")
    void shouldMapReservationRejections() {
        Jwt warehouse = TestTokens.token("warehouse").build();

        doThrow(new BagReservationException(Reason.NOT_FOUND))
                .when(assignBagsUseCase)
                .assignBags(any());
        MvcTestResult missing = post(BODY, warehouse);
        doThrow(new BagReservationException(Reason.NOT_AVAILABLE))
                .when(assignBagsUseCase)
                .assignBags(any());
        MvcTestResult unavailable = post(BODY, warehouse);
        doThrow(new BagReservationException(Reason.ALREADY_RESERVED))
                .when(assignBagsUseCase)
                .assignBags(any());
        MvcTestResult reserved = post(BODY, warehouse);

        assertThat(missing).hasStatus(HttpStatus.NOT_FOUND);
        assertThat(missing).bodyJson().extractingPath("$.code").isEqualTo("BAG_NOT_FOUND");
        assertThat(unavailable).hasStatus(HttpStatus.CONFLICT);
        assertThat(unavailable).bodyJson().extractingPath("$.code").isEqualTo("BAG_NOT_AVAILABLE");
        assertThat(reserved).hasStatus(HttpStatus.CONFLICT);
        assertThat(reserved).bodyJson().extractingPath("$.code").isEqualTo("BAG_ALREADY_RESERVED");
        assertThat(reserved).bodyJson().extractingPath("$.title").isEqualTo("Çuval başka sevkiyatta");
    }

    @Test
    @DisplayName("Çuval servisine ulaşılamazsa BAG_SERVICE_UNAVAILABLE koduyla 502 döner")
    void shouldReturnBadGatewayWhenBagServiceIsDown() {
        when(assignBagsUseCase.assignBags(any())).thenThrow(new BagServiceUnavailableException("down"));

        MvcTestResult result = post(BODY, TestTokens.token("warehouse").build());

        assertThat(result).hasStatus(HttpStatus.BAD_GATEWAY);
        assertThat(result).bodyJson().extractingPath("$.code").isEqualTo("BAG_SERVICE_UNAVAILABLE");
    }

    @Test
    @DisplayName("Boş anahtar listesi 400 alır")
    void shouldRejectEmptyKeys() {
        MvcTestResult result = post(
                "{\"uniqueKeys\":[],\"scanMethod\":\"SCAN\"}",
                TestTokens.token("warehouse").build());

        assertThat(result).hasStatus(HttpStatus.BAD_REQUEST);
    }

    @Test
    @DisplayName("DISPATCH_RECORD izni olmayan rol (şoför) 403 alır")
    void shouldForbidDriver() {
        assertThat(post(BODY, TestTokens.driverToken())).hasStatus(HttpStatus.FORBIDDEN);
    }

    private MvcTestResult post(String body, Jwt jwt) {
        return mvc.post()
                .uri(URI)
                .contentType(MediaType.APPLICATION_JSON)
                .content(body)
                .with(SecurityMockMvcRequestPostProcessors.authentication(TestTokens.CONVERTER.convert(jwt)))
                .with(SecurityMockMvcRequestPostProcessors.csrf())
                .exchange();
    }
}
