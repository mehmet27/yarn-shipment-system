package com.yarnshipment.shipment.masterdata.application.port.in.account;

import com.yarnshipment.shipment.masterdata.domain.model.PhoneNumber;
import java.util.UUID;

/** Yeni açılan hesaba ilk şifresini belirleyeceği kodu SMS'le gönderir. */
public interface IssueActivationCodeUseCase {

    /** SMS gidemezse {@code SmsUnavailableException}; hesap açık kalır, yönetici sıfırlamayı yeniden başlatabilir. */
    void issueActivationCode(UUID userId, String username, PhoneNumber phone);
}
