package com.yarnshipment.shipment.masterdata.adapter.in.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.yarnshipment.shipment.TestTokens;
import com.yarnshipment.shipment.masterdata.application.port.in.ListDriversUseCase;
import com.yarnshipment.shipment.masterdata.application.port.in.ListSitesUseCase;
import com.yarnshipment.shipment.masterdata.application.port.in.ListSubcontractorsUseCase;
import com.yarnshipment.shipment.masterdata.application.port.in.ListVehiclesUseCase;
import com.yarnshipment.shipment.masterdata.application.port.in.MasterDataPage;
import com.yarnshipment.shipment.masterdata.application.port.in.MasterDataQuery;
import com.yarnshipment.shipment.masterdata.domain.model.Driver;
import java.util.List;
import java.util.UUID;
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

/** Ana veri liste uçlarının sözleşmesi ve izin denetimi. */
@WebMvcTest(controllers = MasterDataController.class)
class MasterDataControllerTest {

    private static final UUID DRIVER_ID = UUID.fromString("0192f0a0-0000-7000-8000-000000000004");
    private static final UUID SUBCONTRACTOR_ID = UUID.fromString("0192f0a0-0000-7000-8000-000000000003");

    private static final int SIZE = 20;

    private final MockMvcTester mvc;

    @MockitoBean
    private ListSubcontractorsUseCase subcontractors;

    @MockitoBean
    private ListDriversUseCase drivers;

    @MockitoBean
    private ListVehiclesUseCase vehicles;

    @MockitoBean
    private ListSitesUseCase sites;

    @MockitoBean
    private JwtDecoder jwtDecoder;

    @Autowired
    MasterDataControllerTest(MockMvcTester mvc) {
        this.mvc = mvc;
    }

    @Test
    @DisplayName("Şoför listesi sayfa bilgisiyle döner; fasoncusuz şoförde alan hiç yoktur")
    void shouldListDriversWithPageMetadata() throws Exception {
        // given
        when(drivers.listDrivers(any(), any()))
                .thenReturn(new MasterDataPage<>(
                        List.of(
                                new Driver(DRIVER_ID, "Ali Yılmaz", SUBCONTRACTOR_ID, true, "Örnek Tekstil"),
                                new Driver(UUID.randomUUID(), "Serbest", null)),
                        0,
                        SIZE,
                        2));

        // when
        MvcTestResult result = get("/api/v1/master-data/drivers?q=ali&subcontractorId=" + SUBCONTRACTOR_ID);

        // then
        assertThat(result).hasStatusOk();
        assertThat(result).bodyJson().extractingPath("$.items[0].fullName").isEqualTo("Ali Yılmaz");
        assertThat(result)
                .bodyJson()
                .extractingPath("$.items[0].subcontractorId")
                .isEqualTo(SUBCONTRACTOR_ID.toString());
        assertThat(result)
                .bodyJson()
                .extractingPath("$.items[0].subcontractorTitle")
                .isEqualTo("Örnek Tekstil");
        assertThat(result).bodyJson().doesNotHavePath("$.items[1].subcontractorId");
        assertThat(result).bodyJson().doesNotHavePath("$.items[1].subcontractorTitle");
        assertThat(result).bodyJson().extractingPath("$.page.totalPages").isEqualTo(1);
        ArgumentCaptor<MasterDataQuery> query = ArgumentCaptor.forClass(MasterDataQuery.class);
        verify(drivers).listDrivers(query.capture(), any());
        assertThat(query.getValue().text()).isEqualTo("ali");
    }

    @Test
    @DisplayName("Araç ve fasoncu listeleri de yetkili kullanıcıya açıktır")
    void shouldListVehiclesAndSubcontractors() {
        // given
        when(vehicles.listVehicles(any())).thenReturn(new MasterDataPage<>(List.of(), 0, SIZE, 0));
        when(subcontractors.listSubcontractors(any())).thenReturn(new MasterDataPage<>(List.of(), 0, SIZE, 0));

        // then
        assertThat(get("/api/v1/master-data/vehicles")).hasStatusOk();
        assertThat(get("/api/v1/master-data/subcontractors")).hasStatusOk();
        verify(vehicles).listVehicles(any());
    }

    @Test
    @DisplayName("MASTER_DATA_VIEW izni olmayan rol (şoför) 403 alır")
    void shouldForbidDriverRole() {
        // when
        MvcTestResult result = mvc.get()
                .uri("/api/v1/master-data/sites")
                .with(SecurityMockMvcRequestPostProcessors.authentication(
                        TestTokens.CONVERTER.convert(TestTokens.driverToken())))
                .exchange();

        // then
        assertThat(result).hasStatus(HttpStatus.FORBIDDEN);
    }

    @Test
    @DisplayName("Pasif kayıtları yalnızca yönetici isteyebilir; yönetici istediğinde sorguya iletilir")
    void shouldAllowInactiveOnlyForAdmin() {
        // given
        when(vehicles.listVehicles(any())).thenReturn(new MasterDataPage<>(List.of(), 0, SIZE, 0));
        String uri = "/api/v1/master-data/vehicles?status=ALL";

        // when
        MvcTestResult denied = get(uri);
        MvcTestResult allowed = mvc.get()
                .uri(uri)
                .with(SecurityMockMvcRequestPostProcessors.authentication(
                        TestTokens.CONVERTER.convert(TestTokens.token("admin").build())))
                .exchange();

        // then
        assertThat(denied).hasStatus(HttpStatus.FORBIDDEN);
        assertThat(allowed).hasStatusOk();
        ArgumentCaptor<MasterDataQuery> query = ArgumentCaptor.forClass(MasterDataQuery.class);
        verify(vehicles).listVehicles(query.capture());
        assertThat(query.getValue().includeInactive()).isTrue();
    }

    @Test
    @DisplayName("Sayfa boyu 100'ü geçerse 400 döner")
    void shouldRejectTooLargePageSize() {
        assertThat(get("/api/v1/master-data/vehicles?size=101")).hasStatus(HttpStatus.BAD_REQUEST);
    }

    @Test
    @DisplayName("Token'sız istek 401 alır")
    void shouldRequireAuthentication() {
        assertThat(mvc.get().uri("/api/v1/master-data/sites").exchange()).hasStatus(HttpStatus.UNAUTHORIZED);
    }

    private MvcTestResult get(String uri) {
        Jwt jwt = TestTokens.token("warehouse").build();
        return mvc.get()
                .uri(uri)
                .with(SecurityMockMvcRequestPostProcessors.authentication(TestTokens.CONVERTER.convert(jwt)))
                .exchange();
    }
}
