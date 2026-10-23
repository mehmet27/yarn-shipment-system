package com.yarnshipment.shipment.adapter.out.bag;

import com.yarnshipment.common.security.web.TokenRelayInterceptor;
import com.yarnshipment.shipment.application.port.out.BagReservationPort;
import com.yarnshipment.shipment.domain.bag.BagReservationException;
import com.yarnshipment.shipment.domain.bag.BagReservationException.Reason;
import com.yarnshipment.shipment.domain.bag.BagServiceUnavailableException;
import com.yarnshipment.shipment.domain.bag.ReservedBag;
import java.net.http.HttpClient;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.jspecify.annotations.Nullable;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

/**
 * {@link BagReservationPort}'un bag-service REST istemcisi. İsteyenin token'ı olduğu gibi iletilir; bag-service
 * izni ve lokasyon kapsamını kendisi yeniden denetler (SEC-01). Hata gövdesindeki {@code code} alanı alan hatasına
 * çevrilir; geri kalan her şey {@link BagServiceUnavailableException} olur.
 */
@Component
final class BagServiceClient implements BagReservationPort {

    private static final Duration TIMEOUT = Duration.ofSeconds(5);
    private static final String RESERVE_PATH = "/api/v1/bags/reservations";
    private static final String RELEASE_PATH = "/api/v1/bags/reservations/release";

    private final RestClient client;

    BagServiceClient(@Value("${yarnshipment.bag-service.url:http://localhost:8084}") String baseUrl) {
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
    public List<ReservedBag> reserve(UUID shipmentId, List<String> uniqueKeys) {
        try {
            ReserveResponse response = client.post()
                    .uri(RESERVE_PATH)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(new ReserveRequest(shipmentId, uniqueKeys))
                    .retrieve()
                    .body(ReserveResponse.class);
            if (response == null) {
                throw new BagServiceUnavailableException("Empty reservation response");
            }
            return response.bags().stream()
                    .map(bag -> new ReservedBag(bag.id(), bag.uniqueKey(), bag.lotNo(), bag.qualityGradeCode()))
                    .toList();
        } catch (RestClientResponseException exception) {
            throw translate(exception);
        } catch (RestClientException exception) {
            throw new BagServiceUnavailableException("Bag service call failed", exception);
        }
    }

    @Override
    public void release(UUID shipmentId, String reason) {
        postRelease(Map.of("shipmentId", shipmentId, "reason", reason));
    }

    @Override
    public void releaseBag(UUID shipmentId, String uniqueKey, String reason) {
        postRelease(Map.of("shipmentId", shipmentId, "uniqueKeys", List.of(uniqueKey), "reason", reason));
    }

    private void postRelease(Map<String, Object> body) {
        try {
            client.post()
                    .uri(RELEASE_PATH)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .toBodilessEntity();
        } catch (RestClientResponseException exception) {
            throw new BagServiceUnavailableException(
                    "Bag service rejected the release status=" + exception.getStatusCode(), exception);
        } catch (RestClientException exception) {
            throw new BagServiceUnavailableException("Bag service call failed", exception);
        }
    }

    private static RuntimeException translate(RestClientResponseException exception) {
        HttpStatus status = HttpStatus.resolve(exception.getStatusCode().value());
        if (status == HttpStatus.NOT_FOUND || status == HttpStatus.CONFLICT) {
            Map<String, Object> problem = exception.getResponseBodyAs(new ParameterizedTypeReference<>() {});
            Object code = problem == null ? null : problem.get("code");
            Reason reason = reasonOf(code);
            if (reason != null) {
                return new BagReservationException(reason);
            }
        }
        return new BagServiceUnavailableException("Bag service rejected the call status=" + exception.getStatusCode());
    }

    private static @Nullable Reason reasonOf(@Nullable Object code) {
        return switch (String.valueOf(code)) {
            case "BAG_NOT_FOUND" -> Reason.NOT_FOUND;
            case "BAG_NOT_AVAILABLE" -> Reason.NOT_AVAILABLE;
            case "BAG_ALREADY_RESERVED" -> Reason.ALREADY_RESERVED;
            default -> null;
        };
    }

    private record ReserveRequest(UUID shipmentId, List<String> uniqueKeys) {}

    private record ReserveResponse(List<ReservedBagBody> bags) {}

    private record ReservedBagBody(UUID id, String uniqueKey, String lotNo, String qualityGradeCode) {}
}
