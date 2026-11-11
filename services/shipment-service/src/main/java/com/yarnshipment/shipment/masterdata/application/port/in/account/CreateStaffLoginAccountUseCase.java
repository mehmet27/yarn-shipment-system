package com.yarnshipment.shipment.masterdata.application.port.in.account;

import com.yarnshipment.common.security.Actor;

/**
 * Depo, kalite, müdür ya da yönetici hesabı açar (yalnızca yönetici). İlk şifreyi yönetici belirler; SMS ya da e-posta
 * doğrulaması yoktur. Kişi ilk girişte şifresini değiştirmek zorundadır.
 */
public interface CreateStaffLoginAccountUseCase {

    /** Şifre kurala uymuyorsa {@code WeakPasswordException}; giriş adı alınmışsa {@code LoginAccountExistsException}. */
    void createStaffLoginAccount(Actor actor, StaffAccountRequest request);
}
