package com.yarnshipment.common.web;

import com.yarnshipment.common.web.idempotency.IdempotencyFilter;
import com.yarnshipment.common.web.problem.CommonProblemAdvice;
import com.yarnshipment.common.web.problem.ProblemFactory;
import com.yarnshipment.common.web.request.RequestIdFilter;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import org.springframework.context.support.StaticMessageSource;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import tools.jackson.databind.json.JsonMapper;

/** Standalone MockMvc kurulumu: ortak filtre ve advice, sayaçlı bir test controller'ı ile. */
final class WebTestApp {

    static final ProblemFactory PROBLEMS = new ProblemFactory(new StaticMessageSource());

    private WebTestApp() {}

    static MockMvc mockMvc(FakeIdempotencyStore store, TestController controller) {
        return MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new CommonProblemAdvice(PROBLEMS))
                .setHandlerExceptionResolvers(TestExceptionResolvers.inApplicationOrder(PROBLEMS))
                .addFilters(new RequestIdFilter(), new IdempotencyFilter(store, PROBLEMS, JsonMapper.shared()))
                .build();
    }

    @RestController
    @RequestMapping("/things")
    static final class TestController {

        final AtomicInteger calls = new AtomicInteger();

        @PostMapping
        ResponseEntity<String> create(@RequestBody String body) {
            int call = calls.incrementAndGet();
            if (body.contains("boom")) {
                return ResponseEntity.status(HttpStatus.CONFLICT).body("{\"code\":\"CONFLICT\"}");
            }
            return ResponseEntity.status(HttpStatus.CREATED)
                    .header("Location", "/things/" + call)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body("{\"call\":" + call + "}");
        }

        @PostMapping("/json")
        String json(@RequestBody java.util.Map<String, String> body) {
            return body.toString();
        }

        @GetMapping("/by-id/{id}")
        String byId(@PathVariable UUID id) {
            return id.toString();
        }

        @GetMapping("/search")
        String search(@RequestParam String term) {
            return term;
        }

        @PostMapping("/valid")
        String valid(@Valid @RequestBody Named body) {
            return body.name();
        }

        @GetMapping("/broken")
        String broken() {
            if (calls.get() >= 0) {
                throw new IllegalStateException("select * from secret_table failed password=hunter2");
            }
            return "ok";
        }

        @GetMapping("/denied")
        String denied() {
            if (calls.get() >= 0) {
                throw new AccessDeniedException("no");
            }
            return "ok";
        }

        @PostMapping("/needs-header")
        String needsHeader(@RequestHeader("X-Needed") String needed) {
            return needed;
        }

        record Named(@NotBlank String name) {}
    }
}
