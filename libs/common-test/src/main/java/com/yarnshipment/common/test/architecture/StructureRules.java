package com.yarnshipment.common.test.architecture;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.methods;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noFields;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noMethods;

import com.tngtech.archunit.base.DescribedPredicate;
import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaModifier;
import com.tngtech.archunit.core.domain.JavaPackage;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchCondition;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.lang.ConditionEvents;
import com.tngtech.archunit.lang.SimpleConditionEvent;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZonedDateTime;

/** Yapı, zaman, enjeksiyon ve null güvenliği kuralları (STRUC-02, STRUC-08, SIZE-12, DOM-10, CONF-05, NULL-01). */
public final class StructureRules {

    @ArchTest
    public static final ArchRule CONCRETE_CLASSES_ARE_FINAL = classes()
            .that(ArchitectureConditions.concreteClassesNotDesignedForExtension())
            .and()
            .areTopLevelClasses()
            .should()
            .haveModifier(JavaModifier.FINAL)
            .because("STRUC-02: kalıtım için tasarlanmamış somut sınıflar final'dır");

    @ArchTest
    public static final ArchRule NO_BOOLEAN_FLAG_PARAMETERS = methods()
            .that()
            .arePublic()
            .and()
            .areDeclaredInClassesThat(DescribedPredicate.not(JavaClass.Predicates.RECORDS))
            .and(DescribedPredicate.not(ArchitectureConditions.overridingExternalMethods()))
            .should(ArchitectureConditions.notHaveBooleanParameters())
            .because("STRUC-08: bayrak argümanı yerine ayrı metot veya enum");

    @ArchTest
    public static final ArchRule PACKAGES_HAVE_AT_MOST_30_TYPES = classes()
            .that()
            .areTopLevelClasses()
            .should(haveAtMostTypesInPackage(RuleConstants.MAX_TYPES_PER_PACKAGE))
            .because("SIZE-12: bir pakette en fazla 30 üst düzey tip");

    @ArchTest
    public static final ArchRule TIME_COMES_FROM_INJECTED_CLOCK = noClasses()
            .should()
            .callMethod(Instant.class, "now")
            .orShould()
            .callMethod(LocalDate.class, "now")
            .orShould()
            .callMethod(LocalDateTime.class, "now")
            .orShould()
            .callMethod(OffsetDateTime.class, "now")
            .orShould()
            .callMethod(ZonedDateTime.class, "now")
            .orShould()
            .callMethod(System.class, "currentTimeMillis")
            .because("DOM-10: zaman enjekte edilen Clock'tan alınır");

    @ArchTest
    public static final ArchRule NO_FIELD_INJECTION = noFields()
            .should()
            .beAnnotatedWith(RuleConstants.AUTOWIRED)
            .because("CONF-05: bağımlılık yalnızca yapıcıyla enjekte edilir");

    @ArchTest
    public static final ArchRule NO_SETTER_INJECTION = noMethods()
            .should()
            .beAnnotatedWith(RuleConstants.AUTOWIRED)
            .because("CONF-05: bağımlılık yalnızca yapıcıyla enjekte edilir");

    @ArchTest
    public static final ArchRule PACKAGES_ARE_NULL_MARKED = classes()
            .that()
            .resideInAPackage("com.yarnshipment..")
            .should(ArchitectureConditions.resideInNullMarkedPackage())
            .because("NULL-01, STR-06: her pakette @NullMarked package-info.java bulunur");

    private StructureRules() {}

    private static ArchCondition<JavaClass> haveAtMostTypesInPackage(int maximum) {
        return new ArchCondition<>("reside in a package with at most " + maximum + " top level types") {
            @Override
            public void check(JavaClass javaClass, ConditionEvents events) {
                JavaPackage javaPackage = javaClass.getPackage();
                long count = javaPackage.getClasses().stream()
                        .filter(JavaClass::isTopLevelClass)
                        .filter(type -> !type.getSimpleName().equals(RuleConstants.PACKAGE_INFO))
                        .count();
                String message = javaPackage.getName() + " has " + count + " top level types";
                events.add(new SimpleConditionEvent(javaClass, count <= maximum, message));
            }
        };
    }
}
