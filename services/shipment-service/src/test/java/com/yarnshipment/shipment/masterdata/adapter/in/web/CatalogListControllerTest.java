package com.yarnshipment.shipment.masterdata.adapter.in.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.yarnshipment.shipment.TestTokens;
import com.yarnshipment.shipment.masterdata.application.port.in.MasterDataPage;
import com.yarnshipment.shipment.masterdata.application.port.in.catalog.ListProductsUseCase;
import com.yarnshipment.shipment.masterdata.application.port.in.catalog.ListQualityGradesUseCase;
import com.yarnshipment.shipment.masterdata.application.port.in.lot.ListLotsUseCase;
import com.yarnshipment.shipment.masterdata.domain.catalog.Product;
import com.yarnshipment.shipment.masterdata.domain.catalog.ProductDetails;
import com.yarnshipment.shipment.masterdata.domain.lot.LotDetails;
import com.yarnshipment.shipment.masterdata.domain.lot.ProductionLot;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.HttpStatus;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.test.web.servlet.assertj.MvcTestResult;

/** İplik, kalite ve parti liste uçlarının sözleşmesi ve pasif kayıt izni. */
@WebMvcTest(controllers = CatalogListController.class)
class CatalogListControllerTest {

    private static final UUID ID = UUID.fromString("0192f0a0-0000-7000-8000-000000000101");
    private static final int SIZE = 20;

    private final MockMvcTester mvc;

    @MockitoBean
    private ListProductsUseCase products;

    @MockitoBean
    private ListQualityGradesUseCase qualityGrades;

    @MockitoBean
    private ListLotsUseCase lots;

    @MockitoBean
    private JwtDecoder jwtDecoder;

    @Autowired
    CatalogListControllerTest(MockMvcTester mvc) {
        this.mvc = mvc;
    }

    @Test
    @DisplayName("İplik listesi kodu ve iplik numarasını döner; pasifleri yalnızca yönetici ister")
    void shouldListProducts() {
        // given
        when(products.listProducts(any()))
                .thenReturn(new MasterDataPage<>(
                        List.of(new Product(ID, "PES-30-1", new ProductDetails("Polyester", "Ne 30/1"), true)),
                        0,
                        SIZE,
                        1));

        // when
        MvcTestResult result = get("/api/v1/master-data/products?q=pes", "warehouse");
        MvcTestResult denied = get("/api/v1/master-data/products?status=ALL", "quality");

        // then
        assertThat(result).hasStatusOk();
        assertThat(result).bodyJson().extractingPath("$.items[0].code").isEqualTo("PES-30-1");
        assertThat(result).bodyJson().extractingPath("$.items[0].yarnCount").isEqualTo("Ne 30/1");
        assertThat(denied).hasStatus(HttpStatus.FORBIDDEN);
    }

    @Test
    @DisplayName("Parti listesi kilit anını gösterir; pasif partileri kalite kullanıcısı ister, depo isteyemez")
    void shouldListLotsAndGuardInactive() {
        // given
        ProductionLot lot = new ProductionLot(
                ID,
                TestTokens.SITE_ID,
                new LotDetails("P-1", UUID.randomUUID(), UUID.randomUUID(), LocalDate.parse("2026-10-10")),
                new ProductionLot.Labels("Merkez", "PES-30-1", "Polyester", "A", "Birinci"),
                Instant.parse("2026-10-11T08:00:00Z"),
                true);
        when(lots.listLots(any(), any())).thenReturn(new MasterDataPage<>(List.of(lot), 0, SIZE, 1));

        // when
        MvcTestResult listed = get("/api/v1/master-data/lots", "warehouse");
        MvcTestResult denied = get("/api/v1/master-data/lots?status=ALL", "warehouse");
        MvcTestResult allowed = get("/api/v1/master-data/lots?status=ALL", "quality");

        // then
        assertThat(listed).hasStatusOk();
        assertThat(listed)
                .bodyJson()
                .extractingPath("$.items[0].qualityGradeCode")
                .isEqualTo("A");
        assertThat(listed)
                .bodyJson()
                .extractingPath("$.items[0].qualityLockedAt")
                .isEqualTo("2026-10-11T08:00:00Z");
        assertThat(denied).hasStatus(HttpStatus.FORBIDDEN);
        assertThat(allowed).hasStatusOk();
    }

    private MvcTestResult get(String uri, String role) {
        Jwt jwt = TestTokens.token(role).build();
        return mvc.get()
                .uri(uri)
                .with(SecurityMockMvcRequestPostProcessors.authentication(TestTokens.CONVERTER.convert(jwt)))
                .exchange();
    }
}
