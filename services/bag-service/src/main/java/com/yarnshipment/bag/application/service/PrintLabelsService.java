package com.yarnshipment.bag.application.service;

import com.yarnshipment.bag.application.port.in.BagNotFoundException;
import com.yarnshipment.bag.application.port.in.PrintLabelsCommand;
import com.yarnshipment.bag.application.port.in.PrintLabelsUseCase;
import com.yarnshipment.bag.application.port.out.BagQueryPort;
import com.yarnshipment.bag.application.port.out.LabelPrintPort;
import com.yarnshipment.bag.application.port.out.LabelRenderer;
import com.yarnshipment.bag.domain.model.Bag;
import com.yarnshipment.bag.domain.model.BagLabel;
import com.yarnshipment.bag.domain.model.BagNotAvailableException;
import com.yarnshipment.bag.domain.model.BagStatus;
import com.yarnshipment.bag.domain.model.LabelReprintReasonRequiredException;
import com.yarnshipment.common.security.Permission;
import com.yarnshipment.common.security.scope.ScopeGuard;
import java.time.Clock;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Etiketleri basar. Etiket yalnızca depodaki (oluşturulmuş ya da sevkiyata ayrılmış) çuvala basılır; yola çıkmış
 * çuvala yeni etiket basmak etiket değiştirme riskidir (rapor Bölüm 10). Daha önce basılmış çuval gerekçe ister.
 */
@Service
public class PrintLabelsService implements PrintLabelsUseCase {

    private static final Set<BagStatus> PRINTABLE = EnumSet.of(BagStatus.CREATED, BagStatus.RESERVED);

    private final BagQueryPort bags;
    private final LabelPrintPort prints;
    private final LabelRenderer renderer;
    private final ScopeGuard scopeGuard;
    private final Clock clock;

    public PrintLabelsService(
            BagQueryPort bags, LabelPrintPort prints, LabelRenderer renderer, ScopeGuard scopeGuard, Clock clock) {
        this.bags = bags;
        this.prints = prints;
        this.renderer = renderer;
        this.scopeGuard = scopeGuard;
        this.clock = clock;
    }

    @Override
    @Transactional
    public byte[] printLabels(PrintLabelsCommand command) {
        List<Bag> found = inRequestOrder(command.uniqueKeys(), bags.findByUniqueKeys(command.uniqueKeys()));
        BagScopes.validateAll(scopeGuard, command.actor(), found, Permission.BAG_CREATE.name());
        found.stream()
                .filter(bag -> !PRINTABLE.contains(bag.status()))
                .findFirst()
                .ifPresent(bag -> {
                    throw new BagNotAvailableException(bag.uniqueKey(), bag.status());
                });
        Map<UUID, Integer> printed = prints.lockAndCountPrints(found.stream().map(Bag::id).toList());
        List<BagLabel> labels = found.stream()
                .map(bag -> new BagLabel(bag, printed.getOrDefault(bag.id(), 0) + 1))
                .toList();
        String reason = normalize(command.reprintReason());
        labels.stream()
                .filter(label -> label.isReprint() && reason == null)
                .findFirst()
                .ifPresent(label -> {
                    throw new LabelReprintReasonRequiredException(label.bag().uniqueKey());
                });
        prints.record(labels, reason, command.actor().userId(), clock.instant());
        return renderer.render(labels);
    }

    /** Etiketler istenen sırayla basılır; bulunamayan anahtar varsa hiçbiri basılmaz. */
    private static List<Bag> inRequestOrder(List<String> uniqueKeys, List<Bag> found) {
        Map<String, Bag> byKey = found.stream().collect(Collectors.toMap(Bag::uniqueKey, Function.identity()));
        return uniqueKeys.stream()
                .map(key -> {
                    Bag bag = byKey.get(key);
                    if (bag == null) {
                        throw new BagNotFoundException(key);
                    }
                    return bag;
                })
                .toList();
    }

    private static @Nullable String normalize(@Nullable String reason) {
        return reason == null || reason.isBlank() ? null : reason.strip();
    }
}
