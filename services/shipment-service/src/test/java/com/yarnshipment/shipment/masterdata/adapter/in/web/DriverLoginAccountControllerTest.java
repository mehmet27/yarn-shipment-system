package com.yarnshipment.shipment.masterdata.adapter.in.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import com.yarnshipment.shipment.TestTokens;
import com.yarnshipment.shipment.masterdata.application.port.in.ChangeDriverStatusUseCase;
import com.yarnshipment.shipment.masterdata.application.port.in.CreateDriverUseCase;
import com.yarnshipment.shipment.masterdata.application.port.in.UpdateDriverUseCase;
import com.yarnshipment.shipment.masterdata.application.port.in.account.CreateDriverLoginAccountUseCase;
import com.yarnshipment.shipment.masterdata.application.port.in.account.LoginAccountRequest;
import com.yarnshipment.shipment.masterdata.domain.model.LoginAccountExistsException;
import com.yarnshipment.shipment.masterdata.domain.model.LoginAccountUnavailableException;
import com.yarnshipment.shipment.masterdata.domain.model.SmsUnavailableException;
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

/** Şoför giriş hesabı ucunun sözleşmesi: izin, istek doğrulama, önbellek ve hata eşlemesi. */
@WebMvcTest(controllers = DriverManagementController.class)
class DriverLoginAccountControllerTest {

    private static final UUID DRIVER = UUID.fromString("0192f0a0-0000-7000-8000-000000000004");
    private static final String DRIVER_URI = "/api/v1/master-data/drivers/" + DRIVER + "/login-account";
    private static final String BODY = "{\"username\":\"ali.yilmaz\",\"phone\":\"0532 123 45 67\","
            + "\"siteIds\":[\"0192f0a0-0000-7000-8000-000000000001\"]}";

    private final MockMvcTester mvc;

    @MockitoBean
    private CreateDriverLoginAccountUseCase driverAccount;

    @MockitoBean
    private CreateDriverUseCase createDriver;

    @MockitoBean
    private UpdateDriverUseCase updateDriver;

    @MockitoBean
    private ChangeDriverStatusUseCase driverStatus;

    @MockitoBean
    private JwtDecoder jwtDecoder;

    @Autowired
    DriverLoginAccountControllerTest(MockMvcTester mvc) {
        this.mvc = mvc;
    }

    @Test
    @DisplayName("Şoför hesabı 201 döner, yanıtta şifre ya da gövde yoktur ve önbelleğe alınmaz")
    void shouldCreateDriverAccount() {
        MvcTestResult result = post(DRIVER_URI, BODY, TestTokens.token("admin").build());

        assertThat(result).hasStatus(HttpStatus.CREATED);
        assertThat(result).bodyText().isEmpty();
        assertThat(result).headers().hasValue("Cache-Control", "no-store");
        ArgumentCaptor<LoginAccountRequest> request = ArgumentCaptor.forClass(LoginAccountRequest.class);
        verify(driverAccount).createDriverLoginAccount(any(), eq(DRIVER), request.capture());
        assertThat(request.getValue().username()).isEqualTo("ali.yilmaz");
        assertThat(request.getValue().phone()).isEqualTo("0532 123 45 67");
        assertThat(request.getValue().email()).isNull();
        assertThat(request.getValue().siteIds()).hasSize(1);
    }

    @Test
    @DisplayName("MASTER_DATA_MANAGE izni olmayan rol 403 alır ve use case çağrılmaz")
    void shouldForbidWarehouse() {
        MvcTestResult result =
                post(DRIVER_URI, BODY, TestTokens.token("warehouse").build());

        assertThat(result).hasStatus(HttpStatus.FORBIDDEN);
        verifyNoInteractions(driverAccount);
    }

    @Test
    @DisplayName("Biçimi bozuk giriş adı, eksik telefon ve boş lokasyon listesi şemadan 400 alır")
    void shouldRejectInvalidBody() {
        Jwt admin = TestTokens.token("admin").build();

        MvcTestResult badName = post(
                DRIVER_URI, "{\"username\":\"A B\",\"phone\":\"05321234567\",\"siteIds\":[\"" + DRIVER + "\"]}", admin);
        MvcTestResult noPhone =
                post(DRIVER_URI, "{\"username\":\"ali.yilmaz\",\"siteIds\":[\"" + DRIVER + "\"]}", admin);
        MvcTestResult noSites =
                post(DRIVER_URI, "{\"username\":\"ali.yilmaz\",\"phone\":\"05321234567\",\"siteIds\":[]}", admin);

        assertThat(badName).hasStatus(HttpStatus.BAD_REQUEST);
        assertThat(noSites).hasStatus(HttpStatus.BAD_REQUEST);
        assertThat(noPhone).hasStatus(HttpStatus.BAD_REQUEST);
        verifyNoInteractions(driverAccount);
    }

    @Test
    @DisplayName("Var olan hesap 409, Keycloak ve SMS hataları 502 koduyla eşlenir")
    void shouldMapErrors() {
        Jwt admin = TestTokens.token("admin").build();
        doThrow(new LoginAccountExistsException("username"))
                .doThrow(new LoginAccountUnavailableException("down"))
                .doThrow(new SmsUnavailableException())
                .when(driverAccount)
                .createDriverLoginAccount(any(), any(), any());

        MvcTestResult exists = post(DRIVER_URI, BODY, admin);
        MvcTestResult unavailable = post(DRIVER_URI, BODY, admin);
        MvcTestResult smsDown = post(DRIVER_URI, BODY, admin);

        assertThat(exists).hasStatus(HttpStatus.CONFLICT);
        assertThat(exists).bodyJson().extractingPath("$.code").isEqualTo("LOGIN_ACCOUNT_EXISTS");
        assertThat(unavailable).hasStatus(HttpStatus.BAD_GATEWAY);
        assertThat(unavailable).bodyJson().extractingPath("$.code").isEqualTo("LOGIN_ACCOUNT_UNAVAILABLE");
        assertThat(smsDown).hasStatus(HttpStatus.BAD_GATEWAY);
        assertThat(smsDown).bodyJson().extractingPath("$.code").isEqualTo("SMS_UNAVAILABLE");
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
