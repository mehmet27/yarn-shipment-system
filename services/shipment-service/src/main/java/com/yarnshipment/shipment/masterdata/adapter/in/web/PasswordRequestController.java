package com.yarnshipment.shipment.masterdata.adapter.in.web;

import com.yarnshipment.shipment.adapter.in.web.api.PasswordRequestApi;
import com.yarnshipment.shipment.adapter.in.web.api.model.PasswordRequest;
import com.yarnshipment.shipment.masterdata.application.port.in.auth.RequestPasswordUseCase;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.RestController;

/** Giriş yapmamış hesap sahibinin adminden şifre talebi. Method security proxy'si gerektirdiği için final değildir. */
@RestController
public class PasswordRequestController implements PasswordRequestApi {

    private final RequestPasswordUseCase requestPasswordUseCase;

    public PasswordRequestController(RequestPasswordUseCase requestPasswordUseCase) {
        this.requestPasswordUseCase = requestPasswordUseCase;
    }

    @Override
    @PreAuthorize("permitAll()")
    public ResponseEntity<Void> requestPassword(PasswordRequest body) {
        requestPasswordUseCase.requestPassword(body.getUsername());
        return ResponseEntity.noContent().build();
    }
}
