package com.yarnshipment.shipment.masterdata.adapter.in.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

import com.yarnshipment.shipment.masterdata.application.port.in.auth.RequestPasswordUseCase;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.test.web.servlet.assertj.MvcTestResult;

/** Şifre talebi ucunun sözleşmesi: oturum ve CSRF istemez, cevap hep 204, kullanıcı adı zorunludur. */
@WebMvcTest(controllers = PasswordRequestController.class)
class PasswordRequestControllerTest {

    private static final String URI = "/api/v1/auth/password-requests";

    private final MockMvcTester mvc;

    @MockitoBean
    private RequestPasswordUseCase requestPassword;

    @Autowired
    PasswordRequestControllerTest(MockMvcTester mvc) {
        this.mvc = mvc;
    }

    @Test
    @DisplayName("Giriş yapmamış kişi şifre talep eder: 204 döner ve kullanıcı adı use case'e iletilir")
    void shouldAcceptRequestWithoutSession() {
        MvcTestResult result = post("{\"username\":\"depo\"}");

        assertThat(result).hasStatus(HttpStatus.NO_CONTENT);
        verify(requestPassword).requestPassword("depo");
    }

    @Test
    @DisplayName("Kullanıcı adı olmayan istek 400 alır")
    void shouldRejectMissingUsername() {
        assertThat(post("{}")).hasStatus(HttpStatus.BAD_REQUEST);
    }

    private MvcTestResult post(String body) {
        return mvc.post()
                .uri(URI)
                .contentType(MediaType.APPLICATION_JSON)
                .content(body)
                .exchange();
    }
}
