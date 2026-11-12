package com.yarnshipment.evidence.adapter.out.shipment;

import com.yarnshipment.common.security.web.TokenRelayInterceptor;
import com.yarnshipment.moduleapi.ModuleUnavailableException;
import com.yarnshipment.moduleapi.shipment.ShipmentAccessApi;
import com.yarnshipment.moduleapi.shipment.ShipmentNotVisibleException;
import java.net.http.HttpClient;
import java.time.Duration;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

/**
 * {@link ShipmentAccessApi}'nin shipment-service REST istemcisi (tek süreçe geçişe kadar geçici).
 * İsteyenin token'ı olduğu gibi iletilir; sevkiyat isteyene görünmüyorsa shipment-service 403/404 döner (SEC-01).
 */
@Component
final class ShipmentServiceClient implements ShipmentAccessApi {

    private static final Duration TIMEOUT = Duration.ofSeconds(5);

    private final RestClient client;

    ShipmentServiceClient(@Value("${yarnshipment.shipment-service.url:http://localhost:8083}") String baseUrl) {
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
    public ShipmentSummary findVisibleShipment(UUID shipmentId) {
        try {
            ShipmentView view = client.get()
                    .uri("/api/v1/shipments/{id}", shipmentId)
                    .retrieve()
                    .body(ShipmentView.class);
            if (view == null) {
                throw new ModuleUnavailableException("Empty shipment response");
            }
            return new ShipmentSummary(shipmentId, view.siteId(), view.status());
        } catch (RestClientResponseException exception) {
            if (isNotVisible(exception.getStatusCode())) {
                throw new ShipmentNotVisibleException(shipmentId);
            }
            throw new ModuleUnavailableException("Shipment service answered " + exception.getStatusCode());
        } catch (RestClientException exception) {
            throw new ModuleUnavailableException("Shipment service call failed", exception);
        }
    }

    private static boolean isNotVisible(HttpStatusCode status) {
        return status.isSameCodeAs(HttpStatus.FORBIDDEN) || status.isSameCodeAs(HttpStatus.NOT_FOUND);
    }

    private record ShipmentView(UUID siteId, String status) {}
}
