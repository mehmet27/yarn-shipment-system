package com.yarnshipment.common.web.config;

import com.yarnshipment.common.web.problem.CommonProblemAdvice;
import com.yarnshipment.common.web.problem.MvcProblemAdvice;
import com.yarnshipment.common.web.problem.ProblemFactory;
import com.yarnshipment.common.web.problem.UnhandledExceptionResolver;
import com.yarnshipment.common.web.request.RequestIdFilter;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.MessageSource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import tools.jackson.databind.json.JsonMapper;

/** Ortak hata üretici, hata işleyici ve istek kimliği filtresi. */
@Configuration(proxyBeanMethods = false)
public final class WebComponentsConfiguration {

    /** Karakter kodlaması ve gözlem filtrelerinden hemen sonra. */
    private static final int AFTER_SERVLET_BASICS = 10;

    @Bean
    ProblemFactory problemFactory(MessageSource messageSource) {
        return new ProblemFactory(messageSource);
    }

    @Bean
    CommonProblemAdvice commonProblemAdvice(ProblemFactory problemFactory) {
        return new CommonProblemAdvice(problemFactory);
    }

    @Bean
    MvcProblemAdvice mvcProblemAdvice(ProblemFactory problemFactory) {
        return new MvcProblemAdvice(problemFactory);
    }

    /** Karşılanmayan istisnalar için son durak: loglar ve ayrıntısız 500 döner. */
    @Bean
    UnhandledExceptionResolver unhandledExceptionResolver(
            ProblemFactory problemFactory, ObjectProvider<JsonMapper> jsonMapper) {
        return new UnhandledExceptionResolver(problemFactory, jsonMapper.getIfAvailable(JsonMapper::shared));
    }

    /** Güvenlik zincirinden ve diğer filtrelerden önce çalışır; 401/403 cevapları da istek kimliğini taşır. */
    @Bean
    FilterRegistrationBean<RequestIdFilter> requestIdFilter() {
        FilterRegistrationBean<RequestIdFilter> registration = new FilterRegistrationBean<>(new RequestIdFilter());
        registration.setOrder(Ordered.HIGHEST_PRECEDENCE + AFTER_SERVLET_BASICS);
        return registration;
    }
}
