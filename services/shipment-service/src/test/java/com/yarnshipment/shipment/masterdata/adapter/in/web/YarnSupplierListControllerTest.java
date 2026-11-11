package com.yarnshipment.shipment.masterdata.adapter.in.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.yarnshipment.shipment.TestTokens;
import com.yarnshipment.shipment.masterdata.application.port.in.MasterDataPage;
import com.yarnshipment.shipment.masterdata.application.port.in.catalog.ListProductsUseCase;
import com.yarnshipment.shipment.masterdata.application.port.in.catalog.ListQualityGradesUseCase;
import com.yarnshipment.shipment.masterdata.application.port.in.catalog.ListYarnSuppliersUseCase;
import com.yarnshipment.shipment.masterdata.application.port.in.lot.ListLotsUseCase;
import com.yarnshipment.shipment.masterdata.domain.catalog.YarnSupplier;
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

/** İplik tedarikçisi liste ucunun sözleşmesi ve pasif kayıt izni. */
@WebMvcTest(controllers = CatalogListController.class)
class YarnSupplierListControllerTest {

    private static final UUID ID = UUID.fromString("0192f0a0-0000-7000-8000-000000000101");
    private static final int SIZE = 20;

    private final MockMvcTester mvc;

    @MockitoBean
    private ListProductsUseCase products;

    @MockitoBean
    private ListQualityGradesUseCase qualityGrades;

    @MockitoBean
    private ListYarnSuppliersUseCase yarnSuppliers;

    @MockitoBean
    private ListLotsUseCase lots;

    @MockitoBean
    private JwtDecoder jwtDecoder;

    @Autowired
    YarnSupplierListControllerTest(MockMvcTester mvc) {
        this.mvc = mvc;
    }

    @Test
    @DisplayName("İplik tedarikçisi listesi adı döner; pasifleri yalnızca yönetici ister")
    void shouldListYarnSuppliers() {
        // given
        when(yarnSuppliers.listYarnSuppliers(any()))
                .thenReturn(new MasterDataPage<>(List.of(new YarnSupplier(ID, "Şireci", true)), 0, SIZE, 1));

        // when
        MvcTestResult result = get("/api/v1/master-data/yarn-suppliers", "warehouse");
        MvcTestResult denied = get("/api/v1/master-data/yarn-suppliers?status=ALL", "quality");

        // then
        assertThat(result).hasStatusOk();
        assertThat(result).bodyJson().extractingPath("$.items[0].name").isEqualTo("Şireci");
        assertThat(denied).hasStatus(HttpStatus.FORBIDDEN);
    }

    private MvcTestResult get(String uri, String role) {
        Jwt jwt = TestTokens.token(role).build();
        return mvc.get()
                .uri(uri)
                .with(SecurityMockMvcRequestPostProcessors.authentication(TestTokens.CONVERTER.convert(jwt)))
                .exchange();
    }
}
