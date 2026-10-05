package com.meteor.architecture;

import com.tngtech.archunit.base.DescribedPredicate;
import com.tngtech.archunit.core.domain.JavaClass;
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
import static com.tngtech.archunit.core.domain.JavaClass.Predicates.simpleNameEndingWith;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.methods;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

/**
 * docs/ARCHITECTURE.md 의 구조 규칙(R-xx). 규칙 ID 와 필드 이름이 대응한다.
 *
 * <p>
 * 패키지 규약: {@code com.meteor.<context>.{domain|application|api|storage|clients}}.
 * {@code com.meteor.shared} 와 {@code com.meteor.support} 는 컨텍스트가 아닌 공통 영역이다. 에러 어휘
 * ({@code com.meteor.support.error}) 는 core-domain 모듈에 있지만 {@code ..domain..} 패키지가 아니므로
 * api 도 참조할 수 있다.
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

    @ArchTest
    static final ArchRule R01_domain_knows_nothing_about_framework_or_outer_layers = noClasses().that()
        .resideInAPackage(DOMAIN)
        .should()
        .dependOnClassesThat()
        .resideInAnyPackage("org.springframework..", "jakarta.persistence..", APPLICATION, API, STORAGE, CLIENTS)
        .as("R-01 domain 은 Spring, JPA, application, api, storage, clients 를 모른다");

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
    static final ArchRule R03_api_does_not_depend_on_domain_or_storage = noClasses().that()
        .resideInAPackage(API)
        .should()
        .dependOnClassesThat()
        .resideInAnyPackage(DOMAIN, STORAGE, CLIENTS)
        .as("R-03 api 는 domain(애그리거트), storage, clients 에 의존하지 않는다. 값은 core-shared, 에러 어휘는 support.error 로");

    @ArchTest
    static final ArchRule R04_use_cases_do_not_call_each_other = classes().that()
        .haveSimpleNameEndingWith("UseCase")
        .should(notDependOnOtherClassesNamed("UseCase"))
        .as("R-04 UseCase 는 다른 UseCase 를 호출하지 않는다");

    @ArchTest
    static final ArchRule R05_no_service_suffix_in_domain = noClasses().that()
        .resideInAPackage(DOMAIN)
        .should()
        .haveSimpleNameEndingWith("Service")
        .as("R-05 domain 에 *Service 접미어를 두지 않는다. 규칙은 애그리거트로, 남는 것은 *Policy 같은 역할 이름으로");

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
        .as("R-05 application 의 클래스는 *UseCase, *Command, *Result, *Facade, *Event, *Listener 중 하나다");

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
        .as("R-09 core-shared 는 다른 모듈과 프레임워크에 의존하지 않는다");

    @ArchTest
    static final ArchRule R09_shared_contains_only_records_and_enums = classes().that()
        .resideInAPackage(SHARED)
        .and()
        .areTopLevelClasses()
        .should()
        .beRecords()
        .orShould()
        .beEnums()
        .as("R-09 core-shared 에는 record 와 enum 만 둔다. 가변 객체와 예외는 core-domain 으로");

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
