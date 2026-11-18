package com.yarnshipment.assistant.adapter.out.anthropic;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/** Yardım asistanı ayarlarını bağlar. */
@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(HelpProperties.class)
class HelpConfiguration {}
