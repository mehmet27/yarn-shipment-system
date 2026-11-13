package com.yarnshipment.common.security.config;

import com.yarnshipment.common.security.session.RegistrySessionTerminator;
import com.yarnshipment.common.security.session.SessionAuthenticator;
import com.yarnshipment.common.security.session.SessionTerminator;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.core.session.SessionRegistry;
import org.springframework.security.core.session.SessionRegistryImpl;
import org.springframework.security.web.session.HttpSessionEventPublisher;

/** Oturum defteri, oturum açma ve sonlandırma bileşenleri. */
@Configuration(proxyBeanMethods = false)
class SessionConfiguration {

    @Bean
    SessionRegistry sessionRegistry() {
        return new SessionRegistryImpl();
    }

    /** Oturum bitince kaydını defterden siler. */
    @Bean
    HttpSessionEventPublisher httpSessionEventPublisher() {
        return new HttpSessionEventPublisher();
    }

    @Bean
    SessionTerminator sessionTerminator(SessionRegistry sessionRegistry) {
        return new RegistrySessionTerminator(sessionRegistry);
    }

    @Bean
    SessionAuthenticator sessionAuthenticator(SessionRegistry sessionRegistry) {
        return new SessionAuthenticator(sessionRegistry);
    }

    /**
     * Giriş denetimi uygulamanın kendi servisindedir; Spring Boot'un geliştirme amaçlı rastgele şifreli kullanıcısı
     * devreye girmesin diye hiçbir kullanıcı vermeyen bir kayıt tanımlanır.
     */
    @Bean
    UserDetailsService noDefaultUserDetailsService() {
        return username -> {
            throw new UsernameNotFoundException("Kullanıcı yok");
        };
    }
}
