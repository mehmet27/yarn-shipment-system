package com.yarnshipment.shipment.masterdata.application.port.in.auth;

import com.yarnshipment.shipment.masterdata.domain.model.SessionUser;
import java.util.UUID;

/** Oturumdaki kullanıcının kendi şifresini değiştirmesi. */
public interface ChangeOwnPasswordUseCase {

    /**
     * Mevcut şifreyi doğrular, yenisini kurala göre denetler ve yazar. Mevcut şifre yanlışsa
     * {@code InvalidCurrentPasswordException}; yeni şifre kurala uymuyorsa ya da eskisiyle aynıysa
     * {@code WeakPasswordException}; hesap kilitliyse {@code AccountLockedException} fırlatır.
     */
    SessionUser changeOwnPassword(UUID userId, String currentPassword, String newPassword);
}
