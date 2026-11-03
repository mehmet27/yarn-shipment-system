package com.yarnshipment.shipment.masterdata.adapter.in.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.yarnshipment.shipment.TestTokens;
import com.yarnshipment.shipment.masterdata.application.port.in.catalog.ListTareStandardsUseCase;
import com.yarnshipment.shipment.masterdata.application.port.in.catalog.SaveTareStandardUseCase;
import com.yarnshipment.shipment.masterdata.domain.catalog.PackagingType;
import com.yarnshipment.shipment.masterdata.domain.catalog.TareStandard;
import com.yarnshipment.shipment.masterdata.domain.catalog.TareStandardDetails;
import java.math.BigDecimal;
import java.util.List;
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

/** Standart dara uçlarının sözleşmesi: okuma MASTER_DATA_VIEW, tanımlama yalnızca yönetici. */
@WebMvcTest(controllers = TareStandardManagementController.class)
class TareStandardManagementControllerTest {

    private static final UUID ID = UUID.fromString("0192f0a0-0000-7000-8000-000000000301");
    private static final UUID SITE = UUID.fromString("0192f0a0-0000-7000-8000-000000000302");
    private static final String URI = "/api/v1/master-data/tare-standards";
    private static final String BODY = "{\"siteId\": \"" + SITE
            + "\", \"packagingType\": \"BAG\", \"standardTareKg\": \"0.450\", \"maxDeviationPct\": \"5.00\"}";

    private final MockMvcTester mvc;

    @MockitoBean
    private ListTareStandardsUseCase list;

    @MockitoBean
    private SaveTareStandardUseCase save;

    @MockitoBean
    private JwtDecoder jwtDecoder;

    @Autowired
    TareStandardManagementControllerTest(MockMvcTester mvc) {
        this.mvc = mvc;
    }

    private static TareStandard standard() {
        return new TareStandard(
                ID,
                SITE,
                "Merkez Depo",
                PackagingType.BAG,
                new TareStandardDetails(new BigDecimal("0.45"), new BigDecimal("5")));
    }

    @Test
    @DisplayName("Yönetici standardı tanımlar; kg üç, yüzde iki ondalıkla döner")
    void shouldSave() {
        when(save.saveTareStandard(any(), eq(SITE), eq(PackagingType.BAG), any()))
                .thenReturn(standard());

        MvcTestResult result = put(TestTokens.token("admin").build(), BODY);

        assertThat(result).hasStatus(HttpStatus.OK);
        assertThat(result).bodyJson().extractingPath("$.standardTareKg").isEqualTo("0.450");
        assertThat(result).bodyJson().extractingPath("$.maxDeviationPct").isEqualTo("5.00");
        assertThat(result).bodyJson().extractingPath("$.siteName").isEqualTo("Merkez Depo");
    }

    @Test
    @DisplayName("Depo çalışanı standart tanımlayamaz")
    void shouldForbidWarehouse() {
        assertThat(put(TestTokens.token("warehouse").build(), BODY)).hasStatus(HttpStatus.FORBIDDEN);
        verifyNoInteractions(save);
    }

    @Test
    @DisplayName("Sayı olmayan değer 400 verir")
    void shouldRejectMalformedBody() {
        String body = BODY.replace("\"0.450\"", "\"abc\"");

        assertThat(put(TestTokens.token("admin").build(), body)).hasStatus(HttpStatus.BAD_REQUEST);
        verifyNoInteractions(save);
    }

    @Test
    @DisplayName("Liste lokasyon süzgeciyle istenir")
    void shouldListBySite() {
        when(list.listTareStandards(SITE)).thenReturn(List.of(standard()));

        MvcTestResult result = mvc.get()
                .uri(URI)
                .queryParam("siteId", SITE.toString())
                .with(SecurityMockMvcRequestPostProcessors.authentication(
                        TestTokens.CONVERTER.convert(TestTokens.token("admin").build())))
                .exchange();

        assertThat(result).hasStatus(HttpStatus.OK);
        assertThat(result).bodyJson().extractingPath("$.items[0].packagingType").isEqualTo("BAG");
    }

    private MvcTestResult put(Jwt jwt, String body) {
        return mvc.put()
                .uri(URI)
                .contentType(MediaType.APPLICATION_JSON)
                .content(body)
                .with(SecurityMockMvcRequestPostProcessors.authentication(TestTokens.CONVERTER.convert(jwt)))
                .exchange();
    }
}
