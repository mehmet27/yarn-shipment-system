package com.yarnshipment.assistant.adapter.out.data;

import com.yarnshipment.assistant.application.port.out.HelpDataPort;
import com.yarnshipment.assistant.domain.model.HelpDataArgument;
import com.yarnshipment.assistant.domain.model.HelpDataSource;
import jakarta.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Map;
import java.util.Optional;
import java.util.StringJoiner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import tools.jackson.databind.json.JsonMapper;

/**
 * Canlı veriyi uygulamanın kendi okuma (GET) uçlarından okur ve soran kullanıcının oturum çerezini iletir. Böylece
 * her uçtaki yetki ve kapsam denetimi aynen işler: kullanıcının göremediği veri asistana da gelmez. Yalnızca GET
 * yapılır; hiçbir veri değiştirilemez. Dönen veri loglanmaz.
 */
@Component
final class LoopbackHelpDataAdapter implements HelpDataPort {

    private static final Logger LOG = LoggerFactory.getLogger(LoopbackHelpDataAdapter.class);
    private static final Duration CONNECT_TIMEOUT = Duration.ofSeconds(5);
    private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(20);
    private static final int HTTP_OK = 200;
    private static final int HTTP_UNAUTHORIZED = 401;
    private static final int HTTP_FORBIDDEN = 403;
    private static final int HTTP_NOT_FOUND = 404;
    private static final String FORBIDDEN_TEXT = "Bu kullanıcının bu veriyi görme yetkisi yok. Kullanıcıya bu bilgiyi "
            + "göremediğini, yetkili bir kişiye sorması gerektiğini söyle; tahmin yürütme.";
    private static final String UNAVAILABLE_TEXT =
            "Veri şu an okunamadı. Kullanıcıya daha sonra tekrar denemesini söyle.";

    private static final String NOT_FOUND_TEXT = "Kayıt bulunamadı ya da bu kullanıcı görmüyor.";
    private static final String SHIPMENT_ID = "{shipmentId}";

    private final HttpClient http =
            HttpClient.newBuilder().connectTimeout(CONNECT_TIMEOUT).build();
    private final RowLimiter limiter;
    private final ShipmentFinder finder;
    private final String baseUrl;

    LoopbackHelpDataAdapter(
            JsonMapper json, @Value("${yarnshipment.help.data-base-url:http://localhost:8083}") String baseUrl) {
        this.limiter = new RowLimiter(json);
        this.finder = new ShipmentFinder(json);
        this.baseUrl = baseUrl;
    }

    @Override
    public String read(HelpDataSource source, Map<HelpDataArgument, String> arguments) {
        Optional<String> cookie = sessionCookie();
        return cookie.isEmpty() ? FORBIDDEN_TEXT : readWith(cookie.get(), source, arguments);
    }

    private String readWith(String cookie, HelpDataSource source, Map<HelpDataArgument, String> arguments) {
        try {
            if (!source.path().contains(SHIPMENT_ID)) {
                return fetch(cookie, source.path() + query(source, arguments));
            }
            String shipmentNo =
                    arguments.getOrDefault(HelpDataArgument.SHIPMENT_NO, "").strip();
            Optional<String> id =
                    shipmentNo.isEmpty() ? Optional.empty() : finder.find(path -> fetch(cookie, path), shipmentNo);
            return id.isPresent() ? fetch(cookie, source.path().replace(SHIPMENT_ID, id.get())) : NOT_FOUND_TEXT;
        } catch (HelpDataReadException e) {
            LOG.warn("Help data read failed: {}", e.getMessage());
            return UNAVAILABLE_TEXT;
        }
    }

    private String fetch(String cookie, String pathAndQuery) {
        HttpRequest request = HttpRequest.newBuilder(URI.create(baseUrl + pathAndQuery))
                .timeout(REQUEST_TIMEOUT)
                .header("Cookie", cookie)
                .header("Accept", "application/json")
                .GET()
                .build();
        HttpResponse<String> response = send(request);
        return switch (response.statusCode()) {
            case HTTP_OK -> limiter.limit(response.body());
            case HTTP_UNAUTHORIZED, HTTP_FORBIDDEN -> FORBIDDEN_TEXT;
            case HTTP_NOT_FOUND -> NOT_FOUND_TEXT;
            default -> UNAVAILABLE_TEXT;
        };
    }

    private HttpResponse<String> send(HttpRequest request) {
        try {
            return http.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        } catch (IOException e) {
            throw new HelpDataReadException("data read failed", e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new HelpDataReadException("data read interrupted", e);
        }
    }

    private static String query(HelpDataSource source, Map<HelpDataArgument, String> arguments) {
        StringJoiner joined = new StringJoiner("&", "?", "");
        joined.setEmptyValue("");
        for (HelpDataArgument argument : source.arguments()) {
            String value = arguments.get(argument);
            if (value != null && !value.isBlank() && !argument.queryName().isEmpty()) {
                joined.add(argument.queryName() + "=" + URLEncoder.encode(value.strip(), StandardCharsets.UTF_8));
            }
        }
        return joined.toString();
    }

    private static Optional<String> sessionCookie() {
        RequestAttributes attributes = RequestContextHolder.getRequestAttributes();
        if (attributes instanceof ServletRequestAttributes servlet) {
            HttpServletRequest request = servlet.getRequest();
            return Optional.ofNullable(request.getHeader("Cookie")).filter(value -> !value.isBlank());
        }
        return Optional.empty();
    }
}
