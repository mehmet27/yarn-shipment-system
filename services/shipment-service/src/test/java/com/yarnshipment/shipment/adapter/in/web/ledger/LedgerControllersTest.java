package com.yarnshipment.shipment.adapter.in.web.ledger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.yarnshipment.shipment.TestTokens;
import com.yarnshipment.shipment.application.port.in.fabric.ListFabricReceiptsUseCase;
import com.yarnshipment.shipment.application.port.in.fabric.RecordFabricReceiptCommand;
import com.yarnshipment.shipment.application.port.in.fabric.RecordFabricReceiptUseCase;
import com.yarnshipment.shipment.application.port.in.fire.FireSettlementDetails;
import com.yarnshipment.shipment.application.port.in.fire.GetFireSettlementUseCase;
import com.yarnshipment.shipment.application.port.in.fire.ListFireSettlementsUseCase;
import com.yarnshipment.shipment.application.port.in.stock.RequireWorkAreaUseCase;
import com.yarnshipment.shipment.domain.fabric.FabricReceipt;
import com.yarnshipment.shipment.domain.fabric.YarnShare;
import com.yarnshipment.shipment.domain.fire.FireCalculation;
import com.yarnshipment.shipment.domain.fire.FireCalculation.Inputs;
import com.yarnshipment.shipment.domain.fire.FireSettlement;
import com.yarnshipment.shipment.domain.fire.FireSettlementClosedException;
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
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.test.web.servlet.assertj.MvcTestResult;

/** Kumaş girişi ve fire dosyası uçlarının sözleşmesi: izin, istek doğrulama, düz JSON biçimi ve hata eşlemesi. */
@WebMvcTest(controllers = {FabricReceiptController.class, FireSettlementController.class})
@SuppressWarnings("checkstyle:ClassFanOutComplexity")
class LedgerControllersTest {

    private static final UUID SHIPMENT = UUID.fromString("0192f0a0-0000-7000-8000-000000000801");
    private static final String RECEIPTS = "/api/v1/shipments/" + SHIPMENT + "/fabric-receipts";
    private static final String FIRE = "/api/v1/shipments/" + SHIPMENT + "/fire-settlement";
    private static final String BODY = """
            {"fabricType": "Süprem", "netKg": "32660.000", "rollCount": 12, "waybillNo": "KIR-1"}""";

    private final MockMvcTester mvc;

    @MockitoBean
    private RecordFabricReceiptUseCase recordFabricReceiptUseCase;

    @MockitoBean
    private ListFabricReceiptsUseCase listFabricReceiptsUseCase;

    @MockitoBean
    private GetFireSettlementUseCase getFireSettlementUseCase;

    @MockitoBean
    private ListFireSettlementsUseCase listFireSettlementsUseCase;

    @MockitoBean
    private RequireWorkAreaUseCase workAreaUseCase;

    @Autowired
    LedgerControllersTest(MockMvcTester mvc) {
        this.mvc = mvc;
    }

    private MvcTestResult post(String role, String uri, String body) {
        return mvc.post()
                .uri(uri)
                .header("Idempotency-Key", "0192f0a0-0000-7000-8000-0000000000dd")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body)
                .with(SecurityMockMvcRequestPostProcessors.authentication(
                        TestTokens.CONVERTER.convert(TestTokens.token(role).build())))
                .exchange();
    }

    private MvcTestResult get(String role, String uri) {
        return mvc.get()
                .uri(uri)
                .with(SecurityMockMvcRequestPostProcessors.authentication(
                        TestTokens.CONVERTER.convert(TestTokens.token(role).build())))
                .exchange();
    }

    @Test
    @DisplayName("Kalite kumaş girişini kaydeder: 201, kilolar 3 ondalıklı dize, iplik dağılımı yanıtta")
    void shouldRecordFabricReceipt() {
        // given
        FabricReceipt receipt = new FabricReceipt(
                UUID.randomUUID(),
                SHIPMENT,
                UUID.randomUUID(),
                "Süprem",
                new BigDecimal("32660"),
                null,
                "KIR-1",
                Instant.parse("2026-11-12T09:00:00Z"),
                List.of(new YarnShare("30/1 penye", new BigDecimal("32660"))),
                UUID.randomUUID());
        when(recordFabricReceiptUseCase.recordFabricReceipt(any())).thenReturn(receipt);

        // when
        MvcTestResult result = post("quality", RECEIPTS, BODY);

        // then
        assertThat(result).hasStatus(HttpStatus.CREATED);
        assertThat(result).bodyJson().extractingPath("$.netKg").isEqualTo("32660.000");
        assertThat(result).bodyJson().extractingPath("$.yarns[0].yarnType").isEqualTo("30/1 penye");
        ArgumentCaptor<RecordFabricReceiptCommand> command = ArgumentCaptor.forClass(RecordFabricReceiptCommand.class);
        verify(recordFabricReceiptUseCase).recordFabricReceipt(command.capture());
        assertThat(command.getValue().netKg()).isEqualByComparingTo("32660");
        assertThat(command.getValue().yarns()).isNull();
    }

    @Test
    @DisplayName("Kumaş girişi izni olmayan depo 403 alır; geçersiz kilo ve eksik alan 400 alır")
    void shouldGuardAndValidateFabricReceipt() {
        assertThat(post("warehouse", RECEIPTS, BODY)).hasStatus(HttpStatus.FORBIDDEN);
        MvcTestResult badKg = post("quality", RECEIPTS, BODY.replace("32660.000", "12.3456"));
        MvcTestResult missing = post("quality", RECEIPTS, "{\"netKg\": \"10\"}");

        assertThat(badKg).hasStatus(HttpStatus.BAD_REQUEST);
        assertThat(badKg).bodyJson().extractingPath("$.code").isEqualTo("INVALID_LEDGER_INPUT");
        assertThat(missing).hasStatus(HttpStatus.BAD_REQUEST);
        verifyNoInteractions(recordFabricReceiptUseCase);
    }

    @Test
    @DisplayName("Fire dosyası sözleşmedeki düz alanlarla döner: oran, G, D, I, K, T, B, E, H ve fire yüzdesi")
    void shouldReturnFlatFireSettlement() {
        // given
        FireSettlement settlement = FireSettlement.open(SHIPMENT, UUID.randomUUID(), new BigDecimal("34032"));
        FireCalculation calculation = FireCalculation.of(new Inputs(
                settlement.sentKg(), BigDecimal.ZERO, BigDecimal.ZERO, new BigDecimal("32660"), settlement.fireRate()));
        when(getFireSettlementUseCase.getFireSettlement(any()))
                .thenReturn(new FireSettlementDetails(settlement.afterFabricReceipt(), calculation));

        // when
        MvcTestResult result = get("manager", FIRE);

        // then
        assertThat(result).hasStatus(HttpStatus.OK);
        assertThat(result).bodyJson().extractingPath("$.status").isEqualTo("TO_INVOICE");
        assertThat(result).bodyJson().extractingPath("$.fireRate").isEqualTo("0.0400");
        assertThat(result).bodyJson().extractingPath("$.sentKg").isEqualTo("34032.000");
        assertThat(result).bodyJson().extractingPath("$.fabricKg").isEqualTo("32660.000");
        assertThat(result).bodyJson().extractingPath("$.expectedFabricKg").isEqualTo("32670.720");
        assertThat(result).bodyJson().extractingPath("$.faultyKg").isEqualTo("10.720");
        assertThat(result).bodyJson().extractingPath("$.actualFirePercent").isEqualTo("4.03");
    }

    @Test
    @DisplayName("Kapanmış dosya 409 FIRE_SETTLEMENT_CLOSED; liste ucu BALANCE_VIEW'siz role kapalıdır")
    void shouldMapClosedFileAndGuardList() {
        // given
        when(recordFabricReceiptUseCase.recordFabricReceipt(any()))
                .thenThrow(new FireSettlementClosedException(SHIPMENT));

        // when / then
        MvcTestResult closed = post("quality", RECEIPTS, BODY);
        assertThat(closed).hasStatus(HttpStatus.CONFLICT);
        assertThat(closed).bodyJson().extractingPath("$.code").isEqualTo("FIRE_SETTLEMENT_CLOSED");
        assertThat(get("warehouse", "/api/v1/fire-settlements")).hasStatus(HttpStatus.FORBIDDEN);
    }
}
