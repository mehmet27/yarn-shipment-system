package com.yarnshipment.shipment.adapter.out.evidence;

import com.yarnshipment.common.security.web.TokenRelayInterceptor;
import com.yarnshipment.shipment.application.port.out.EvidenceLookupPort;
import com.yarnshipment.shipment.domain.evidence.EvidenceReference;
import com.yarnshipment.shipment.domain.evidence.EvidenceServiceUnavailableException;
import java.net.http.HttpClient;
import java.time.Duration;
import java.util.Optional;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

/**
 * {@link EvidenceLookupPort}'un evidence-service REST istemcisi. İsteyenin token'ı olduğu gibi iletilir;
 * evidence-service izni ve sevkiyat görünürlüğünü kendisi yeniden denetler (SEC-01). 403 ve 404 "kanıt yok" demektir.
 */
@Component
final class EvidenceServiceClient implements EvidenceLookupPort {

    private static final Duration TIMEOUT = Duration.ofSeconds(5);

    private final RestClient client;

    EvidenceServiceClient(@Value("${yarnshipment.evidence-service.url:http://localhost:8086}") String baseUrl) {
        HttpClient http = HttpClient.newBuilder().connectTimeout(TIMEOUT).build();
        JdkClientHttpRequestFactory requests = new JdkClientHttpRequestFactory(http);
        requests.setReadTimeout(TIMEOUT);
        this.client = RestClient.builder()
                .baseUrl(baseUrl)
                .requestFactory(requests)
                .requestInterceptor(new TokenRelayInterceptor())
                .build();
    }

    @Override
    public Optional<EvidenceReference> findEvidence(UUID evidenceId) {
        try {
            EvidenceView view = client.get()
                    .uri("/api/v1/evidence/{id}", evidenceId)
                    .retrieve()
                    .body(EvidenceView.class);
            if (view == null) {
                throw new EvidenceServiceUnavailableException("Empty evidence response");
            }
            return Optional.of(new EvidenceReference(view.id(), view.shipmentId(), view.evidenceType()));
        } catch (RestClientResponseException exception) {
            if (exception.getStatusCode().isSameCodeAs(HttpStatus.NOT_FOUND)
                    || exception.getStatusCode().isSameCodeAs(HttpStatus.FORBIDDEN)) {
                return Optional.empty();
            }
            throw new EvidenceServiceUnavailableException("Evidence service answered " + exception.getStatusCode());
        } catch (RestClientException exception) {
            throw new EvidenceServiceUnavailableException("Evidence service call failed", exception);
        }
    }

    private record EvidenceView(UUID id, UUID shipmentId, String evidenceType) {}
}
