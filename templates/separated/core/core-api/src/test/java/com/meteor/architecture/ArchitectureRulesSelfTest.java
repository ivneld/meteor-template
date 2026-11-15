package com.meteor.architecture;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.lang.ArchRule;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.TestFactory;

import java.util.Map;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * 직접 만든 조건(ArchCondition)을 쓰는 규칙이 실제로 위반을 잡는지 확인한다. main 코드가 규칙을 통과한다는 것만으로는 조건이 아무것도
 * 검사하지 않는 경우를 구분할 수 없다. 위반 예시는 테스트 소스의 {@code *.archfixture} 패키지에 있다.
 */
class ArchitectureRulesSelfTest {

    private static final JavaClasses FIXTURES = new ClassFileImporter().importPackages("com.meteor.domain.archfixture",
            "com.meteor.usecase.archfixture", "com.meteor.api.archfixture");

    private static final Map<String, ArchRule> RULES_WITH_FIXTURES = Map.ofEntries(
            Map.entry("R03_api_classes_are_controllers", ArchitectureRules.R03_api_classes_are_controllers),
            Map.entry("R05_repository_adapters_expose_only_aggregates",
                    ArchitectureRules.R05_repository_adapters_expose_only_aggregates),
            Map.entry("R14_aggregates_reference_others_by_id", ArchitectureRules.R14_aggregates_reference_others_by_id),
            Map.entry("R14_no_jpa_associations", ArchitectureRules.R14_no_jpa_associations),
            Map.entry("R15_domain_model_has_no_setters", ArchitectureRules.R15_domain_model_has_no_setters),
            Map.entry("R16_writers_save_what_they_change", ArchitectureRules.R16_writers_save_what_they_change),
            Map.entry("R17_readers_do_not_write", ArchitectureRules.R17_readers_do_not_write),
            Map.entry("R18_transactional_use_cases_do_not_call_clients",
                    ArchitectureRules.R18_transactional_use_cases_do_not_call_clients),
            Map.entry("R19_api_does_not_compute_with_value_objects",
                    ArchitectureRules.R19_api_does_not_compute_with_value_objects),
            Map.entry("R20_aggregates_have_no_public_constructors",
                    ArchitectureRules.R20_aggregates_have_no_public_constructors),
            Map.entry("R20_aggregates_are_constructed_only_by_themselves",
                    ArchitectureRules.R20_aggregates_are_constructed_only_by_themselves),
            Map.entry("R21_only_own_repository_restores_aggregates",
                    ArchitectureRules.R21_only_own_repository_restores_aggregates));

    @TestFactory
    Stream<DynamicTest> rulesDetectTheirFixtureViolations() {
        return RULES_WITH_FIXTURES.entrySet()
            .stream()
            .sorted(Map.Entry.comparingByKey())
            .map(entry -> DynamicTest.dynamicTest(entry.getKey(),
                    () -> assertThatThrownBy(() -> entry.getValue().check(FIXTURES)).isInstanceOf(AssertionError.class)
                        .hasMessageContaining(entry.getKey().substring(0, 3).replace("R", "R-"))));
    }

}
