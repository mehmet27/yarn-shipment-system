package com.yarnshipment.shipment.masterdata.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.yarnshipment.common.security.Actor;
import com.yarnshipment.shipment.TestTokens;
import com.yarnshipment.shipment.masterdata.application.port.out.SmsPort;
import com.yarnshipment.shipment.masterdata.domain.model.InvalidMasterDataException;
import com.yarnshipment.shipment.masterdata.domain.model.InvalidResetCodeException;
import com.yarnshipment.shipment.masterdata.domain.model.LoginAccountNotFoundException;
import com.yarnshipment.shipment.masterdata.domain.model.PhoneNumber;
import com.yarnshipment.shipment.masterdata.domain.model.ResetTarget;
import com.yarnshipment.shipment.masterdata.domain.model.SmsUnavailableException;
import com.yarnshipment.shipment.masterdata.domain.model.WeakPasswordException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** SMS ile şifre belirleme: kod üretimi, süre, deneme sınırı, tekrar sınırı ve kullanıcı sızdırmama. */
class PasswordResetServiceTest {

    private static final UUID USER = UUID.fromString("0192f0a0-0000-7000-8000-000000000101");
    private static final PhoneNumber PHONE = new PhoneNumber("+905321234567");
    private static final Pattern CODE = Pattern.compile("kodunuz: (\\d{6})");
    private static final String STRONG = "Yeni-Sifre-2026";

    private final InMemoryResetCodes codes = new InMemoryResetCodes();
    private final FakeCredentials accounts = new FakeCredentials();
    private final MutableClock clock = new MutableClock(Instant.parse("2026-11-04T09:00:00Z"));
    private final List<String> messages = new ArrayList<>();
    private final Actor admin = TestTokens.actor("admin");
    private boolean smsDown;
    private final SmsPort sms = (to, text) -> {
        if (smsDown) {
            throw new SmsUnavailableException();
        }
        messages.add(to.e164() + " " + text);
    };
    private final PasswordResetService service = new PasswordResetService(accounts, codes, sms, clock);

    PasswordResetServiceTest() {
        accounts.target(Optional.of(new ResetTarget(USER, "ali.yilmaz", true, PHONE)));
    }

    @Test
    @DisplayName("İstek kayıtlı telefona altı haneli kod gönderir; kod düz metin saklanmaz")
    void shouldSendCodeAndStoreOnlyHash() {
        service.requestPasswordReset("ali.yilmaz");

        assertThat(messages)
                .singleElement()
                .satisfies(message -> assertThat(message).startsWith("+905321234567 "));
        String code = lastCode();
        assertThat(codes.get(USER).codeHash()).doesNotContain(code);
        assertThat(codes.get(USER).expiresAt()).isEqualTo(clock.now().plus(PasswordResetService.SELF_SERVICE_LIFESPAN));
    }

    @Test
    @DisplayName("Giriş adı büyük harfle ve boşlukla yazılsa da bulunur")
    void shouldNormalizeUsername() {
        service.requestPasswordReset("  ALI.Yilmaz ");

        assertThat(messages).hasSize(1);
    }

    @Test
    @DisplayName("Hesap yok, pasif ya da telefonsuzsa sessizce döner; hiçbir SMS gitmez ve hata fırlamaz")
    void shouldStaySilentForUnknownDisabledOrPhonelessAccounts() {
        service.requestPasswordReset("yok.biri");
        accounts.target(Optional.of(new ResetTarget(USER, "ali.yilmaz", false, PHONE)));
        service.requestPasswordReset("ali.yilmaz");
        accounts.target(Optional.of(new ResetTarget(USER, "ali.yilmaz", true, null)));
        service.requestPasswordReset("ali.yilmaz");

        assertThat(messages).isEmpty();
        assertThat(codes.isEmpty()).isTrue();
    }

    @Test
    @DisplayName("Bir dakika dolmadan ikinci SMS gitmez; dolunca yeni kod eskisini geçersiz kılar")
    void shouldThrottleResend() {
        service.requestPasswordReset("ali.yilmaz");
        String first = lastCode();
        service.requestPasswordReset("ali.yilmaz");
        clock.advance(PasswordResetService.RESEND_COOLDOWN.plusSeconds(1));
        service.requestPasswordReset("ali.yilmaz");
        String second = lastCode();

        assertThat(messages).hasSize(2);
        assertThatThrownBy(() -> service.confirmPasswordReset("ali.yilmaz", first, STRONG))
                .isInstanceOf(InvalidResetCodeException.class);
        service.confirmPasswordReset("ali.yilmaz", second, STRONG);
        assertThat(accounts.passwords()).containsExactly(USER + ":" + STRONG);
    }

    @Test
    @DisplayName("Doğru kodla şifre belirlenir ve kod yanar; aynı kod ikinci kez kullanılamaz")
    void shouldSetPasswordOnce() {
        service.requestPasswordReset("ali.yilmaz");
        String code = lastCode();

        service.confirmPasswordReset("ali.yilmaz", " " + code + " ", STRONG);

        assertThat(accounts.passwords()).containsExactly(USER + ":" + STRONG);
        assertThat(codes.isEmpty()).isTrue();
        assertThatThrownBy(() -> service.confirmPasswordReset("ali.yilmaz", code, STRONG))
                .isInstanceOf(InvalidResetCodeException.class);
    }

    @Test
    @DisplayName("Yanlış kod beşinci denemede kodu yakar; sonra doğru kod da kabul edilmez")
    void shouldBurnCodeAfterTooManyWrongAttempts() {
        service.requestPasswordReset("ali.yilmaz");
        String code = lastCode();
        String wrong = code.equals("000000") ? "000001" : "000000";

        for (int attempt = 0; attempt < PasswordResetService.MAX_ATTEMPTS; attempt++) {
            assertThatThrownBy(() -> service.confirmPasswordReset("ali.yilmaz", wrong, STRONG))
                    .isInstanceOf(InvalidResetCodeException.class);
        }

        assertThatThrownBy(() -> service.confirmPasswordReset("ali.yilmaz", code, STRONG))
                .isInstanceOf(InvalidResetCodeException.class);
        assertThat(accounts.passwords()).isEmpty();
        assertThat(codes.isEmpty()).isTrue();
    }

    @Test
    @DisplayName("Süresi dolan kod kabul edilmez")
    void shouldRejectExpiredCode() {
        service.requestPasswordReset("ali.yilmaz");
        String code = lastCode();
        clock.advance(PasswordResetService.SELF_SERVICE_LIFESPAN.plusSeconds(1));

        assertThatThrownBy(() -> service.confirmPasswordReset("ali.yilmaz", code, STRONG))
                .isInstanceOf(InvalidResetCodeException.class);
        assertThat(accounts.passwords()).isEmpty();
    }

    @Test
    @DisplayName("Kısa ya da kurala uymayan şifre WeakPasswordException verir ve kod yanmaz")
    void shouldKeepCodeWhenPasswordIsWeak() {
        service.requestPasswordReset("ali.yilmaz");
        String code = lastCode();

        assertThatThrownBy(() -> service.confirmPasswordReset("ali.yilmaz", code, "kisa"))
                .isInstanceOf(WeakPasswordException.class);
        accounts.rejectPasswords(true);
        assertThatThrownBy(() -> service.confirmPasswordReset("ali.yilmaz", code, STRONG))
                .isInstanceOf(WeakPasswordException.class);

        accounts.rejectPasswords(false);
        service.confirmPasswordReset("ali.yilmaz", code, STRONG);
        assertThat(accounts.passwords()).hasSize(1);
    }

    @Test
    @DisplayName("Kodsuz, bilinmeyen ya da pasif hesap için onay aynı InvalidResetCodeException ile reddedilir")
    void shouldRejectConfirmWithoutCodeOrAccount() {
        assertThatThrownBy(() -> service.confirmPasswordReset("ali.yilmaz", "123456", STRONG))
                .isInstanceOf(InvalidResetCodeException.class);
        assertThatThrownBy(() -> service.confirmPasswordReset("yok.biri", "123456", STRONG))
                .isInstanceOf(InvalidResetCodeException.class);
        accounts.target(Optional.of(new ResetTarget(USER, "ali.yilmaz", false, PHONE)));
        assertThatThrownBy(() -> service.confirmPasswordReset("ali.yilmaz", "123456", STRONG))
                .isInstanceOf(InvalidResetCodeException.class);
    }

    @Test
    @DisplayName("Yönetici sıfırlamayı başlatınca üç gün geçerli kod kayıtlı telefona gider")
    void shouldStartResetForAdmin() {
        service.startPasswordReset(admin, USER);

        assertThat(messages).hasSize(1);
        assertThat(codes.get(USER).expiresAt()).isEqualTo(clock.now().plus(PasswordResetService.INVITE_LIFESPAN));
    }

    @Test
    @DisplayName("Yönetici sıfırlaması: kullanıcı yoksa NotFound, telefon yoksa phone alanı hatası")
    void shouldRejectAdminResetForMissingUserOrPhone() {
        assertThatThrownBy(() -> service.startPasswordReset(admin, UUID.randomUUID()))
                .isInstanceOf(LoginAccountNotFoundException.class);
        accounts.target(Optional.of(new ResetTarget(USER, "ali.yilmaz", true, null)));

        assertThatThrownBy(() -> service.startPasswordReset(admin, USER))
                .isInstanceOf(InvalidMasterDataException.class)
                .extracting(exception -> ((InvalidMasterDataException) exception).field())
                .isEqualTo("phone");
        assertThat(messages).isEmpty();
    }

    @Test
    @DisplayName("Hesap açılışında giriş adını içeren davet SMS'i gider; şifre içermez")
    void shouldIssueActivationCode() {
        service.issueActivationCode(USER, "ali.yilmaz", PHONE);

        assertThat(messages).singleElement().satisfies(message -> {
            assertThat(message).contains("ali.yilmaz").contains("Şifremi unuttum");
            assertThat(message).containsPattern(CODE);
        });
        assertThat(codes.get(USER).expiresAt()).isEqualTo(clock.now().plus(PasswordResetService.INVITE_LIFESPAN));
    }

    @Test
    @DisplayName("SMS gönderilemezse SmsUnavailableException yayılır")
    void shouldPropagateSmsFailure() {
        smsDown = true;

        assertThatThrownBy(() -> service.issueActivationCode(USER, "ali.yilmaz", PHONE))
                .isInstanceOf(SmsUnavailableException.class);
    }

    private String lastCode() {
        Matcher matcher = CODE.matcher(messages.getLast());
        assertThat(matcher.find()).isTrue();
        return matcher.group(1);
    }
}
