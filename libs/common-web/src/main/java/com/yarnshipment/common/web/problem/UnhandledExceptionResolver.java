package com.yarnshipment.common.web.problem;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.Ordered;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.servlet.HandlerExceptionResolver;
import org.springframework.web.servlet.ModelAndView;
import tools.jackson.databind.json.JsonMapper;

/**
 * Hiçbir hata işleyicinin karşılamadığı istisnayı tek yerde ele alır: bir kez, yığın iziyle ve istek bağlamıyla
 * loglar (LOG-04, LOG-05) ve istemciye ayrıntısız {@code 500 INTERNAL_ERROR} döner (ERR-08). İstisna mesajı, SQL ve
 * sınıf adı yanıta yazılmaz. Yetki ve oturum istisnaları güvenlik zincirine bırakılır (401/403 orada üretilir).
 */
public final class UnhandledExceptionResolver implements HandlerExceptionResolver, Ordered {

    private static final Logger LOG = LoggerFactory.getLogger(UnhandledExceptionResolver.class);
    private static final String CODE = "INTERNAL_ERROR";

    private final ProblemFactory problems;
    private final JsonMapper jsonMapper;

    public UnhandledExceptionResolver(ProblemFactory problems, JsonMapper jsonMapper) {
        this.problems = problems;
        this.jsonMapper = jsonMapper;
    }

    /** Tüm advice'lardan ve Spring'in kendi çözücülerinden sonra çalışır. */
    @Override
    public int getOrder() {
        return Ordered.LOWEST_PRECEDENCE;
    }

    @Override
    public @Nullable ModelAndView resolveException(
            HttpServletRequest request, HttpServletResponse response, @Nullable Object handler, Exception exception) {
        if (exception instanceof AccessDeniedException
                || exception instanceof AuthenticationException
                || response.isCommitted()) {
            return null;
        }
        LOG.error(
                "Unhandled exception method={} path={} actorId={}",
                request.getMethod(),
                request.getRequestURI(),
                actorId(),
                exception);
        try {
            write(request, response);
        } catch (IOException writeFailure) {
            // İstemci bağlantıyı kapatmış olabilir; asıl hata yukarıda loglandı, kapsayıcı yanıtı kendisi kapatır.
            return null;
        }
        return new ModelAndView();
    }

    private void write(HttpServletRequest request, HttpServletResponse response) throws IOException {
        ProblemDetail problem = problems.create(request, HttpStatus.INTERNAL_SERVER_ERROR, CODE);
        response.resetBuffer();
        response.setStatus(HttpStatus.INTERNAL_SERVER_ERROR.value());
        response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        jsonMapper.writeValue(response.getOutputStream(), flatten(problem));
    }

    /** Mapper yapılandırmasından bağımsız olarak {@code code} ve {@code requestId} üst düzeyde yazılır. */
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

    /** Kullanıcı kimliği (ad değil, SEC-12); oturumsuz istekte boş. */
    private static @Nullable String actorId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return authentication == null ? null : authentication.getName();
    }
}
