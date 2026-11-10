package com.yarnshipment.shipment.masterdata.adapter.in.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.yarnshipment.shipment.TestTokens;
import com.yarnshipment.shipment.masterdata.application.port.in.account.ListUserNamesUseCase;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.HttpStatus;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.test.web.servlet.assertj.MvcTestResult;

/** Kullanıcı adı ucunun sözleşmesi: denetim görüntüleyen roller okur, yanıtta yalnızca kimlik ve ad vardır. */
@WebMvcTest(controllers = UserNameController.class)
class UserNameControllerTest {

    private static final UUID USER_ID = UUID.fromString("0192f0a0-0000-7000-8000-0000000000aa");
    private static final String URI = "/api/v1/master-data/user-names?ids=" + USER_ID;

    private final MockMvcTester mvc;

    @MockitoBean
    private ListUserNamesUseCase listUserNames;

    @MockitoBean
    private JwtDecoder jwtDecoder;

    @Autowired
    UserNameControllerTest(MockMvcTester mvc) {
        this.mvc = mvc;
    }

    @Test
    @DisplayName("Denetim görüntüleyen rol kimliği okunur ada çevirir; yanıt önbelleğe alınmaz, telefon ve e-posta yok")
    void shouldResolveNamesForAuditViewers() {
        when(listUserNames.listUserNames(any())).thenReturn(Map.of(USER_ID, "Ayşe Demir"));

        MvcTestResult result = get(TestTokens.token("quality").build());

        assertThat(result).hasStatus(HttpStatus.OK);
        assertThat(result).bodyJson().extractingPath("$.items[0].name").isEqualTo("Ayşe Demir");
        assertThat(result).headers().hasValue("Cache-Control", "no-store");
        assertThat(result).bodyText().doesNotContain("phone").doesNotContain("email");
    }

    @Test
    @DisplayName("Denetim izni olmayan roller 403 alır")
    void shouldForbidOthers() {
        assertThat(get(TestTokens.subcontractorToken())).hasStatus(HttpStatus.FORBIDDEN);
    }

    private MvcTestResult get(Jwt jwt) {
        return mvc.get()
                .uri(URI)
                .with(SecurityMockMvcRequestPostProcessors.authentication(TestTokens.CONVERTER.convert(jwt)))
                .exchange();
    }
}
