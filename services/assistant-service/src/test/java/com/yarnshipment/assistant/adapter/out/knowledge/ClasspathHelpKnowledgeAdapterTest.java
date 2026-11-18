package com.yarnshipment.assistant.adapter.out.knowledge;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** Paketteki yardım belgeleri okunur ve her seferinde aynı metni verir. */
class ClasspathHelpKnowledgeAdapterTest {

    @Test
    @DisplayName("Süreç analizi ve yetki belgeleri tek metinde, aynı sırayla birleşir")
    void shouldLoadBundledDocumentsDeterministically() {
        ClasspathHelpKnowledgeAdapter adapter = new ClasspathHelpKnowledgeAdapter();

        String first = adapter.documents();

        assertThat(first)
                .contains("<belge adi=\"surec-analizi-v2.md\">")
                .contains("<belge adi=\"giris-hesabi-acma.md\">");
        assertThat(new ClasspathHelpKnowledgeAdapter().documents()).isEqualTo(first);
    }

    @Test
    @DisplayName("Rol yetkileri koddan üretilir: Kalite kumaş girişi yapabilir, Depo yapamaz")
    void shouldDescribeRolesFromPermissionMatrix() {
        String text = new ClasspathHelpKnowledgeAdapter().documents();
        int quality = text.indexOf("Kalite rolü şunları yapabilir");
        int warehouse = text.indexOf("Depo rolü şunları yapabilir");

        assertThat(quality).isPositive();
        assertThat(warehouse).isPositive();
        assertThat(text.substring(quality, text.indexOf("rolü şunları", quality + "Kalite rolü".length())))
                .contains("kumaşı kaydeder (kumaş girişi)");
        assertThat(text.substring(warehouse, text.indexOf("rolü şunları", warehouse + "Depo rolü".length())))
                .doesNotContain("kumaş girişi");
    }

    @Test
    @DisplayName("Her iznin günlük dilde bir açıklaması vardır")
    void shouldLabelEveryPermission() {
        for (com.yarnshipment.common.security.Permission permission :
                com.yarnshipment.common.security.Permission.values()) {
            assertThat(RoleGuide.label(permission)).isNotBlank();
        }
    }
}
