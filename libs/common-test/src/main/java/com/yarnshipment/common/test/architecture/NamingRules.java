package com.yarnshipment.common.test.architecture;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.methods;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noMethods;

import com.tngtech.archunit.base.DescribedPredicate;
import com.tngtech.archunit.core.domain.JavaModifier;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

/** Checkstyle'ın göremediği adlandırma kuralları (kod standartları Bölüm 3, Ek C.3). */
public final class NamingRules {

    @ArchTest
    public static final ArchRule NO_VAGUE_CLASS_NAMES = noClasses()
            .should(ArchitectureConditions.haveSimpleNameEndingWithAnyOf(RuleConstants.FORBIDDEN_SUFFIXES))
            .orShould()
            .haveSimpleNameStartingWith("Base")
            .because("NAME-13: sınıf adı tek bir sorumluluğu anlatır");

    @ArchTest
    public static final ArchRule HANDLER_ONLY_FOR_ERROR_HANDLERS = classes()
            .that()
            .haveSimpleNameEndingWith("Handler")
            .should()
            .haveSimpleNameEndingWith("ErrorHandler")
            .orShould()
            .haveSimpleNameEndingWith("ExceptionHandler")
            .because("NAME-13: Handler soneki yalnızca Spring ve Kafka hata işleyicilerinde");

    @ArchTest
    public static final ArchRule EXCEPTIONS_END_WITH_EXCEPTION = classes()
            .that()
            .areAssignableTo(Throwable.class)
            .should()
            .haveSimpleNameEndingWith("Exception")
            .because("NAME-15: istisna sınıfları Exception ile biter");

    @ArchTest
    public static final ArchRule NO_GENERIC_EXCEPTION_NAMES = noClasses()
            .should()
            .haveSimpleName("GeneralException")
            .orShould()
            .haveSimpleName("ServiceException")
            .because("NAME-15: istisna adı neyin yanlış gittiğini söyler");

    @ArchTest
    public static final ArchRule NO_I_PREFIXED_INTERFACES = noClasses()
            .that()
            .areInterfaces()
            .should()
            .haveNameMatching(".*\\.I[A-Z][^.]*$")
            .because("NAME-16: arayüz adında I öneki kullanılmaz");

    @ArchTest
    public static final ArchRule ABSTRACT_CLASSES_ARE_COMMON_AND_PREFIXED = classes()
            .that()
            .haveModifier(JavaModifier.ABSTRACT)
            .and()
            .areNotInterfaces()
            .and()
            .areNotEnums()
            .and(DescribedPredicate.not(ArchitectureConditions.sealedTypes()))
            .should()
            .haveSimpleNameStartingWith("Abstract")
            .andShould()
            .resideInAPackage("..common..")
            .because("NAME-17: soyut sınıf yalnızca libs/common-* içinde ve Abstract önekiyle");

    @ArchTest
    public static final ArchRule NO_VAGUE_METHOD_NAMES = noMethods()
            .that()
            .areDeclaredInClassesThat()
            .resideInAPackage("com.yarnshipment..")
            .and(DescribedPredicate.not(ArchitectureConditions.overridingExternalMethods()))
            .and()
            .areNotAnnotatedWith("org.springframework.web.bind.annotation.ExceptionHandler")
            .should()
            .haveNameMatching(RuleConstants.FORBIDDEN_METHOD_NAMES)
            .because("NAME-21: metot adı ne yaptığını söyler");

    @ArchTest
    public static final ArchRule USE_CASES_ARE_SINGLE_METHOD_INTERFACES = classes()
            .that()
            .resideInAPackage("..application.port.in..")
            .and()
            .areInterfaces()
            .and()
            .doNotHaveSimpleName(RuleConstants.PACKAGE_INFO)
            .should()
            .haveSimpleNameEndingWith("UseCase")
            .andShould(ArchitectureConditions.declareExactlyOneMethod())
            .because("NAME-22: use case arayüzü tek metotludur");

    @ArchTest
    public static final ArchRule USE_CASE_METHOD_IS_PUBLIC = methods()
            .that()
            .areDeclaredInClassesThat()
            .haveSimpleNameEndingWith("UseCase")
            .should()
            .bePublic()
            .because("NAME-22");

    private NamingRules() {}
}
