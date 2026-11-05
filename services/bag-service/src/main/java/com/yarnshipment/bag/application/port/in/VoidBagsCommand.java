package com.yarnshipment.bag.application.port.in;

import com.yarnshipment.common.security.Actor;
import java.util.List;

/**
 * Çuval iptal isteği.
 *
 * @param reason iptal gerekçesi; boş olamaz
 */
public record VoidBagsCommand(Actor actor, List<String> uniqueKeys, String reason) {}
