package com.yarnshipment.shipment.masterdata.adapter.in.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.yarnshipment.shipment.TestTokens;
import com.yarnshipment.shipment.masterdata.application.port.in.ChangeDriverStatusUseCase;
import com.yarnshipment.shipment.masterdata.application.port.in.CreateDriverLoginAccountUseCase;
import com.yarnshipment.shipment.masterdata.application.port.in.CreateDriverUseCase;
import com.yarnshipment.shipment.masterdata.application.port.in.UpdateDriverUseCase;
import com.yarnshipment.shipment.masterdata.domain.model.Driver;
import com.yarnshipment.shipment.masterdata.domain.model.DriverDetails;
import com.yarnshipment.shipment.masterdata.domain.model.InvalidMasterDataException;
import com.yarnshipment.shipment.masterdata.domain.model.MasterDataNotFoundException;
import com.yarnshipment.shipment.masterdata.domain.model.MasterDataStatus;
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
@WebMvcTest(controllers = DriverManagementController.class)
class DriverManagementControllerTest {

    private static final String KEY = "0192f0a0-0000-7000-8000-0000000000dd";
    private static final UUID ID = UUID.fromString("0192f0a0-0000-7000-8000-000000000003");
    private static final String DRIVERS = "/api/v1/master-data/drivers";

    private final MockMvcTester mvc;

    @MockitoBean
    private CreateDriverUseCase createDriver;

    @MockitoBean
    private UpdateDriverUseCase updateDriver;

    @MockitoBean
    private ChangeDriverStatusUseCase driverStatus;

    @MockitoBean
    private CreateDriverLoginAccountUseCase driverLoginAccount;

    @MockitoBean
    private JwtDecoder jwtDecoder;

    @Autowired
    DriverManagementControllerTest(MockMvcTester mvc) {
        this.mvc = mvc;
    }

    @Test
    @DisplayName("Alan kuralı ihlali INVALID_MASTER_DATA ile 400 döner")
    void shouldMapInvalidMasterData() throws Exception {
        // given
        when(createDriver.createDriver(any(), any())).thenThrow(new InvalidMasterDataException("fullName", "blank"));

        // when
        MvcTestResult result = post(admin(), DRIVERS, "{\"fullName\": \"  \"}");

        // then
        assertThat(result).hasStatus(HttpStatus.BAD_REQUEST);
        assertThat(result).bodyJson().extractingPath("$.code").isEqualTo("INVALID_MASTER_DATA");
        assertThat(result).bodyJson().extractingPath("$.errors[0].field").isEqualTo("fullName");
    }

    @Test
    @DisplayName("Şoför fasoncusuyla eklenir ve güncellenir")
    void shouldCreateAndUpdateDriver() throws Exception {
        // given
        UUID driverId = UUID.randomUUID();
        DriverDetails details = new DriverDetails("Ali Yılmaz", ID);
        when(createDriver.createDriver(any(), eq(details))).thenReturn(new Driver(driverId, "Ali Yılmaz", ID));
        when(updateDriver.updateDriver(any(), eq(driverId), eq(details)))
                .thenReturn(new Driver(driverId, "Ali Yılmaz", ID));
        String body = "{\"fullName\": \"Ali Yılmaz\", \"subcontractorId\": \"" + ID + "\"}";

        // when
        MvcTestResult created = post(admin(), DRIVERS, body);
        MvcTestResult updated = send(mvc.put().uri(DRIVERS + "/" + driverId), admin(), body);

        // then
        assertThat(created).hasStatus(HttpStatus.CREATED);
        assertThat(created).bodyJson().extractingPath("$.subcontractorId").isEqualTo(ID.toString());
        assertThat(updated).hasStatusOk();
        assertThat(updated).bodyJson().extractingPath("$.fullName").isEqualTo("Ali Yılmaz");
    }

    @Test
    @DisplayName("Pasife alınan kayıt yoksa 404 döner")
    void shouldMapNotFoundOnDeactivate() throws Exception {
        // given
        doThrow(new MasterDataNotFoundException("Driver", ID))
                .when(driverStatus)
                .changeDriverStatus(any(), eq(ID), eq(MasterDataStatus.INACTIVE));

        // when
        MvcTestResult result = send(mvc.post().uri(DRIVERS + "/" + ID + "/deactivate"), admin(), null);

        // then
        assertThat(result).hasStatus(HttpStatus.NOT_FOUND);
    }

    @Test
    @DisplayName("Şoför rolü 403 alır; pasife alma ve etkinleştirme 204 döner")
    void shouldForbidDriverRoleAndToggleStatus() throws Exception {
        MvcTestResult forbidden = post(TestTokens.driverToken(), DRIVERS, "{\"fullName\": \"X\"}");
        MvcTestResult off = send(mvc.post().uri(DRIVERS + "/" + ID + "/deactivate"), admin(), null);
        MvcTestResult on = send(mvc.post().uri(DRIVERS + "/" + ID + "/activate"), admin(), null);

        assertThat(forbidden).hasStatus(HttpStatus.FORBIDDEN);
        assertThat(off).hasStatus(HttpStatus.NO_CONTENT);
        assertThat(on).hasStatus(HttpStatus.NO_CONTENT);
        verifyNoInteractions(createDriver);
        verify(driverStatus).changeDriverStatus(any(), eq(ID), eq(MasterDataStatus.INACTIVE));
        verify(driverStatus).changeDriverStatus(any(), eq(ID), eq(MasterDataStatus.ACTIVE));
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
