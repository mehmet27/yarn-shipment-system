package com.yarnshipment.shipment.masterdata.application.service;

import com.yarnshipment.common.security.Actor;
import com.yarnshipment.shipment.masterdata.application.port.in.account.ConfirmPasswordResetUseCase;
import com.yarnshipment.shipment.masterdata.application.port.in.account.IssueActivationCodeUseCase;
import com.yarnshipment.shipment.masterdata.application.port.in.account.RequestPasswordResetUseCase;
import com.yarnshipment.shipment.masterdata.application.port.in.account.StartPasswordResetUseCase;
import com.yarnshipment.shipment.masterdata.application.port.out.AccountCredentialPort;
import com.yarnshipment.shipment.masterdata.application.port.out.ResetCode;
import com.yarnshipment.shipment.masterdata.application.port.out.ResetCodePort;
import com.yarnshipment.shipment.masterdata.application.port.out.ResetTarget;
import com.yarnshipment.shipment.masterdata.application.port.out.SmsPort;
import com.yarnshipment.shipment.masterdata.domain.model.InvalidMasterDataException;
import com.yarnshipment.shipment.masterdata.domain.model.InvalidResetCodeException;
import com.yarnshipment.shipment.masterdata.domain.model.LoginAccountNotFoundException;
import com.yarnshipment.shipment.masterdata.domain.model.PhoneNumber;
import com.yarnshipment.shipment.masterdata.domain.model.WeakPasswordException;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Locale;
import java.util.Objects;
import java.util.UUID;
import org.springframework.stereotype.Service;

/**
 * SMS ile şifre belirleme/sıfırlama. Kendi isteğiyle gelen kod 10 dakika, yönetici ya da hesap açılışı kaynaklı kod 3
 * gün geçerlidir; kullanıcı başına tek kod vardır, yenisi eskisini geçersiz kılar. Kod 5 yanlış denemeden sonra yanar.
 * Şifre hiçbir yerde saklanmaz ya da loglanmaz. Yerel veritabanına tek satır yazıldığı için transaction açılmaz.
 */
@Service
public final class PasswordResetService
        implements RequestPasswordResetUseCase,
                ConfirmPasswordResetUseCase,
                StartPasswordResetUseCase,
                IssueActivationCodeUseCase {

    static final Duration SELF_SERVICE_LIFESPAN = Duration.ofMinutes(10);
    static final Duration INVITE_LIFESPAN = Duration.ofDays(3);
    static final Duration RESEND_COOLDOWN = Duration.ofMinutes(1);
    static final int MAX_ATTEMPTS = 5;
    static final int MIN_PASSWORD_LENGTH = 8;
    private static final String RESET_MESSAGE = "Şifre sıfırlama kodunuz: %s. Kimseyle paylaşmayın.";

    private final AccountCredentialPort accounts;
    private final ResetCodePort codes;
    private final SmsPort sms;
    private final Clock clock;

    public PasswordResetService(AccountCredentialPort accounts, ResetCodePort codes, SmsPort sms, Clock clock) {
        this.accounts = accounts;
        this.codes = codes;
        this.sms = sms;
        this.clock = clock;
    }

    @Override
    public void requestPasswordReset(String username) {
        accounts.findByUsername(username.strip().toLowerCase(Locale.ROOT))
                .filter(ResetTarget::enabled)
                .filter(target -> target.phone() != null && !sentRecently(target.userId()))
                .ifPresent(target -> {
                    PhoneNumber phone = Objects.requireNonNull(target.phone());
                    issue(target.userId(), phone, SELF_SERVICE_LIFESPAN, RESET_MESSAGE);
                });
    }

    /** Aynı kullanıcıya dakikada birden çok SMS gitmesin; sınırı aşan istek sessizce yok sayılır. */
    private boolean sentRecently(UUID userId) {
        Instant now = clock.instant();
        return codes.find(userId)
                .map(code -> code.issuedAt().plus(RESEND_COOLDOWN).isAfter(now))
                .orElse(false);
    }

    @Override
    public void startPasswordReset(Actor actor, UUID userId) {
        ResetTarget target = accounts.findById(userId).orElseThrow(LoginAccountNotFoundException::new);
        PhoneNumber phone = target.phone();
        if (phone == null) {
            throw new InvalidMasterDataException("phone", "missing");
        }
        issue(userId, phone, INVITE_LIFESPAN, RESET_MESSAGE);
    }

    @Override
    public void issueActivationCode(UUID userId, String username, PhoneNumber phone) {
        issue(
                userId,
                phone,
                INVITE_LIFESPAN,
                "İplik Sevkiyat hesabınız açıldı. Giriş adınız: " + username
                        + ". Giriş sayfasında 'Şifremi unuttum' ile şifre belirlemek için kodunuz: %s");
    }

    @Override
    public void confirmPasswordReset(String username, String code, String newPassword) {
        ResetTarget target = accounts.findByUsername(username.strip().toLowerCase(Locale.ROOT))
                .filter(ResetTarget::enabled)
                .orElseThrow(InvalidResetCodeException::new);
        ResetCode stored = codes.find(target.userId()).orElseThrow(InvalidResetCodeException::new);
        if (!stored.expiresAt().isAfter(clock.instant()) || stored.attempts() >= MAX_ATTEMPTS) {
            codes.delete(target.userId());
            throw new InvalidResetCodeException();
        }
        if (!ResetCodes.matches(stored.salt(), code.strip(), stored.codeHash())) {
            codes.recordFailedAttempt(target.userId());
            throw new InvalidResetCodeException();
        }
        if (newPassword.length() < MIN_PASSWORD_LENGTH) {
            throw new WeakPasswordException();
        }
        accounts.setPassword(target.userId(), newPassword);
        codes.delete(target.userId());
    }

    private void issue(UUID userId, PhoneNumber phone, Duration lifespan, String template) {
        String code = ResetCodes.generate();
        String salt = ResetCodes.newSalt();
        Instant now = clock.instant();
        codes.save(new ResetCode(userId, ResetCodes.hash(salt, code), salt, now, now.plus(lifespan), 0));
        sms.send(phone, template.formatted(code));
    }
}
