# Implementation Plan: dependency-plugin-modernization

## Overview

design.md의 Testing Strategy와 동일하게 3개 Step으로 진행한다. Step 1은 Component 1~3(Version Catalog 도입, 의존성 갱신, `spring-test` 제거)을, Step 2는 Component 4(Javadoc/소스 jar), Step 3은 Component 5~6(GPG 서명, Central Portal 배포 플러그인)을 구현한다. 이 스펙은 순수 빌드 설정/인프라 작업이라 Red→Green→Refactor 구조를 강제하지 않는다 — Correctness Properties에서도 의미 있는 property가 없다고 결론 냈다.

## Checkbox States

작성 시점의 모든 태스크는 `[ ]`다 (SKILL.md 참고). `[ ]`→작성됨(실행 범위 아님), `[-]`→대기(실행 시작 시 이번 범위 전체를 일괄 마킹), `[~]`→진행 중(시작하는 즉시 개별 갱신), `[x]`→완료(검증까지 마친 뒤 개별 갱신). 전환은 항상 `[ ]`→`[-]`(일괄)→`[~]`→`[x]`(개별) 순서다.

---

## Tasks

- [x] 1. Step 1: Version Catalog 도입 + 의존성 갱신 + `spring-test` 제거

  - [x] 1.1 `gradle/libs.versions.toml` 신규 생성
    - design.md Component 1의 카탈로그 전체를 작성한다: `[versions]`(commons-lang3 3.20.0, junit-bom 5.14.4, mockito 5.22.0, assertj 3.27.7, lombok 1.18.38, autoparams 11.3.2, nmcp 1.4.4), `[libraries]`(commons-lang3, junit-bom, junit-jupiter, junit-platform-launcher, mockito-core, mockito-junit-jupiter, assertj-core, autoparams, autoparams-lombok, lombok), `[plugins]`(nmcp)
    - _Requirements: 1.1, 2.1, 2.2, 2.3, 2.4, 4.1, 4.2_

  - [x] 1.2 `build.gradle.kts` 의존성 블록을 카탈로그 참조로 갱신
    - `testImplementation("org.springframework:spring-test:5.0.0.RELEASE")` 줄 삭제
    - `api("org.apache.commons:commons-lang3:...")`를 `implementation(libs.commons.lang3)`로 교체(스코프를 `api`에서 `implementation`으로 변경)
    - 나머지 테스트 의존성(junit-bom, mockito-core, mockito-junit-jupiter, assertj-core, autoparams, autoparams-lombok, lombok, junit-platform-launcher)을 전부 `libs.*` 참조로 교체
    - _Requirements: 1.1, 2.1, 2.2, 2.3, 2.4, 3.1_

  - [x] 1.3 `ByteToObjectConverterTest.java`: `ReflectionTestUtils` → `FieldUtils` 치환
    - `import org.springframework.test.util.ReflectionTestUtils;` 제거, `import org.apache.commons.lang3.reflect.FieldUtils;` 추가
    - `ReflectionTestUtils.setField(target, name, value)` 호출 8곳을 `FieldUtils.writeField(target, name, value, true)`로 교체 (기존 테스트 로직·단언은 변경하지 않는다 — design.md Component 2 before/after 참고)
    - _Requirements: 3.1, 3.2, 3.3_

  - [x] 1.4 Step 1 검증
    - `./gradlew compileTestJava` — 컴파일 오류 없음 확인
    - `./gradlew clean test` — 기존 8개 테스트 메서드 전부 통과 확인
    - `./gradlew dependencies` — 버전 충돌 없음, `commons-lang3`가 `implementation`으로 분류되는지 확인
    - 실패 시 design.md Error Handling 표에 따른 폴백을 적용하고 `docs/backlog.md`에 사유를 기록한다: `commons-lang3`/junit/mockito/assertj/lombok 갱신 실패 → 직전 최신 버전(1.3, 2.6) / `autoparams` 컴파일 실패 → 직전 호환 major(4.4) / `FieldUtils.writeField()`가 `IllegalAccessException`을 던지는 개별 호출 → 해당 호출만 표준 `java.lang.reflect`로 폴백(3.4)
    - _Requirements: 1.2, 1.3, 2.5, 2.6, 3.3, 3.4, 4.3, 4.4_

- [x] 2. Checkpoint — Step 1 완료 확인
  - `./gradlew clean test`가 8/8 그린인지, `build.gradle.kts`/`gradle/libs.versions.toml` 어디에도 `spring-test`가 남아 있지 않은지 확인한다. 문제가 있으면 작업을 멈추고 사용자에게 질문한다.

- [x] 3. Step 2: Javadoc·소스 jar 파이프라인

  - [x] 3.1 `java {}` 블록에 소스/Javadoc jar 생성 설정 추가
    - `withSourcesJar()`, `withJavadocJar()` 호출 추가 (Java 25 toolchain 설정은 그대로 유지)
    - _Requirements: 5.1_

  - [x] 3.2 Javadoc 인코딩 및 `Xdoclint:none` 설정 추가
    - `tasks.withType<Javadoc> {}` 블록: `options.encoding`, `charSet`, `docEncoding` 모두 `UTF-8`, `addBooleanOption("Xdoclint:none", true)`
    - _Requirements: 5.3, 5.4_

  - [x] 3.3 `publishing {}` 블록으로 `MavenPublication` 구성
    - `create<MavenPublication>("mavenJava")`, `from(components["java"])`
    - POM 메타데이터는 삭제된 `pom.xml`의 실제 값을 그대로 옮긴다: name, description("Byte To Object Converter"), url, licenses(Apache-2.0), developers(id `libedi`, name "Sangjun, Park", email `libedi@gmail.com`), scm(connection/developerConnection/url)
    - _Requirements: 5.5_

  - [x] 3.4 Step 2 검증
    - `./gradlew build` 실행 — `build/libs/`에 `byte-to-object-converter-2.0.0.jar`, `-sources.jar`, `-javadoc.jar` 3개 생성 확인
    - javadoc 로그에 인코딩 오류·`{@link}`/`@see` 참조 실패로 인한 빌드 실패가 없는지 확인
    - _Requirements: 5.2, 5.3, 5.4_

- [x] 4. Checkpoint — Step 2 완료 확인
  - `./gradlew build`가 성공하고 `build/libs/`에 3개 jar가 모두 생성되었는지 확인한다. 문제가 있으면 작업을 멈추고 사용자에게 질문한다.

- [x] 5. Step 3: GPG 서명 + Central Portal 배포 플러그인

  - [x] 5.1 `plugins {}` 블록에 `maven-publish`, `signing`, `com.gradleup.nmcp` 추가 (`maven-publish`는 task 3.3의 `publishing{}` 블록 컴파일에 필요해 그때 선행 추가함 — 여기서는 `signing`, `nmcp` 추가로 완료)
    - `` `maven-publish` ``, `signing`, `alias(libs.plugins.nmcp)` 추가
    - _Requirements: 6.1, 7.1_

  - [x] 5.2 `signing {}` 블록 구성
    - `isRequired = project.hasProperty("signing.required")`, `sign(publishing.publications["mavenJava"])`
    - _Requirements: 6.1, 6.2, 6.3, 6.4_

  - [x] 5.3 `nmcp {}` 블록 구성
    - `publishAllPublicationsToCentralPortal { username = providers.gradleProperty("centralPortalUsername").orElse(""); password = providers.gradleProperty("centralPortalPassword").orElse(""); publishingType = "USER_MANAGED" }`
    - 자격증명 값이나 값을 담는 템플릿 파일을 저장소에 추가하지 않는다 — design.md Component 5·6 참고
    - _Requirements: 7.1, 7.2, 7.3, 7.4_

  - [x] 5.4 `.gitignore`에 `gradle.properties` 추가
    - 저장소에 이 파일을 만들 계획은 없지만(Component 5·6 설계), 개발자가 로컬 편의상 프로젝트 루트에 실수로 만들었을 때 커밋되지 않도록 안전장치로 추가한다
    - _Requirements: 6.4, 7.2_

  - [x] 5.5 Step 3 검증 (로컬에 GPG 키 없어 `.asc` 생성 케이스는 미검증 — 나머지 세 항목은 모두 확인)
    - GPG 키가 로컬(`~/.gradle/gradle.properties`)에 설정된 경우: `./gradlew signMavenJavaPublication` → `build/libs/*.asc` 4개 파일 생성 확인
    - GPG 키가 없는 경우: `signing.required` 미설정 상태에서 `./gradlew build`가 서명 없이 성공하는지 확인
    - `./gradlew publishAllPublicationsToCentralPortal --dry-run` — sign 관련 태스크가 publish 태스크보다 먼저 실행 순서에 오는지 확인
    - 자격증명 미설정 상태에서 배포 태스크 실행 시 업로드 전에 인증 실패로 중단되는지 확인(부분 업로드 없음)
    - _Requirements: 6.2, 6.3, 7.3, 7.4, 7.5_

- [x] 6. Checkpoint — Step 3 완료 확인
  - 서명·배포 파이프라인이 로컬에서 의도대로 동작하는지(또는 키/자격증명 없이도 빌드가 깨지지 않는지) 확인한다. 문제가 있으면 작업을 멈추고 사용자에게 질문한다.

- [x] 7. Final Checkpoint — 전체 통합 검증
  - `./gradlew clean test` — 기존 8개 테스트 메서드가 모두 통과하며 빌드가 성공적으로 완료되는지 확인
  - `./gradlew build` — 오류 없이 빌드가 성공적으로 완료되는지 확인
  - Java 25 toolchain 설정이 그대로인지 확인
  - `./gradlew dependencies`에 미해결 버전 충돌이 없는지(있다면 `constraints`/`resolutionStrategy`로 해결) 확인
  - _Requirements: 8.1, 8.2, 8.3, 8.4_
  - 모든 검증이 통과하면 다음 태스크로 진행한다. 문제가 있으면 작업을 멈추고 사용자에게 질문한다.

- [x] 8. 관련 문서 업데이트 (필수)
  - 이 프로젝트에는 아직 `specs/_baseline.md`가 없다. 이번 스펙에서 나온 결정·제약 중 `references/spec-promotion-rules.md` 기준으로 재사용성·일관성·정책 성격을 가진 항목이 다수 있어, 이번에 처음으로 baseline을 만든다.
  - [x] 8.1 `specs/_baseline.md` 신규 생성 (`assets/baseline-template.md` 기준, Overview는 CLAUDE.md 요약)
  - [x] 8.2 G-1 추가: 테스트 전용 의존성은 `testImplementation`/`testCompileOnly`/`testAnnotationProcessor`/`testRuntimeOnly` 등 test 스코프로만 선언하고 런타임 아티팩트에 노출하지 않는다
  - [x] 8.3 G-2 추가: 공개 API(public/protected 시그니처)에 노출되지 않는 런타임 의존성은 `api`가 아니라 `implementation`으로 선언한다 — 추가 전에 실제로 노출되는지 확인한다
  - [x] 8.4 G-3 추가: 서명 키·Central Portal 토큰 등 자격증명은 저장소 안 어떤 파일에도(템플릿 포함) 두지 않는다 — 프로퍼티의 존재 여부로만 필수 여부를 판단하고, 실제 값은 개발자 개인의 `~/.gradle/gradle.properties` 또는 CI 환경 변수에서만 읽는다
  - [x] 8.5 G-4 추가: 의존성 좌표·버전은 `build.gradle.kts`에 직접 쓰지 않고 `gradle/libs.versions.toml` Version Catalog에 선언한다
  - [x] 8.6 G-5 추가: Gradle 빌드 스크립트는 Kotlin DSL(`build.gradle.kts`, `settings.gradle.kts`)을 쓴다 — Groovy DSL로 새로 작성하지 않는다 (원본: `docs/backlog.md` 2026-08-22 세션 로그)
  - [x] 8.7 D-1 추가: Central Portal 배포 플러그인으로 `com.gradleup.nmcp`(`publishingType = USER_MANAGED`)를 채택 — 근거 한 줄 요약 + 이 스펙 [design.md](design.md) Component 6 링크
  - [x] 8.8 Glossary에 "최신 안정 버전" 승격 — 다른 스펙에서도 의존성 버전 하한을 정의할 때 반복될 공통 용어
  - 이 태스크가 완료(`[x]`)되면 스펙 전체가 완료된 것이다. 완료된 스펙은 이후 편집하지 않는다 — 새로운 필요가 생기면 새 스펙을 만든다.

---

## Notes

- `autoparams` 1.1.1 → 11.3.2는 major 버전 10개 차이이므로 breaking change 가능성이 있다. 컴파일 오류 시 design.md Error Handling 표대로 직전 호환 major로 낮추고 `docs/backlog.md`에 기록한다.
- 서명 키·Central Portal 토큰 등 자격증명 값은 이 저장소 어디에도 두지 않는다 — 로컬 검증이 필요하면 각자 `~/.gradle/gradle.properties`에 설정한다. `gradle.properties` 자체가 금지된 것은 아니며, 비밀이 아닌 Gradle 설정이 필요해지면 프로젝트에 커밋된 `gradle.properties`를 추가할 수 있다(이 스펙에서는 그런 설정이 없어 추가하지 않는다).
- Central Portal 실제 업로드·정식 배포는 이 스펙의 범위 밖이다(Constraints C-3) — Step 3 검증은 `--dry-run`과 로컬 서명까지만 확인한다.
