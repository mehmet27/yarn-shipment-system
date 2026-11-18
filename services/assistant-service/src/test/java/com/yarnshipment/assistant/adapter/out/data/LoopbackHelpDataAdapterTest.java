package com.yarnshipment.assistant.adapter.out.data;

import static org.assertj.core.api.Assertions.assertThat;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import com.yarnshipment.assistant.domain.model.HelpDataArgument;
import com.yarnshipment.assistant.domain.model.HelpDataSource;
import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import tools.jackson.databind.json.JsonMapper;

/** Canlı veri okuyucusu: oturum çerezini iletir, yalnızca GET yapar, yetki ve hata durumlarını metne çevirir. */
class LoopbackHelpDataAdapterTest {

    private static final String COOKIE = "YARNSHIPMENT_SESSION=abc";
    private static final int ROWS = 150;
    private static final int OK = 200;
    private static final int FORBIDDEN = 403;
    private static final int SERVER_ERROR = 500;

    private @Nullable HttpServer server;
    private final List<String> requests = new CopyOnWriteArrayList<>();
    private final List<String> cookies = new CopyOnWriteArrayList<>();
    private final List<String> methods = new CopyOnWriteArrayList<>();

    @AfterEach
    void stop() {
        RequestContextHolder.resetRequestAttributes();
        if (server != null) {
            server.stop(0);
        }
    }

    @Test
    @DisplayName("Süzgeçler adrese yazılır ve soran kullanıcının oturum çerezi iletilir; yalnız GET yapılır")
    void shouldForwardCookieAndFiltersWithGetOnly() throws IOException {
        LoopbackHelpDataAdapter adapter = adapterServing(path -> new Reply(OK, "{\"ok\":true}"));
        signedIn(COOKIE);

        String result = adapter.read(
                HelpDataSource.DASHBOARD,
                Map.of(
                        HelpDataArgument.START, "2026-10-19",
                        HelpDataArgument.END, "2026-11-18",
                        HelpDataArgument.SEARCH, "yok sayılır"));

        assertThat(result).isEqualTo("{\"ok\":true}");
        assertThat(requests).containsExactly("/api/v1/reports/dashboard?from=2026-10-19&to=2026-11-18");
        assertThat(cookies).containsExactly(COOKIE);
        assertThat(methods).containsExactly("GET");
    }

    @Test
    @DisplayName("Yetki yoksa (403) modele açıklayıcı bir metin döner; tahmin yürütülmez")
    void shouldExplainMissingPermission() throws IOException {
        LoopbackHelpDataAdapter adapter = adapterServing(path -> new Reply(FORBIDDEN, "{}"));
        signedIn(COOKIE);

        assertThat(adapter.read(HelpDataSource.AMOUNTS, Map.of())).contains("yetkisi yok");
    }

    @Test
    @DisplayName("Oturum çerezi yoksa hiçbir istek yapılmaz")
    void shouldNotCallWithoutSession() throws IOException {
        LoopbackHelpDataAdapter adapter = adapterServing(path -> new Reply(OK, "{}"));

        assertThat(adapter.read(HelpDataSource.DASHBOARD, Map.of())).contains("yetkisi yok");
        assertThat(requests).isEmpty();
    }

    @Test
    @DisplayName("Uzun liste ilk 100 satıra kısaltılır ve toplam satır sayısı bildirilir")
    void shouldShortenLongLists() throws IOException {
        List<String> rows = new ArrayList<>();
        for (int i = 0; i < ROWS; i++) {
            rows.add("{\"n\":" + i + "}");
        }
        LoopbackHelpDataAdapter adapter = adapterServing(path -> new Reply(OK, "[" + String.join(",", rows) + "]"));
        signedIn(COOKIE);

        String result = adapter.read(HelpDataSource.SHIPMENT_REPORT, Map.of());

        assertThat(result)
                .contains("\"toplamSatir\":150")
                .contains("kısaltıldı")
                .contains("{\"n\":99}");
        assertThat(result).doesNotContain("{\"n\":100}");
    }

    @Test
    @DisplayName("Nesne içindeki uzun liste de kısaltılır, diğer alanlar (toplam gibi) aynen kalır")
    void shouldShortenLongListInsideObject() throws IOException {
        List<String> rows = new ArrayList<>();
        for (int i = 0; i < ROWS; i++) {
            rows.add("{\"n\":" + i + "}");
        }
        LoopbackHelpDataAdapter adapter = adapterServing(
                path -> new Reply(OK, "{\"totalKg\":\"2143.814\",\"movements\":[" + String.join(",", rows) + "]}"));
        signedIn(COOKIE);

        String result = adapter.read(HelpDataSource.CENTRAL_STOCK, Map.of());

        assertThat(result)
                .contains("\"totalKg\":\"2143.814\"")
                .contains("\"toplamSatir\":150")
                .doesNotContain("{\"n\":100}");
        assertThat(requests).containsExactly("/api/v1/central-stock");
    }

    @Test
    @DisplayName("Sevkiyat numarasından kimlik bulunur ve ayrıntı okunur")
    void shouldResolveShipmentNumber() throws IOException {
        LoopbackHelpDataAdapter adapter = adapterServing(path -> path.startsWith("/api/v1/shipments?")
                ? new Reply(
                        OK,
                        "{\"items\":[{\"shipmentNo\":\"SHP-0009\",\"shipmentId\":\"a\"},"
                                + "{\"shipmentNo\":\"SHP-0010\",\"shipmentId\":\"b\"}]}")
                : new Reply(OK, "{\"detay\":\"b\"}"));
        signedIn(COOKIE);

        String result = adapter.read(HelpDataSource.SHIPMENT_DETAIL, Map.of(HelpDataArgument.SHIPMENT_NO, "shp-0010"));

        assertThat(result).isEqualTo("{\"detay\":\"b\"}");
        assertThat(requests).last().isEqualTo("/api/v1/shipments/b");
    }

    @Test
    @DisplayName("Numara bulunamazsa ya da verilmezse kayıt bulunamadı denir")
    void shouldReportUnknownShipment() throws IOException {
        LoopbackHelpDataAdapter adapter = adapterServing(path -> new Reply(OK, "{\"items\":[]}"));
        signedIn(COOKIE);

        assertThat(adapter.read(HelpDataSource.SHIPMENT_HISTORY, Map.of(HelpDataArgument.SHIPMENT_NO, "SHP-9999")))
                .contains("bulunamadı");
        assertThat(adapter.read(HelpDataSource.SHIPMENT_HISTORY, Map.of())).contains("bulunamadı");
    }

    @Test
    @DisplayName("Sunucu hata verirse ya da yanıt vermezse veri okunamadı denir")
    void shouldReportUnavailable() throws IOException {
        LoopbackHelpDataAdapter adapter = adapterServing(path -> new Reply(SERVER_ERROR, "{}"));
        signedIn(COOKIE);

        assertThat(adapter.read(HelpDataSource.DASHBOARD, Map.of())).contains("okunamadı");
        assertThat(requests).hasSize(1);
    }

    private void signedIn(String cookie) {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Cookie", cookie);
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));
    }

    private LoopbackHelpDataAdapter adapterServing(java.util.function.Function<String, Reply> reply)
            throws IOException {
        HttpServer created = HttpServer.create(new InetSocketAddress(InetAddress.getLoopbackAddress(), 0), 0);
        server = created;
        created.createContext("/", exchange -> serve(exchange, reply));
        created.start();
        return new LoopbackHelpDataAdapter(
                JsonMapper.builder().build(),
                "http://" + created.getAddress().getHostString() + ":"
                        + created.getAddress().getPort());
    }

    private void serve(HttpExchange exchange, java.util.function.Function<String, Reply> reply) throws IOException {
        String path = exchange.getRequestURI().getRawPath()
                + (exchange.getRequestURI().getRawQuery() == null
                        ? ""
                        : "?" + exchange.getRequestURI().getRawQuery());
        requests.add(path);
        methods.add(exchange.getRequestMethod());
        cookies.add(exchange.getRequestHeaders().getFirst("Cookie"));
        Reply answer = reply.apply(path);
        byte[] bytes = answer.body().getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().add("Content-Type", "application/json");
        exchange.sendResponseHeaders(answer.status(), bytes.length);
        exchange.getResponseBody().write(bytes);
        exchange.close();
    }

    private record Reply(int status, String body) {}
}
