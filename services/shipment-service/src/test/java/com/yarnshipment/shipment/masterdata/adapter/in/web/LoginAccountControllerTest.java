package com.yarnshipment.shipment.masterdata.adapter.in.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.yarnshipment.shipment.TestTokens;
import com.yarnshipment.shipment.masterdata.application.port.in.ChangeLoginAccountRoleUseCase;
import com.yarnshipment.shipment.masterdata.application.port.in.CreateStaffLoginAccountUseCase;
import com.yarnshipment.shipment.masterdata.application.port.in.ListLoginAccountsUseCase;
import com.yarnshipment.shipment.masterdata.application.port.in.MasterDataPage;
import com.yarnshipment.shipment.masterdata.application.port.in.MasterDataQuery;
import com.yarnshipment.shipment.masterdata.domain.model.LoginAccountSummary;
import com.yarnshipment.shipment.masterdata.domain.model.LoginAccountUnavailableException;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.HttpStatus;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.test.web.servlet.assertj.MvcTestResult;

/** Giriş hesabı listesi ucunun sözleşmesi: yalnızca yönetici, şifresiz yanıt, sayfa ve hata eşlemesi. */
@WebMvcTest(controllers = LoginAccountController.class)
class LoginAccountControllerTest {

    private static final int PAGE_SIZE = 20;
    private static final String URI = "/api/v1/master-data/login-accounts";
    private static final LoginAccountSummary DRIVER = new LoginAccountSummary(
            "u-2",
            "ali.yilmaz",
            "Ali Yılmaz",
            "ali@ornek.com",
            "***4567",
            List.of("DRIVER"),
            true,
            true,
            Instant.parse("2026-01-01T00:00:00Z"));

    private final MockMvcTester mvc;

    @MockitoBean
    private ListLoginAccountsUseCase listLoginAccounts;

    @MockitoBean
    private CreateStaffLoginAccountUseCase createStaff;

    @MockitoBean
    private ChangeLoginAccountRoleUseCase changeRole;

    @MockitoBean
    private JwtDecoder jwtDecoder;

    @Autowired
    LoginAccountControllerTest(MockMvcTester mvc) {
        this.mvc = mvc;
    }

    @Test
    @DisplayName("Yönetici listeyi alır; yanıtta giriş adı, rol ve durum var, şifre yok ve önbelleğe alınmaz")
    void shouldListAccountsForAdmin() {
        when(listLoginAccounts.listLoginAccounts(any()))
                .thenReturn(new MasterDataPage<>(List.of(DRIVER), 0, PAGE_SIZE, 1));

        MvcTestResult result = get(URI + "?q=ali", TestTokens.token("admin").build());

        assertThat(result).hasStatus(HttpStatus.OK);
        assertThat(result).bodyJson().extractingPath("$.items[0].username").isEqualTo("ali.yilmaz");
        assertThat(result).bodyJson().extractingPath("$.items[0].fullName").isEqualTo("Ali Yılmaz");
        assertThat(result).bodyJson().extractingPath("$.items[0].roles[0]").isEqualTo("DRIVER");
        assertThat(result).bodyJson().extractingPath("$.items[0].enabled").isEqualTo(true);
        assertThat(result)
                .bodyJson()
                .extractingPath("$.items[0].passwordChangeRequired")
                .isEqualTo(true);
        assertThat(result).bodyJson().extractingPath("$.page.totalElements").isEqualTo(1);
        assertThat(result).headers().hasValue("Cache-Control", "no-store");
        assertThat(result).bodyText().doesNotContain("temporaryPassword").doesNotContain("credentials");
        ArgumentCaptor<MasterDataQuery> query = ArgumentCaptor.forClass(MasterDataQuery.class);
        verify(listLoginAccounts).listLoginAccounts(query.capture());
        assertThat(query.getValue().text()).isEqualTo("ali");
        assertThat(query.getValue().page()).isZero();
    }

    @Test
    @DisplayName("USER_MANAGE izni olmayan roller 403 alır ve use case çağrılmaz")
    void shouldForbidNonAdmins() {
        MvcTestResult warehouse = get(URI, TestTokens.token("warehouse").build());
        MvcTestResult manager = get(URI, TestTokens.token("manager").build());
        MvcTestResult driver = get(URI, TestTokens.driverToken());

        assertThat(warehouse).hasStatus(HttpStatus.FORBIDDEN);
        assertThat(manager).hasStatus(HttpStatus.FORBIDDEN);
        assertThat(driver).hasStatus(HttpStatus.FORBIDDEN);
        verifyNoInteractions(listLoginAccounts);
    }

    @Test
    @DisplayName("Sayfa boyu sınır dışındaysa 400, Keycloak'a ulaşılamazsa 502 LOGIN_ACCOUNT_UNAVAILABLE")
    void shouldMapErrors() {
        Jwt admin = TestTokens.token("admin").build();
        when(listLoginAccounts.listLoginAccounts(any())).thenThrow(new LoginAccountUnavailableException("down"));

        MvcTestResult tooLarge = get(URI + "?size=500", admin);
        MvcTestResult unavailable = get(URI, admin);

        assertThat(tooLarge).hasStatus(HttpStatus.BAD_REQUEST);
        assertThat(unavailable).hasStatus(HttpStatus.BAD_GATEWAY);
        assertThat(unavailable).bodyJson().extractingPath("$.code").isEqualTo("LOGIN_ACCOUNT_UNAVAILABLE");
    }

    private MvcTestResult get(String uri, Jwt jwt) {
        return mvc.get()
                .uri(uri)
                .with(SecurityMockMvcRequestPostProcessors.authentication(TestTokens.CONVERTER.convert(jwt)))
                .exchange();
    }
}
