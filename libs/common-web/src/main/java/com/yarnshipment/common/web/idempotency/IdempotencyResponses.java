package com.yarnshipment.common.web.idempotency;

import com.yarnshipment.common.web.problem.ProblemFactory;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import tools.jackson.databind.json.JsonMapper;

/** Filtrenin yazdığı yanıtlar: saklı yanıtın tekrarı ve idempotency hataları (ERR-07 biçimi). */
final class IdempotencyResponses {

    static final String REPLAYED_HEADER = "Idempotency-Replayed";

    private final ProblemFactory problems;
    private final JsonMapper jsonMapper;

    IdempotencyResponses(ProblemFactory problems, JsonMapper jsonMapper) {
        this.problems = problems;
        this.jsonMapper = jsonMapper;
    }

    void replay(HttpServletResponse response, StoredResponse stored) throws IOException {
        response.setStatus(stored.status());
        if (stored.contentType() != null) {
            response.setContentType(stored.contentType());
        }
        if (stored.location() != null) {
            response.setHeader(HttpHeaders.LOCATION, stored.location());
        }
        response.setHeader(REPLAYED_HEADER, "true");
        response.getOutputStream().write(stored.body());
    }

    void problem(HttpServletRequest request, HttpServletResponse response, HttpStatus status, String code)
            throws IOException {
        response.setStatus(status.value());
        response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        jsonMapper.writeValue(response.getOutputStream(), flatten(problems.create(request, status, code)));
    }

    /** Mapper yapılandırmasından bağımsız olarak {@code code} ve {@code requestId} gibi alanlar üst düzeyde yazılır. */
    private static Map<String, Object> flatten(ProblemDetail problem) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("type", String.valueOf(problem.getType()));
        body.put("title", problem.getTitle());
        body.put("status", problem.getStatus());
        body.put("detail", problem.getDetail());
        body.put("instance", String.valueOf(problem.getInstance()));
        Map<String, Object> properties = problem.getProperties();
        if (properties != null) {
            body.putAll(properties);
        }
        return body;
    }
}
