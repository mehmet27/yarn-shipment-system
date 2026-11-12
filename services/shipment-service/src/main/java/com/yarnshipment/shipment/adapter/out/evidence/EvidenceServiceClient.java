package com.yarnshipment.shipment.adapter.out.evidence;

import com.yarnshipment.common.security.web.TokenRelayInterceptor;
import com.yarnshipment.moduleapi.ModuleUnavailableException;
import com.yarnshipment.moduleapi.evidence.EvidenceQueryApi;
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
 * {@link EvidenceQueryApi}'nin evidence-service REST istemcisi (tek süreçe geçişe kadar geçici).
 * İsteyenin token'ı olduğu gibi iletilir; evidence-service izni ve görünürlüğü kendisi denetler (SEC-01).
 * 403 ve 404 "kanıt yok" demektir.
 */
@Component
final class EvidenceServiceClient implements EvidenceQueryApi {

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
    public Optional<EvidenceSummary> findVisibleEvidence(UUID evidenceId) {
        try {
            EvidenceView view = client.get()
                    .uri("/api/v1/evidence/{id}", evidenceId)
                    .retrieve()
                    .body(EvidenceView.class);
            if (view == null) {
                throw new ModuleUnavailableException("Empty evidence response");
            }
            return Optional.of(new EvidenceSummary(view.id(), view.shipmentId(), view.evidenceType(), view.status()));
        } catch (RestClientResponseException exception) {
            if (exception.getStatusCode().isSameCodeAs(HttpStatus.NOT_FOUND)
                    || exception.getStatusCode().isSameCodeAs(HttpStatus.FORBIDDEN)) {
                return Optional.empty();
            }
            throw new ModuleUnavailableException("Evidence service answered " + exception.getStatusCode());
        } catch (RestClientException exception) {
            throw new ModuleUnavailableException("Evidence service call failed", exception);
        }
    }

    private record EvidenceView(UUID id, UUID shipmentId, String evidenceType, String status) {}
}
