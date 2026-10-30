package com.yarnshipment.shipment.masterdata.adapter.in.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.yarnshipment.shipment.TestTokens;
import com.yarnshipment.shipment.masterdata.application.port.in.catalog.ChangeProductStatusUseCase;
import com.yarnshipment.shipment.masterdata.application.port.in.catalog.CreateProductUseCase;
import com.yarnshipment.shipment.masterdata.application.port.in.catalog.UpdateProductUseCase;
import com.yarnshipment.shipment.masterdata.domain.catalog.DuplicateCodeException;
import com.yarnshipment.shipment.masterdata.domain.catalog.Product;
import com.yarnshipment.shipment.masterdata.domain.catalog.ProductDetails;
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

/** İplik yönetim uçlarının sözleşmesi: yalnızca yönetici, kod tekilliği eşlemesi. */
@WebMvcTest(controllers = ProductManagementController.class)
class ProductManagementControllerTest {

    private static final String KEY = "0192f0a0-0000-7000-8000-0000000000dd";
    private static final UUID ID = UUID.fromString("0192f0a0-0000-7000-8000-000000000201");
    private static final String URI = "/api/v1/master-data/products";
    private static final String BODY =
            "{\"code\": \"pes-30-1\", \"name\": \"Polyester 30/1\", \"yarnCount\": \"Ne 30/1\"}";

    private final MockMvcTester mvc;

    @MockitoBean
    private CreateProductUseCase create;

    @MockitoBean
    private UpdateProductUseCase update;

    @MockitoBean
    private ChangeProductStatusUseCase status;

    @MockitoBean
    private JwtDecoder jwtDecoder;

    @Autowired
    ProductManagementControllerTest(MockMvcTester mvc) {
        this.mvc = mvc;
    }

    @Test
    @DisplayName("Yönetici iplik ekler; 201 ve kodu büyük harfli kayıt döner")
    void shouldCreate() {
        // given
        when(create.createProduct(any(), eq("pes-30-1"), any()))
                .thenReturn(new Product(ID, "PES-30-1", new ProductDetails("Polyester 30/1", "Ne 30/1"), true));

        // when
        MvcTestResult result = post(TestTokens.token("admin").build(), BODY);

        // then
        assertThat(result).hasStatus(HttpStatus.CREATED);
        assertThat(result).bodyJson().extractingPath("$.code").isEqualTo("PES-30-1");
        assertThat(result).bodyJson().extractingPath("$.yarnCount").isEqualTo("Ne 30/1");
    }

    @Test
    @DisplayName("Kalite kullanıcısı iplik ekleyemez")
    void shouldForbidQuality() {
        assertThat(post(TestTokens.token("quality").build(), BODY)).hasStatus(HttpStatus.FORBIDDEN);
        verifyNoInteractions(create);
    }

    @Test
    @DisplayName("Kullanılmış kod DUPLICATE_CODE ile 409 döner")
    void shouldMapDuplicateCode() {
        when(create.createProduct(any(), any(), any())).thenThrow(new DuplicateCodeException("code", "PES-30-1"));

        MvcTestResult result = post(TestTokens.token("admin").build(), BODY);

        assertThat(result).hasStatus(HttpStatus.CONFLICT);
        assertThat(result).bodyJson().extractingPath("$.code").isEqualTo("DUPLICATE_CODE");
        assertThat(result).bodyJson().extractingPath("$.title").isEqualTo("Kod zaten kayıtlı");
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
