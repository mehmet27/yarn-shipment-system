package com.yarnshipment.bag.adapter.in.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.yarnshipment.bag.TestTokens;
import com.yarnshipment.bag.application.port.in.BagNotFoundException;
import com.yarnshipment.bag.application.port.in.GetBagUseCase;
import com.yarnshipment.bag.application.port.in.ReleaseBagsUseCase;
import com.yarnshipment.bag.application.port.in.ReserveBagsUseCase;
import com.yarnshipment.bag.domain.model.Bag;
import com.yarnshipment.bag.domain.model.BagAlreadyReservedException;
import com.yarnshipment.bag.domain.model.BagNotAvailableException;
import com.yarnshipment.bag.domain.model.BagStatus;
import com.yarnshipment.bag.domain.model.PackageType;
import java.util.List;
import java.util.UUID;
import org.jspecify.annotations.Nullable;
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

/** Çuval uçlarının sözleşmesi, hata kodları ve izin denetimi. */
@WebMvcTest(controllers = BagController.class)
class BagControllerTest {

    private static final UUID SHIPMENT_ID = UUID.fromString("0192f0a0-0000-7000-8000-0000000009a1");
    private static final String RESERVE_BODY = "{\"shipmentId\":\"" + SHIPMENT_ID + "\",\"uniqueKeys\":[\"BAG-0001\"]}";

    private final MockMvcTester mvc;

    @MockitoBean
    private GetBagUseCase getBagUseCase;

    @MockitoBean
    private ReserveBagsUseCase reserveBagsUseCase;

    @MockitoBean
    private ReleaseBagsUseCase releaseBagsUseCase;

    @MockitoBean
    private JwtDecoder jwtDecoder;

    @Autowired
    BagControllerTest(MockMvcTester mvc) {
        this.mvc = mvc;
    }

    @Test
    @DisplayName("Çuval alanları döner; ayrılmamış çuvalda sevkiyat alanı hiç yoktur")
    void shouldReturnBag() {
        // given
        when(getBagUseCase.getBag(any())).thenReturn(bag(BagStatus.CREATED, null));

        // when
        MvcTestResult result = get("/api/v1/bags/BAG-0001", warehouse());

        // then
        assertThat(result).hasStatusOk();
        assertThat(result).bodyJson().extractingPath("$.uniqueKey").isEqualTo("BAG-0001");
        assertThat(result).bodyJson().extractingPath("$.status").isEqualTo("CREATED");
        assertThat(result).bodyJson().doesNotHavePath("$.reservedShipmentId");
    }

    @Test
    @DisplayName("Olmayan çuval BAG_NOT_FOUND koduyla 404 döner")
    void shouldReturnProblemForMissingBag() {
        when(getBagUseCase.getBag(any())).thenThrow(new BagNotFoundException("YOK"));

        MvcTestResult result = get("/api/v1/bags/YOK", warehouse());

        assertThat(result).hasStatus(HttpStatus.NOT_FOUND);
        assertThat(result).bodyJson().extractingPath("$.code").isEqualTo("BAG_NOT_FOUND");
        assertThat(result).bodyJson().extractingPath("$.title").isEqualTo("Çuval bulunamadı");
    }

    @Test
    @DisplayName("Rezervasyon ayrılan çuvalı sevkiyatıyla döner")
    void shouldReserveBags() {
        when(reserveBagsUseCase.reserveBags(any())).thenReturn(List.of(bag(BagStatus.RESERVED, SHIPMENT_ID)));

        MvcTestResult result = post("/api/v1/bags/reservations", RESERVE_BODY, warehouse());

        assertThat(result).hasStatusOk();
        assertThat(result).bodyJson().extractingPath("$.bags[0].status").isEqualTo("RESERVED");
        assertThat(result)
                .bodyJson()
                .extractingPath("$.bags[0].reservedShipmentId")
                .isEqualTo(SHIPMENT_ID.toString());
    }

    @Test
    @DisplayName("Başka sevkiyattaki çuval BAG_ALREADY_RESERVED koduyla 409 döner")
    void shouldReturnConflictForReservedBag() {
        when(reserveBagsUseCase.reserveBags(any())).thenThrow(new BagAlreadyReservedException("BAG-0001"));

        MvcTestResult result = post("/api/v1/bags/reservations", RESERVE_BODY, warehouse());

        assertThat(result).hasStatus(HttpStatus.CONFLICT);
        assertThat(result).bodyJson().extractingPath("$.code").isEqualTo("BAG_ALREADY_RESERVED");
    }

    @Test
    @DisplayName("Kullanılamaz durumdaki çuval BAG_NOT_AVAILABLE koduyla 409 döner")
    void shouldReturnConflictForUnavailableBag() {
        when(reserveBagsUseCase.reserveBags(any())).thenThrow(new BagNotAvailableException("BAG-0004", BagStatus.VOID));

        MvcTestResult result = post("/api/v1/bags/reservations", RESERVE_BODY, warehouse());

        assertThat(result).hasStatus(HttpStatus.CONFLICT);
        assertThat(result).bodyJson().extractingPath("$.code").isEqualTo("BAG_NOT_AVAILABLE");
    }

    @Test
    @DisplayName("Boş anahtar listesi 400 alır")
    void shouldRejectEmptyKeyList() {
        MvcTestResult result = post(
                "/api/v1/bags/reservations", "{\"shipmentId\":\"" + SHIPMENT_ID + "\",\"uniqueKeys\":[]}", warehouse());

        assertThat(result).hasStatus(HttpStatus.BAD_REQUEST);
    }

    @Test
    @DisplayName("Bırakma yalnızca SHIPMENT_CANCEL iznine açıktır")
    void shouldReleaseOnlyWithCancelPermission() {
        when(releaseBagsUseCase.releaseBags(any())).thenReturn(2);
        String body = "{\"shipmentId\":\"" + SHIPMENT_ID + "\",\"reason\":\"iptal\"}";

        MvcTestResult allowed = post(
                "/api/v1/bags/reservations/release",
                body,
                TestTokens.token("admin").build());
        MvcTestResult denied = post("/api/v1/bags/reservations/release", body, warehouse());

        assertThat(allowed).hasStatusOk();
        assertThat(allowed).bodyJson().extractingPath("$.releasedCount").isEqualTo(2);
        assertThat(denied).hasStatus(HttpStatus.FORBIDDEN);
    }

    @Test
    @DisplayName("Şoför rolü çuval okuyamaz ve ayıramaz")
    void shouldForbidDriver() {
        assertThat(get("/api/v1/bags/BAG-0001", TestTokens.driverToken())).hasStatus(HttpStatus.FORBIDDEN);
        assertThat(post("/api/v1/bags/reservations", RESERVE_BODY, TestTokens.driverToken()))
                .hasStatus(HttpStatus.FORBIDDEN);
    }

    @Test
    @DisplayName("Token'sız istek 401 alır")
    void shouldRequireAuthentication() {
        assertThat(mvc.get().uri("/api/v1/bags/BAG-0001").exchange()).hasStatus(HttpStatus.UNAUTHORIZED);
    }

    private static Jwt warehouse() {
        return TestTokens.token("warehouse").build();
    }

    private static Bag bag(BagStatus status, @Nullable UUID reservedShipmentId) {
        return new Bag(
                UUID.randomUUID(),
                TestTokens.SITE_ID,
                "BAG-0001",
                PackageType.BAG,
                "P-2026-0001",
                "A",
                status,
                reservedShipmentId);
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
