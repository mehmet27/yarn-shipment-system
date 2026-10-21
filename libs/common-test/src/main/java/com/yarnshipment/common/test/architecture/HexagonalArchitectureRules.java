package com.yarnshipment.common.test.architecture;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.methods;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.library.dependencies.SlicesRuleDefinition;

/**
 * Altıgen mimari katman kuralları (mimari doküman ARCH-01…08) ve kalıcılık katmanı kuralları (PERS-08, PERS-17).
 * Bağımlılık yönü dışarıdan içeriye doğrudur: {@code adapter → application → domain}.
 */
public final class HexagonalArchitectureRules {

    @ArchTest
    public static final ArchRule DOMAIN_IS_FRAMEWORK_FREE = noClasses()
            .that()
            .resideInAPackage(RuleConstants.DOMAIN)
            .should()
            .dependOnClassesThat()
            .resideInAnyPackage(
                    "org.springframework..",
                    "jakarta..",
                    "org.hibernate..",
                    "tools.jackson..",
                    "com.fasterxml..",
                    "org.apache.kafka..",
                    "org.slf4j..")
            .because("ARCH-01: domain katmanı framework bağımsızdır");

    @ArchTest
    public static final ArchRule DOMAIN_DOES_NOT_DEPEND_ON_OUTER_LAYERS = noClasses()
            .that()
            .resideInAPackage(RuleConstants.DOMAIN)
            .should()
            .dependOnClassesThat()
            .resideInAnyPackage(RuleConstants.APPLICATION, "..adapter..", "..config..")
            .because("ARCH-01: domain yalnızca kendi paketine bağımlıdır");

    @ArchTest
    public static final ArchRule APPLICATION_DOES_NOT_DEPEND_ON_ADAPTERS = noClasses()
            .that()
            .resideInAPackage(RuleConstants.APPLICATION)
            .should()
            .dependOnClassesThat()
            .resideInAPackage("..adapter..")
            .because("ARCH-02: application katmanı adapter'ları görmez");

    @ArchTest
    public static final ArchRule APPLICATION_USES_ONLY_ALLOWED_SPRING_TYPES = noClasses()
            .that()
            .resideInAPackage(RuleConstants.APPLICATION)
            .should()
            .dependOnClassesThat(ArchitectureConditions.springTypesOtherThan(
                    "org.springframework.stereotype", "org.springframework.transaction.annotation"))
            .because("ARCH-02: application yalnızca stereotype ve @Transactional kullanır");

    @ArchTest
    public static final ArchRule INBOUND_ADAPTERS_DO_NOT_SEE_OUTBOUND_ADAPTERS = noClasses()
            .that()
            .resideInAPackage(RuleConstants.ADAPTER_IN)
            .should()
            .dependOnClassesThat()
            .resideInAPackage(RuleConstants.ADAPTER_OUT)
            .because("ARCH-03: adapter'lar birbirine port üzerinden ulaşır");

    @ArchTest
    public static final ArchRule OUTBOUND_ADAPTERS_DO_NOT_SEE_INBOUND_ADAPTERS = noClasses()
            .that()
            .resideInAPackage(RuleConstants.ADAPTER_OUT)
            .should()
            .dependOnClassesThat()
            .resideInAPackage(RuleConstants.ADAPTER_IN)
            .because("ARCH-03: adapter'lar birbirine port üzerinden ulaşır");

    @ArchTest
    public static final ArchRule CONTROLLERS_CALL_ONLY_INBOUND_PORTS = noClasses()
            .that()
            .resideInAPackage(RuleConstants.WEB)
            .should()
            .dependOnClassesThat()
            .resideInAnyPackage(RuleConstants.APPLICATION_SERVICE, "..application.port.out..", "..domain.service..")
            .because("ARCH-04: controller'lar yalnızca port.in arayüzlerini çağırır");

    @ArchTest
    public static final ArchRule JPA_ENTITIES_STAY_IN_PERSISTENCE_ADAPTER = classes()
            .that()
            .haveSimpleNameEndingWith("JpaEntity")
            .should()
            .resideInAPackage(RuleConstants.PERSISTENCE)
            .andShould()
            .onlyHaveDependentClassesThat()
            .resideInAPackage(RuleConstants.PERSISTENCE)
            .because("ARCH-05: kalıcılık modeli persistence adapter'ı dışına sızmaz");

    @ArchTest
    public static final ArchRule WEB_DTOS_STAY_IN_WEB_ADAPTER = classes()
            .that()
            .resideInAPackage(RuleConstants.WEB)
            .and()
            .haveNameMatching(".*(Request|Response|View)$")
            .should()
            .onlyHaveDependentClassesThat()
            .resideInAPackage(RuleConstants.WEB)
            .because("ARCH-06: web DTO'ları yalnızca adapter.in.web içinde yaşar");

    @ArchTest
    public static final ArchRule KAFKA_TEMPLATE_ONLY_IN_OUTBOX_PUBLISHER = noClasses()
            .that()
            .resideOutsideOfPackage("com.yarnshipment.common.outbox..")
            .should()
            .dependOnClassesThat()
            .haveFullyQualifiedName("org.springframework.kafka.core.KafkaTemplate")
            .because("ARCH-07: servis kodu olayı outbox'a yazar");

    @ArchTest
    public static final ArchRule PACKAGES_ARE_FREE_OF_CYCLES = SlicesRuleDefinition.slices()
            .matching("com.yarnshipment.(**)")
            .should()
            .beFreeOfCycles()
            .because("ARCH-08: döngüsel paket bağımlılığı yasaktır");

    @ArchTest
    public static final ArchRule SPRING_DATA_REPOSITORIES_ARE_PACKAGE_PRIVATE_IN_PERSISTENCE = classes()
            .that()
            .areAssignableTo("org.springframework.data.repository.Repository")
            .and()
            .areInterfaces()
            .and()
            .doNotHaveFullyQualifiedName("org.springframework.data.repository.Repository")
            .should()
            .resideInAPackage(RuleConstants.PERSISTENCE)
            .andShould()
            .notBePublic()
            .because("PERS-08: Spring Data repository'leri persistence adapter'ında paket-özeldir");

    @ArchTest
    public static final ArchRule TRANSACTIONAL_METHODS_ONLY_IN_APPLICATION_SERVICES = methods()
            .that()
            .areAnnotatedWith(RuleConstants.TRANSACTIONAL)
            .should()
            .beDeclaredInClassesThat()
            .resideInAPackage(RuleConstants.APPLICATION_SERVICE)
            .andShould()
            .bePublic()
            .because("PERS-17: @Transactional yalnızca application.service public metotlarında");

    @ArchTest
    public static final ArchRule TRANSACTIONAL_CLASSES_ONLY_IN_APPLICATION_SERVICES = classes()
            .that()
            .areAnnotatedWith(RuleConstants.TRANSACTIONAL)
            .should()
            .resideInAPackage(RuleConstants.APPLICATION_SERVICE)
            .because("PERS-17: @Transactional yalnızca application.service sınıflarında");

    private HexagonalArchitectureRules() {}
}
