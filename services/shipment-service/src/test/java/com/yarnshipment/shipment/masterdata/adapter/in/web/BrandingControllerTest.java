package com.yarnshipment.shipment.masterdata.adapter.in.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.yarnshipment.shipment.TestTokens;
import com.yarnshipment.shipment.masterdata.application.port.in.branding.GetBrandingUseCase;
import com.yarnshipment.shipment.masterdata.application.port.in.branding.SaveBrandingUseCase;
import com.yarnshipment.shipment.masterdata.domain.branding.Branding;
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

/** Marka ayarı uçlarının sözleşmesi: okuma girişsiz, yazma yalnızca yönetici (SETTINGS_MANAGE). */
@WebMvcTest(controllers = BrandingController.class)
class BrandingControllerTest {

    private static final String URI = "/api/v1/branding";
    private static final String BODY = "{\"companyName\": \"Gencer Fabric\", \"primaryColor\": \"#17527A\"}";

    private final MockMvcTester mvc;

    @MockitoBean
    private GetBrandingUseCase get;

    @MockitoBean
    private SaveBrandingUseCase save;

    @MockitoBean
    private JwtDecoder jwtDecoder;

    @Autowired
    BrandingControllerTest(MockMvcTester mvc) {
        this.mvc = mvc;
    }

    @Test
    @DisplayName("Marka ayarı token olmadan okunur ve önbelleğe alınmaz")
    void shouldReadWithoutLogin() {
        when(get.getBranding()).thenReturn(new Branding("Gencer Fabric", "#17527a", null));

        MvcTestResult result = mvc.get().uri(URI).exchange();

        assertThat(result).hasStatus(HttpStatus.OK);
        assertThat(result).headers().hasValue("Cache-Control", "no-store");
        assertThat(result).bodyJson().extractingPath("$.primaryColor").isEqualTo("#17527a");
        assertThat(result).bodyJson().extractingPath("$.companyName").isEqualTo("Gencer Fabric");
    }

    @Test
    @DisplayName("Yönetici marka ayarını kaydeder")
    void shouldSave() {
        when(save.saveBranding(any(), any())).thenReturn(new Branding("Gencer Fabric", "#17527a", null));

        MvcTestResult result = put(TestTokens.token("admin").build(), BODY);

        assertThat(result).hasStatus(HttpStatus.OK);
        assertThat(result).bodyJson().extractingPath("$.primaryColor").isEqualTo("#17527a");
    }

    @Test
    @DisplayName("Depo çalışanı marka ayarını değiştiremez")
    void shouldForbidWarehouse() {
        assertThat(put(TestTokens.token("warehouse").build(), BODY)).hasStatus(HttpStatus.FORBIDDEN);
        verifyNoInteractions(save);
    }

    @Test
    @DisplayName("Renk biçimi hatalıysa 400 verir")
    void shouldRejectBadColor() {
        String body = BODY.replace("#17527A", "mavi");

        assertThat(put(TestTokens.token("admin").build(), body)).hasStatus(HttpStatus.BAD_REQUEST);
        verifyNoInteractions(save);
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
