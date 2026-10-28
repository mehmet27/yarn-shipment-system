package com.yarnshipment.shipment.adapter.out.evidence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.sun.net.httpserver.HttpServer;
import com.yarnshipment.shipment.domain.evidence.EvidenceReference;
import com.yarnshipment.shipment.domain.evidence.EvidenceServiceUnavailableException;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** evidence-service istemcisi, gerçek bir HTTP sunucusuna karşı: yanıt eşlemesi ve hata çevirisi. */
class EvidenceServiceClientTest {

    private static final UUID EVIDENCE = UUID.fromString("0192f0a0-0000-7000-8000-0000000e0001");
    private static final UUID SHIPMENT = UUID.fromString("0192f0a0-0000-7000-8000-000000000301");
    private static final int OK = 200;
    private static final int FORBIDDEN = 403;
    private static final int NOT_FOUND = 404;
    private static final int SERVER_ERROR = 500;

    private @Nullable HttpServer server;

    @AfterEach
    void stopServer() {
        HttpServer running = server;
        if (running != null) {
            running.stop(0);
            server = null;
        }
    }

    @Test
    @DisplayName("Başarılı yanıt kanıt özetine çevrilir")
    void shouldMapEvidence() throws Exception {
        EvidenceServiceClient client = clientAnswering(
                OK,
                "{\"id\":\"" + EVIDENCE + "\",\"shipmentId\":\"" + SHIPMENT
                        + "\",\"evidenceType\":\"SIGNED_DELIVERY_DOC\",\"sha256\":\"x\"}");

        assertThat(client.findEvidence(EVIDENCE))
                .contains(new EvidenceReference(EVIDENCE, SHIPMENT, EvidenceReference.SIGNED_DELIVERY_DOC));
    }

    @Test
    @DisplayName("404 ve 403 kanıt yok demektir; diğer hatalar ve ulaşılamama servis hatasıdır")
    void shouldTranslateFailures() throws Exception {
        assertThat(clientAnswering(NOT_FOUND, "{}").findEvidence(EVIDENCE)).isEmpty();
        stopServer();
        assertThat(clientAnswering(FORBIDDEN, "{}").findEvidence(EVIDENCE)).isEmpty();
        stopServer();
        EvidenceServiceClient failing = clientAnswering(SERVER_ERROR, "{}");
        assertThatThrownBy(() -> failing.findEvidence(EVIDENCE))
                .isInstanceOf(EvidenceServiceUnavailableException.class);
        stopServer();
        EvidenceServiceClient down = clientAnswering(OK, "{}");
        stopServer();
        assertThatThrownBy(() -> down.findEvidence(EVIDENCE)).isInstanceOf(EvidenceServiceUnavailableException.class);
    }

    private EvidenceServiceClient clientAnswering(int status, String body) throws IOException {
        HttpServer created = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
        server = created;
        created.createContext("/api/v1/evidence/", exchange -> {
            byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "application/json");
            exchange.sendResponseHeaders(status, bytes.length);
            exchange.getResponseBody().write(bytes);
            exchange.close();
        });
        created.start();
        return new EvidenceServiceClient(
                "http://localhost:" + created.getAddress().getPort());
    }
}
