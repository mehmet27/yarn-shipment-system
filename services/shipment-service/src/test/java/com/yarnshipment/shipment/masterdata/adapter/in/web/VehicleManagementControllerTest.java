package com.yarnshipment.shipment.masterdata.adapter.in.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.yarnshipment.shipment.TestTokens;
import com.yarnshipment.shipment.masterdata.application.port.in.ChangeVehicleStatusUseCase;
import com.yarnshipment.shipment.masterdata.application.port.in.CreateVehicleUseCase;
import com.yarnshipment.shipment.masterdata.application.port.in.UpdateVehicleUseCase;
import com.yarnshipment.shipment.masterdata.domain.model.MasterDataNotFoundException;
import com.yarnshipment.shipment.masterdata.domain.model.MasterDataStatus;
import com.yarnshipment.shipment.masterdata.domain.model.Vehicle;
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

/** Vehicle yönetim uçlarının sözleşmesi: izin, istek doğrulama ve hata eşlemesi. */
@WebMvcTest(controllers = VehicleManagementController.class)
class VehicleManagementControllerTest {

    private static final String KEY = "0192f0a0-0000-7000-8000-0000000000dd";
    private static final UUID ID = UUID.fromString("0192f0a0-0000-7000-8000-000000000003");
    private static final String URI = "/api/v1/master-data/vehicles";
    private static final String BODY = "{\"plateNo\": \"34ABC123\"}";

    private final MockMvcTester mvc;

    @MockitoBean
    private CreateVehicleUseCase create;

    @MockitoBean
    private UpdateVehicleUseCase update;

    @MockitoBean
    private ChangeVehicleStatusUseCase status;

    @MockitoBean
    private JwtDecoder jwtDecoder;

    @Autowired
    VehicleManagementControllerTest(MockMvcTester mvc) {
        this.mvc = mvc;
    }

    @Test
    @DisplayName("Yönetici ekler; 201 ve eklenen kayıt döner")
    void shouldCreate() throws Exception {
        // given
        when(create.createVehicle(any(), eq("34ABC123"))).thenReturn(new Vehicle(ID, "34ABC123"));

        // when
        MvcTestResult result = post(admin(), BODY);

        // then
        assertThat(result).hasStatus(HttpStatus.CREATED);
        assertThat(result).bodyJson().extractingPath("$.id").isEqualTo(ID.toString());
        assertThat(result).bodyJson().extractingPath("$.plateNo").isEqualTo("34ABC123");
    }

    @Test
    @DisplayName("Yönetici olmayan rol 403 alır ve use case çağrılmaz")
    void shouldForbidNonAdmin() throws Exception {
        MvcTestResult result = post(TestTokens.token("warehouse").build(), BODY);

        assertThat(result).hasStatus(HttpStatus.FORBIDDEN);
        verifyNoInteractions(create);
    }

    @Test
    @DisplayName("Boş alan şemadan 400 alır")
    void shouldRejectEmptyField() throws Exception {
        assertThat(post(admin(), "{\"plateNo\": \"\"}")).hasStatus(HttpStatus.BAD_REQUEST);
        verifyNoInteractions(create);
    }

    @Test
    @DisplayName("Kullanılmış plaka DUPLICATE_PLATE ile 409 döner")
    void shouldMapDuplicatePlate() throws Exception {
        // given
        when(create.createVehicle(any(), any()))
                .thenThrow(new com.yarnshipment.shipment.masterdata.domain.model.DuplicatePlateException("34ABC123"));

        // when
        MvcTestResult result = post(admin(), "{\"plateNo\": \"34 abc 123\"}");

        // then
        assertThat(result).hasStatus(HttpStatus.CONFLICT);
        assertThat(result).bodyJson().extractingPath("$.code").isEqualTo("DUPLICATE_PLATE");
    }

    @Test
    @DisplayName("Biçimsiz plaka INVALID_PLATE ile 400 döner ve alanı gösterir")
    void shouldMapInvalidPlate() throws Exception {
        // given
        when(create.createVehicle(any(), any()))
                .thenThrow(new com.yarnshipment.shipment.masterdata.domain.model.InvalidPlateException("ABC"));

        // when
        MvcTestResult result = post(admin(), "{\"plateNo\": \"abc\"}");

        // then
        assertThat(result).hasStatus(HttpStatus.BAD_REQUEST);
        assertThat(result).bodyJson().extractingPath("$.code").isEqualTo("INVALID_PLATE");
        assertThat(result).bodyJson().extractingPath("$.errors[0].field").isEqualTo("plateNo");
        assertThat(result).bodyJson().extractingPath("$.title").isEqualTo("Plaka geçersiz");
    }

    @Test
    @DisplayName("Güncelleme 200, olmayan kayıt MASTER_DATA_NOT_FOUND ile 404 döner")
    void shouldUpdateAndMapNotFound() throws Exception {
        // given
        when(update.updateVehicle(any(), eq(ID), any())).thenReturn(new Vehicle(ID, "34ABC123"));
        UUID missing = UUID.randomUUID();
        when(update.updateVehicle(any(), eq(missing), any()))
                .thenThrow(new MasterDataNotFoundException("Vehicle", missing));

        // when
        MvcTestResult ok = send(mvc.put().uri(URI + "/" + ID), BODY);
        MvcTestResult notFound = send(mvc.put().uri(URI + "/" + missing), BODY);

        // then
        assertThat(ok).hasStatusOk();
        assertThat(notFound).hasStatus(HttpStatus.NOT_FOUND);
        assertThat(notFound).bodyJson().extractingPath("$.code").isEqualTo("MASTER_DATA_NOT_FOUND");
    }

    @Test
    @DisplayName("Pasife alma ve yeniden etkinleştirme 204 döner ve doğru durumu iletir")
    void shouldDeactivateAndActivate() throws Exception {
        MvcTestResult off = send(mvc.post().uri(URI + "/" + ID + "/deactivate"), null);
        MvcTestResult on = send(mvc.post().uri(URI + "/" + ID + "/activate"), null);

        assertThat(off).hasStatus(HttpStatus.NO_CONTENT);
        assertThat(on).hasStatus(HttpStatus.NO_CONTENT);
        verify(status).changeVehicleStatus(any(), eq(ID), eq(MasterDataStatus.INACTIVE));
        verify(status).changeVehicleStatus(any(), eq(ID), eq(MasterDataStatus.ACTIVE));
    }

    private static Jwt admin() {
        return TestTokens.token("admin").build();
    }

    private MvcTestResult post(Jwt jwt, String body) {
        return exchange(mvc.post().uri(URI).header("Idempotency-Key", KEY), jwt, body);
    }

    private MvcTestResult send(
            MockMvcTester.MockMvcRequestBuilder builder, @org.jspecify.annotations.Nullable String body) {
        return exchange(builder, admin(), body);
    }

    private MvcTestResult exchange(
            MockMvcTester.MockMvcRequestBuilder builder, Jwt jwt, @org.jspecify.annotations.Nullable String body) {
        if (body != null) {
            builder.contentType(MediaType.APPLICATION_JSON).content(body);
        }
        return builder.with(SecurityMockMvcRequestPostProcessors.authentication(TestTokens.CONVERTER.convert(jwt)))
                .exchange();
    }
}
