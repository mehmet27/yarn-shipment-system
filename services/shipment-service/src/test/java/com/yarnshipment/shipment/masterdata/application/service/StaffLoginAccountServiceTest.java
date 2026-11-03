package com.yarnshipment.shipment.masterdata.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.yarnshipment.common.security.Actor;
import com.yarnshipment.shipment.TestTokens;
import com.yarnshipment.shipment.masterdata.application.port.in.StaffAccountRequest;
import com.yarnshipment.shipment.masterdata.application.port.out.LoginAccountTargetPort;
import com.yarnshipment.shipment.masterdata.application.port.out.StaffAccountPort;
import com.yarnshipment.shipment.masterdata.domain.model.InvalidMasterDataException;
import com.yarnshipment.shipment.masterdata.domain.model.StaffAccountSpec;
import com.yarnshipment.shipment.masterdata.domain.model.StaffRole;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class StaffLoginAccountServiceTest {

    private static final UUID SITE = UUID.fromString("0192f0a0-0000-7000-8000-000000000001");
    private static final UUID OTHER_SITE = UUID.fromString("0192f0a0-0000-7000-8000-000000000002");

    private final List<StaffAccountSpec> openedStaff = new ArrayList<>();
    private final List<String> roleChanges = new ArrayList<>();
    private final Actor admin = TestTokens.actor("admin");
    private final LoginAccountTargetPort targets = new LoginAccountTargetPort() {
        @Override
        public Optional<String> findActiveDriverName(UUID id) {
            return Optional.empty();
        }

        @Override
        public Optional<String> findActiveSubcontractorTitle(UUID id) {
            return Optional.empty();
        }

        @Override
        public Set<UUID> findActiveSiteIds(Collection<UUID> ids) {
            return ids.contains(SITE) ? Set.of(SITE) : Set.of();
        }
    };
    private final StaffAccountPort accounts = new StaffAccountPort() {
        @Override
        public void createStaff(StaffAccountSpec spec) {
            openedStaff.add(spec);
        }

        @Override
        public void changeStaffRole(UUID userId, StaffRole role) {
            roleChanges.add(userId + ":" + role);
        }
    };
    private final StaffLoginAccountService service = new StaffLoginAccountService(targets, accounts);

    @Test
    @DisplayName("Fabrika içi hesap şifresiz, verilen rol ve lokasyonla açılır")
    void shouldOpenStaffAccountWithoutPassword() {
        service.createStaffLoginAccount(
                admin,
                new StaffAccountRequest("depo2", "depo2@ornek.com", "Ayşe Depo", StaffRole.WAREHOUSE, List.of(SITE)));

        assertThat(openedStaff).singleElement().satisfies(spec -> {
            assertThat(spec.username()).isEqualTo("depo2");
            assertThat(spec.role()).isEqualTo(StaffRole.WAREHOUSE);
            assertThat(spec.siteIds()).containsExactly(SITE);
        });
    }

    @Test
    @DisplayName("Lokasyon boşsa ya da etkin değilse hesap açılmaz")
    void shouldRejectBadSites() {
        StaffAccountRequest none =
                new StaffAccountRequest("d", "d@ornek.com", "Ayşe Depo", StaffRole.QUALITY, List.of());
        StaffAccountRequest inactive =
                new StaffAccountRequest("d", "d@ornek.com", "Ayşe Depo", StaffRole.QUALITY, List.of(SITE, OTHER_SITE));

        assertThatThrownBy(() -> service.createStaffLoginAccount(admin, none))
                .isInstanceOf(InvalidMasterDataException.class);
        assertThatThrownBy(() -> service.createStaffLoginAccount(admin, inactive))
                .isInstanceOf(InvalidMasterDataException.class);
        assertThat(openedStaff).isEmpty();
    }

    @Test
    @DisplayName("Rol başkasının hesabında değişir; kişi kendi rolünü değiştiremez")
    void shouldChangeRoleOfOthersOnly() {
        UUID other = UUID.randomUUID();

        service.changeLoginAccountRole(admin, other, StaffRole.QUALITY);

        assertThat(roleChanges).containsExactly(other + ":QUALITY");
        assertThatThrownBy(() -> service.changeLoginAccountRole(admin, admin.userId(), StaffRole.MANAGER))
                .isInstanceOf(InvalidMasterDataException.class);
        assertThat(roleChanges).hasSize(1);
    }
}
