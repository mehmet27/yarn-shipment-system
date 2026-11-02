package com.yarnshipment.shipment.adapter.in.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.yarnshipment.shipment.TestTokens;
import com.yarnshipment.shipment.application.port.in.correction.GetCorrectionsUseCase;
import com.yarnshipment.shipment.application.port.in.correction.RecordCorrectionCommand;
import com.yarnshipment.shipment.application.port.in.correction.RecordCorrectionUseCase;
import com.yarnshipment.shipment.application.port.in.correction.ShipmentCorrection;
import com.yarnshipment.shipment.domain.correction.CorrectedValue;
import com.yarnshipment.shipment.domain.correction.CorrectionConflictException;
import com.yarnshipment.shipment.domain.correction.CorrectionTarget;
import com.yarnshipment.shipment.domain.correction.InvalidCorrectionException;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
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

/** Gerekçeli düzeltme uçlarının sözleşmesi: yalnızca Admin yazar, değerler alan modeline çevrilir, hatalar eşlenir. */
@WebMvcTest(controllers = ShipmentCorrectionController.class)
class ShipmentCorrectionControllerTest {

    private static final UUID SHIPMENT = UUID.fromString("0192f0a0-0000-7000-8000-000000000201");
    private static final UUID KEY = UUID.fromString("0192f0a0-0000-7000-8000-0000000000dd");
    private static final String URI = "/api/v1/shipments/" + SHIPMENT + "/corrections";
    private static final String WEIGHING = """
            {"target": "DELIVERY_WEIGHING", "grossKg": "1990.000", "tareKg": "18.000",
             "reason": "Kantar fişindeki brüt yanlış girilmiş"}""";
    private static final ShipmentCorrection SAVED = new ShipmentCorrection(
            KEY,
            SHIPMENT,
            CorrectionTarget.DELIVERY_WEIGHING,
            "gross=1995.250 tare=18.000 net=1977.250",
            "gross=1990.000 tare=18.000 net=1972.000",
            "Kantar fişindeki brüt yanlış girilmiş",
            "APPROVED",
            KEY,
            KEY,
            Instant.parse("2026-10-24T09:00:00Z"));

    private final MockMvcTester mvc;

    @MockitoBean
    private RecordCorrectionUseCase recordCorrectionUseCase;

    @MockitoBean
    private GetCorrectionsUseCase getCorrectionsUseCase;

    @MockitoBean
    private JwtDecoder jwtDecoder;

    @Autowired
    ShipmentCorrectionControllerTest(MockMvcTester mvc) {
        this.mvc = mvc;
    }

    @Test
    @DisplayName("Admin tartımı düzeltir; 201 ile eski ve yeni değer döner, tekrar anahtarı use case'e iletilir")
    void shouldRecordWeighingCorrection() {
        when(recordCorrectionUseCase.recordCorrection(any())).thenReturn(SAVED);

        MvcTestResult result = post(TestTokens.token("admin").build(), WEIGHING);

        assertThat(result).hasStatus(HttpStatus.CREATED);
        assertThat(result).bodyJson().extractingPath("$.oldValue").isEqualTo(SAVED.oldValue());
        assertThat(result).bodyJson().extractingPath("$.status").isEqualTo("APPROVED");
        ArgumentCaptor<RecordCorrectionCommand> command = ArgumentCaptor.forClass(RecordCorrectionCommand.class);
        verify(recordCorrectionUseCase).recordCorrection(command.capture());
        assertThat(command.getValue().idempotencyKey()).isEqualTo(KEY);
        assertThat(command.getValue().request().value())
                .isEqualTo(new CorrectedValue.Weight(new BigDecimal("1990"), new BigDecimal("18")));
    }

    @Test
    @DisplayName("Düzeltme izni olmayan yönetici 403 alır; düzeltmeleri yine de okuyabilir")
    void shouldForbidManagerWrite() {
        when(getCorrectionsUseCase.getCorrections(any())).thenReturn(List.of(SAVED));

        assertThat(post(TestTokens.token("manager").build(), WEIGHING)).hasStatus(HttpStatus.FORBIDDEN);
        MvcTestResult list = mvc.get()
                .uri(URI)
                .with(SecurityMockMvcRequestPostProcessors.authentication(
                        TestTokens.CONVERTER.convert(TestTokens.token("manager").build())))
                .exchange();

        verifyNoInteractions(recordCorrectionUseCase);
        assertThat(list).hasStatus(HttpStatus.OK);
        assertThat(list).bodyJson().extractingPath("$.items[0].newValue").isEqualTo(SAVED.newValue());
    }

    @Test
    @DisplayName("Tartımda eksik dara, alan hatası ve çakışma kodlarıyla eşlenir")
    void shouldMapErrors() {
        Jwt admin = TestTokens.token("admin").build();
        MvcTestResult missingTare =
                post(admin, "{\"target\": \"DISPATCH_WEIGHING\", \"grossKg\": \"10.000\", \"reason\": \"x\"}");
        doThrow(new InvalidCorrectionException("value"))
                .when(recordCorrectionUseCase)
                .recordCorrection(any());
        MvcTestResult unchanged = post(admin, WEIGHING);
        doThrow(new CorrectionConflictException(SHIPMENT, CorrectionTarget.DELIVERY_WEIGHING))
                .when(recordCorrectionUseCase)
                .recordCorrection(any());
        MvcTestResult conflict = post(admin, WEIGHING);

        assertThat(missingTare).hasStatus(HttpStatus.BAD_REQUEST);
        assertThat(missingTare).bodyJson().extractingPath("$.errors[0].field").isEqualTo("tareKg");
        assertThat(unchanged).bodyJson().extractingPath("$.code").isEqualTo("INVALID_CORRECTION");
        assertThat(conflict).hasStatus(HttpStatus.CONFLICT);
        assertThat(conflict).bodyJson().extractingPath("$.code").isEqualTo("CORRECTION_CONFLICT");
    }

    private MvcTestResult post(Jwt jwt, String body) {
        return mvc.post()
                .uri(URI)
                .header("Idempotency-Key", KEY.toString())
                .contentType(MediaType.APPLICATION_JSON)
                .content(body)
                .with(SecurityMockMvcRequestPostProcessors.authentication(TestTokens.CONVERTER.convert(jwt)))
                .exchange();
    }
}
