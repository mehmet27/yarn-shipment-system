package com.yarnshipment.bag.application.port.in;

import com.yarnshipment.common.security.Actor;
import java.util.List;
import org.jspecify.annotations.Nullable;

/**
 * Çuval etiketlerini basma isteği.
 *
 * @param reprintReason daha önce basılmış bir çuval varsa zorunlu
 */
public record PrintLabelsCommand(Actor actor, List<String> uniqueKeys, @Nullable String reprintReason) {}
