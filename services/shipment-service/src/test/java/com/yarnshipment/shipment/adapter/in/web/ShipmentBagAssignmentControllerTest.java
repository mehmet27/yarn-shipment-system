package com.yarnshipment.shipment.adapter.in.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.yarnshipment.shipment.TestShipments;
import com.yarnshipment.shipment.TestTokens;
import com.yarnshipment.shipment.application.port.in.AssignBagsCommand;
import com.yarnshipment.shipment.application.port.in.AssignBagsUseCase;
import com.yarnshipment.shipment.application.port.in.RemoveBagUseCase;
import com.yarnshipment.shipment.application.port.in.RemoveBagUseCase.RemoveBagCommand;
import java.math.BigDecimal;
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
    private static final String BODY = "{\"weightsKg\":[\"25.000\",\"24.500\"]}";

    private final MockMvcTester mvc;

    @MockitoBean
    private AssignBagsUseCase assignBagsUseCase;

    @MockitoBean
    private RemoveBagUseCase removeBagUseCase;

    @MockitoBean
    private JwtDecoder jwtDecoder;

    @Autowired
    ShipmentBagAssignmentControllerTest(MockMvcTester mvc) {
        this.mvc = mvc;
    }

    @Test
    @DisplayName("Eklenen çuval sayısı döner ve komut sevkiyat ile kiloları taşır")
    void shouldAssignBags() {
        when(assignBagsUseCase.assignBags(any())).thenReturn(2);

        MvcTestResult result = post(BODY, TestTokens.token("warehouse").build());

        assertThat(result).hasStatusOk();
        assertThat(result).bodyJson().extractingPath("$.assignedCount").isEqualTo(2);
        ArgumentCaptor<AssignBagsCommand> command = ArgumentCaptor.forClass(AssignBagsCommand.class);
        verify(assignBagsUseCase).assignBags(command.capture());
        assertThat(command.getValue().shipmentId()).isEqualTo(TestShipments.SHIPMENT_ID);
        assertThat(command.getValue().weightsKg()).containsExactly(new BigDecimal("25.000"), new BigDecimal("24.500"));
    }

    @Test
    @DisplayName("Boş kilo listesi 400 alır")
    void shouldRejectEmptyWeights() {
        MvcTestResult result =
                post("{\"weightsKg\":[]}", TestTokens.token("warehouse").build());

        assertThat(result).hasStatus(HttpStatus.BAD_REQUEST);
    }

    @Test
    @DisplayName("DISPATCH_RECORD izni olmayan rol (şoför) 403 alır")
    void shouldForbidDriver() {
        assertThat(post(BODY, TestTokens.token("offline_access").build())).hasStatus(HttpStatus.FORBIDDEN);
    }

    @Test
    @DisplayName("Çuval çıkarma 204 döner ve komut sevkiyat ile sıra numarasını taşır; şoför 403 alır")
    void shouldRemoveBag() {
        MvcTestResult removed = delete(TestTokens.token("warehouse").build());
        MvcTestResult denied = delete(TestTokens.token("offline_access").build());

        assertThat(removed).hasStatus(HttpStatus.NO_CONTENT);
        assertThat(denied).hasStatus(HttpStatus.FORBIDDEN);
        ArgumentCaptor<RemoveBagCommand> command = ArgumentCaptor.forClass(RemoveBagCommand.class);
        verify(removeBagUseCase).removeBag(command.capture());
        assertThat(command.getValue().shipmentId()).isEqualTo(TestShipments.SHIPMENT_ID);
        assertThat(command.getValue().seqNo()).isEqualTo(1);
    }

    private MvcTestResult delete(Jwt jwt) {
        return mvc.delete()
                .uri(URI + "/1")
                .with(SecurityMockMvcRequestPostProcessors.authentication(TestTokens.CONVERTER.convert(jwt)))
                .with(SecurityMockMvcRequestPostProcessors.csrf())
                .exchange();
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
