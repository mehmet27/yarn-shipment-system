package com.yarnshipment.shipment.adapter.in.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import com.yarnshipment.shipment.TestShipments;
import com.yarnshipment.shipment.TestTokens;
import com.yarnshipment.shipment.application.port.in.CompleteShipmentCommand;
import com.yarnshipment.shipment.application.port.in.CompleteShipmentUseCase;
import com.yarnshipment.shipment.application.port.in.GetShipmentUseCase;
import com.yarnshipment.shipment.application.port.in.ListShipmentsUseCase;
import com.yarnshipment.shipment.application.port.in.RecordDispatchUseCase;
import com.yarnshipment.shipment.domain.evidence.InvalidWaybillEvidenceException;
import com.yarnshipment.shipment.domain.model.InvalidShipmentStatusException;
import com.yarnshipment.shipment.domain.model.ShipmentStatus;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
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
import org.springframework.test.web.servlet.request.RequestPostProcessor;

/** Tamamlama ucunun sözleşmesi: yalnızca müdür ve admin, zorunlu irsaliye belgesi, Idempotency-Key ve hata eşlemesi. */
@WebMvcTest(controllers = ShipmentController.class)
class ShipmentCompletionControllerTest {

    private static final String URI = "/api/v1/shipments/" + TestShipments.SHIPMENT_ID + "/completion";
    private static final String IDEMPOTENCY_KEY = "Idempotency-Key";
    private static final UUID KEY = UUID.fromString("0192f0a0-0000-7000-8000-0000000000cc");
    private static final UUID EVIDENCE = UUID.fromString("0192f0a0-0000-7000-8000-0000000000ee");
    private static final String BODY = "{\"waybillNo\":\"IRS-1\",\"waybillEvidenceId\":\"" + EVIDENCE + "\"}";

    private final MockMvcTester mvc;

    @MockitoBean
    private ListShipmentsUseCase listShipmentsUseCase;

    @MockitoBean
    private GetShipmentUseCase getShipmentUseCase;

    @MockitoBean
    private RecordDispatchUseCase recordDispatchUseCase;

    @MockitoBean
    private CompleteShipmentUseCase completeShipmentUseCase;

    @MockitoBean
    private JwtDecoder jwtDecoder;

    @Autowired
    ShipmentCompletionControllerTest(MockMvcTester mvc) {
        this.mvc = mvc;
    }

    @Test
    @DisplayName("Müdürün tamamlaması 204 döner; komut irsaliye numarası ve belgesiyle kurulur")
    void shouldCompleteForManager() {
        // when
        MvcTestResult result = post(TestTokens.token("manager").build(), BODY);

        // then
        assertThat(result).hasStatus(HttpStatus.NO_CONTENT);
        verify(completeShipmentUseCase)
                .completeShipment(argThat((CompleteShipmentCommand command) -> KEY.equals(command.idempotencyKey())
                        && "IRS-1".equals(command.waybillNo())
                        && EVIDENCE.equals(command.waybillEvidenceId())));
    }

    @Test
    @DisplayName("Depo sevkiyatı tamamlayamaz (403)")
    void shouldRejectWarehouse() {
        // when
        MvcTestResult result = post(TestTokens.token("warehouse").build(), BODY);

        // then
        assertThat(result).hasStatus(HttpStatus.FORBIDDEN);
        verifyNoInteractions(completeShipmentUseCase);
    }

    @Test
    @DisplayName("İrsaliye belgesi olmayan istek 400 alır")
    void shouldRequireWaybillEvidence() {
        // when
        MvcTestResult result = post(TestTokens.token("manager").build(), "{\"waybillNo\":\"IRS-1\"}");

        // then
        assertThat(result).hasStatus(HttpStatus.BAD_REQUEST);
        verifyNoInteractions(completeShipmentUseCase);
    }

    @Test
    @DisplayName("Idempotency-Key başlığı olmayan istek 400 MISSING_HEADER alır")
    void shouldRequireIdempotencyKey() {
        // when
        MvcTestResult result = mvc.post()
                .uri(URI)
                .contentType(MediaType.APPLICATION_JSON)
                .content(BODY)
                .with(authenticated(TestTokens.token("manager").build()))
                .exchange();

        // then
        assertThat(result).hasStatus(HttpStatus.BAD_REQUEST);
        assertThat(result).bodyJson().extractingPath("$.code").isEqualTo("MISSING_HEADER");
        verifyNoInteractions(completeShipmentUseCase);
    }

    @Test
    @DisplayName("Geçersiz irsaliye belgesi 422 WAYBILL_EVIDENCE_INVALID döner")
    void shouldMapInvalidEvidence() {
        // given
        doThrow(new InvalidWaybillEvidenceException(TestShipments.SHIPMENT_ID, EVIDENCE))
                .when(completeShipmentUseCase)
                .completeShipment(any());

        // when
        MvcTestResult result = post(TestTokens.token("manager").build(), BODY);

        // then
        assertThat(result).hasStatus(HttpStatus.UNPROCESSABLE_CONTENT);
        assertThat(result).bodyJson().extractingPath("$.code").isEqualTo("WAYBILL_EVIDENCE_INVALID");
    }

    @Test
    @DisplayName("Teslim beklemeyen sevkiyat için 409 INVALID_SHIPMENT_STATUS döner")
    void shouldMapInvalidStatusToConflict() {
        // given
        doThrow(new InvalidShipmentStatusException(
                        TestShipments.SHIPMENT_ID, ShipmentStatus.DRAFT, ShipmentStatus.COMPLETED))
                .when(completeShipmentUseCase)
                .completeShipment(any());

        // when
        MvcTestResult result = post(TestTokens.token("manager").build(), BODY);

        // then
        assertThat(result).hasStatus(HttpStatus.CONFLICT);
        assertThat(result).bodyJson().extractingPath("$.code").isEqualTo("INVALID_SHIPMENT_STATUS");
    }

    private MvcTestResult post(Jwt jwt, String body) {
        return mvc.post()
                .uri(URI)
                .header(IDEMPOTENCY_KEY, KEY.toString())
                .contentType(MediaType.APPLICATION_JSON)
                .content(body)
                .with(authenticated(jwt))
                .exchange();
    }

    private static RequestPostProcessor authenticated(Jwt jwt) {
        return SecurityMockMvcRequestPostProcessors.authentication(TestTokens.CONVERTER.convert(jwt));
    }
}
