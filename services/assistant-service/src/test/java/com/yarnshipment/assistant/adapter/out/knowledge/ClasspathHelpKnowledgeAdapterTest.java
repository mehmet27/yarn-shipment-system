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
}
