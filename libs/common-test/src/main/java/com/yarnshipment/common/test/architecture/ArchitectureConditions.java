package com.yarnshipment.common.test.architecture;

import com.tngtech.archunit.base.DescribedPredicate;
import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaMethod;
import com.tngtech.archunit.core.domain.JavaModifier;
import com.tngtech.archunit.core.domain.JavaPackage;
import com.tngtech.archunit.lang.ArchCondition;
import com.tngtech.archunit.lang.ConditionEvents;
import com.tngtech.archunit.lang.SimpleConditionEvent;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.stream.Stream;

/** Ortak kural setinin kullandığı ArchUnit koşul ve yüklemleri (Ek C.3'teki yardımcılar). */
public final class ArchitectureConditions {

    private static final String CONFIGURATION = "org.springframework.context.annotation.Configuration";
    private static final String ENTITY = "jakarta.persistence.Entity";
    private static final String TRANSACTIONAL = "org.springframework.transaction.annotation.Transactional";
    private static final String PRE_AUTHORIZE = "org.springframework.security.access.prepost.PreAuthorize";

    /** Metot seviyesinde konduğunda sınıfın CGLIB proxy'si gerektiren anotasyonlar (SEC-02, @Transactional). */
    private static final List<String> PROXYING_METHOD_ANNOTATIONS = List.of(TRANSACTIONAL, PRE_AUTHORIZE);

    private static final List<Predicate<JavaClass>> NOT_FINAL_CANDIDATES = List.of(
            JavaClass::isInterface,
            JavaClass::isEnum,
            JavaClass::isRecord,
            JavaClass::isAnnotation,
            JavaClass::isAnonymousClass,
            javaClass -> javaClass.getModifiers().contains(JavaModifier.ABSTRACT),
            ArchitectureConditions::isFrameworkProxied);

    private ArchitectureConditions() {}

    /** Verilen paketler dışındaki Spring tipleri. */
    public static DescribedPredicate<JavaClass> springTypesOtherThan(String... allowedPackages) {
        List<String> allowed = List.of(allowedPackages);
        return DescribedPredicate.describe(
                "Spring types other than " + allowed,
                javaClass -> javaClass.getPackageName().startsWith("org.springframework")
                        && allowed.stream().noneMatch(javaClass.getPackageName()::startsWith));
    }

    /** NAME-13: adı yasaklı soneklerden biriyle biten sınıflar. */
    public static ArchCondition<JavaClass> haveSimpleNameEndingWithAnyOf(String... suffixes) {
        return new ArchCondition<>("have simple name ending with any of " + Arrays.toString(suffixes)) {
            @Override
            public void check(JavaClass javaClass, ConditionEvents events) {
                boolean matches = Arrays.stream(suffixes).anyMatch(javaClass.getSimpleName()::endsWith);
                events.add(new SimpleConditionEvent(javaClass, matches, javaClass.getName()));
            }
        };
    }

    /** STRUC-08: {@code boolean} veya {@code Boolean} parametre taşımayan metotlar. */
    public static ArchCondition<JavaMethod> notHaveBooleanParameters() {
        return new ArchCondition<>("not have boolean parameters") {
            @Override
            public void check(JavaMethod method, ConditionEvents events) {
                boolean hasBoolean = method.getRawParameterTypes().stream()
                        .anyMatch(type -> type.isEquivalentTo(boolean.class) || type.isEquivalentTo(Boolean.class));
                events.add(new SimpleConditionEvent(method, !hasBoolean, method.getFullName()));
            }
        };
    }

    /** NULL-01: sınıfın paketinde {@code @NullMarked} taşıyan bir {@code package-info.java} bulunur. */
    public static ArchCondition<JavaClass> resideInNullMarkedPackage() {
        return new ArchCondition<>("reside in a package annotated with @NullMarked") {
            @Override
            public void check(JavaClass javaClass, ConditionEvents events) {
                JavaPackage javaPackage = javaClass.getPackage();
                boolean nullMarked = javaPackage.tryGetPackageInfo().isPresent()
                        && javaPackage.isAnnotatedWith("org.jspecify.annotations.NullMarked");
                events.add(new SimpleConditionEvent(javaClass, nullMarked, javaPackage.getName()));
            }
        };
    }

    /** NAME-22: tek metotlu arayüzler. */
    public static ArchCondition<JavaClass> declareExactlyOneMethod() {
        return new ArchCondition<>("declare exactly one method") {
            @Override
            public void check(JavaClass javaClass, ConditionEvents events) {
                long count = javaClass.getMethods().stream()
                        .filter(method -> !method.getModifiers().contains(JavaModifier.SYNTHETIC))
                        .count();
                events.add(new SimpleConditionEvent(javaClass, count == 1, javaClass.getName() + " has " + count));
            }
        };
    }

    /** STRUC-02: kalıtım için tasarlanmamış somut sınıflar ({@code final} olması gerekenler). */
    public static DescribedPredicate<JavaClass> concreteClassesNotDesignedForExtension() {
        return DescribedPredicate.describe(
                "concrete classes not designed for extension",
                javaClass -> NOT_FINAL_CANDIDATES.stream().noneMatch(exclusion -> exclusion.test(javaClass)));
    }

    /** NAME-21: bir framework veya JDK tipindeki metodu ezen metotlar adlandırma kuralı dışındadır. */
    public static DescribedPredicate<JavaMethod> overridingExternalMethods() {
        return DescribedPredicate.describe(
                "overriding a method of a non-project type",
                method -> supertypesOf(method.getOwner())
                        .filter(type -> !type.getPackageName().startsWith("com.yarnshipment"))
                        .anyMatch(type -> declaresMethodNamed(type, method.getName())));
    }

    /** NAME-17: kapalı ({@code sealed}) hiyerarşi kökleri (ERR-01'deki {@code DomainException} gibi). */
    public static DescribedPredicate<JavaClass> sealedTypes() {
        return DescribedPredicate.describe("sealed types", ArchitectureConditions::isSealed);
    }

    private static boolean isFrameworkProxied(JavaClass javaClass) {
        return javaClass.isMetaAnnotatedWith(CONFIGURATION)
                || javaClass.isAnnotatedWith(ENTITY)
                || javaClass.isAnnotatedWith(TRANSACTIONAL)
                || hasProxyingMethod(javaClass);
    }

    private static boolean hasProxyingMethod(JavaClass javaClass) {
        return javaClass.getMethods().stream()
                .anyMatch(method -> PROXYING_METHOD_ANNOTATIONS.stream().anyMatch(method::isAnnotatedWith));
    }

    private static Stream<JavaClass> supertypesOf(JavaClass javaClass) {
        return Stream.concat(javaClass.getAllRawSuperclasses().stream(), javaClass.getAllRawInterfaces().stream());
    }

    private static boolean declaresMethodNamed(JavaClass type, String name) {
        return type.getMethods().stream().anyMatch(method -> method.getName().equals(name));
    }

    private static boolean isSealed(JavaClass javaClass) {
        return load(javaClass).map(Class::isSealed).orElse(false);
    }

    private static Optional<Class<?>> load(JavaClass javaClass) {
        try {
            return Optional.of(Class.forName(
                    javaClass.getName(), false, Thread.currentThread().getContextClassLoader()));
        } catch (ClassNotFoundException | LinkageError _) {
            // Sınıf yüklenemiyorsa sealed olduğu kanıtlanamaz; kural güvenli tarafta kalır.
            return Optional.empty();
        }
    }
}
