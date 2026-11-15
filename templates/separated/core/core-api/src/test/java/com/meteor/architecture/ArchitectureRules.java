package com.meteor.architecture;

import java.util.List;
import java.util.Optional;

import com.tngtech.archunit.base.DescribedPredicate;
import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaField;
import com.tngtech.archunit.core.domain.JavaMethod;
import com.tngtech.archunit.core.domain.JavaMethodCall;
import com.tngtech.archunit.core.domain.JavaModifier;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchCondition;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.lang.ConditionEvents;
import com.tngtech.archunit.lang.SimpleConditionEvent;
import jakarta.persistence.Entity;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;

import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionManager;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionOperations;

import static com.tngtech.archunit.core.domain.JavaClass.Predicates.assignableTo;
import static com.tngtech.archunit.core.domain.JavaClass.Predicates.resideInAPackage;
import static com.tngtech.archunit.core.domain.JavaClass.Predicates.resideInAnyPackage;
import static com.tngtech.archunit.core.domain.JavaClass.Predicates.simpleNameEndingWith;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.methods;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noConstructors;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noFields;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noMethods;

/**
 * 구조 규칙(R-xx). 규칙 ID 와 필드 이름의 접두어(R01_, R02_ ...)가 대응한다. 규칙의 이유와 예시는 사람용
 * docs/architecture/rules.md, 에이전트용 harness/rules/R-xx-*.md 에 있고, {@code RuleCatalogTest}
 * 가 세 곳의 ID 가 어긋나지 않는지 확인한다. 새 규칙이 실제로 위반을 잡는지는 {@code ArchitectureRulesSelfTest} 가
 * fixture 로 확인한다.
 *
 * <p>
 * 패키지: {@code com.meteor.{domain|usecase|api}} (core-api), {@code com.meteor.enums}
 * (core-enum), {@code com.meteor.storage} (storage/db-core), {@code com.meteor.client.*}
 * (clients/*), {@code com.meteor.support.*} (공통). 모든 모듈이 테스트 클래스패스에 있으므로 com.meteor 전체를 한
 * 번에 검사한다.
 *
 * <p>
 * domain 의 클래스는 둘로 나뉜다. Spring 빈인 <b>도메인 서비스</b>({@code *Reader}, {@code *Writer})와
 * <b>저장소 어댑터</b>({@code *Repository}), 그리고 순수한 <b>도메인 모델</b>(애그리거트, 값 객체 record,
 * {@code *Policy}).
 */
@AnalyzeClasses(packages = "com.meteor", importOptions = ImportOption.DoNotIncludeTests.class)
class ArchitectureRules {

    private static final String DOMAIN = "com.meteor.domain..";

    private static final String USECASE = "com.meteor.usecase..";

    private static final String API = "com.meteor.api..";

    private static final String API_REQUEST = "com.meteor.api..request..";

    private static final String API_RESPONSE = "com.meteor.api..response..";

    private static final String STORAGE = "com.meteor.storage..";

    private static final String CLIENT = "com.meteor.client..";

    private static final String ENUMS = "com.meteor.enums..";

    private static final String SUPPORT = "com.meteor.support..";

    private static final String SUPPORT_STORAGE = "com.meteor.support.storage..";

    private static final String ERROR = "com.meteor.support.error..";

    // ----- R-01 도메인 모델은 순수하다 -----

    @ArchTest
    static final ArchRule R01_domain_model_is_pure = noClasses().that(domainModel())
        .should()
        .dependOnClassesThat(
                resideInAnyPackage("org.springframework..", "jakarta.persistence..", STORAGE, CLIENT, USECASE, API)
                    .or(domainBeans()))
        .as("R-01 도메인 모델(애그리거트, 값 객체, *Policy)은 Spring, JPA, storage, client, usecase, api, *Repository, *Reader, *Writer 를 모른다");

    @ArchTest
    static final ArchRule R01_domain_knows_no_outer_layers = noClasses().that()
        .resideInAPackage(DOMAIN)
        .should()
        .dependOnClassesThat()
        .resideInAnyPackage(USECASE, API, CLIENT)
        .as("R-01 domain 은 usecase, api, client 를 모른다. 외부 연동과 여러 도메인을 엮는 흐름은 usecase 가 한다");

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
        .as("R-01 에러 어휘(support.error)는 도메인 모델이 던지므로 프레임워크와 다른 패키지를 모른다");

    // ----- R-02 계층 의존 방향 -----

    @ArchTest
    static final ArchRule R02_usecase_does_not_know_api = noClasses().that()
        .resideInAPackage(USECASE)
        .should()
        .dependOnClassesThat()
        .resideInAPackage(API)
        .as("R-02 usecase 는 api 를 모른다");

    @ArchTest
    static final ArchRule R02_adapters_do_not_know_core = noClasses().that()
        .resideInAnyPackage(STORAGE, CLIENT)
        .should()
        .dependOnClassesThat()
        .resideInAnyPackage(DOMAIN, USECASE, API)
        .as("R-02 storage 와 client 는 domain, usecase, api 를 모른다");

    // ----- R-03 이름 -----

    @ArchTest
    static final ArchRule R03_no_service_suffix = noClasses().that()
        .resideInAPackage("com.meteor..")
        .should()
        .haveSimpleNameEndingWith("Service")
        .as("R-03 *Service 접미어는 쓰지 않는다. 도메인 서비스는 *Reader / *Writer, 흐름은 *UseCase");

    @ArchTest
    static final ArchRule R03_domain_beans_are_readers_writers_or_repositories = classes().that()
        .resideInAPackage(DOMAIN)
        .and()
        .areMetaAnnotatedWith(Component.class)
        .should()
        .haveSimpleNameEndingWith("Reader")
        .orShould()
        .haveSimpleNameEndingWith("Writer")
        .orShould()
        .haveSimpleNameEndingWith("Repository")
        .as("R-03 domain 의 Spring 빈은 *Reader, *Writer, *Repository 뿐이다");

    @ArchTest
    static final ArchRule R03_usecase_classes_are_use_cases_commands_or_results = classes().that()
        .resideInAPackage(USECASE)
        .and()
        .areTopLevelClasses()
        .should()
        .haveSimpleNameEndingWith("UseCase")
        .orShould()
        .haveSimpleNameEndingWith("Command")
        .orShould()
        .haveSimpleNameEndingWith("Result")
        .as("R-03 usecase 의 클래스는 *UseCase, *Command, *Result 중 하나다");

    @ArchTest
    static final ArchRule R03_api_classes_are_controllers = classes().that()
        .resideInAPackage(API)
        .and()
        .resideOutsideOfPackages(API_REQUEST, API_RESPONSE)
        .and()
        .areTopLevelClasses()
        .should()
        .haveSimpleNameEndingWith("Controller")
        .as("R-03 api 의 클래스는 *Controller 다. 요청·응답 객체는 api.request / api.response 에 둔다");

    @ArchTest
    static final ArchRule R03_api_requests_are_named_request = classes().that()
        .resideInAPackage(API_REQUEST)
        .and()
        .areTopLevelClasses()
        .should()
        .haveSimpleNameEndingWith("Request")
        .allowEmptyShould(true)
        .as("R-03 api.request 의 클래스는 *Request 다");

    @ArchTest
    static final ArchRule R03_api_responses_are_named_response = classes().that()
        .resideInAPackage(API_RESPONSE)
        .and()
        .areTopLevelClasses()
        .should()
        .haveSimpleNameEndingWith("Response")
        .allowEmptyShould(true)
        .as("R-03 api.response 의 클래스는 *Response 다");

    // ----- R-04 도메인 서비스의 범위 -----

    @ArchTest
    static final ArchRule R04_domain_services_touch_only_their_own_repository = classes().that(domainServices())
        .should(dependOnlyOnOwnRepository())
        .as("R-04 *Reader / *Writer 는 같은 이름의 *Repository 만 쓰고, 다른 *Repository, *Reader, *Writer 를 부르지 않는다");

    @ArchTest
    static final ArchRule R04_domain_services_do_not_touch_storage = noClasses().that(domainServices())
        .should()
        .dependOnClassesThat()
        .resideInAPackage(STORAGE)
        .as("R-04 *Reader / *Writer 는 storage 를 직접 쓰지 않는다. 저장은 *Repository 어댑터를 거친다");

    // ----- R-05 영속 모델은 storage 와 어댑터 안에 -----

    @ArchTest
    static final ArchRule R05_entities_live_in_storage_only = classes().that()
        .areAnnotatedWith(Entity.class)
        .should()
        .resideInAPackage(STORAGE)
        .as("R-05 JPA 엔티티는 storage 패키지에만 있다");

    @ArchTest
    static final ArchRule R05_only_repository_adapters_touch_storage = noClasses().that()
        .resideOutsideOfPackages(STORAGE, SUPPORT_STORAGE)
        .and(DescribedPredicate.not(repositories()))
        .should()
        .dependOnClassesThat()
        .resideInAPackage(STORAGE)
        .as("R-05 storage(엔티티, Spring Data)는 domain 의 *Repository 어댑터만 참조한다");

    @ArchTest
    static final ArchRule R05_repository_adapters_expose_only_aggregates = methods().that()
        .areDeclaredInClassesThat(repositories())
        .and()
        .arePublic()
        .should(notExposeStorageTypes())
        .allowEmptyShould(true)
        .as("R-05 *Repository 의 public 메서드는 엔티티·Spring Data 타입을 주고받지 않는다. 변환은 어댑터 안에서 끝난다");

    // ----- R-06 트랜잭션을 선언할 수 있는 곳 -----

    @ArchTest
    static final ArchRule R06_transactional_classes_only_in_usecase_or_domain_services = classes().that()
        .areAnnotatedWith(Transactional.class)
        .should(beTransactionBoundaryOwners())
        .allowEmptyShould(true)
        .as("R-06 @Transactional 클래스는 usecase 와 domain 의 *Reader / *Writer 에만 있다");

    @ArchTest
    static final ArchRule R06_transactional_methods_only_in_usecase_or_domain_services = methods().that()
        .areAnnotatedWith(Transactional.class)
        .should()
        .beDeclaredInClassesThat(resideInAPackage(USECASE).or(domainServices()))
        .allowEmptyShould(true)
        .as("R-06 @Transactional 메서드는 usecase 와 domain 의 *Reader / *Writer 에만 있다");

    @ArchTest
    static final ArchRule R06_programmatic_transactions_only_in_usecase = noClasses().that()
        .resideOutsideOfPackage(USECASE)
        .should()
        .dependOnClassesThat(transactionApi())
        .as("R-06 TransactionTemplate 같은 프로그래밍 방식 트랜잭션은 usecase 에만 있다");

    // ----- R-07 도메인 서비스는 기본 전파만 쓴다 -----

    @ArchTest
    static final ArchRule R07_readers_are_read_only = classes().that(readers())
        .should(beAnnotatedWithTransactional(true))
        .as("R-07 *Reader 는 클래스에 @Transactional(readOnly = true) 를 붙인다");

    @ArchTest
    static final ArchRule R07_writers_are_read_write = classes().that(writers())
        .should(beAnnotatedWithTransactional(false))
        .as("R-07 *Writer 는 클래스에 @Transactional(readOnly = false) 를 붙인다");

    @ArchTest
    static final ArchRule R07_domain_services_use_default_propagation = methods().that()
        .areDeclaredInClassesThat(domainServices())
        .and()
        .areAnnotatedWith(Transactional.class)
        .should(useDefaultPropagation())
        .allowEmptyShould(true)
        .as("R-07 도메인 서비스는 기본 전파(REQUIRED)만 쓴다. REQUIRES_NEW 같은 경계 조절은 usecase 에서 한다");

    // ----- R-08 트랜잭션 가드 -----

    @ArchTest
    static final ArchRule R08_use_case_combining_writes_opens_a_transaction = methods().that()
        .areDeclaredInClassesThat(useCases())
        .should(openATransactionWhenCombiningWrites())
        .allowEmptyShould(true)
        .as("R-08 도메인 서비스를 둘 이상 부르고 그중 *Writer 가 있는 usecase 메서드는 @Transactional 이거나 TransactionTemplate 으로 감싼다");

    @ArchTest
    static final ArchRule R08_read_only_use_cases_do_not_write = methods().that()
        .areDeclaredInClassesThat(useCases())
        .should(notCallWritersWhenReadOnly())
        .allowEmptyShould(true)
        .as("R-08 readOnly 트랜잭션의 usecase 메서드는 *Writer 를 부르지 않는다");

    // ----- R-09 usecase 는 도메인 서비스를 거친다 -----

    @ArchTest
    static final ArchRule R09_use_cases_go_through_domain_services = noClasses().that()
        .resideInAPackage(USECASE)
        .should()
        .dependOnClassesThat(repositories().or(resideInAPackage(STORAGE)))
        .as("R-09 usecase 는 *Repository 와 storage 를 쓰지 않는다. 저장소는 *Reader / *Writer 를 거친다");

    @ArchTest
    static final ArchRule R09_use_cases_do_not_call_each_other = classes().that(useCases())
        .should(notDependOnOtherClassesNamed("UseCase"))
        .as("R-09 UseCase 는 다른 UseCase 를 호출하지 않는다");

    // ----- R-10 api 가 쓸 수 있는 것 -----

    @ArchTest
    static final ArchRule R10_api_depends_on_use_cases_and_domain_services_only = noClasses().that()
        .resideInAPackage(API)
        .should()
        .dependOnClassesThat(resideInAnyPackage(STORAGE, CLIENT).or(repositories())
            .or(resideInAPackage(DOMAIN).and(simpleNameEndingWith("Policy"))))
        .as("R-10 api 는 *Repository, *Policy, storage, client 를 쓰지 않는다. *UseCase 나 *Reader / *Writer 를 부른다");

    @ArchTest
    static final ArchRule R10_api_only_reads_aggregates = noClasses().that()
        .resideInAPackage(API)
        .should()
        .callMethodWhere(DescribedPredicate.describe("a state-changing method of a domain aggregate",
                (JavaMethodCall call) -> isAggregate(call.getTargetOwner()) && !isAccessor(call.getTarget().getName())))
        .as("R-10 api 는 돌려받은 애그리거트에서 getter 만 읽는다. 상태를 바꾸는 메서드는 부르지 않는다");

    // ----- R-11 엔드포인트 하나에 호출 하나 -----

    @ArchTest
    static final ArchRule R11_endpoint_calls_one_use_case_or_domain_service = methods().that()
        .areDeclaredInClassesThat()
        .resideInAPackage(API)
        .should(callAtMostOneEntryPoint())
        .allowEmptyShould(true)
        .as("R-11 엔드포인트 하나는 *UseCase 나 *Reader / *Writer 의 메서드를 하나만 부른다. 여러 개를 묶어야 하면 usecase 를 만든다");

    // ----- R-12 API 계약은 도메인과 분리 -----

    @ArchTest
    static final ArchRule R12_api_dtos_carry_no_domain_types = noFields().that()
        .areDeclaredInClassesThat()
        .resideInAnyPackage(API_REQUEST, API_RESPONSE)
        .should()
        .haveRawType(resideInAPackage(DOMAIN))
        .as("R-12 api 의 요청·응답 객체는 domain 타입을 필드로 갖지 않는다. 값 객체는 원시 타입이나 api 전용 record 로 푼다. enum 은 core-enum 이라 허용");

    // ----- R-13 enum 은 의존이 없다 -----

    @ArchTest
    static final ArchRule R13_enums_have_no_dependencies = noClasses().that()
        .resideInAPackage(ENUMS)
        .should()
        .dependOnClassesThat(resideInAPackage("com.meteor..").and(DescribedPredicate.not(resideInAPackage(ENUMS)))
            .or(resideInAPackage("org.springframework.."))
            .or(resideInAPackage("jakarta..")))
        .as("R-13 core-enum 의 enum 은 다른 패키지와 프레임워크에 의존하지 않는다");

    // ----- R-14 애그리거트끼리는 ID 로만 참조한다 -----

    @ArchTest
    static final ArchRule R14_aggregates_reference_others_by_id = classes().that(aggregates())
        .should(notHoldOtherAggregates())
        .as("R-14 애그리거트는 다른 애그리거트를 필드로 갖지 않는다. memberId 처럼 ID 로만 참조한다");

    @ArchTest
    static final ArchRule R14_no_jpa_associations = noFields().that()
        .areDeclaredInClassesThat()
        .resideInAPackage("com.meteor..")
        .should()
        .beAnnotatedWith(OneToMany.class)
        .orShould()
        .beAnnotatedWith(ManyToOne.class)
        .orShould()
        .beAnnotatedWith(OneToOne.class)
        .orShould()
        .beAnnotatedWith(ManyToMany.class)
        .as("R-14 엔티티 사이에 JPA 연관관계(@OneToMany, @ManyToOne, @OneToOne, @ManyToMany)를 걸지 않는다");

    // ----- R-15 도메인 모델에는 setter 가 없다 -----

    @ArchTest
    static final ArchRule R15_domain_model_has_no_setters = noMethods().that()
        .areDeclaredInClassesThat(domainModel())
        .and()
        .arePublic()
        .should()
        .haveNameMatching("set[A-Z].*")
        .as("R-15 도메인 모델은 public setter 를 갖지 않는다. 상태는 규칙을 가진 메서드(cancel, markPaid)로만 바뀐다");

    // ----- R-16 저장은 명시적이다 -----

    @ArchTest
    static final ArchRule R16_writers_save_what_they_change = methods().that()
        .areDeclaredInClassesThat(writers())
        .should(saveWhenChangingAggregateState())
        .allowEmptyShould(true)
        .as("R-16 애그리거트의 상태를 바꾸는 *Writer 메서드는 같은 메서드에서 *Repository.save 를 부른다");

    // ----- R-17 *Reader 는 쓰지 않는다 -----

    @ArchTest
    static final ArchRule R17_readers_do_not_write = noClasses().that(readers())
        .should()
        .callMethodWhere(DescribedPredicate.describe("a save or delete of a repository",
                (JavaMethodCall call) -> repositories().test(call.getTargetOwner())
                        && isWriteMethod(call.getTarget().getName())))
        .as("R-17 *Reader 는 *Repository 의 save / delete 를 부르지 않는다. 쓰기는 *Writer 가 한다");

    // ----- R-18 트랜잭션 안에서 외부 시스템을 부르지 않는다 -----

    @ArchTest
    static final ArchRule R18_transactional_use_cases_do_not_call_clients = methods().that()
        .areDeclaredInClassesThat(useCases())
        .should(notCallClientsInsideDeclaredTransaction())
        .allowEmptyShould(true)
        .as("R-18 @Transactional 이 적용된 usecase 메서드는 client 를 부르지 않는다. 외부 호출은 TransactionTemplate 구간 밖에서 한다");

    // ----- R-19 api 는 값 객체를 만들고 읽기만 한다 -----

    @ArchTest
    static final ArchRule R19_api_does_not_compute_with_value_objects = noClasses().that()
        .resideInAPackage(API)
        .should()
        .callMethodWhere(DescribedPredicate.describe("an operation on a domain value object",
                (JavaMethodCall call) -> isValueObject(call.getTargetOwner()) && !isValueObjectReadOrFactory(call)))
        .as("R-19 api 는 값 객체를 of / from / 생성자로 만들고 구성 요소만 읽는다. 연산(plus, times 등)은 도메인 모델이 한다");

    // ----- R-20 애그리거트는 자기 정적 팩토리로만 만든다 -----

    @ArchTest
    static final ArchRule R20_aggregates_have_no_public_constructors = noConstructors().that()
        .areDeclaredInClassesThat(aggregates())
        .should()
        .bePublic()
        .as("R-20 애그리거트는 public 생성자를 갖지 않는다. 생성은 규칙을 가진 정적 팩토리(place, register, request)로 한다");

    @ArchTest
    static final ArchRule R20_aggregates_are_constructed_only_by_themselves = classes().that()
        .resideInAPackage("com.meteor..")
        .should(notConstructOtherAggregates())
        .as("R-20 애그리거트의 생성자는 그 애그리거트 안에서만 부른다");

    // ----- R-21 복원은 저장소 어댑터만 한다 -----

    @ArchTest
    static final ArchRule R21_only_own_repository_restores_aggregates = classes().that()
        .resideInAPackage("com.meteor..")
        .should(notRestoreAggregatesOfOthers())
        .as("R-21 애그리거트의 restore(...) 는 같은 이름의 *Repository 만 부른다. 상태 변경은 restore 로 조립하지 않고 애그리거트 메서드로 한다");

    // ----- 분류 -----

    private static DescribedPredicate<JavaClass> readers() {
        return resideInAPackage(DOMAIN).and(simpleNameEndingWith("Reader")).as("*Reader");
    }

    private static DescribedPredicate<JavaClass> writers() {
        return resideInAPackage(DOMAIN).and(simpleNameEndingWith("Writer")).as("*Writer");
    }

    private static DescribedPredicate<JavaClass> repositories() {
        return resideInAPackage(DOMAIN).and(simpleNameEndingWith("Repository")).as("*Repository");
    }

    private static DescribedPredicate<JavaClass> domainServices() {
        return readers().or(writers()).as("domain services (*Reader, *Writer)");
    }

    private static DescribedPredicate<JavaClass> domainBeans() {
        return domainServices().or(repositories()).as("domain beans (*Reader, *Writer, *Repository)");
    }

    private static DescribedPredicate<JavaClass> domainModel() {
        return resideInAPackage(DOMAIN).and(DescribedPredicate.not(domainBeans()))
            .as("domain model (aggregates, value objects, *Policy)");
    }

    private static DescribedPredicate<JavaClass> useCases() {
        return resideInAPackage(USECASE).and(simpleNameEndingWith("UseCase")).as("*UseCase");
    }

    private static DescribedPredicate<JavaClass> transactionApi() {
        return assignableTo(TransactionOperations.class).or(assignableTo(TransactionManager.class))
            .or(assignableTo(PlatformTransactionManager.class))
            .as("programmatic transaction API");
    }

    private static DescribedPredicate<JavaClass> aggregates() {
        return DescribedPredicate.describe("aggregates", ArchitectureRules::isAggregate);
    }

    /** 애그리거트: domain 모델 중 record, enum, interface, *Policy 가 아닌 최상위 클래스. */
    private static boolean isAggregate(JavaClass javaClass) {
        return domainModel().test(javaClass) && javaClass.isTopLevelClass() && !javaClass.isEnum()
                && !javaClass.isRecord() && !javaClass.isInterface() && !javaClass.getSimpleName().endsWith("Policy");
    }

    /** 값 객체: domain 의 record. */
    private static boolean isValueObject(JavaClass javaClass) {
        return domainModel().test(javaClass) && javaClass.isRecord();
    }

    /** api 가 값 객체에서 불러도 되는 메서드: 정적 팩토리(of, from), 구성 요소 접근자, Object 메서드. */
    private static boolean isValueObjectReadOrFactory(JavaMethodCall call) {
        String name = call.getTarget().getName();
        if (isStatic(call)) {
            return name.equals("of") || name.equals("from");
        }
        boolean componentAccessor = call.getTarget().getRawParameterTypes().isEmpty()
                && call.getTargetOwner().tryGetField(name).isPresent();
        return componentAccessor || name.equals("toString") || name.equals("equals") || name.equals("hashCode");
    }

    private static boolean isStatic(JavaMethodCall call) {
        return call.getTarget()
            .resolveMember()
            .map(method -> method.getModifiers().contains(JavaModifier.STATIC))
            .orElse(false);
    }

    /** 애그리거트의 상태를 바꿀 수 있는 호출: 정적 메서드와 읽기 전용 접근자가 아닌 인스턴스 메서드. */
    private static boolean changesAggregateState(JavaMethodCall call) {
        return isAggregate(call.getTargetOwner()) && !isStatic(call) && !isAccessor(call.getTarget().getName());
    }

    private static boolean isWriteMethod(String name) {
        return name.startsWith("save") || name.startsWith("delete") || name.startsWith("flush");
    }

    /** api 가 애그리거트에서 불러도 되는 메서드: 읽기 전용 접근자. */
    private static boolean isAccessor(String name) {
        return name.startsWith("get") || name.startsWith("is") || name.startsWith("has") || name.equals("toString")
                || name.equals("equals") || name.equals("hashCode") || name.endsWith("Amount");
    }

    /** 메서드의 호출. ArchUnit 은 람다 본문의 호출도 감싸는 메서드의 것으로 센다. */
    private static List<JavaMethodCall> callsOf(JavaMethod method) {
        return List.copyOf(method.getMethodCallsFromSelf());
    }

    private static Optional<Transactional> transactionalOf(JavaMethod method) {
        Optional<Transactional> onMethod = method.tryGetAnnotationOfType(Transactional.class);
        return onMethod.isPresent() ? onMethod : method.getOwner().tryGetAnnotationOfType(Transactional.class);
    }

    // ----- 조건 -----

    private static ArchCondition<JavaClass> beTransactionBoundaryOwners() {
        return new ArchCondition<>("reside in usecase or be a *Reader / *Writer") {
            @Override
            public void check(JavaClass javaClass, ConditionEvents events) {
                if (!resideInAPackage(USECASE).test(javaClass) && !domainServices().test(javaClass)) {
                    events.add(SimpleConditionEvent.violated(javaClass,
                            javaClass.getName() + " declares @Transactional outside usecase and domain services"));
                }
            }
        };
    }

    private static ArchCondition<JavaClass> dependOnlyOnOwnRepository() {
        return new ArchCondition<>("depend only on the repository of the same aggregate") {
            @Override
            public void check(JavaClass service, ConditionEvents events) {
                String aggregate = service.getSimpleName().replaceAll("(Reader|Writer)$", "");
                service.getDirectDependenciesFromSelf()
                    .stream()
                    .filter(dependency -> domainBeans().test(dependency.getTargetClass()))
                    .filter(dependency -> !dependency.getTargetClass().equals(service))
                    .filter(dependency -> !dependency.getTargetClass().getSimpleName().equals(aggregate + "Repository"))
                    .forEach(dependency -> events
                        .add(SimpleConditionEvent.violated(dependency, dependency.getDescription())));
            }
        };
    }

    private static ArchCondition<JavaClass> beAnnotatedWithTransactional(boolean readOnly) {
        return new ArchCondition<>("be annotated with @Transactional(readOnly = " + readOnly + ")") {
            @Override
            public void check(JavaClass javaClass, ConditionEvents events) {
                Optional<Transactional> transactional = javaClass.tryGetAnnotationOfType(Transactional.class);
                if (transactional.isEmpty() || transactional.get().readOnly() != readOnly
                        || transactional.get().propagation() != Propagation.REQUIRED) {
                    events.add(SimpleConditionEvent.violated(javaClass, javaClass.getName()
                            + " must be annotated with @Transactional(readOnly = " + readOnly + ") on the class"));
                }
            }
        };
    }

    private static ArchCondition<JavaMethod> useDefaultPropagation() {
        return new ArchCondition<>("use the default propagation") {
            @Override
            public void check(JavaMethod method, ConditionEvents events) {
                Propagation propagation = method.getAnnotationOfType(Transactional.class).propagation();
                if (propagation != Propagation.REQUIRED) {
                    events.add(SimpleConditionEvent.violated(method,
                            method.getFullName() + " uses propagation " + propagation));
                }
            }
        };
    }

    /**
     * 람다 본문의 호출도 감싸는 메서드의 것으로 세므로, TransactionTemplate 을 쓰는 메서드에서 쓰기가 모두 템플릿 안에 있는지는 구조로
     * 판별할 수 없다. 그 부분은 리뷰(S-06)가 본다.
     */
    private static ArchCondition<JavaMethod> openATransactionWhenCombiningWrites() {
        return new ArchCondition<>("open a transaction when it combines domain service calls with a write") {
            @Override
            public void check(JavaMethod method, ConditionEvents events) {
                List<JavaMethodCall> serviceCalls = callsOf(method).stream()
                    .filter(call -> domainServices().test(call.getTargetOwner()))
                    .toList();
                boolean writes = serviceCalls.stream().anyMatch(call -> writers().test(call.getTargetOwner()));
                if (serviceCalls.size() < 2 || !writes) {
                    return;
                }
                boolean declared = transactionalOf(method).filter(transactional -> !transactional.readOnly())
                    .isPresent();
                boolean programmatic = callsOf(method).stream()
                    .anyMatch(call -> transactionApi().test(call.getTargetOwner()));
                if (!declared && !programmatic) {
                    events.add(SimpleConditionEvent.violated(method, method.getFullName()
                            + " calls several domain services including a writer without a transaction"));
                }
            }
        };
    }

    private static ArchCondition<JavaMethod> notCallWritersWhenReadOnly() {
        return new ArchCondition<>("not call writers in a read-only transaction") {
            @Override
            public void check(JavaMethod method, ConditionEvents events) {
                boolean readOnly = transactionalOf(method).map(Transactional::readOnly).orElse(false);
                if (!readOnly) {
                    return;
                }
                callsOf(method).stream()
                    .filter(call -> writers().test(call.getTargetOwner()))
                    .forEach(call -> events.add(SimpleConditionEvent.violated(call, call.getDescription())));
            }
        };
    }

    private static ArchCondition<JavaMethod> callAtMostOneEntryPoint() {
        return new ArchCondition<>("call at most one use case or domain service method") {
            @Override
            public void check(JavaMethod method, ConditionEvents events) {
                List<JavaMethodCall> calls = callsOf(method).stream()
                    .filter(call -> useCases().or(domainServices()).test(call.getTargetOwner()))
                    .toList();
                if (calls.size() > 1) {
                    events.add(SimpleConditionEvent.violated(method,
                            method.getFullName() + " calls " + calls.size() + " use case / domain service methods"));
                }
            }
        };
    }

    private static ArchCondition<JavaMethod> notExposeStorageTypes() {
        return new ArchCondition<>("not expose storage types") {
            @Override
            public void check(JavaMethod method, ConditionEvents events) {
                boolean returnsStorage = method.getReturnType()
                    .getAllInvolvedRawTypes()
                    .stream()
                    .anyMatch(type -> resideInAPackage(STORAGE).test(type));
                boolean acceptsStorage = method.getParameterTypes()
                    .stream()
                    .flatMap(type -> type.getAllInvolvedRawTypes().stream())
                    .anyMatch(type -> resideInAPackage(STORAGE).test(type));
                if (returnsStorage || acceptsStorage) {
                    events.add(SimpleConditionEvent.violated(method,
                            method.getFullName() + " exposes a storage type in its signature"));
                }
            }
        };
    }

    private static ArchCondition<JavaClass> notHoldOtherAggregates() {
        return new ArchCondition<>("not hold other aggregates in fields") {
            @Override
            public void check(JavaClass aggregate, ConditionEvents events) {
                for (JavaField field : aggregate.getFields()) {
                    field.getType()
                        .getAllInvolvedRawTypes()
                        .stream()
                        .filter(ArchitectureRules::isAggregate)
                        .filter(type -> !type.equals(aggregate))
                        .forEach(type -> events.add(SimpleConditionEvent.violated(field, field.getFullName()
                                + " holds aggregate " + type.getSimpleName() + ". Reference it by id")));
                }
            }
        };
    }

    private static ArchCondition<JavaMethod> saveWhenChangingAggregateState() {
        return new ArchCondition<>("save the aggregate when changing its state") {
            @Override
            public void check(JavaMethod method, ConditionEvents events) {
                List<JavaMethodCall> calls = callsOf(method);
                boolean changes = calls.stream().anyMatch(ArchitectureRules::changesAggregateState);
                boolean saves = calls.stream()
                    .anyMatch(call -> repositories().test(call.getTargetOwner())
                            && call.getTarget().getName().startsWith("save"));
                if (changes && !saves) {
                    events.add(SimpleConditionEvent.violated(method,
                            method.getFullName() + " changes an aggregate without calling *Repository.save"));
                }
            }
        };
    }

    private static ArchCondition<JavaMethod> notCallClientsInsideDeclaredTransaction() {
        return new ArchCondition<>("not call clients inside a declared transaction") {
            @Override
            public void check(JavaMethod method, ConditionEvents events) {
                if (transactionalOf(method).isEmpty()) {
                    return;
                }
                callsOf(method).stream()
                    .filter(call -> resideInAPackage(CLIENT).test(call.getTargetOwner()))
                    .forEach(call -> events.add(SimpleConditionEvent.violated(call,
                            method.getFullName() + " calls a client inside @Transactional: " + call.getDescription())));
            }
        };
    }

    private static ArchCondition<JavaClass> notConstructOtherAggregates() {
        return new ArchCondition<>("not construct aggregates other than itself") {
            @Override
            public void check(JavaClass origin, ConditionEvents events) {
                origin.getConstructorCallsFromSelf()
                    .stream()
                    .filter(call -> isAggregate(call.getTargetOwner()))
                    .filter(call -> !call.getTargetOwner().equals(origin)
                            && !call.getTargetOwner().equals(origin.getEnclosingClass().orElse(null)))
                    .forEach(call -> events.add(SimpleConditionEvent.violated(call, call.getDescription())));
            }
        };
    }

    private static ArchCondition<JavaClass> notRestoreAggregatesOfOthers() {
        return new ArchCondition<>("restore an aggregate only from its own repository") {
            @Override
            public void check(JavaClass origin, ConditionEvents events) {
                origin.getMethodCallsFromSelf()
                    .stream()
                    .filter(call -> isAggregate(call.getTargetOwner()) && call.getTarget().getName().equals("restore"))
                    .filter(call -> !origin.getSimpleName()
                        .equals(call.getTargetOwner().getSimpleName() + "Repository"))
                    .forEach(call -> events.add(SimpleConditionEvent.violated(call, call.getDescription())));
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
