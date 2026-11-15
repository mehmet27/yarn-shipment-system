package com.yarnshipment.shipment.masterdata.adapter.in.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.yarnshipment.common.security.Permission;
import com.yarnshipment.common.security.Role;
import com.yarnshipment.shipment.TestTokens;
import com.yarnshipment.shipment.masterdata.application.port.in.rolepermission.ListRolePermissionsUseCase;
import com.yarnshipment.shipment.masterdata.application.port.in.rolepermission.ResetRolePermissionsUseCase;
import com.yarnshipment.shipment.masterdata.application.port.in.rolepermission.SaveRolePermissionsUseCase;
import java.util.Map;
import java.util.Set;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.test.web.servlet.assertj.MvcTestResult;

/** Yetkiler ekranı uçları yalnızca yöneticidedir (PERMISSION_MANAGE); müdür bile giremez. */
@WebMvcTest(controllers = RolePermissionController.class)
class RolePermissionControllerTest {

    private static final String URI = "/api/v1/role-permissions";
    private static final String BODY = "{\"permissions\": [\"SHIPMENT_VIEW\"]}";

    private final MockMvcTester mvc;

    @MockitoBean
    private ListRolePermissionsUseCase list;

    @MockitoBean
    private SaveRolePermissionsUseCase save;

    @MockitoBean
    private ResetRolePermissionsUseCase reset;

    @Autowired
    RolePermissionControllerTest(MockMvcTester mvc) {
        this.mvc = mvc;
    }

    @Test
    @DisplayName("Yönetici rollerin izinlerini, varsayılanlarını ve sabit izinleri okur")
    void shouldList() {
        when(list.listRolePermissions()).thenReturn(Map.of(Role.WAREHOUSE, Set.of(Permission.SHIPMENT_VIEW)));

        MvcTestResult result = exchange(mvc.get().uri(URI), "admin", null);

        assertThat(result).hasStatus(HttpStatus.OK);
        assertThat(result).headers().hasValue("Cache-Control", "no-store");
        assertThat(result).bodyJson().extractingPath("$.roles[0].role").isEqualTo("WAREHOUSE");
        assertThat(result).bodyJson().extractingPath("$.roles[0].editable").isEqualTo(true);
        assertThat(result)
                .bodyJson()
                .extractingPath("$.roles[0].permissions[0]")
                .isEqualTo("SHIPMENT_VIEW");
        assertThat(result).bodyJson().extractingPath("$.lockedPermissions[0]").isNotNull();
    }

    @Test
    @DisplayName("Yönetici rolün izinlerini kaydeder")
    void shouldSave() {
        when(list.listRolePermissions()).thenReturn(Map.of());

        MvcTestResult result = exchange(mvc.put().uri(URI + "/WAREHOUSE"), "admin", BODY);

        assertThat(result).hasStatus(HttpStatus.OK);
    }

    @Test
    @DisplayName("Yönetici rolü varsayılana döndürür")
    void shouldReset() {
        when(list.listRolePermissions()).thenReturn(Map.of());

        assertThat(exchange(mvc.post().uri(URI + "/MANAGER/reset"), "admin", null))
                .hasStatus(HttpStatus.OK);
    }

    @Test
    @DisplayName("Müdür, kalite ve depo Yetkiler ekranına giremez")
    void shouldForbidOtherRoles() {
        for (String role : new String[] {"manager", "quality", "warehouse"}) {
            assertThat(exchange(mvc.get().uri(URI), role, null)).hasStatus(HttpStatus.FORBIDDEN);
            assertThat(exchange(mvc.put().uri(URI + "/WAREHOUSE"), role, BODY)).hasStatus(HttpStatus.FORBIDDEN);
        }
        verifyNoInteractions(save);
    }

    private static MvcTestResult exchange(
            MockMvcTester.MockMvcRequestBuilder builder, String role, @Nullable String body) {
        Jwt jwt = TestTokens.token(role).build();
        if (body != null) {
            builder.contentType(MediaType.APPLICATION_JSON).content(body);
        }
        return builder.with(SecurityMockMvcRequestPostProcessors.authentication(TestTokens.CONVERTER.convert(jwt)))
                .exchange();
    }
}
