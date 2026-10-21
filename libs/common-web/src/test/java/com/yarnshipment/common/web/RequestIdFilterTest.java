package com.yarnshipment.common.web;

import static org.assertj.core.api.Assertions.assertThat;

import com.yarnshipment.common.web.request.RequestIdFilter;
import com.yarnshipment.common.web.request.RequestIds;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

class RequestIdFilterTest {

    private static final int UUID_LENGTH = 36;

    private final RequestIdFilter filter = new RequestIdFilter();

    private MockHttpServletResponse run(MockHttpServletRequest request, AtomicReference<String> seenInMdc)
            throws Exception {
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();
        filter.doFilter(request, response, (req, res) -> {
            seenInMdc.set(MDC.get(RequestIds.MDC_KEY));
            chain.doFilter(req, res);
        });
        return response;
    }

    @Test
    @DisplayName("İstemcinin gönderdiği makul istek kimliği korunur ve log bağlamına konur")
    void shouldKeepClientRequestId() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader(RequestIds.HEADER, "client-123");
        AtomicReference<String> seen = new AtomicReference<>();

        MockHttpServletResponse response = run(request, seen);

        assertThat(response.getHeader(RequestIds.HEADER)).isEqualTo("client-123");
        assertThat(seen.get()).isEqualTo("client-123");
        assertThat(request.getAttribute(RequestIds.ATTRIBUTE)).isEqualTo("client-123");
        assertThat(MDC.get(RequestIds.MDC_KEY)).isNull();
    }

    @Test
    @DisplayName("Başlık yoksa istek kimliği üretilir")
    void shouldGenerateRequestIdWhenMissing() throws Exception {
        MockHttpServletResponse response = run(new MockHttpServletRequest(), new AtomicReference<>());

        assertThat(response.getHeader(RequestIds.HEADER)).hasSize(UUID_LENGTH);
    }

    @Test
    @DisplayName("Log enjeksiyonuna açık değer atılır ve yenisi üretilir")
    void shouldReplaceUnsafeRequestId() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader(RequestIds.HEADER, "evil\nINFO fake");

        MockHttpServletResponse response = run(request, new AtomicReference<>());

        assertThat(response.getHeader(RequestIds.HEADER)).doesNotContain("evil").hasSize(UUID_LENGTH);
    }
}
