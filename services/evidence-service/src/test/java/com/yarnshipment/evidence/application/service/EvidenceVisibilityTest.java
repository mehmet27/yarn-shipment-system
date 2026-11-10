package com.yarnshipment.evidence.application.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.yarnshipment.evidence.TestTokens;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/** Kör giriş kalktı: kanıtı görme izni olan her rol bütün kanıtları görür. */
class EvidenceVisibilityTest {

    @ParameterizedTest
    @ValueSource(strings = {"warehouse", "manager", "quality", "admin"})
    @DisplayName("Kanıt görme izni olan rol kanıtı görür")
    void shouldLetEveryViewerSeeEvidence(String role) {
        // when / then
        assertThat(EvidenceVisibility.mayView(TestTokens.actor(role))).isTrue();
    }

    @ParameterizedTest
    @ValueSource(strings = {"offline_access"})
    @DisplayName("İzni olmayan kullanıcı kanıt göremez")
    void shouldHideEvidenceWithoutPermission(String role) {
        // when / then
        assertThat(EvidenceVisibility.mayView(TestTokens.actor(role))).isFalse();
    }
}
