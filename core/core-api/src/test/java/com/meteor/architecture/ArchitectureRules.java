package com.meteor.architecture;

import com.tngtech.archunit.base.DescribedPredicate;
import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaMethod;
import com.tngtech.archunit.core.domain.JavaMethodCall;
import com.tngtech.archunit.core.domain.properties.HasName;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchCondition;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.lang.ConditionEvents;
import com.tngtech.archunit.lang.SimpleConditionEvent;
import com.tngtech.archunit.library.dependencies.SliceAssignment;
import com.tngtech.archunit.library.dependencies.SliceIdentifier;
import com.tngtech.archunit.library.dependencies.SlicesRuleDefinition;
import jakarta.persistence.Entity;

import org.springframework.transaction.annotation.Transactional;

import static com.tngtech.archunit.core.domain.JavaClass.Predicates.resideInAPackage;
import static com.tngtech.archunit.core.domain.JavaClass.Predicates.resideInAnyPackage;
import static com.tngtech.archunit.core.domain.JavaClass.Predicates.simpleNameEndingWith;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.methods;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noFields;

/**
 * docs/ARCHITECTURE.md 의 구조 규칙(R-xx). 규칙 ID 와 필드 이름이 대응한다.
 *
 * <p>
 * 패키지 규약: {@code com.meteor.<context>.{domain|application|api|storage|clients}}.
 * {@code com.meteor.shared} 와 {@code com.meteor.support} 는 컨텍스트가 아닌 공통 영역이다.
 *
 * <p>
 * 컨텍스트의 네 계층은 모두 core-api 모듈 안에 있다. 계층 사이 경계는 Gradle 모듈이 아니라 이 규칙들이 강제한다. 특히 domain 이
 * Spring·JPA 를 모른다는 것(R-01)은 컴파일러가 아니라 여기서 잡힌다.
 */
@AnalyzeClasses(packages = "com.meteor", importOptions = ImportOption.DoNotIncludeTests.class)
class ArchitectureRules {

    private static final String DOMAIN = "..domain..";

    private static final String APPLICATION = "..application..";

    private static final String API = "..api..";

    private static final String STORAGE = "..storage..";

    private static final String CLIENTS = "..clients..";

    private static final String SHARED = "com.meteor.shared..";

    private static final String SUPPORT = "com.meteor.support..";

    private static final String ERROR = "com.meteor.support.error..";

    @ArchTest
    static final ArchRule R01_domain_knows_nothing_about_framework_or_outer_layers = noClasses().that()
        .resideInAPackage(DOMAIN)
        .should()
        .dependOnClassesThat()
        .resideInAnyPackage("org.springframework..", "jakarta.persistence..", APPLICATION, API, STORAGE, CLIENTS)
        .as("R-01 domain 은 Spring, JPA, application, api, storage, clients 를 모른다");

    @ArchTest
    static final ArchRule R01_domain_uses_only_error_vocabulary_from_support = noClasses().that()
        .resideInAPackage(DOMAIN)
        .should()
        .dependOnClassesThat(resideInAPackage(SUPPORT).and(DescribedPredicate.not(resideInAPackage(ERROR))))
        .as("R-01 domain 이 support 에서 쓰는 것은 에러 어휘(support.error)뿐이다");

    @ArchTest
    static final ArchRule R01_error_vocabulary_knows_no_framework = noClasses().that()
        .resideInAPackage(ERROR)
        .should()
        .dependOnClassesThat(resideInAPackage("org.springframework..").or(resideInAPackage("jakarta.."))
            .or(resideInAPackage("com.meteor..").and(DescribedPredicate.not(resideInAPackage(ERROR)))))
        .as("R-01 에러 어휘(support.error)는 domain 이 던지므로 프레임워크와 다른 패키지를 모른다");

    @ArchTest
    static final ArchRule R02_transactional_classes_only_in_application = classes().that()
        .areAnnotatedWith(Transactional.class)
        .should()
        .resideInAPackage(APPLICATION)
        .allowEmptyShould(true)
        .as("R-02 @Transactional 클래스는 application 에만 있다");

    @ArchTest
    static final ArchRule R02_transactional_methods_only_in_application = methods().that()
        .areAnnotatedWith(Transactional.class)
        .should()
        .beDeclaredInClassesThat()
        .resideInAPackage(APPLICATION)
        .as("R-02 @Transactional 메서드는 application 에만 있다");

    @ArchTest
    static final ArchRule R03_api_depends_only_on_use_cases = noClasses().that()
        .resideInAPackage(API)
        .should()
        .dependOnClassesThat(resideInAPackage(STORAGE).or(resideInAPackage(CLIENTS))
            .or(domainServices())
            .or(resideInAPackage(APPLICATION).and(simpleNameEndingWith("Facade").or(simpleNameEndingWith("Listener"))
                .or(simpleNameEndingWith("Event")))))
        .as("R-03 api 는 UseCase(와 Command, Query)만 의존한다. storage, clients, *Policy, Facade, Listener, Event 에 의존하지 않는다");

    @ArchTest
    static final ArchRule R03_api_only_reads_aggregates = noClasses().that()
        .resideInAPackage(API)
        .should()
        .callMethodWhere(DescribedPredicate.describe("a state-changing method of a domain aggregate",
                (JavaMethodCall call) -> isAggregate(call.getTargetOwner()) && !isAccessor(call.getTarget().getName())))
        .as("R-03 api 는 UseCase 가 돌려준 애그리거트에서 getter 만 읽는다. 상태를 바꾸는 메서드는 부르지 않는다");

    @ArchTest
    static final ArchRule R04_use_cases_do_not_call_each_other = classes().that()
        .haveSimpleNameEndingWith("UseCase")
        .should(notDependOnOtherClassesNamed("UseCase"))
        .as("R-04 UseCase 는 다른 UseCase 를 호출하지 않는다");

    @ArchTest
    static final ArchRule R05_no_service_suffix_in_domain = noClasses().that()
        .resideInAPackage(DOMAIN)
        .and()
        .haveSimpleNameNotEndingWith("DomainService")
        .should()
        .haveSimpleNameEndingWith("Service")
        .as("R-05 domain 의 서비스는 *Policy 나 *DomainService 로 이름 짓는다. 맨 *Service 는 application 의 UseCase 와 헷갈린다");

    @ArchTest
    static final ArchRule R05_application_classes_are_use_cases_commands_or_results = classes().that()
        .resideInAPackage(APPLICATION)
        .and()
        .areTopLevelClasses()
        .should()
        .haveSimpleNameEndingWith("UseCase")
        .orShould()
        .haveSimpleNameEndingWith("Command")
        .orShould()
        .haveSimpleNameEndingWith("Result")
        .orShould()
        .haveSimpleNameEndingWith("Facade")
        .orShould()
        .haveSimpleNameEndingWith("Event")
        .orShould()
        .haveSimpleNameEndingWith("Listener")
        .orShould()
        .haveSimpleNameEndingWith("Query")
        .orShould()
        .haveSimpleNameEndingWith("View")
        .as("R-05 application 의 클래스는 *UseCase, *Command, *Result, *Facade, *Event, *Listener, *Query, *View 중 하나다");

    @ArchTest
    static final ArchRule R06_adapters_do_not_depend_on_application_or_api = noClasses().that()
        .resideInAnyPackage(STORAGE, CLIENTS)
        .should()
        .dependOnClassesThat()
        .resideInAnyPackage(APPLICATION, API)
        .as("R-06 storage, clients 어댑터는 application, api 에 의존하지 않는다");

    @ArchTest
    static final ArchRule R07_contexts_do_not_depend_on_each_other = SlicesRuleDefinition.slices()
        .assignedFrom(contexts())
        .should()
        .notDependOnEachOther()
        .ignoreDependency(DescribedPredicate.alwaysTrue(),
                simpleNameEndingWith("Facade").or(simpleNameEndingWith("Event")))
        .as("R-07 컨텍스트끼리 직접 의존하지 않는다. 허용 통로는 상대 컨텍스트의 *Facade 와 *Event 뿐");

    @ArchTest
    static final ArchRule R08_entities_live_in_storage_only = classes().that()
        .areAnnotatedWith(Entity.class)
        .should()
        .resideInAPackage(STORAGE)
        .as("R-08 JPA 엔티티는 storage 패키지에만 있다");

    @ArchTest
    static final ArchRule R08_nothing_outside_storage_touches_entities = noClasses().that()
        .resideOutsideOfPackage(STORAGE)
        .should()
        .dependOnClassesThat()
        .areAnnotatedWith(Entity.class)
        .as("R-08 storage 밖의 클래스는 JPA 엔티티를 모른다");

    @ArchTest
    static final ArchRule R09_shared_has_no_dependencies = noClasses().that()
        .resideInAPackage(SHARED)
        .should()
        .dependOnClassesThat(resideInAPackage("com.meteor..").and(DescribedPredicate.not(resideInAPackage(SHARED)))
            .or(resideInAPackage("org.springframework.."))
            .or(resideInAPackage("jakarta..")))
        .as("R-09 shared 는 다른 패키지와 프레임워크에 의존하지 않는다");

    @ArchTest
    static final ArchRule R09_shared_contains_only_records_and_enums = classes().that()
        .resideInAPackage(SHARED)
        .and()
        .areTopLevelClasses()
        .should()
        .beRecords()
        .orShould()
        .beEnums()
        .as("R-09 shared 에는 record 와 enum 만 둔다. 한 컨텍스트만 쓰는 값은 그 컨텍스트의 domain 으로");

    @ArchTest
    static final ArchRule R10_facades_return_decisions_not_models = methods().that()
        .areDeclaredInClassesThat()
        .haveSimpleNameEndingWith("Facade")
        .and()
        .arePublic()
        .should(returnOnlyDecisionTypes())
        .as("R-10 Facade 는 판단 결과만 돌려준다. 반환 타입은 void, 원시 타입, java.lang, shared 의 값뿐이다");

    @ArchTest
    static final ArchRule R11_api_dtos_carry_no_domain_types_except_enums = noFields().that()
        .areDeclaredInClassesThat()
        .resideInAPackage(API)
        .should()
        .haveRawType(resideInAnyPackage(DOMAIN, SHARED).and(DescribedPredicate.not(JavaClass.Predicates.ENUMS)))
        .as("R-11 api 의 요청·응답 객체는 enum 을 제외한 도메인 타입을 필드로 갖지 않는다. 값 객체는 원시 타입이나 api 전용 record 로 푼다");

    /** 컨텍스트 = com.meteor 바로 아래 패키지. shared 와 support 는 컨텍스트가 아니므로 제외한다. */
    private static SliceAssignment contexts() {
        return new SliceAssignment() {
            @Override
            public SliceIdentifier getIdentifierOf(JavaClass javaClass) {
                String name = javaClass.getPackageName();
                if (!name.startsWith("com.meteor.") || javaClass.getPackageName().startsWith("com.meteor.shared")
                        || javaClass.getPackageName().startsWith("com.meteor.support")) {
                    return SliceIdentifier.ignore();
                }
                String context = name.substring("com.meteor.".length()).split("\\.")[0];
                return SliceIdentifier.of(context);
            }

            @Override
            public String getDescription() {
                return "contexts (com.meteor.<context>)";
            }
        };
    }

    /** 도메인 서비스: 규칙을 담되 애그리거트가 아닌 domain 클래스. *Policy, *DomainService. */
    private static DescribedPredicate<JavaClass> domainServices() {
        return resideInAPackage(DOMAIN).and(simpleNameEndingWith("Policy").or(simpleNameEndingWith("DomainService")));
    }

    /** 애그리거트: domain 의 클래스 중 enum, record, 도메인 서비스가 아닌 것. */
    private static boolean isAggregate(JavaClass javaClass) {
        return javaClass.getPackageName().contains(".domain") && !javaClass.isEnum() && !javaClass.isRecord()
                && !domainServices().test(javaClass);
    }

    /** api 가 애그리거트에서 불러도 되는 메서드: 읽기 전용 접근자. */
    private static boolean isAccessor(String name) {
        return name.startsWith("get") || name.startsWith("is") || name.startsWith("has") || name.equals("toString")
                || name.equals("equals") || name.equals("hashCode") || name.endsWith("Amount");
    }

    private static ArchCondition<JavaMethod> returnOnlyDecisionTypes() {
        return new ArchCondition<>("return void, a primitive, a java.lang type or a shared value") {
            @Override
            public void check(JavaMethod method, ConditionEvents events) {
                JavaClass type = method.getRawReturnType();
                boolean allowed = type.isPrimitive() || type.getPackageName().equals("java.lang")
                        || type.getPackageName().startsWith("com.meteor.shared");
                if (!allowed) {
                    events.add(SimpleConditionEvent.violated(method,
                            method.getFullName() + " returns " + type.getName() + " (a model, not a decision)"));
                }
            }
        };
    }

    private static ArchCondition<JavaClass> notDependOnOtherClassesNamed(String suffix) {
        return new ArchCondition<>("not depend on other classes ending with " + suffix) {
            @Override
            public void check(JavaClass origin, ConditionEvents events) {
                origin.getDirectDependenciesFromSelf()
                    .stream()
                    .filter(dependency -> dependency.getTargetClass().getSimpleName().endsWith(suffix))
                    .filter(dependency -> !dependency.getTargetClass().equals(origin))
                    .forEach(dependency -> events
                        .add(SimpleConditionEvent.violated(dependency, dependency.getDescription())));
            }
        };
    }

}
