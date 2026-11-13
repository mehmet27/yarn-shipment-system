package com.yarnshipment.shipment.masterdata.adapter.in.web;

import com.yarnshipment.common.security.CurrentActorProvider;
import com.yarnshipment.common.security.session.SessionAuthenticator;
import com.yarnshipment.common.security.session.SessionTerminator;
import com.yarnshipment.shipment.adapter.in.web.api.AuthenticationApi;
import com.yarnshipment.shipment.adapter.in.web.api.model.ChangeOwnPasswordRequest;
import com.yarnshipment.shipment.adapter.in.web.api.model.LoginRequest;
import com.yarnshipment.shipment.adapter.in.web.api.model.LoginResponse;
import com.yarnshipment.shipment.masterdata.application.port.in.auth.AuthenticateUseCase;
import com.yarnshipment.shipment.masterdata.application.port.in.auth.ChangeOwnPasswordUseCase;
import com.yarnshipment.shipment.masterdata.domain.model.SessionUser;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.RestController;

/**
 * Giriş, çıkış ve kendi şifresini değiştirme. Oturum sunucuda tutulur; başarılı girişte oturum kimliği yenilenir ve
 * HttpOnly çerez verilir. Yanıtlar önbelleğe alınmaz. Method security proxy'si gerektirdiği için final değildir.
 */
@RestController
public class AuthController implements AuthenticationApi {

    private final AuthenticateUseCase authenticate;
    private final ChangeOwnPasswordUseCase changePassword;
    private final SessionAuthenticator sessions;
    private final SessionTerminator terminator;
    private final CurrentActorProvider currentActorProvider;
    private final HttpServletRequest request;
    private final HttpServletResponse response;

    public AuthController(
            AuthenticateUseCase authenticate,
            ChangeOwnPasswordUseCase changePassword,
            SessionAuthenticator sessions,
            SessionTerminator terminator,
            CurrentActorProvider currentActorProvider,
            HttpServletRequest request,
            HttpServletResponse response) {
        this.authenticate = authenticate;
        this.changePassword = changePassword;
        this.sessions = sessions;
        this.terminator = terminator;
        this.currentActorProvider = currentActorProvider;
        this.request = request;
        this.response = response;
    }

    @Override
    @PreAuthorize("permitAll()")
    public ResponseEntity<LoginResponse> login(LoginRequest body) {
        SessionUser user = authenticate.authenticate(body.getUsername(), body.getPassword());
        sessions.establish(SessionActors.tokenOf(user), request, response);
        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore())
                .body(new LoginResponse().passwordChangeRequired(user.passwordChangeRequired()));
    }

    @Override
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Void> logout() {
        sessions.clear(request, response);
        return ResponseEntity.noContent().build();
    }

    @Override
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Void> changeOwnPassword(ChangeOwnPasswordRequest body) {
        SessionUser user = changePassword.changeOwnPassword(
                currentActorProvider.getCurrentActor().userId(), body.getCurrentPassword(), body.getNewPassword());
        String keep = request.getSession(true).getId();
        terminator.terminateOthers(user.id(), keep);
        sessions.establish(SessionActors.tokenOf(user), request, response);
        return ResponseEntity.noContent().build();
    }
}
