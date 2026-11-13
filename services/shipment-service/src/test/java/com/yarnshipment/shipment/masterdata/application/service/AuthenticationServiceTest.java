package com.yarnshipment.shipment.masterdata.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.yarnshipment.shipment.masterdata.domain.model.AccountLockedException;
import com.yarnshipment.shipment.masterdata.domain.model.AuthenticationFailedException;
import com.yarnshipment.shipment.masterdata.domain.model.InvalidCurrentPasswordException;
import com.yarnshipment.shipment.masterdata.domain.model.SessionUser;
import com.yarnshipment.shipment.masterdata.domain.model.WeakPasswordException;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class AuthenticationServiceTest {

    private static final String PASSWORD = "Sifre-12345";
    private static final Instant NOW = Instant.parse("2026-11-13T09:00:00Z");
    private static final Duration LOCK = Duration.ofMinutes(AuthenticationService.LOCK_MINUTES);

    private final FakeAuthUsers users = new FakeAuthUsers();
    private final FakePasswordHasher hasher = new FakePasswordHasher();
    private final MutableClock clock = new MutableClock(NOW);
    private final AuthenticationService service = new AuthenticationService(users, hasher, clock);

    @Test
    @DisplayName("Doğru şifreyle giriş yapılır; kullanıcı adı büyük/küçük harf duyarsızdır")
    void shouldAuthenticate() {
        UUID id = users.add("depo");

        SessionUser user = service.authenticate("  DEPO ", PASSWORD);

        assertThat(user.id()).isEqualTo(id);
        assertThat(user.passwordChangeRequired()).isTrue();
    }

    @Test
    @DisplayName("Yanlış şifre, olmayan kullanıcı ve kapalı hesap aynı hatayı alır; süre yine de harcanır")
    void shouldNotRevealWhichPartIsWrong() {
        users.add("depo");
        users.add("kapali", false, null);

        assertThatThrownBy(() -> service.authenticate("depo", "yanlis-sifre"))
                .isInstanceOf(AuthenticationFailedException.class);
        assertThatThrownBy(() -> service.authenticate("yok", PASSWORD))
                .isInstanceOf(AuthenticationFailedException.class);
        assertThatThrownBy(() -> service.authenticate("kapali", PASSWORD))
                .isInstanceOf(AuthenticationFailedException.class);
        assertThat(hasher.spentVerifications()).isEqualTo(1);
    }

    @Test
    @DisplayName("Beşinci yanlış denemede hesap 15 dakika kilitlenir; doğru şifre bile kilitliyken geçmez")
    void shouldLockAfterFiveFailures() {
        UUID id = users.add("depo");

        for (int attempt = 0; attempt < AuthenticationService.MAX_FAILED_ATTEMPTS; attempt++) {
            assertThatThrownBy(() -> service.authenticate("depo", "yanlis-sifre"))
                    .isInstanceOf(AuthenticationFailedException.class);
        }

        assertThat(users.user(id).lockedUntil()).isEqualTo(NOW.plus(LOCK));
        assertThatThrownBy(() -> service.authenticate("depo", PASSWORD))
                .isInstanceOf(AccountLockedException.class)
                .extracting(thrown -> ((AccountLockedException) thrown).lockedUntil())
                .isEqualTo(NOW.plus(LOCK));
        clock.advance(LOCK.plusMinutes(1));
        assertThat(service.authenticate("depo", PASSWORD).id()).isEqualTo(id);
        assertThat(users.user(id).lockedUntil()).isNull();
    }

    @Test
    @DisplayName("Başarılı giriş yanlış deneme sayacını sıfırlar")
    void shouldResetFailuresOnSuccess() {
        UUID id = users.add("depo");

        assertThatThrownBy(() -> service.authenticate("depo", "yanlis-sifre"))
                .isInstanceOf(AuthenticationFailedException.class);
        service.authenticate("depo", PASSWORD);

        assertThat(users.failures(id)).isZero();
    }

    @Test
    @DisplayName("Şifre değişince kişi kendi şifresini seçmiş sayılır ve yeni şifreyle girer")
    void shouldChangeOwnPassword() {
        UUID id = users.add("depo");

        SessionUser changed = service.changeOwnPassword(id, PASSWORD, "Yeni-Sifre-678");

        assertThat(changed.passwordChangeRequired()).isFalse();
        assertThat(users.user(id).passwordChangeRequired()).isFalse();
        assertThat(service.authenticate("depo", "Yeni-Sifre-678").passwordChangeRequired())
                .isFalse();
        assertThatThrownBy(() -> service.authenticate("depo", PASSWORD))
                .isInstanceOf(AuthenticationFailedException.class);
    }

    @Test
    @DisplayName("Şifre değiştirirken mevcut şifre yanlışsa reddedilir ve deneme sayılır")
    void shouldRejectWrongCurrentPassword() {
        UUID id = users.add("depo");

        assertThatThrownBy(() -> service.changeOwnPassword(id, "yanlis-sifre", "Yeni-Sifre-678"))
                .isInstanceOf(InvalidCurrentPasswordException.class);

        assertThat(users.failures(id)).isEqualTo(1);
        assertThat(users.user(id).passwordHash()).isEqualTo("hash:" + PASSWORD);
    }

    @Test
    @DisplayName("Yeni şifre kolaysa ya da eskisiyle aynıysa reddedilir")
    void shouldRejectWeakOrUnchangedPassword() {
        UUID id = users.add("depo");

        assertThatThrownBy(() -> service.changeOwnPassword(id, PASSWORD, "12345678"))
                .isInstanceOf(WeakPasswordException.class);
        assertThatThrownBy(() -> service.changeOwnPassword(id, PASSWORD, PASSWORD))
                .isInstanceOf(WeakPasswordException.class);
        assertThat(users.user(id).passwordHash()).isEqualTo("hash:" + PASSWORD);
    }
}
