package com.yarnshipment.assistant.adapter.out.knowledge;

import com.yarnshipment.assistant.application.port.out.HelpKnowledgePort;
import com.yarnshipment.assistant.domain.model.HelpUnavailableException;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Comparator;
import org.jspecify.annotations.Nullable;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.stereotype.Component;

/**
 * Yardım dokümanlarını paketin {@code help/} klasöründen okur. Dosyalar derleme sırasında {@code docs/} klasöründen
 * kopyalanır (assistant-service/pom.xml); böylece asistan ile doküman ayrı ayrı eskimez. Metin ilk kullanımda bir kez
 * okunur ve hep aynı sırada birleştirilir; sıra ya da içerik değişirse önbellek boşa gider.
 */
@Component
final class ClasspathHelpKnowledgeAdapter implements HelpKnowledgePort {

    private static final String PATTERN = "classpath*:help/**/*.md";

    private @Nullable String cached;

    @Override
    public synchronized String documents() {
        if (cached == null) {
            cached = load();
        }
        return cached;
    }

    private static String load() {
        try {
            Resource[] resources = new PathMatchingResourcePatternResolver().getResources(PATTERN);
            if (resources.length == 0) {
                throw new HelpUnavailableException("no help documents on the classpath");
            }
            StringBuilder text = new StringBuilder();
            Arrays.stream(resources)
                    .sorted(Comparator.comparing(resource -> String.valueOf(resource.getFilename())))
                    .forEach(resource -> append(text, resource));
            return text.toString();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static void append(StringBuilder text, Resource resource) {
        try {
            text.append("\n\n<belge adi=\"")
                    .append(resource.getFilename())
                    .append("\">\n")
                    .append(new String(resource.getInputStream().readAllBytes(), StandardCharsets.UTF_8))
                    .append("\n</belge>");
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
