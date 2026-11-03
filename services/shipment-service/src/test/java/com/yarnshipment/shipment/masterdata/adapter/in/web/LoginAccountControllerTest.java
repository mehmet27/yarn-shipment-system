package com.yarnshipment.shipment.masterdata.adapter.in.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.yarnshipment.shipment.TestTokens;
import com.yarnshipment.shipment.masterdata.application.port.in.ChangeLoginAccountRoleUseCase;
import com.yarnshipment.shipment.masterdata.application.port.in.CreateStaffLoginAccountUseCase;
import com.yarnshipment.shipment.masterdata.application.port.in.ListLoginAccountsUseCase;
import com.yarnshipment.shipment.masterdata.application.port.in.MasterDataPage;
import com.yarnshipment.shipment.masterdata.application.port.in.MasterDataQuery;
import com.yarnshipment.shipment.masterdata.application.port.in.StaffAccountRequest;
import com.yarnshipment.shipment.masterdata.domain.model.InvalidMasterDataException;
import com.yarnshipment.shipment.masterdata.domain.model.LoginAccountExistsException;
import com.yarnshipment.shipment.masterdata.domain.model.LoginAccountNotFoundException;
import com.yarnshipment.shipment.masterdata.domain.model.LoginAccountRoleLockedException;
import com.yarnshipment.shipment.masterdata.domain.model.LoginAccountSummary;
import com.yarnshipment.shipment.masterdata.domain.model.LoginAccountUnavailableException;
import com.yarnshipment.shipment.masterdata.domain.model.StaffRole;
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
            List.of("DRIVER"),
            true,
            true,
            Instant.parse("2026-01-01T00:00:00Z"));

    private static final UUID SITE = UUID.fromString("0192f0a0-0000-7000-8000-000000000001");
    private static final UUID USER = UUID.fromString("0192f0a0-0000-7000-8000-000000000101");
    private static final String STAFF_BODY = "{\"username\":\"depo2\",\"email\":\"depo2@ornek.com\","
            + "\"fullName\":\"Ayşe Depo\",\"role\":\"WAREHOUSE\",\"siteIds\":[\"" + SITE + "\"]}";
    private static final String ROLE_BODY = "{\"role\":\"QUALITY\"}";

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

    @Test
    @DisplayName("Yönetici fabrika içi hesap açar: 201, gövdede şifre yok, istek use case'e rol ve lokasyonla gider")
    void shouldCreateStaffAccount() {
        MvcTestResult result =
                send(mvc.post().uri(URI), STAFF_BODY, TestTokens.token("admin").build());

        assertThat(result).hasStatus(HttpStatus.CREATED);
        assertThat(result).headers().hasValue("Cache-Control", "no-store");
        assertThat(result).bodyText().isEmpty();
        ArgumentCaptor<StaffAccountRequest> request = ArgumentCaptor.forClass(StaffAccountRequest.class);
        verify(createStaff).createStaffLoginAccount(any(), request.capture());
        assertThat(request.getValue().username()).isEqualTo("depo2");
        assertThat(request.getValue().role()).isEqualTo(StaffRole.WAREHOUSE);
        assertThat(request.getValue().siteIds()).containsExactly(SITE);
    }

    @Test
    @DisplayName(
            "Fabrika içi hesap açma: admin dışı 403; şoför rolü, bozuk giriş adı ve boş lokasyon 400; var olan 409")
    void shouldGuardStaffAccountCreation() {
        Jwt admin = TestTokens.token("admin").build();
        MvcTestResult forbidden = send(
                mvc.post().uri(URI), STAFF_BODY, TestTokens.token("warehouse").build());
        MvcTestResult driverRole = send(mvc.post().uri(URI), STAFF_BODY.replace("WAREHOUSE", "DRIVER"), admin);
        MvcTestResult badName = send(mvc.post().uri(URI), STAFF_BODY.replace("depo2", "A B"), admin);
        MvcTestResult noSites = send(mvc.post().uri(URI), STAFF_BODY.replace(SITE.toString(), ""), admin);
        verifyNoInteractions(createStaff);
        doThrow(new LoginAccountExistsException("username")).when(createStaff).createStaffLoginAccount(any(), any());
        MvcTestResult exists = send(mvc.post().uri(URI), STAFF_BODY, admin);

        assertThat(forbidden).hasStatus(HttpStatus.FORBIDDEN);
        assertThat(driverRole).hasStatus(HttpStatus.BAD_REQUEST);
        assertThat(badName).hasStatus(HttpStatus.BAD_REQUEST);
        assertThat(noSites).hasStatus(HttpStatus.BAD_REQUEST);
        assertThat(exists).hasStatus(HttpStatus.CONFLICT);
        assertThat(exists).bodyJson().extractingPath("$.code").isEqualTo("LOGIN_ACCOUNT_EXISTS");
    }

    @Test
    @DisplayName("Rol değiştirme: 204; kendi hesabı 400, bulunamayan 404, kayda bağlı hesap 409, admin dışı 403")
    void shouldChangeRole() {
        Jwt admin = TestTokens.token("admin").build();
        String uri = URI + "/" + USER + "/role";
        MvcTestResult changed = send(mvc.put().uri(uri), ROLE_BODY, admin);
        MvcTestResult forbidden =
                send(mvc.put().uri(uri), ROLE_BODY, TestTokens.token("quality").build());
        MvcTestResult badRole = send(mvc.put().uri(uri), "{\"role\":\"DRIVER\"}", admin);
        verify(changeRole).changeLoginAccountRole(any(), eq(USER), eq(StaffRole.QUALITY));
        doThrow(new InvalidMasterDataException("userId", "own account"))
                .doThrow(new LoginAccountNotFoundException())
                .doThrow(new LoginAccountRoleLockedException())
                .when(changeRole)
                .changeLoginAccountRole(any(), any(), any());
        MvcTestResult own = send(mvc.put().uri(uri), ROLE_BODY, admin);
        MvcTestResult missing = send(mvc.put().uri(uri), ROLE_BODY, admin);
        MvcTestResult locked = send(mvc.put().uri(uri), ROLE_BODY, admin);

        assertThat(changed).hasStatus(HttpStatus.NO_CONTENT);
        assertThat(forbidden).hasStatus(HttpStatus.FORBIDDEN);
        assertThat(badRole).hasStatus(HttpStatus.BAD_REQUEST);
        assertThat(own).hasStatus(HttpStatus.BAD_REQUEST);
        assertThat(missing).hasStatus(HttpStatus.NOT_FOUND);
        assertThat(missing).bodyJson().extractingPath("$.code").isEqualTo("LOGIN_ACCOUNT_NOT_FOUND");
        assertThat(locked).hasStatus(HttpStatus.CONFLICT);
        assertThat(locked).bodyJson().extractingPath("$.code").isEqualTo("LOGIN_ACCOUNT_ROLE_LOCKED");
    }

    private MvcTestResult send(MockMvcTester.MockMvcRequestBuilder builder, String body, Jwt jwt) {
        return builder.contentType(MediaType.APPLICATION_JSON)
                .content(body)
                .with(SecurityMockMvcRequestPostProcessors.authentication(TestTokens.CONVERTER.convert(jwt)))
                .with(SecurityMockMvcRequestPostProcessors.csrf())
                .exchange();
    }

    private MvcTestResult get(String uri, Jwt jwt) {
        return mvc.get()
                .uri(uri)
                .with(SecurityMockMvcRequestPostProcessors.authentication(TestTokens.CONVERTER.convert(jwt)))
                .exchange();
    }
}
