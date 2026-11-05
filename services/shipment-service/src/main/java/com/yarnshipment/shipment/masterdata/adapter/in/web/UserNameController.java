package com.yarnshipment.shipment.masterdata.adapter.in.web;

import com.yarnshipment.shipment.adapter.in.web.api.UserNamesApi;
import com.yarnshipment.shipment.adapter.in.web.api.model.UserNameListResponse;
import com.yarnshipment.shipment.adapter.in.web.api.model.UserNameView;
import com.yarnshipment.shipment.masterdata.application.port.in.account.ListUserNamesUseCase;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.RestController;

/**
 * Kullanıcı kimliklerini okunur adlara çevirir (denetim kaydı gibi ekranlar için); yalnızca ad soyad döner, telefon ve
 * e-posta dönmez. Method security proxy'si gerektirdiği için final değildir (SEC-02).
 */
@RestController
public class UserNameController implements UserNamesApi {

    private final ListUserNamesUseCase listUserNames;

    public UserNameController(ListUserNamesUseCase listUserNames) {
        this.listUserNames = listUserNames;
    }

    @Override
    @PreAuthorize("hasAnyAuthority('USER_MANAGE', 'AUDIT_VIEW')")
    public ResponseEntity<UserNameListResponse> listUserNames(List<UUID> ids) {
        List<UserNameView> items = listUserNames.listUserNames(ids).entrySet().stream()
                .sorted(Comparator.comparing(entry -> entry.getKey().toString()))
                .map(entry -> new UserNameView(entry.getKey(), entry.getValue()))
                .toList();
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(new UserNameListResponse(items));
    }
}
