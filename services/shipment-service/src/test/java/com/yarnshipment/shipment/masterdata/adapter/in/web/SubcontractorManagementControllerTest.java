package com.yarnshipment.shipment.masterdata.adapter.in.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.yarnshipment.shipment.TestTokens;
import com.yarnshipment.shipment.masterdata.application.port.in.ChangeSubcontractorStatusUseCase;
import com.yarnshipment.shipment.masterdata.application.port.in.CreateSubcontractorLoginAccountUseCase;
import com.yarnshipment.shipment.masterdata.application.port.in.CreateSubcontractorUseCase;
import com.yarnshipment.shipment.masterdata.application.port.in.UpdateSubcontractorUseCase;
import com.yarnshipment.shipment.masterdata.domain.model.DuplicateSubcontractorException;
import com.yarnshipment.shipment.masterdata.domain.model.MasterDataNotFoundException;
import com.yarnshipment.shipment.masterdata.domain.model.MasterDataStatus;
import com.yarnshipment.shipment.masterdata.domain.model.Subcontractor;
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

/** Fasoncu ve şoför yönetim uçlarının sözleşmesi: izin, istek doğrulama ve hata eşlemesi. */
@WebMvcTest(controllers = SubcontractorManagementController.class)
class SubcontractorManagementControllerTest {

    private static final String KEY = "0192f0a0-0000-7000-8000-0000000000dd";
    private static final UUID ID = UUID.fromString("0192f0a0-0000-7000-8000-000000000003");
    private static final String SUBCONTRACTORS = "/api/v1/master-data/subcontractors";

    private final MockMvcTester mvc;

    @MockitoBean
    private CreateSubcontractorUseCase createSubcontractor;

    @MockitoBean
    private UpdateSubcontractorUseCase updateSubcontractor;

    @MockitoBean
    private ChangeSubcontractorStatusUseCase subcontractorStatus;

    @MockitoBean
    private CreateSubcontractorLoginAccountUseCase subcontractorLoginAccount;

    @MockitoBean
    private JwtDecoder jwtDecoder;

    @Autowired
    SubcontractorManagementControllerTest(MockMvcTester mvc) {
        this.mvc = mvc;
    }

    @Test
    @DisplayName("Yönetici fasoncu ekler; 201 ve eklenen kayıt döner")
    void shouldCreateSubcontractor() throws Exception {
        // given
        when(createSubcontractor.createSubcontractor(any(), eq("Yeni Tekstil")))
                .thenReturn(new Subcontractor(ID, "Yeni Tekstil"));

        // when
        MvcTestResult result = post(admin(), SUBCONTRACTORS, "{\"title\": \"Yeni Tekstil\"}");

        // then
        assertThat(result).hasStatus(HttpStatus.CREATED);
        assertThat(result).bodyJson().extractingPath("$.id").isEqualTo(ID.toString());
        assertThat(result).bodyJson().extractingPath("$.title").isEqualTo("Yeni Tekstil");
    }

    @Test
    @DisplayName("Boş ünvan şemadan 400 alır; use case çağrılmaz")
    void shouldRejectMissingTitle() throws Exception {
        assertThat(post(admin(), SUBCONTRACTORS, "{\"title\": \"\"}")).hasStatus(HttpStatus.BAD_REQUEST);
        verifyNoInteractions(createSubcontractor);
    }

    @Test
    @DisplayName("Kullanılmış ünvan DUPLICATE_SUBCONTRACTOR ile 409 döner")
    void shouldMapDuplicateTitle() throws Exception {
        // given
        when(createSubcontractor.createSubcontractor(any(), any()))
                .thenThrow(new DuplicateSubcontractorException("Yeni Tekstil"));

        // when
        MvcTestResult result = post(admin(), SUBCONTRACTORS, "{\"title\": \"yeni tekstil\"}");

        // then
        assertThat(result).hasStatus(HttpStatus.CONFLICT);
        assertThat(result).bodyJson().extractingPath("$.code").isEqualTo("DUPLICATE_SUBCONTRACTOR");
        assertThat(result).bodyJson().extractingPath("$.title").isEqualTo("Fasoncu zaten kayıtlı");
    }

    @Test
    @DisplayName("Güncellenen kayıt yoksa MASTER_DATA_NOT_FOUND ile 404 döner")
    void shouldMapNotFoundOnUpdate() throws Exception {
        // given
        when(updateSubcontractor.updateSubcontractor(any(), eq(ID), any()))
                .thenThrow(new MasterDataNotFoundException("Subcontractor", ID));

        // when
        MvcTestResult result = send(mvc.put().uri(SUBCONTRACTORS + "/" + ID), admin(), "{\"title\": \"Yeni\"}");

        // then
        assertThat(result).hasStatus(HttpStatus.NOT_FOUND);
        assertThat(result).bodyJson().extractingPath("$.code").isEqualTo("MASTER_DATA_NOT_FOUND");
    }

    @Test
    @DisplayName("Yönetici olmayan rol 403 alır ve use case çağrılmaz")
    void shouldForbidNonAdminRole() throws Exception {
        MvcTestResult result = post(TestTokens.token("warehouse").build(), SUBCONTRACTORS, "{\"title\": \"X\"}");

        assertThat(result).hasStatus(HttpStatus.FORBIDDEN);
        verifyNoInteractions(createSubcontractor);
    }

    @Test
    @DisplayName("Pasife alma ve yeniden etkinleştirme 204 döner ve doğru durumu iletir")
    void shouldDeactivateAndActivate() throws Exception {
        MvcTestResult off = send(mvc.post().uri(SUBCONTRACTORS + "/" + ID + "/deactivate"), admin(), null);
        MvcTestResult on = send(mvc.post().uri(SUBCONTRACTORS + "/" + ID + "/activate"), admin(), null);

        assertThat(off).hasStatus(HttpStatus.NO_CONTENT);
        assertThat(on).hasStatus(HttpStatus.NO_CONTENT);
        verify(subcontractorStatus).changeSubcontractorStatus(any(), eq(ID), eq(MasterDataStatus.INACTIVE));
        verify(subcontractorStatus).changeSubcontractorStatus(any(), eq(ID), eq(MasterDataStatus.ACTIVE));
    }

    private static Jwt admin() {
        return TestTokens.token("admin").build();
    }

    private MvcTestResult post(Jwt jwt, String uri, String body) {
        return send(mvc.post().uri(uri).header("Idempotency-Key", KEY), jwt, body);
    }

    private MvcTestResult send(
            MockMvcTester.MockMvcRequestBuilder builder, Jwt jwt, @org.jspecify.annotations.Nullable String body) {
        if (body != null) {
            builder.contentType(MediaType.APPLICATION_JSON).content(body);
        }
        return builder.with(SecurityMockMvcRequestPostProcessors.authentication(TestTokens.CONVERTER.convert(jwt)))
                .exchange();
    }
}
