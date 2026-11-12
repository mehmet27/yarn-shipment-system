package com.yarnshipment.discrepancy.adapter.out.evidence;

import com.yarnshipment.common.security.web.TokenRelayInterceptor;
import com.yarnshipment.moduleapi.ModuleUnavailableException;
import com.yarnshipment.moduleapi.evidence.EvidenceQueryApi;
import java.net.http.HttpClient;
import java.time.Duration;
import java.util.Optional;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

/**
 * {@link EvidenceQueryApi}'nin evidence-service REST istemcisi (tek süreçe geçişe kadar geçici).
 * İsteyenin token'ı olduğu gibi iletilir; kanıt isteyene görünmüyorsa evidence-service 403/404 döner ve kanıt yok
 * sayılır (SEC-01).
 */
@Component
final class EvidenceServiceClient implements EvidenceQueryApi {

    private static final Duration TIMEOUT = Duration.ofSeconds(5);

    private final RestClient client;

    @Autowired
    EvidenceServiceClient(@Value("${yarnshipment.evidence-service.url:http://localhost:8086}") String baseUrl) {
        this(RestClient.builder().baseUrl(baseUrl).requestFactory(requestFactory()));
    }

    /** Testte sahte sunucuya bağlanan kurucu. */
    EvidenceServiceClient(RestClient.Builder builder) {
        this.client = builder.requestInterceptor(new TokenRelayInterceptor()).build();
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
            if (isNotVisible(exception.getStatusCode())) {
                return Optional.empty();
            }
            throw new ModuleUnavailableException("Evidence service answered " + exception.getStatusCode());
        } catch (RestClientException exception) {
            throw new ModuleUnavailableException("Evidence service call failed", exception);
        }
    }

    private static JdkClientHttpRequestFactory requestFactory() {
        HttpClient http = HttpClient.newBuilder().connectTimeout(TIMEOUT).build();
        JdkClientHttpRequestFactory requests = new JdkClientHttpRequestFactory(http);
        requests.setReadTimeout(TIMEOUT);
        return requests;
    }

    private static boolean isNotVisible(HttpStatusCode status) {
        return status.isSameCodeAs(HttpStatus.FORBIDDEN) || status.isSameCodeAs(HttpStatus.NOT_FOUND);
    }

    private record EvidenceView(UUID id, UUID shipmentId, String evidenceType, String status) {}
}
