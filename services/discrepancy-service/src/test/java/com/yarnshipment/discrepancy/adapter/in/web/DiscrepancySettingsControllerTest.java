package com.yarnshipment.discrepancy.adapter.in.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.yarnshipment.discrepancy.TestTokens;
import com.yarnshipment.discrepancy.application.port.in.DiscrepancySettings;
import com.yarnshipment.discrepancy.application.port.in.GetDiscrepancySettingsUseCase;
import com.yarnshipment.discrepancy.application.port.in.UpdateDiscrepancySettingsCommand;
import com.yarnshipment.discrepancy.application.port.in.UpdateDiscrepancySettingsUseCase;
import com.yarnshipment.discrepancy.domain.model.InvalidSettingException;
import com.yarnshipment.discrepancy.domain.model.ToleranceBands;
import com.yarnshipment.discrepancy.domain.rule.SettingVersion;
import com.yarnshipment.discrepancy.domain.rule.TimeLimit;
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
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.test.web.servlet.assertj.MvcTestResult;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

/** Ayar uçlarının sözleşmesi, hata kodları ve izin denetimi (yalnızca Admin). */
@WebMvcTest(controllers = DiscrepancySettingsController.class)
class DiscrepancySettingsControllerTest {

    private static final String SETTINGS = "/api/v1/discrepancy-settings";
    private static final int DEFAULT_HOURS = 24;
    private static final int NEW_HOURS = 36;
    private static final String BODY = "{\"greenMaxPct\":\"0.80\",\"yellowMaxPct\":\"2\",\"inTransitMaxHours\":36}";
    private static final Instant FROM = Instant.parse("2026-01-01T00:00:00Z");
    private static final DiscrepancySettings CURRENT = new DiscrepancySettings(
            ToleranceBands.REPORT_DEFAULT,
            TimeLimit.REPORT_DEFAULT,
            List.of(new SettingVersion<>(ToleranceBands.REPORT_DEFAULT, FROM, null, new UUID(0L, 0L))),
            List.of(new SettingVersion<>(TimeLimit.REPORT_DEFAULT, FROM, null, new UUID(0L, 0L))));

    private final MockMvcTester mvc;

    @MockitoBean
    private GetDiscrepancySettingsUseCase getSettings;

    @MockitoBean
    private UpdateDiscrepancySettingsUseCase updateSettings;

    @MockitoBean
    private JwtDecoder jwtDecoder;

    @Autowired
    DiscrepancySettingsControllerTest(MockMvcTester mvc) {
        this.mvc = mvc;
    }

    @Test
    @DisplayName("Ayarlar iki ondalıklı yüzde, saat ve geçmişle döner")
    void shouldReturnSettings() {
        when(getSettings.getSettings()).thenReturn(CURRENT);

        MvcTestResult result = mvc.get().uri(SETTINGS).with(auth(admin())).exchange();

        assertThat(result).hasStatusOk();
        assertThat(result).bodyJson().extractingPath("$.tolerance.greenMaxPct").isEqualTo("0.50");
        assertThat(result).bodyJson().extractingPath("$.tolerance.yellowMaxPct").isEqualTo("1.50");
        assertThat(result).bodyJson().extractingPath("$.inTransitMaxHours").isEqualTo(DEFAULT_HOURS);
        assertThat(result)
                .bodyJson()
                .extractingPath("$.toleranceHistory[0].validFrom")
                .isEqualTo("2026-01-01T00:00:00Z");
    }

    @Test
    @DisplayName("Güncelleme girilen değerleri ve Admin'i use case'e taşır")
    void shouldUpdateSettings() {
        when(updateSettings.updateSettings(any())).thenReturn(CURRENT);

        assertThat(put(BODY, admin())).hasStatusOk();

        ArgumentCaptor<UpdateDiscrepancySettingsCommand> command =
                ArgumentCaptor.forClass(UpdateDiscrepancySettingsCommand.class);
        verify(updateSettings).updateSettings(command.capture());
        assertThat(command.getValue().greenMaxPct()).isEqualByComparingTo(new BigDecimal("0.80"));
        assertThat(command.getValue().yellowMaxPct()).isEqualByComparingTo(new BigDecimal("2"));
        assertThat(command.getValue().inTransitMaxHours()).isEqualTo(NEW_HOURS);
    }

    @Test
    @DisplayName("Kural dışı bant TOLERANCE_BANDS_INVALID, kural dışı süre TIME_LIMIT_INVALID; biçim hatası 400")
    void shouldRejectInvalidSettings() {
        doThrow(new InvalidSettingException("yellowMaxPct"))
                .when(updateSettings)
                .updateSettings(any());
        MvcTestResult bands = put(BODY, admin());
        assertThat(bands).hasStatus(HttpStatus.BAD_REQUEST);
        assertThat(bands).bodyJson().extractingPath("$.code").isEqualTo("TOLERANCE_BANDS_INVALID");

        doThrow(new InvalidSettingException("inTransitMaxHours"))
                .when(updateSettings)
                .updateSettings(any());
        MvcTestResult hours = put(BODY, admin());
        assertThat(hours).bodyJson().extractingPath("$.code").isEqualTo("TIME_LIMIT_INVALID");

        assertThat(put(BODY.replace("\"0.80\"", "\"%0,8\""), admin())).hasStatus(HttpStatus.BAD_REQUEST);
        assertThat(put(BODY.replace("36", "0"), admin())).hasStatus(HttpStatus.BAD_REQUEST);
    }

    @Test
    @DisplayName("Admin dışındaki roller ayarları göremez ve değiştiremez")
    void shouldAllowOnlyAdmin() {
        Jwt quality = TestTokens.token("quality").claim("amr", List.of("otp")).build();
        Jwt manager = TestTokens.token("manager").build();

        assertThat(mvc.get().uri(SETTINGS).with(auth(manager)).exchange()).hasStatus(HttpStatus.FORBIDDEN);
        assertThat(put(BODY, quality)).hasStatus(HttpStatus.FORBIDDEN);
        assertThat(put(BODY, TestTokens.token("warehouse").build())).hasStatus(HttpStatus.FORBIDDEN);
        verifyNoInteractions(getSettings, updateSettings);
    }

    private static Jwt admin() {
        return TestTokens.token("admin").claim("amr", List.of("otp")).build();
    }

    private static RequestPostProcessor auth(Jwt jwt) {
        return SecurityMockMvcRequestPostProcessors.authentication(TestTokens.CONVERTER.convert(jwt));
    }

    private MvcTestResult put(String body, Jwt jwt) {
        return mvc.put()
                .uri(SETTINGS)
                .contentType(MediaType.APPLICATION_JSON)
                .content(body)
                .with(auth(jwt))
                .with(SecurityMockMvcRequestPostProcessors.csrf())
                .exchange();
    }
}
