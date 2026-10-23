package com.yarnshipment.shipment.masterdata.adapter.in.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.yarnshipment.shipment.TestTokens;
import com.yarnshipment.shipment.masterdata.application.port.in.ChangeSubcontractorStatusUseCase;
import com.yarnshipment.shipment.masterdata.application.port.in.CreateSubcontractorLoginAccountUseCase;
import com.yarnshipment.shipment.masterdata.application.port.in.CreateSubcontractorUseCase;
import com.yarnshipment.shipment.masterdata.application.port.in.UpdateSubcontractorUseCase;
import com.yarnshipment.shipment.masterdata.domain.model.LoginAccount;
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

/** Fasoncu giriş hesabı ucunun izni ve yanıtı. */
@WebMvcTest(controllers = SubcontractorManagementController.class)
class SubcontractorLoginAccountControllerTest {

    private static final UUID SUBCONTRACTOR = UUID.fromString("0192f0a0-0000-7000-8000-000000000003");
    private static final String SUBCONTRACTOR_URI =
            "/api/v1/master-data/subcontractors/" + SUBCONTRACTOR + "/login-account";
    private static final String BODY =
            "{\"username\":\"ali.yilmaz\",\"siteIds\":[\"0192f0a0-0000-7000-8000-000000000001\"]}";

    private final MockMvcTester mvc;

    @MockitoBean
    private CreateSubcontractorLoginAccountUseCase subcontractorAccount;

    @MockitoBean
    private CreateSubcontractorUseCase createSubcontractor;

    @MockitoBean
    private UpdateSubcontractorUseCase updateSubcontractor;

    @MockitoBean
    private ChangeSubcontractorStatusUseCase subcontractorStatus;

    @MockitoBean
    private JwtDecoder jwtDecoder;

    @Autowired
    SubcontractorLoginAccountControllerTest(MockMvcTester mvc) {
        this.mvc = mvc;
    }

    @Test
    @DisplayName("Fasoncu hesabı da 201 döner")
    void shouldCreateSubcontractorAccount() {
        when(subcontractorAccount.createSubcontractorLoginAccount(any(), eq(SUBCONTRACTOR), any()))
                .thenReturn(new LoginAccount("ornek", "Gecici-Sifre-456"));

        MvcTestResult result =
                post(SUBCONTRACTOR_URI, BODY, TestTokens.token("admin").build());

        assertThat(result).hasStatus(HttpStatus.CREATED);
        assertThat(result).bodyJson().extractingPath("$.username").isEqualTo("ornek");
    }

    @Test
    @DisplayName("MASTER_DATA_MANAGE izni olmayan rol 403 alır ve use case çağrılmaz")
    void shouldForbidWarehouse() {
        MvcTestResult result =
                post(SUBCONTRACTOR_URI, BODY, TestTokens.token("warehouse").build());

        assertThat(result).hasStatus(HttpStatus.FORBIDDEN);
        verifyNoInteractions(subcontractorAccount);
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
