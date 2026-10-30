package com.yarnshipment.bag.adapter.in.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.yarnshipment.bag.TestTokens;
import com.yarnshipment.bag.application.port.in.CreateBagsUseCase;
import com.yarnshipment.bag.application.port.in.ListLockedLotsUseCase;
import com.yarnshipment.bag.application.port.in.ListLotBagsUseCase;
import com.yarnshipment.bag.application.port.in.PrintLabelsUseCase;
import com.yarnshipment.bag.domain.model.Bag;
import com.yarnshipment.bag.domain.model.BagStatus;
import com.yarnshipment.bag.domain.model.LabelReprintReasonRequiredException;
import com.yarnshipment.bag.domain.model.LabeledBag;
import com.yarnshipment.bag.domain.model.LockedLot;
import com.yarnshipment.bag.domain.model.LotNotLockedException;
import com.yarnshipment.bag.domain.model.PackageType;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.test.web.servlet.assertj.MvcTestResult;

/** Çuval üretimi ve etiket basımı uçlarının sözleşmesi, hata kodları ve izin denetimi. */
@WebMvcTest(controllers = {BagCreationController.class, LabelController.class})
class BagProductionControllerTest {

    private static final UUID LOT_ID = UUID.fromString("0192f0a0-0000-7000-8000-000000000101");
    private static final Instant LOCKED_AT = Instant.parse("2026-10-30T08:00:00Z");
    private static final String CREATE_BODY = "{\"lotId\":\"" + LOT_ID + "\",\"packageType\":\"BAG\",\"count\":2}";
    private static final int BAG_COUNT = 4;
    private static final int PRINT_COUNT = 2;
    private static final String PRINT_BODY = "{\"uniqueKeys\":[\"BAG-7K3M-Q9XD\"]}";

    private final MockMvcTester mvc;

    @MockitoBean
    private CreateBagsUseCase createBagsUseCase;

    @MockitoBean
    private ListLockedLotsUseCase listLockedLotsUseCase;

    @MockitoBean
    private ListLotBagsUseCase listLotBagsUseCase;

    @MockitoBean
    private PrintLabelsUseCase printLabelsUseCase;

    @MockitoBean
    private JwtDecoder jwtDecoder;

    @Autowired
    BagProductionControllerTest(MockMvcTester mvc) {
        this.mvc = mvc;
    }

    @Test
    @DisplayName("Depo personeli çuval üretir; 201 ile üretilen çuvallar döner")
    void shouldCreateBags() {
        when(createBagsUseCase.createBags(any())).thenReturn(List.of(bag(), bag()));

        MvcTestResult result = post("/api/v1/bags", CREATE_BODY, TestTokens.token("warehouse").build());

        assertThat(result).hasStatus(HttpStatus.CREATED);
        assertThat(result).bodyJson().extractingPath("$.bags.length()").isEqualTo(2);
        assertThat(result).bodyJson().extractingPath("$.bags[0].qualityGradeCode").isEqualTo("A");
    }

    @Test
    @DisplayName("Sayı 1 ile 200 arasında olmalıdır")
    void shouldRejectInvalidCount() {
        String body = "{\"lotId\":\"" + LOT_ID + "\",\"packageType\":\"BAG\",\"count\":201}";

        assertThat(post("/api/v1/bags", body, TestTokens.token("warehouse").build()))
                .hasStatus(HttpStatus.BAD_REQUEST);
    }

    @Test
    @DisplayName("Kalitesi kilitli olmayan parti LOT_NOT_LOCKED koduyla 409 döner")
    void shouldReturnConflictForUnlockedLot() {
        when(createBagsUseCase.createBags(any())).thenThrow(new LotNotLockedException(LOT_ID));

        MvcTestResult result = post("/api/v1/bags", CREATE_BODY, TestTokens.token("warehouse").build());

        assertThat(result).hasStatus(HttpStatus.CONFLICT);
        assertThat(result).bodyJson().extractingPath("$.code").isEqualTo("LOT_NOT_LOCKED");
        assertThat(result).bodyJson().extractingPath("$.title").isEqualTo("Partinin kalitesi kilitli değil");
    }

    @Test
    @DisplayName("Kilitli partiler çuval sayısıyla, partinin çuvalları baskı sayısıyla listelenir")
    void shouldListLotsAndBags() {
        when(listLockedLotsUseCase.listLockedLots(any()))
                .thenReturn(List.of(new LockedLot(
                        LOT_ID, TestTokens.SITE_ID, "P-2026-0001", UUID.randomUUID(), "A", LOCKED_AT, BAG_COUNT)));
        when(listLotBagsUseCase.listLotBags(any()))
                .thenReturn(List.of(new LabeledBag(bag(), PRINT_COUNT, LOCKED_AT), new LabeledBag(bag(), 0, null)));

        MvcTestResult lots = get("/api/v1/bags/lots", TestTokens.token("warehouse").build());
        MvcTestResult bags = get("/api/v1/bags/lots/" + LOT_ID + "/bags", TestTokens.token("warehouse").build());

        assertThat(lots).hasStatusOk();
        assertThat(lots).bodyJson().extractingPath("$.lots[0].lotNo").isEqualTo("P-2026-0001");
        assertThat(lots).bodyJson().extractingPath("$.lots[0].bagCount").isEqualTo(BAG_COUNT);
        assertThat(bags).hasStatusOk();
        assertThat(bags).bodyJson().extractingPath("$.bags[0].printCount").isEqualTo(PRINT_COUNT);
        assertThat(bags).bodyJson().extractingPath("$.bags[0].lastPrintedAt").isNotNull();
        assertThat(bags).bodyJson().doesNotHavePath("$.bags[1].lastPrintedAt");
    }

    @Test
    @DisplayName("Etiketler PDF olarak, tarayıcıda açılacak biçimde döner")
    void shouldReturnLabelPdf() {
        when(printLabelsUseCase.printLabels(any())).thenReturn("%PDF-1.4".getBytes(StandardCharsets.US_ASCII));

        MvcTestResult result = post("/api/v1/bags/labels", PRINT_BODY, TestTokens.token("warehouse").build());

        assertThat(result).hasStatusOk();
        assertThat(result).hasContentType(MediaType.APPLICATION_PDF);
        assertThat(result)
                .headers()
                .hasValue(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"cuval-etiketleri.pdf\"");
        assertThat(result).hasBodyTextEqualTo("%PDF-1.4");
    }

    @Test
    @DisplayName("Gerekçesiz yeniden baskı LABEL_REPRINT_REASON_REQUIRED koduyla 422 döner")
    void shouldRequireReprintReason() {
        when(printLabelsUseCase.printLabels(any()))
                .thenThrow(new LabelReprintReasonRequiredException("BAG-7K3M-Q9XD"));

        MvcTestResult result = post("/api/v1/bags/labels", PRINT_BODY, TestTokens.token("warehouse").build());

        assertThat(result).hasStatus(HttpStatus.UNPROCESSABLE_CONTENT);
        assertThat(result).bodyJson().extractingPath("$.code").isEqualTo("LABEL_REPRINT_REASON_REQUIRED");
    }

    @Test
    @DisplayName("Kalite, yönetici, şoför ve fasoncu çuval üretemez ve etiket basamaz")
    void shouldForbidRolesWithoutBagCreate() {
        for (Jwt jwt : List.of(
                TestTokens.token("quality").claim("amr", List.of("otp")).build(),
                TestTokens.token("manager").build(),
                TestTokens.driverToken(),
                TestTokens.subcontractorToken())) {
            assertThat(post("/api/v1/bags", CREATE_BODY, jwt)).hasStatus(HttpStatus.FORBIDDEN);
            assertThat(post("/api/v1/bags/labels", PRINT_BODY, jwt)).hasStatus(HttpStatus.FORBIDDEN);
            assertThat(get("/api/v1/bags/lots", jwt)).hasStatus(HttpStatus.FORBIDDEN);
        }
    }

    private static Bag bag() {
        return new Bag(
                UUID.randomUUID(),
                TestTokens.SITE_ID,
                "BAG-7K3M-Q9XD",
                PackageType.BAG,
                "P-2026-0001",
                "A",
                BagStatus.CREATED,
                null);
    }

    private MvcTestResult get(String uri, Jwt jwt) {
        return mvc.get()
                .uri(uri)
                .with(SecurityMockMvcRequestPostProcessors.authentication(TestTokens.CONVERTER.convert(jwt)))
                .exchange();
    }

    private MvcTestResult post(String uri, String body, Jwt jwt) {
        return mvc.post()
                .uri(uri)
                .contentType(MediaType.APPLICATION_JSON)
                .content(body)
                .with(SecurityMockMvcRequestPostProcessors.authentication(TestTokens.CONVERTER.convert(jwt)))
                .with(SecurityMockMvcRequestPostProcessors.csrf())
                .exchange();
    }
}
