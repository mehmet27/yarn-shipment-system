package com.yarnshipment.shipment.masterdata.application.service;

import com.yarnshipment.shipment.masterdata.application.port.in.auth.AuthenticateUseCase;
import com.yarnshipment.shipment.masterdata.application.port.in.auth.ChangeOwnPasswordUseCase;
import com.yarnshipment.shipment.masterdata.application.port.in.auth.RequestPasswordUseCase;
import com.yarnshipment.shipment.masterdata.application.port.out.AuthUserPort;
import com.yarnshipment.shipment.masterdata.application.port.out.LoginEventPort;
import com.yarnshipment.shipment.masterdata.application.port.out.PasswordHasherPort;
import com.yarnshipment.shipment.masterdata.domain.model.AccountLockedException;
import com.yarnshipment.shipment.masterdata.domain.model.AuthUser;
import com.yarnshipment.shipment.masterdata.domain.model.AuthenticationFailedException;
import com.yarnshipment.shipment.masterdata.domain.model.InvalidCurrentPasswordException;
import com.yarnshipment.shipment.masterdata.domain.model.PasswordPolicy;
import com.yarnshipment.shipment.masterdata.domain.model.SessionUser;
import com.yarnshipment.shipment.masterdata.domain.model.WeakPasswordException;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Service;

/**
 * Giriş ve kendi şifresini değiştirme. Kullanıcı adı ya da şifre yanlışsa, hesap kapalıysa ya da kullanıcı yoksa aynı
 * hata döner ve aynı süre harcanır (hesap varlığı sızmaz). Art arda {@value #MAX_FAILED_ATTEMPTS} yanlış denemede
 * hesap {@value #LOCK_MINUTES} dakika kilitlenir. Sayaç tek SQL ifadesiyle güncellendiği için transaction açılmaz.
 *
 * <p>Başarılı giriş, var olan hesaba yanlış şifre denemesi ve hesabın kilitlenmesi denetim kaydına yazılır. Olmayan
 * giriş adı, kapalı hesap ve kilitliyken yapılan denemeler yazılmaz (yazılan metin şifre olabilir; kilitli hesaba
 * yapılan her deneme kaydı şişirir).
 */
@Service
public final class AuthenticationService
        implements AuthenticateUseCase, ChangeOwnPasswordUseCase, RequestPasswordUseCase {

    static final int MAX_FAILED_ATTEMPTS = 5;
    static final int FAILED_ATTEMPTS_TO_REQUEST_PASSWORD = 3;
    static final long LOCK_MINUTES = 15;
    private static final Duration LOCK_DURATION = Duration.ofMinutes(LOCK_MINUTES);

    private final AuthUserPort users;
    private final PasswordHasherPort hasher;
    private final Clock clock;
    private final LoginEventPort loginEvents;

    public AuthenticationService(
            AuthUserPort users, PasswordHasherPort hasher, Clock clock, LoginEventPort loginEvents) {
        this.users = users;
        this.hasher = hasher;
        this.clock = clock;
        this.loginEvents = loginEvents;
    }

    @Override
    public SessionUser authenticate(String username, String password) {
        Instant now = clock.instant();
        Optional<AuthUser> found = users.findByUsername(username.strip());
        if (found.isEmpty()) {
            hasher.spendVerificationTime(password);
            throw new AuthenticationFailedException();
        }
        AuthUser user = found.orElseThrow();
        requireNotLocked(user, now);
        if (!hasher.matches(password, user.passwordHash())) {
            users.recordFailedAttempt(user.id(), MAX_FAILED_ATTEMPTS, LOCK_DURATION, now);
            recordFailedLogin(user, now);
            throw new AuthenticationFailedException();
        }
        if (!user.enabled()) {
            throw new AuthenticationFailedException();
        }
        users.recordSuccessfulLogin(user.id(), now);
        loginEvents.publishSucceeded(user, now);
        return SessionUser.of(user);
    }

    /** Hatalı denemeyi, bu deneme hesabı kilitlediyse kilidi de denetim kaydına yazar. */
    private void recordFailedLogin(AuthUser user, Instant now) {
        loginEvents.publishFailed(user, now);
        users.findById(user.id())
                .map(AuthUser::lockedUntil)
                .filter(lockedUntil -> lockedUntil.isAfter(now))
                .ifPresent(lockedUntil -> loginEvents.publishLocked(user, lockedUntil, now));
    }

    @Override
    public void requestPassword(String username) {
        users.requestPassword(username.strip(), FAILED_ATTEMPTS_TO_REQUEST_PASSWORD, clock.instant());
    }

    @Override
    public SessionUser changeOwnPassword(UUID userId, String currentPassword, String newPassword) {
        Instant now = clock.instant();
        AuthUser user = users.findById(userId).orElseThrow(AuthenticationFailedException::new);
        if (!user.enabled()) {
            throw new AuthenticationFailedException();
        }
        requireNotLocked(user, now);
        if (!hasher.matches(currentPassword, user.passwordHash())) {
            users.recordFailedAttempt(user.id(), MAX_FAILED_ATTEMPTS, LOCK_DURATION, now);
            throw new InvalidCurrentPasswordException();
        }
        PasswordPolicy.require(user.username(), newPassword);
        if (hasher.matches(newPassword, user.passwordHash())) {
            throw new WeakPasswordException();
        }
        users.replacePasswordHash(user.id(), hasher.hash(newPassword), now);
        return SessionUser.of(user).withPasswordChanged();
    }

    private static void requireNotLocked(AuthUser user, Instant now) {
        Instant lockedUntil = user.lockedUntil();
        if (lockedUntil != null && user.isLockedAt(now)) {
            throw new AccountLockedException(lockedUntil);
        }
    }
}
