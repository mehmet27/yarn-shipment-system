package com.yarnshipment.common.web;

import com.yarnshipment.common.web.problem.CommonProblemAdvice;
import com.yarnshipment.common.web.problem.MvcProblemAdvice;
import com.yarnshipment.common.web.problem.ProblemFactory;
import com.yarnshipment.common.web.problem.UnhandledExceptionResolver;
import java.util.List;
import org.springframework.context.support.StaticApplicationContext;
import org.springframework.http.converter.json.JacksonJsonHttpMessageConverter;
import org.springframework.web.servlet.HandlerExceptionResolver;
import org.springframework.web.servlet.mvc.method.annotation.ExceptionHandlerExceptionResolver;
import tools.jackson.databind.json.JsonMapper;

/** Standalone MockMvc için uygulamadaki çözücü sırası: önce advice'lar, en sonda karşılanmayan istisna çözücüsü. */
final class TestExceptionResolvers {

    private TestExceptionResolvers() {}

    static List<HandlerExceptionResolver> inApplicationOrder(ProblemFactory problems) {
        StaticApplicationContext context = new StaticApplicationContext();
        context.registerBean(CommonProblemAdvice.class, () -> new CommonProblemAdvice(problems));
        context.registerBean(MvcProblemAdvice.class, () -> new MvcProblemAdvice(problems));
        context.refresh();
        ExceptionHandlerExceptionResolver advices = new ExceptionHandlerExceptionResolver();
        advices.setApplicationContext(context);
        advices.setMessageConverters(List.of(new JacksonJsonHttpMessageConverter()));
        advices.afterPropertiesSet();
        return List.of(advices, new UnhandledExceptionResolver(problems, JsonMapper.shared()));
    }
}
