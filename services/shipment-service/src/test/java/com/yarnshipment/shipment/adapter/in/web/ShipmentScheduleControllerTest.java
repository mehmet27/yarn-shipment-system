package com.yarnshipment.shipment.adapter.in.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import com.yarnshipment.shipment.TestTokens;
import com.yarnshipment.shipment.application.port.in.UpdateShipmentDateCommand;
import com.yarnshipment.shipment.application.port.in.UpdateShipmentDateUseCase;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.test.web.servlet.assertj.MvcTestResult;

/** Sevkiyat günü güncelleme ucunun sözleşmesi: izin ve istek doğrulama. */
@WebMvcTest(controllers = ShipmentScheduleController.class)
class ShipmentScheduleControllerTest {

    private static final UUID SHIPMENT = UUID.fromString("0192f0a0-0000-7000-8000-000000000201");
    private static final String URI = "/api/v1/shipments/" + SHIPMENT + "/shipment-date";
    private static final String BODY = "{\"shipmentDate\": \"2026-12-01\"}";

    private final MockMvcTester mvc;

    @MockitoBean
    private UpdateShipmentDateUseCase updateShipmentDateUseCase;

    @Autowired
    ShipmentScheduleControllerTest(MockMvcTester mvc) {
        this.mvc = mvc;
    }

    @Test
    @DisplayName("Müdür günü günceller; 204 döner ve gün use case'e iletilir")
    void shouldUpdate() {
        MvcTestResult result = put(TestTokens.token("manager").build(), BODY);

        assertThat(result).hasStatus(HttpStatus.NO_CONTENT);
        ArgumentCaptor<UpdateShipmentDateCommand> command = ArgumentCaptor.forClass(UpdateShipmentDateCommand.class);
        verify(updateShipmentDateUseCase).updateShipmentDate(command.capture());
        assertThat(command.getValue().shipmentDate().toString()).isEqualTo("2026-12-01");
        assertThat(command.getValue().shipmentId()).isEqualTo(SHIPMENT);
    }

    @Test
    @DisplayName("Sevkiyat açma izni olmayan rol 403 alır; gün olmayan istek 400 alır")
    void shouldForbidWarehouseAndRejectMissingDate() {
        assertThat(put(TestTokens.token("warehouse").build(), BODY)).hasStatus(HttpStatus.FORBIDDEN);
        assertThat(put(TestTokens.token("manager").build(), "{}")).hasStatus(HttpStatus.BAD_REQUEST);
        verifyNoInteractions(updateShipmentDateUseCase);
    }

    private MvcTestResult put(Jwt jwt, String body) {
        return mvc.put()
                .uri(URI)
                .contentType(MediaType.APPLICATION_JSON)
                .content(body)
                .with(SecurityMockMvcRequestPostProcessors.authentication(TestTokens.CONVERTER.convert(jwt)))
                .exchange();
    }
}
