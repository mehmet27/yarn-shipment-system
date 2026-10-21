package com.yarnshipment.common.test.architecture;

/** Kural sınıflarının paylaştığı paket kalıpları ve tip adları. */
final class RuleConstants {

    static final String DOMAIN = "..domain..";
    static final String APPLICATION = "..application..";
    static final String APPLICATION_SERVICE = "..application.service..";
    static final String ADAPTER_IN = "..adapter.in..";
    static final String ADAPTER_OUT = "..adapter.out..";
    static final String WEB = "..adapter.in.web..";
    static final String PERSISTENCE = "..adapter.out.persistence..";
    static final String TRANSACTIONAL = "org.springframework.transaction.annotation.Transactional";
    static final String AUTOWIRED = "org.springframework.beans.factory.annotation.Autowired";
    static final String DISPLAY_NAME = "org.junit.jupiter.api.DisplayName";
    static final String PACKAGE_INFO = "package-info";
    static final int MAX_TYPES_PER_PACKAGE = 30;

    /** NAME-13: yasaklı sınıf adı sonekleri. */
    static final String[] FORBIDDEN_SUFFIXES = {
        "Manager", "Helper", "Util", "Utils", "Impl", "Data", "Info", "Processor"
    };

    /** NAME-21: yasaklı belirsiz metot adları; {@code do…}, {@code manage…}, {@code perform…} önekleri dahil. */
    static final String FORBIDDEN_METHOD_NAMES =
            "^(process|handle|doIt|do[A-Z].*|manage.*|perform.*|execute|run|data|info|check|update)$";

    private RuleConstants() {}
}
