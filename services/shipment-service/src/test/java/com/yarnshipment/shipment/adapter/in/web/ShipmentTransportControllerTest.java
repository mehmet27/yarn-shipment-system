package com.yarnshipment.shipment.adapter.in.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import com.yarnshipment.shipment.TestTokens;
import com.yarnshipment.shipment.application.port.in.transport.UpdateShipmentTransportCommand;
import com.yarnshipment.shipment.application.port.in.transport.UpdateShipmentTransportUseCase;
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

/** Şoför ve araç düzeltme ucunun sözleşmesi: izin ve istek doğrulama. */
@WebMvcTest(controllers = ShipmentTransportController.class)
class ShipmentTransportControllerTest {

    private static final UUID SHIPMENT = UUID.fromString("0192f0a0-0000-7000-8000-000000000201");
    private static final String DRIVER = "0192f0a0-0000-7000-8000-000000000d01";
    private static final String URI = "/api/v1/shipments/" + SHIPMENT + "/transport";
    private static final String BODY = "{\"driverId\": \"" + DRIVER + "\", \"driverName\": \"Ali Veli\"}";

    private final MockMvcTester mvc;

    @MockitoBean
    private UpdateShipmentTransportUseCase updateTransportUseCase;

    @Autowired
    ShipmentTransportControllerTest(MockMvcTester mvc) {
        this.mvc = mvc;
    }

    @Test
    @DisplayName("Müdür şoförü günceller; 204 döner ve bilgi use case'e iletilir")
    void shouldUpdate() {
        MvcTestResult result = put(TestTokens.token("manager").build(), BODY);

        assertThat(result).hasStatus(HttpStatus.NO_CONTENT);
        ArgumentCaptor<UpdateShipmentTransportCommand> command =
                ArgumentCaptor.forClass(UpdateShipmentTransportCommand.class);
        verify(updateTransportUseCase).updateShipmentTransport(command.capture());
        assertThat(command.getValue().transport().driverName()).isEqualTo("Ali Veli");
        assertThat(command.getValue().transport().plateNo()).isNull();
        assertThat(command.getValue().shipmentId()).isEqualTo(SHIPMENT);
    }

    @Test
    @DisplayName("Şoför ve araç ikisi de verilmezse ya da kimlik adsız verilirse 400 döner")
    void shouldRejectEmptyOrUnpairedBody() {
        assertThat(put(TestTokens.token("manager").build(), "{}")).hasStatus(HttpStatus.BAD_REQUEST);
        assertThat(put(TestTokens.token("manager").build(), "{\"driverId\": \"" + DRIVER + "\"}"))
                .hasStatus(HttpStatus.BAD_REQUEST);
        verifyNoInteractions(updateTransportUseCase);
    }

    @Test
    @DisplayName("Sevkiyat açma izni olmayan rol 403 alır")
    void shouldForbidWarehouse() {
        assertThat(put(TestTokens.token("warehouse").build(), BODY)).hasStatus(HttpStatus.FORBIDDEN);
        verifyNoInteractions(updateTransportUseCase);
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
