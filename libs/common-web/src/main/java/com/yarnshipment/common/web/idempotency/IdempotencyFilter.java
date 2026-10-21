package com.yarnshipment.common.web.idempotency;

import com.yarnshipment.common.web.problem.ProblemFactory;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Set;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.ContentCachingResponseWrapper;
import tools.jackson.databind.json.JsonMapper;

/**
 * API-04: durum değiştiren isteklerde {@code Idempotency-Key} başlığı varsa isteği tekrar-güvenli yapar. Aynı anahtar
 * ve aynı içerik ilk başarılı yanıtı tekrar döner, farklı içerik 422 alır. Yalnızca 2xx yanıtlar saklanır; başarısız
 * istek anahtarı bırakır ve aynı anahtarla yeniden denenebilir. Spring Security zincirinden sonra çalışır, anahtar
 * kullanıcıya özeldir.
 */
public final class IdempotencyFilter extends OncePerRequestFilter {

    public static final String KEY_HEADER = "Idempotency-Key";
    public static final String REPLAYED_HEADER = IdempotencyResponses.REPLAYED_HEADER;

    private static final int MAX_KEY_LENGTH = 255;
    private static final char FIRST_VISIBLE = '!';
    private static final char LAST_VISIBLE = '~';
    private static final Set<String> PROTECTED_METHODS =
            Set.of(HttpMethod.POST.name(), HttpMethod.PUT.name(), HttpMethod.PATCH.name());

    private final IdempotencyStore store;
    private final IdempotencyResponses responses;

    public IdempotencyFilter(IdempotencyStore store, ProblemFactory problems, JsonMapper jsonMapper) {
        this.store = store;
        this.responses = new IdempotencyResponses(problems, jsonMapper);
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String key = request.getHeader(KEY_HEADER);
        String actor = ActorNames.current();
        if (key == null || actor == null || !PROTECTED_METHODS.contains(request.getMethod())) {
            chain.doFilter(request, response);
        } else if (validKey(key)) {
            byte[] body = request.getInputStream().readAllBytes();
            String fingerprint = RequestFingerprint.of(request, body);
            handle(new Call(new CachedBodyRequest(request, body), response, chain), new Slot(actor, key), fingerprint);
        } else {
            responses.problem(request, response, HttpStatus.BAD_REQUEST, "INVALID_IDEMPOTENCY_KEY");
        }
    }

    private void handle(Call call, Slot slot, String fingerprint) throws ServletException, IOException {
        switch (store.claim(slot.actor(), slot.key(), fingerprint)) {
            case Claim.Acquired _ -> execute(call, slot);
            case Claim.Completed completed -> responses.replay(call.response(), completed.response());
            case Claim.InProgress _ ->
                responses.problem(call.request(), call.response(), HttpStatus.CONFLICT, "IDEMPOTENCY_IN_PROGRESS");
            case Claim.Mismatch _ ->
                responses.problem(
                        call.request(), call.response(), HttpStatus.UNPROCESSABLE_CONTENT, "IDEMPOTENCY_KEY_REUSED");
        }
    }

    private void execute(Call call, Slot slot) throws ServletException, IOException {
        ContentCachingResponseWrapper wrapper = new ContentCachingResponseWrapper(call.response());
        boolean stored = false;
        try {
            call.chain().doFilter(call.request(), wrapper);
            stored = storeIfSuccessful(wrapper, slot);
        } finally {
            if (!stored) {
                store.release(slot.actor(), slot.key());
            }
        }
        wrapper.copyBodyToResponse();
    }

    private boolean storeIfSuccessful(ContentCachingResponseWrapper wrapper, Slot slot) {
        if (!HttpStatus.valueOf(wrapper.getStatus()).is2xxSuccessful()) {
            return false;
        }
        store.complete(
                slot.actor(),
                slot.key(),
                new StoredResponse(
                        wrapper.getStatus(),
                        wrapper.getContentType(),
                        wrapper.getHeader(HttpHeaders.LOCATION),
                        wrapper.getContentAsByteArray()));
        return true;
    }

    private static boolean validKey(String key) {
        return !key.isBlank()
                && key.length() <= MAX_KEY_LENGTH
                && key.chars().allMatch(c -> c >= FIRST_VISIBLE && c <= LAST_VISIBLE);
    }

    private record Slot(String actor, String key) {}

    private record Call(HttpServletRequest request, HttpServletResponse response, FilterChain chain) {}
}
