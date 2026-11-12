package com.yarnshipment.shipment.masterdata.adapter.in.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import com.yarnshipment.shipment.TestTokens;
import com.yarnshipment.shipment.masterdata.application.port.in.account.ChangeLoginAccountRoleUseCase;
import com.yarnshipment.shipment.masterdata.application.port.in.account.CreateStaffLoginAccountUseCase;
import com.yarnshipment.shipment.masterdata.application.port.in.account.ListLoginAccountsUseCase;
import com.yarnshipment.shipment.masterdata.application.port.in.account.StaffAccountRequest;
import com.yarnshipment.shipment.masterdata.domain.model.InvalidMasterDataException;
import com.yarnshipment.shipment.masterdata.domain.model.LoginAccountExistsException;
import com.yarnshipment.shipment.masterdata.domain.model.LoginAccountNotFoundException;
import com.yarnshipment.shipment.masterdata.domain.model.LoginAccountRoleLockedException;
import com.yarnshipment.shipment.masterdata.domain.model.StaffRole;
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

/** Fabrika içi hesap açma ve rol değiştirme uçlarının sözleşmesi: yalnızca yönetici, şifresiz, hata eşlemesi. */
@WebMvcTest(controllers = LoginAccountController.class)
class StaffLoginAccountControllerTest {

    private static final String URI = "/api/v1/master-data/login-accounts";
    private static final UUID USER = UUID.fromString("0192f0a0-0000-7000-8000-000000000101");
    private static final String STAFF_BODY = "{\"username\":\"depo2\",\"fullName\":\"Ayşe Depo\","
            + "\"role\":\"WAREHOUSE\",\"password\":\"Gecici-Sifre-1\"}";
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
    StaffLoginAccountControllerTest(MockMvcTester mvc) {
        this.mvc = mvc;
    }

    @Test
    @DisplayName("Yönetici fabrika içi hesap açar: 201, gövdede şifre yok, istek use case'e rol ve şifreyle gider")
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
        assertThat(request.getValue().password()).isEqualTo("Gecici-Sifre-1");
    }

    @Test
    @DisplayName("Fabrika içi hesap açma: admin dışı 403; şoför rolü, bozuk giriş adı ve kısa şifre 400; var olan 409")
    void shouldGuardStaffAccountCreation() {
        Jwt admin = TestTokens.token("admin").build();
        MvcTestResult forbidden = send(
                mvc.post().uri(URI), STAFF_BODY, TestTokens.token("warehouse").build());
        MvcTestResult driverRole = send(mvc.post().uri(URI), STAFF_BODY.replace("WAREHOUSE", "DRIVER"), admin);
        MvcTestResult badName = send(mvc.post().uri(URI), STAFF_BODY.replace("depo2", "A B"), admin);
        MvcTestResult shortPassword = send(mvc.post().uri(URI), STAFF_BODY.replace("Gecici-Sifre-1", "kisa"), admin);
        verifyNoInteractions(createStaff);
        doThrow(new LoginAccountExistsException("username")).when(createStaff).createStaffLoginAccount(any(), any());
        MvcTestResult exists = send(mvc.post().uri(URI), STAFF_BODY, admin);

        assertThat(forbidden).hasStatus(HttpStatus.FORBIDDEN);
        assertThat(driverRole).hasStatus(HttpStatus.BAD_REQUEST);
        assertThat(badName).hasStatus(HttpStatus.BAD_REQUEST);
        assertThat(shortPassword).hasStatus(HttpStatus.BAD_REQUEST);
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
        verify(changeRole).changeLoginAccountRole(any(), eq(USER), eq(StaffRole.QUALITY), isNull());
        doThrow(new InvalidMasterDataException("userId", "own account"))
                .doThrow(new LoginAccountNotFoundException())
                .doThrow(new LoginAccountRoleLockedException())
                .when(changeRole)
                .changeLoginAccountRole(any(), any(), any(), any());
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
}
