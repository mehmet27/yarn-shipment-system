package com.yarnshipment.shipment.adapter.in.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import com.yarnshipment.shipment.TestShipments;
import com.yarnshipment.shipment.TestTokens;
import com.yarnshipment.shipment.application.port.in.CompleteShipmentUseCase;
import com.yarnshipment.shipment.application.port.in.GetShipmentUseCase;
import com.yarnshipment.shipment.application.port.in.ListShipmentsUseCase;
import com.yarnshipment.shipment.application.port.in.RecordDispatchCommand;
import com.yarnshipment.shipment.application.port.in.RecordDispatchUseCase;
import java.math.BigDecimal;
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

/** Çıkış tartımı ucunun sözleşmesi: izin, istek doğrulama ve hata eşlemesi. */
@WebMvcTest(controllers = ShipmentController.class)
class DispatchWeighingControllerTest {

    private static final String URI = "/api/v1/shipments/" + TestShipments.SHIPMENT_ID + "/dispatch-weighing";
    private static final int BAG_COUNT = 40;
    private static final String KEY = "0192f0a0-0000-7000-8000-0000000000dd";
    private static final String VALID_BODY =
            "{\"grossKg\": \"2000.500\", \"tareKg\": \"18.000\", \"bagCount\": 40, \"scaleTicketNo\": \"KF-1\"}";

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
    DispatchWeighingControllerTest(MockMvcTester mvc) {
        this.mvc = mvc;
    }

    private MvcTestResult post(Jwt jwt, String body) {
        return mvc.post()
                .uri(URI)
                .header("Idempotency-Key", KEY)
                .contentType(MediaType.APPLICATION_JSON)
                .content(body)
                .with(SecurityMockMvcRequestPostProcessors.authentication(TestTokens.CONVERTER.convert(jwt)))
                .exchange();
    }

    @Test
    @DisplayName("Depo çalışanının çıkış kaydı 204 döner ve değerler domain'e çevrilir")
    void shouldRecordDispatchForWarehouse() {
        MvcTestResult result = post(TestTokens.token("warehouse").build(), VALID_BODY);

        assertThat(result).hasStatus(HttpStatus.NO_CONTENT);
        verify(recordDispatchUseCase)
                .recordDispatch(argThat((RecordDispatchCommand command) ->
                        command.measurement().grossKg().compareTo(new BigDecimal("2000.500")) == 0
                                && "KF-1".equals(command.measurement().scaleTicketNo())
                                && command.measurement().bagCount() == BAG_COUNT));
    }

    @Test
    @DisplayName("Depo olmayan kullanıcı çıkış kaydı yapamaz (403)")
    void shouldRejectDispatchFromSubcontractor() {
        MvcTestResult result = post(TestTokens.subcontractorToken(), VALID_BODY);

        assertThat(result).hasStatus(HttpStatus.FORBIDDEN);
        verifyNoInteractions(recordDispatchUseCase);
    }

    @Test
    @DisplayName("Sözleşmeye uymayan kilo veya eksik çuval sayısı 400 alır")
    void shouldRejectInvalidBody() {
        MvcTestResult badWeight = post(
                TestTokens.token("warehouse").build(),
                "{\"grossKg\": \"2000\", \"tareKg\": \"18.000\", \"bagCount\": 40}");
        MvcTestResult noBagCount =
                post(TestTokens.token("warehouse").build(), "{\"grossKg\": \"2000.500\", \"tareKg\": \"18.000\"}");

        assertThat(badWeight).hasStatus(HttpStatus.BAD_REQUEST);
        assertThat(noBagCount).hasStatus(HttpStatus.BAD_REQUEST);
        verifyNoInteractions(recordDispatchUseCase);
    }

    @Test
    @DisplayName("Dara brütten büyükse 400 INVALID_DISPATCH döner")
    void shouldMapInvalidDispatch() {
        MvcTestResult result = post(
                TestTokens.token("warehouse").build(),
                "{\"grossKg\": \"10.000\", \"tareKg\": \"18.000\", \"bagCount\": 40}");

        assertThat(result).hasStatus(HttpStatus.BAD_REQUEST);
        assertThat(result).bodyJson().extractingPath("$.code").isEqualTo("INVALID_DISPATCH");
        assertThat(result).bodyJson().extractingPath("$.errors[0].field").isEqualTo("tareKg");
    }
}
