package com.yarnshipment.discrepancy.adapter.out.evidence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.yarnshipment.discrepancy.application.port.out.EvidenceLookupPort.EvidenceRef;
import com.yarnshipment.discrepancy.domain.claim.EvidenceServiceUnavailableException;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.test.web.client.match.MockRestRequestMatchers;
import org.springframework.test.web.client.response.MockRestResponseCreators;
import org.springframework.web.client.RestClient;

class EvidenceServiceClientTest {

    private static final UUID EVIDENCE = UUID.fromString("0192f0a0-0000-7000-8000-0000000c0003");
    private static final UUID SHIPMENT = UUID.fromString("0192f0a0-0000-7000-8000-000000000301");
    private static final String URL = "http://evidence/api/v1/evidence/" + EVIDENCE;

    private final RestClient.Builder builder = RestClient.builder().baseUrl("http://evidence");
    private final MockRestServiceServer server =
            MockRestServiceServer.bindTo(builder).build();
    private final EvidenceServiceClient client = new EvidenceServiceClient(builder);

    @Test
    @DisplayName("Kanıtın sevkiyatı ve türü okunur; görünmeyen kanıt yok sayılır")
    void shouldReadEvidence() {
        server.expect(MockRestRequestMatchers.requestTo(URL))
                .andRespond(MockRestResponseCreators.withSuccess(
                        "{\"id\":\"" + EVIDENCE + "\",\"shipmentId\":\"" + SHIPMENT
                                + "\",\"evidenceType\":\"QUALITY_CLAIM\",\"sizeBytes\":10}",
                        MediaType.APPLICATION_JSON));
        server.expect(MockRestRequestMatchers.requestTo(URL))
                .andRespond(MockRestResponseCreators.withStatus(HttpStatus.NOT_FOUND));

        assertThat(client.findEvidence(EVIDENCE)).contains(new EvidenceRef(EVIDENCE, SHIPMENT, "QUALITY_CLAIM"));
        assertThat(client.findEvidence(EVIDENCE)).isEmpty();
    }

    @Test
    @DisplayName("Kanıt servisi hata verirse doğrulama yapılamaz")
    void shouldFailWhenServiceIsDown() {
        server.expect(MockRestRequestMatchers.requestTo(URL))
                .andRespond(MockRestResponseCreators.withStatus(HttpStatus.BAD_GATEWAY));

        assertThatThrownBy(() -> client.findEvidence(EVIDENCE)).isInstanceOf(EvidenceServiceUnavailableException.class);
    }
}
