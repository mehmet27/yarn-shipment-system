package com.yarnshipment.shipment.masterdata.application.port.in.account;

/** SMS kodunu doğrular ve kişinin seçtiği yeni şifreyi belirler. */
public interface ConfirmPasswordResetUseCase {

    /**
     * Kod doğruysa şifreyi belirler. Kod yanlış, süresi dolmuş ya da deneme hakkı bitmişse
     * {@code InvalidResetCodeException}; şifre kurala uymuyorsa {@code WeakPasswordException} fırlatır.
     */
    void confirmPasswordReset(String username, String code, String newPassword);
}
