package com.yarnshipment.shipment.masterdata.adapter.in.web;

import com.yarnshipment.common.security.CurrentActorProvider;
import com.yarnshipment.common.security.RolePermissionMatrix;
import com.yarnshipment.shipment.adapter.in.web.api.RolePermissionsApi;
import com.yarnshipment.shipment.adapter.in.web.api.model.Permission;
import com.yarnshipment.shipment.adapter.in.web.api.model.Role;
import com.yarnshipment.shipment.adapter.in.web.api.model.RolePermissionRow;
import com.yarnshipment.shipment.adapter.in.web.api.model.RolePermissionsSaveRequest;
import com.yarnshipment.shipment.adapter.in.web.api.model.RolePermissionsView;
import com.yarnshipment.shipment.masterdata.application.port.in.rolepermission.ListRolePermissionsUseCase;
import com.yarnshipment.shipment.masterdata.application.port.in.rolepermission.ResetRolePermissionsUseCase;
import com.yarnshipment.shipment.masterdata.application.port.in.rolepermission.SaveRolePermissionsUseCase;
import java.util.Collection;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.RestController;

/** Yetkiler ekranı uçları; yalnızca PERMISSION_MANAGE (yönetici). SEC-02: final değildir. */
@RestController
public class RolePermissionController implements RolePermissionsApi {

    private static final String ACCESS = "hasAuthority('PERMISSION_MANAGE')";

    private final ListRolePermissionsUseCase list;
    private final SaveRolePermissionsUseCase save;
    private final ResetRolePermissionsUseCase reset;
    private final CurrentActorProvider currentActorProvider;

    public RolePermissionController(
            ListRolePermissionsUseCase list,
            SaveRolePermissionsUseCase save,
            ResetRolePermissionsUseCase reset,
            CurrentActorProvider currentActorProvider) {
        this.list = list;
        this.save = save;
        this.reset = reset;
        this.currentActorProvider = currentActorProvider;
    }

    @Override
    @PreAuthorize(ACCESS)
    public ResponseEntity<RolePermissionsView> listRolePermissions() {
        return respond();
    }

    @Override
    @PreAuthorize(ACCESS)
    public ResponseEntity<RolePermissionsView> saveRolePermissions(Role role, RolePermissionsSaveRequest request) {
        Set<com.yarnshipment.common.security.Permission> permissions =
                EnumSet.noneOf(com.yarnshipment.common.security.Permission.class);
        request.getPermissions()
                .forEach(p -> permissions.add(com.yarnshipment.common.security.Permission.valueOf(p.name())));
        save.saveRolePermissions(currentActorProvider.getCurrentActor(), domain(role), permissions);
        return respond();
    }

    @Override
    @PreAuthorize(ACCESS)
    public ResponseEntity<RolePermissionsView> resetRolePermissions(Role role) {
        reset.resetRolePermissions(currentActorProvider.getCurrentActor(), domain(role));
        return respond();
    }

    private ResponseEntity<RolePermissionsView> respond() {
        List<RolePermissionRow> rows = list.listRolePermissions().entrySet().stream()
                .map(entry -> new RolePermissionRow(
                        Role.valueOf(entry.getKey().name()),
                        RolePermissionMatrix.EDITABLE_ROLES.contains(entry.getKey()),
                        views(entry.getValue()),
                        views(RolePermissionMatrix.defaultsOf(entry.getKey()))))
                .toList();
        RolePermissionsView body = new RolePermissionsView(rows, views(RolePermissionMatrix.ADMIN_ONLY));
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(body);
    }

    private static List<Permission> views(Collection<com.yarnshipment.common.security.Permission> permissions) {
        return permissions.stream()
                .map(p -> Permission.valueOf(p.name()))
                .sorted()
                .toList();
    }

    private static com.yarnshipment.common.security.Role domain(Role role) {
        return com.yarnshipment.common.security.Role.valueOf(role.name());
    }
}
