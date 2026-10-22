package com.yarnshipment.shipment.adapter.out.bag;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.sun.net.httpserver.HttpServer;
import com.yarnshipment.shipment.domain.bag.BagReservationException;
import com.yarnshipment.shipment.domain.bag.BagReservationException.Reason;
import com.yarnshipment.shipment.domain.bag.BagServiceUnavailableException;
import com.yarnshipment.shipment.domain.bag.ReservedBag;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** bag-service istemcisi, gerçek bir HTTP sunucusuna karşı: yanıt eşlemesi ve hata çevirisi. */
class BagServiceClientTest {

    private static final UUID SHIPMENT = UUID.fromString("0192f0a0-0000-7000-8000-0000000009a1");
    private static final UUID BAG_ID = UUID.fromString("0192f0a0-0000-7000-8000-000000000301");
    private static final int OK = 200;
    private static final int NOT_FOUND = 404;
    private static final int CONFLICT = 409;
    private static final int SERVER_ERROR = 500;

    private @Nullable HttpServer server;
    private final AtomicReference<String> requestBody = new AtomicReference<>();

    @AfterEach
    void stopServer() {
        HttpServer running = server;
        if (running != null) {
            running.stop(0);
            server = null;
        }
    }

    @Test
    @DisplayName("Başarılı yanıt ayrılan çuvallara çevrilir ve istek gövdesi sevkiyat ile anahtarları taşır")
    void shouldMapReservedBags() throws Exception {
        BagServiceClient client = clientAnswering(
                OK,
                "{\"bags\":[{\"id\":\"" + BAG_ID + "\",\"uniqueKey\":\"BAG-0001\",\"lotNo\":\"P-1\","
                        + "\"qualityGradeCode\":\"A\",\"status\":\"RESERVED\"}]}");

        List<ReservedBag> bags = client.reserve(SHIPMENT, List.of("BAG-0001"));

        assertThat(bags).containsExactly(new ReservedBag(BAG_ID, "BAG-0001", "P-1", "A"));
        assertThat(requestBody.get()).contains(SHIPMENT.toString()).contains("BAG-0001");
    }

    @Test
    @DisplayName("BAG_ALREADY_RESERVED, BAG_NOT_AVAILABLE ve BAG_NOT_FOUND alan hatasına çevrilir")
    void shouldTranslateProblemCodes() throws Exception {
        assertReason(CONFLICT, "BAG_ALREADY_RESERVED", Reason.ALREADY_RESERVED);
        assertReason(CONFLICT, "BAG_NOT_AVAILABLE", Reason.NOT_AVAILABLE);
        assertReason(NOT_FOUND, "BAG_NOT_FOUND", Reason.NOT_FOUND);
    }

    @Test
    @DisplayName("Tanınmayan 409 ve 500 yanıtı çuval servisi hatası olur")
    void shouldTreatUnknownFailuresAsUnavailable() throws Exception {
        BagServiceClient unknownConflict = clientAnswering(CONFLICT, "{\"code\":\"BAŞKA\"}");
        assertThatThrownBy(() -> unknownConflict.reserve(SHIPMENT, List.of("BAG-0001")))
                .isInstanceOf(BagServiceUnavailableException.class);
        stopServer();
        BagServiceClient failing = clientAnswering(SERVER_ERROR, "{}");
        assertThatThrownBy(() -> failing.reserve(SHIPMENT, List.of("BAG-0001")))
                .isInstanceOf(BagServiceUnavailableException.class);
    }

    @Test
    @DisplayName("Servise ulaşılamazsa çuval servisi hatası olur")
    void shouldFailWhenServiceIsDown() throws Exception {
        BagServiceClient client = clientAnswering(OK, "{}");
        stopServer();

        assertThatThrownBy(() -> client.reserve(SHIPMENT, List.of("BAG-0001")))
                .isInstanceOf(BagServiceUnavailableException.class);
    }

    private void assertReason(int status, String code, Reason expected) throws Exception {
        BagServiceClient client = clientAnswering(status, "{\"code\":\"" + code + "\"}");
        assertThatThrownBy(() -> client.reserve(SHIPMENT, List.of("BAG-0001")))
                .isInstanceOfSatisfying(
                        BagReservationException.class,
                        exception -> assertThat(exception.reason()).isEqualTo(expected));
        stopServer();
    }

    private BagServiceClient clientAnswering(int status, String body) throws IOException {
        HttpServer created = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
        server = created;
        created.createContext("/api/v1/bags/reservations", exchange -> {
            requestBody.set(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
            byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "application/json");
            exchange.sendResponseHeaders(status, bytes.length);
            exchange.getResponseBody().write(bytes);
            exchange.close();
        });
        created.start();
        return new BagServiceClient("http://localhost:" + created.getAddress().getPort());
    }
}
