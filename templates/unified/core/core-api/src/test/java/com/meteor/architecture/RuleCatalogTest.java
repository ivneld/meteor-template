package com.meteor.architecture;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.tngtech.archunit.junit.ArchTest;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 규칙이 있는 네 곳의 ID 가 어긋나지 않는지 확인한다.
 *
 * <ul>
 * <li>ArchUnit: {@link ArchitectureRules} 의 {@code @ArchTest} 필드. 접두어 {@code Rnn_} 가 규칙
 * ID 다 (구조 규칙만)</li>
 * <li>에이전트용 규칙 파일: {@code harness/rules/<ID>-*.md}. frontmatter 의 {@code archunit_fields}
 * 와 {@code human_doc}</li>
 * <li>에이전트용 인덱스: {@code AGENTS.md} 의 규칙 인덱스 표</li>
 * <li>사람용 인덱스: {@code docs/architecture/rules.md} 의 표와 각 장의 {@code <a id="ID"></a>}
 * 앵커</li>
 * </ul>
 *
 * 규칙을 추가·변경·삭제하면 네 곳을 한 변경에서 함께 고친다.
 */
class RuleCatalogTest {

    private static final Pattern RULE_ID = Pattern.compile("[RS]-\\d\\d");

    private static final Pattern ARCH_FIELD = Pattern.compile("^R(\\d\\d)_.+");

    private static final Pattern INDEX_ROW = Pattern.compile("^\\|\\s*\\[?([RS]-\\d\\d)\\]?(?:\\(([^)]+)\\))?\\s*\\|");

    private static final Path ROOT = projectRoot();

    @Test
    void everyArchUnitRuleHasARuleFileAndEveryStructuralRuleFileHasArchUnitFields() {
        Map<String, RuleFile> files = ruleFiles();
        Set<String> fieldIds = archUnitFields().stream().map(RuleCatalogTest::idOfField).collect(toSortedSet());
        Set<String> structuralFileIds = files.values()
            .stream()
            .filter(file -> file.id().startsWith("R-"))
            .map(RuleFile::id)
            .collect(toSortedSet());

        assertThat(structuralFileIds).as("harness/rules 의 R 규칙 파일 = ArchitectureRules 의 Rnn_ 필드").isEqualTo(fieldIds);
    }

    @Test
    void ruleFilesListExactlyTheArchUnitFieldsOfTheirRule() {
        List<String> fields = archUnitFields();
        Map<String, RuleFile> files = ruleFiles();
        List<String> listed = new ArrayList<>();

        for (RuleFile file : files.values()) {
            if (file.id().startsWith("S-")) {
                assertThat(file.archUnitFields()).as(file.path() + " 의미 규칙은 archunit_fields 가 없다").isEmpty();
                assertThat(file.frontmatter()).as(file.path() + " 의미 규칙은 review_verdict 가 있다")
                    .containsKey("review_verdict");
                continue;
            }
            for (String field : file.archUnitFields()) {
                assertThat(fields).as(file.path() + " 의 archunit_fields 는 ArchitectureRules 에 있다").contains(field);
                assertThat(idOfField(field)).as(file.path() + " 의 archunit_fields 는 같은 규칙 ID 의 필드다")
                    .isEqualTo(file.id());
                listed.add(field);
            }
        }

        assertThat(listed).as("모든 @ArchTest 필드는 정확히 한 규칙 파일에 적혀 있다").containsExactlyInAnyOrderElementsOf(fields);
    }

    @Test
    void agentIndexListsEveryRuleFile() {
        Map<String, String> index = indexRows(ROOT.resolve("AGENTS.md"), "## 규칙 인덱스");
        Map<String, RuleFile> files = ruleFiles();

        assertThat(new TreeSet<>(index.keySet())).as("AGENTS.md 규칙 인덱스 = harness/rules").isEqualTo(files.keySet());
        files.values()
            .forEach(file -> assertThat(read(ROOT.resolve("AGENTS.md"))).as("AGENTS.md 가 " + file.path() + " 를 가리킨다")
                .contains("(" + file.path() + ")"));
    }

    @Test
    void humanIndexListsEveryRuleAndPointsToItsAnchor() {
        Path rulesDoc = ROOT.resolve("docs/architecture/rules.md");
        Map<String, String> index = indexRows(rulesDoc, null);
        Map<String, RuleFile> files = ruleFiles();

        assertThat(new TreeSet<>(index.keySet())).as("docs/architecture/rules.md 표 = harness/rules")
            .isEqualTo(files.keySet());
        for (RuleFile file : files.values()) {
            String humanDoc = file.frontmatter().get("human_doc");
            assertThat(humanDoc).as(file.path() + " 의 human_doc")
                .isEqualTo("docs/architecture/" + index.get(file.id()));
            String[] target = humanDoc.split("#", 2);
            Path chapter = ROOT.resolve(target[0]);
            assertThat(chapter).as(humanDoc).exists();
            assertThat(read(chapter)).as(humanDoc + " 앵커").contains("<a id=\"" + target[1] + "\"></a>");
        }
    }

    // ----- 읽기 -----

    private static String idOfField(String field) {
        Matcher matcher = ARCH_FIELD.matcher(field);
        assertThat(matcher.matches()).as("@ArchTest 필드 이름은 Rnn_ 로 시작한다: " + field).isTrue();
        return "R-" + matcher.group(1);
    }

    private static List<String> archUnitFields() {
        return Arrays.stream(ArchitectureRules.class.getDeclaredFields())
            .filter(field -> field.isAnnotationPresent(ArchTest.class) && Modifier.isStatic(field.getModifiers()))
            .map(Field::getName)
            .sorted()
            .toList();
    }

    private static Map<String, RuleFile> ruleFiles() {
        Map<String, RuleFile> files = new LinkedHashMap<>();
        try (Stream<Path> paths = Files.list(ROOT.resolve("harness/rules"))) {
            paths.filter(path -> path.getFileName().toString().endsWith(".md")).sorted().forEach(path -> {
                RuleFile file = RuleFile.parse(ROOT.relativize(path).toString(), read(path));
                assertThat(files.put(file.id(), file)).as("규칙 ID 중복: " + file.id()).isNull();
            });
        }
        catch (IOException ex) {
            throw new UncheckedIOException(ex);
        }
        return new java.util.TreeMap<>(files);
    }

    /** 표의 첫 열이 규칙 ID 인 행. 값은 링크 대상(없으면 빈 문자열). section 이 있으면 그 제목 아래 첫 표만 읽는다. */
    private static Map<String, String> indexRows(Path path, String section) {
        List<String> lines = read(path).lines().toList();
        int start = section == null ? 0 : lines.indexOf(section);
        assertThat(start).as(path + " 에 " + section + " 절이 있다").isGreaterThanOrEqualTo(0);
        Map<String, String> rows = new LinkedHashMap<>();
        boolean inTable = false;
        for (String line : lines.subList(start, lines.size())) {
            if (line.startsWith("|")) {
                inTable = true;
                Matcher matcher = INDEX_ROW.matcher(line);
                if (matcher.find()) {
                    assertThat(rows.put(matcher.group(1), matcher.group(2) == null ? "" : matcher.group(2)))
                        .as(path + " 에서 규칙 ID 중복: " + matcher.group(1))
                        .isNull();
                }
            }
            else if (inTable) {
                break;
            }
        }
        return rows;
    }

    private static String read(Path path) {
        try {
            return Files.readString(path);
        }
        catch (IOException ex) {
            throw new UncheckedIOException(ex);
        }
    }

    private static Path projectRoot() {
        Path dir = Path.of("").toAbsolutePath();
        while (dir != null && !Files.exists(dir.resolve("AGENTS.md"))) {
            dir = dir.getParent();
        }
        assertThat(dir).as("AGENTS.md 가 있는 프로젝트 루트").isNotNull();
        return dir;
    }

    private static <T extends Comparable<T>> java.util.stream.Collector<T, ?, TreeSet<T>> toSortedSet() {
        return Collectors.toCollection(TreeSet::new);
    }

    record RuleFile(String id, String path, Map<String, String> frontmatter, List<String> archUnitFields) {

        static RuleFile parse(String path, String text) {
            String fileName = Path.of(path).getFileName().toString();
            Matcher idInName = RULE_ID.matcher(fileName);
            assertThat(idInName.lookingAt()).as(path + " 파일 이름은 규칙 ID 로 시작한다").isTrue();
            assertThat(text).as(path + " 는 frontmatter 로 시작한다").startsWith("---\n");
            int end = text.indexOf("\n---\n", 4);
            assertThat(end).as(path + " frontmatter 끝").isPositive();
            Map<String, String> frontmatter = new LinkedHashMap<>();
            for (String line : text.substring(4, end).split("\n")) {
                int colon = line.indexOf(':');
                if (colon > 0) {
                    frontmatter.put(line.substring(0, colon).trim(), line.substring(colon + 1).trim());
                }
            }
            String id = frontmatter.get("id");
            assertThat(id).as(path + " 의 id 는 파일 이름과 같다").isEqualTo(idInName.group());
            String fields = frontmatter.getOrDefault("archunit_fields", "[]").replaceAll("[\\[\\]]", "");
            List<String> archUnitFields = Arrays.stream(fields.split(","))
                .map(String::trim)
                .filter(field -> !field.isEmpty())
                .toList();
            return new RuleFile(id, path, frontmatter, archUnitFields);
        }

    }

}
