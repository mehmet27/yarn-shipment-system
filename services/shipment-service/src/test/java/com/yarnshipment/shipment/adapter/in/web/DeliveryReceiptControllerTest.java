package com.yarnshipment.shipment.adapter.in.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import com.yarnshipment.shipment.TestShipments;
import com.yarnshipment.shipment.TestTokens;
import com.yarnshipment.shipment.application.port.in.ConfirmPickupUseCase;
import com.yarnshipment.shipment.application.port.in.GetShipmentUseCase;
import com.yarnshipment.shipment.application.port.in.ListShipmentsUseCase;
import com.yarnshipment.shipment.application.port.in.RecordDispatchUseCase;
import com.yarnshipment.shipment.application.port.in.SubmitDeliveryReceiptCommand;
import com.yarnshipment.shipment.application.port.in.SubmitDeliveryReceiptUseCase;
import com.yarnshipment.shipment.domain.model.ScanMethod;
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

/** Teslim tutanağı ucunun sözleşmesi: izin, istek doğrulama ve domain değerlerine çeviri. */
@WebMvcTest(controllers = ShipmentController.class)
class DeliveryReceiptControllerTest {

    private static final String URI = "/api/v1/shipments/" + TestShipments.SHIPMENT_ID + "/delivery-receipt";
    private static final String KEY = "0192f0a0-0000-7000-8000-0000000000dd";
    private static final String VALID_BODY = """
            {"grossKg": "1995.250", "tareKg": "18.000", "bagCount": 40, "sealCondition": "INTACT",
             "sealNo": "M-0001", "bagUniqueKeys": ["UK-0001", "UK-0001", "UK-0002"],
             "document": {"shipmentNo": "SV-1", "netKg": "1982.500", "bagCount": 40}}""";

    private final MockMvcTester mvc;

    @MockitoBean
    private ListShipmentsUseCase listShipmentsUseCase;

    @MockitoBean
    private GetShipmentUseCase getShipmentUseCase;

    @MockitoBean
    private ConfirmPickupUseCase confirmPickupUseCase;

    @MockitoBean
    private RecordDispatchUseCase recordDispatchUseCase;

    @MockitoBean
    private SubmitDeliveryReceiptUseCase submitDeliveryReceiptUseCase;

    @MockitoBean
    private JwtDecoder jwtDecoder;

    @Autowired
    DeliveryReceiptControllerTest(MockMvcTester mvc) {
        this.mvc = mvc;
    }

    @Test
    @DisplayName("Fasoncunun tutanağı 204 döner; tekrar eden anahtarlar bir kez, varsayılan yöntem SCAN")
    void shouldSubmitDeliveryForSubcontractor() {
        // when
        MvcTestResult result = post(TestTokens.subcontractorToken(), VALID_BODY);

        // then
        assertThat(result).hasStatus(HttpStatus.NO_CONTENT);
        verify(submitDeliveryReceiptUseCase)
                .submitDeliveryReceipt(argThat((SubmitDeliveryReceiptCommand command) ->
                        command.measurement().scannedKeys().size() == 2
                                && command.measurement().scanMethod() == ScanMethod.SCAN
                                && command.measurement().document().netKg() != null));
    }

    @Test
    @DisplayName("Şoför teslim tutanağı gönderemez (403)")
    void shouldRejectDriver() {
        // when
        MvcTestResult result = post(TestTokens.driverToken(), VALID_BODY);

        // then
        assertThat(result).hasStatus(HttpStatus.FORBIDDEN);
        verifyNoInteractions(submitDeliveryReceiptUseCase);
    }

    @Test
    @DisplayName("Üç ondalıklı dize olmayan kilo 400 VALIDATION_FAILED alır")
    void shouldRejectMalformedKg() {
        // when
        MvcTestResult result = post(TestTokens.subcontractorToken(), VALID_BODY.replace("\"1995.250\"", "1995.25"));

        // then
        assertThat(result).hasStatus(HttpStatus.BAD_REQUEST);
        verifyNoInteractions(submitDeliveryReceiptUseCase);
    }

    @Test
    @DisplayName("Dara brütten büyükse 400 INVALID_DELIVERY ve alan adı döner")
    void shouldRejectInconsistentWeights() {
        // when
        MvcTestResult result = post(TestTokens.subcontractorToken(), VALID_BODY.replace("\"18.000\"", "\"2000.000\""));

        // then
        assertThat(result).hasStatus(HttpStatus.BAD_REQUEST);
        assertThat(result).bodyJson().extractingPath("$.code").isEqualTo("INVALID_DELIVERY");
        assertThat(result).bodyJson().extractingPath("$.errors[0].field").isEqualTo("tareKg");
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
}
