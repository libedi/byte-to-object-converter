# Requirements Document: dependency-plugin-modernization

## Introduction

`byte-to-object-converter`는 Phase 1·2에서 Gradle 9.7.0 + Java 25 toolchain 기반으로 빌드 시스템을 전환했다(`docs/backlog.md` 참고). 이 스펙은 그 뒤를 잇는 **Phase 3**로, 남은 두 가지 미해결 문제를 다룬다.

1. 런타임 1종 + 테스트 6종 의존성이 노후화되어 있으며, 특히 `spring-test 5.0.0.RELEASE`(2017년산)의 존속 여부가 불확실하다.
2. `pom.xml` 기준으로만 존재했던 Javadoc/소스 jar/GPG 서명/Sonatype 배포 파이프라인이 Gradle 전환 이후 아직 재구성되지 않았다.

이 스펙이 완료되면 `./gradlew build` 한 번으로 서명 가능한 배포 아티팩트 일체(바이너리 jar, 소스 jar, javadoc jar)가 생성되고, Central Portal에 게시할 Gradle 플러그인이 정해져 인증 정보와 함께 연결된 상태가 된다. 실제 Maven Central 정식 배포 실행은 이 스펙의 범위가 아니다 — Phase 6에서 다룬다.

---

## Glossary

*이 프로젝트에는 아직 `specs/_baseline.md`가 없다. 아래 용어는 모두 이 스펙 한정이며, 이후 다른 스펙에서도 반복되면 baseline으로 승격할 후보다.*

| 용어 | 정의 |
| ---- | ---- |
| **Build_System** | 이 프로젝트의 빌드 도구. Gradle 9.7.0 + Kotlin DSL (`build.gradle.kts`). |
| **Central_Portal** | Sonatype이 운영하는 Maven Central 신규 게시 경로(`central.sonatype.com`). 2025-06-30 OSSRH sunset 이후 유일한 게시 경로. |
| **Publishing_Plugin** | Central Portal에 아티팩트를 업로드하는 Gradle 서드파티 플러그인(공식 Gradle 플러그인은 아직 없음). |
| **ReflectionTestUtils** | `spring-test`가 제공하던 테스트 유틸리티. 테스트 코드에서 `private` 필드에 값을 직접 주입하는 데 쓰인다. |
| **BOM** | Bill of Materials. 여러 관련 의존성의 버전을 한 번에 관리하는 Gradle/Maven 메커니즘(예: `junit-bom`). |
| **SNAPSHOT** | 버전 문자열이 `-SNAPSHOT`으로 끝나는 개발 중 비릴리스 버전. Central Portal의 별도 SNAPSHOT 저장소에 게시된다. |
| **최신 안정 버전** | 이 스펙의 각 Acceptance Criteria에 명시된 하한 버전(스펙 작성일 2026-08-26 기준 Maven Central 조회 결과) 이상이면서, pre-release/milestone(`-M1`, `-RC1` 등)이 아닌 가장 최근 버전. 실제 구현 시점에 더 새 버전이 나와 있으면 그 버전을 우선 시도한다. |

---

## Requirements

### Requirement 1: 런타임 의존성(`commons-lang3`) 최신화

**User Story:** As a 메인테이너, I want 런타임 의존성 `commons-lang3`를 현재 안정 버전으로 갱신하는 것, so that 이 라이브러리를 쓰는 소비자가 최신 보안 패치와 JDK 25 호환성을 얻을 수 있다.

#### Acceptance Criteria

1. THE Build_System SHALL 버전 `3.20.0` 이상의 `org.apache.commons:commons-lang3`를, 이 라이브러리의 소비자에게 컴파일 타임에는 노출되지 않고 런타임에만 포함되는 내부 구현 전용 의존성으로 포함한다.
2. WHEN 갱신 후 `./gradlew compileJava`를 실행하면, THE Build_System SHALL 오류 없이 컴파일한다.
3. IF 대상 `commons-lang3` 버전이 이 프로젝트가 실제로 사용하는 API에 컴파일을 깨는 변경을 포함하면, THEN THE Build_System SHALL 컴파일이 성공하는 가장 최신 버전을 사용하고 그 사유를 `docs/backlog.md`에 기록한다.

---

### Requirement 2: 테스트 전용 의존성(JUnit5·Mockito·AssertJ·Lombok) 최신화

**User Story:** As a 메인테이너, I want JUnit 5, Mockito, AssertJ, 테스트 스코프 Lombok 의존성을 현재 안정 버전으로 갱신하는 것, so that 테스트 스위트가 지원·패치되는 버전 위에서 동작할 수 있다.

#### Acceptance Criteria

1. THE Build_System SHALL `org.junit:junit-bom`을 버전 `5.14.4` 이상으로 선언한다.
2. THE Build_System SHALL `org.mockito:mockito-core`와 `org.mockito:mockito-junit-jupiter`를 버전 `5.22.0` 이상으로 선언한다.
3. THE Build_System SHALL `org.assertj:assertj-core`를 버전 `3.27.7` 이상으로 선언한다.
4. THE Build_System SHALL 버전 `1.18.38` 이상의 `org.projectlombok:lombok`을, 테스트 코드 컴파일에는 쓰이되 런타임 아티팩트에는 포함되지 않는 의존성으로 선언한다.
5. WHEN 위 갱신 후 `./gradlew test`를 실행하면, THE Build_System SHALL `ByteToObjectConverterTest`의 기존 테스트 메서드를 모두 통과시킨다.
6. IF 위 네 의존성 중 하나의 갱신이 컴파일 또는 테스트 실패를 유발하면, THEN THE Build_System SHALL 실패 직전의 최신 버전을 사용하고 그 사유를 `docs/backlog.md`에 기록한다.

---

### Requirement 3: `spring-test` 의존성 제거

**User Story:** As a 메인테이너, I want 2017년산 `spring-test` 의존성을 테스트 의존성 목록에서 제거하는 것, so that 테스트 환경이 JDK 25 모듈 시스템과 함께 유지보수되지 않는 전이 의존성 그래프를 짊어지지 않을 수 있다.

#### Acceptance Criteria

1. THE Build_System SHALL `build.gradle.kts` 어디에도 `org.springframework:spring-test`를 선언하지 않는다.
2. THE Build_System SHALL `ByteToObjectConverterTest`가 `ReflectionTestUtils`를 통해 현재 쓰고 있는 private 필드 주입 기능을, 새 의존성을 추가하지 않고 프로젝트가 이미 선언한 의존성만으로 제공한다.
3. WHEN `./gradlew test`를 실행하면, THE Build_System SHALL `ReflectionTestUtils`에 의존하던 기존 테스트 메서드를 테스트 로직·단언(assertion) 변경 없이 모두 통과시킨다.
4. IF `spring-test` 제거가 이미 선언된 의존성만으로는 해결할 수 없는 방식으로 테스트를 깨뜨리면, THEN THE Build_System SHALL 새 의존성을 추가하지 않고 해당 테스트를 통과시키는 다른 방법을 적용하고 그 사유를 `docs/backlog.md`에 기록한다.

---

### Requirement 4: `autoparams` / `autoparams-lombok` 최신화

**User Story:** As a 메인테이너, I want `autoparams`와 `autoparams-lombok`을 현재 major 버전으로 갱신하는 것, so that 파라미터화 테스트 데이터 생성이 현재 툴체인과 계속 호환되며 유지보수될 수 있다.

#### Acceptance Criteria

1. THE Build_System SHALL `io.github.autoparams:autoparams`와 `io.github.autoparams:autoparams-lombok`을 서로 일치하는 버전 번호로 선언한다.
2. THE Build_System SHALL 둘 다, 테스트 단언(assertion)을 바꾸지 않고 컴파일되고 기존 테스트 스위트를 통과시키는(기계적인 import/API 이름 변경은 허용) Maven Central상의 최신 안정 major 버전으로 선언한다.
3. WHEN 갱신 후 `./gradlew clean test`를 실행하면, THE Build_System SHALL 기존 테스트 메서드를 모두 통과시킨다.
4. IF 최신 major 버전이 기존 테스트가 의존하는 breaking API 변경을 포함하고 기계적인 이름 변경만으로 해결할 수 없으면, THEN THE Build_System SHALL 호환되는 직전 major 버전을 사용하고 그 사유를 `docs/backlog.md`에 기록한다.

---

### Requirement 5: Javadoc·소스 jar 생성 파이프라인 (JDK 25 대응)

**User Story:** As a 메인테이너, I want `./gradlew build`가 JDK 25 인코딩이나 `{@link}` 오류 없이 소스 jar와 javadoc jar를 생성하는 것, so that 게시 전 로컬에서 릴리스 아티팩트를 조립해 검증할 수 있다.

#### Acceptance Criteria

1. THE Build_System SHALL 프로젝트의 Maven publication 일부로 소스 jar와 javadoc jar를 생성한다.
2. WHEN `./gradlew build`를 실행하면, THE Build_System SHALL `build/libs/` 아래에 `byte-to-object-converter-<version>.jar`, `byte-to-object-converter-<version>-sources.jar`, `byte-to-object-converter-<version>-javadoc.jar`를 생성한다.
3. WHEN javadoc 태스크가 JDK 25 toolchain에서 실행되면, THE Build_System SHALL 문자 인코딩 오류 없이 완료한다.
4. IF `{@link}`/`@see` 참조 검증이 javadoc 태스크를 실패시킬 상황이면, THEN THE Build_System SHALL 그런 참조 문제가 빌드를 실패시키지 않도록 구성되어 있다.
5. THE Build_System SHALL publication의 POM 메타데이터에 다음 필드를 포함한다: `groupId`(`io.github.libedi`), `artifactId`(`byte-to-object-converter`), `version`, `name`, `description`, `url`, `licenses`, `developers`(최소 1명), `scm`(`connection`, `developerConnection`, `url`).

---

### Requirement 6: GPG 서명 파이프라인 (선택적 적용)

**User Story:** As a 메인테이너, I want 서명 키가 있을 때 로컬에서 publication 아티팩트에 GPG 서명을 적용하는 설정, so that Phase 6의 실제 릴리스 전에 완전히 서명된 아티팩트 세트를 검증할 수 있다.

#### Acceptance Criteria

1. THE Build_System SHALL Maven publication의 각 아티팩트(바이너리 jar, 소스 jar, javadoc jar, POM)에 GPG 서명을 적용할 수 있다.
2. WHEN 로컬 GPG 서명 프로퍼티가 설정된 상태에서 서명 태스크를 실행하면, THE Build_System SHALL 4개 아티팩트 각각에 대해 `.asc` 파일을 생성한다.
3. IF 로컬 환경에 GPG 서명 프로퍼티가 설정되어 있지 않으면, THEN `./gradlew build`는 서명을 시도하지 않고 성공적으로 완료된다.
4. THE Build_System SHALL git으로 추적되는 어떤 파일에도 GPG 자격증명 값(key id, passphrase, keyring 경로)을 포함하지 않는다.

---

### Requirement 7: Central Portal 배포 플러그인 선정 및 인증 연결

**User Story:** As a 메인테이너, I want SNAPSHOT 버전을 포함해 Sonatype Central Portal에 게시할 수 있는 Gradle 플러그인, so that Maven 빌드가 갖고 있던 배포 파이프라인(`central-publishing-maven-plugin`)이 Gradle 환경에서도 복원될 수 있다.

#### Acceptance Criteria

1. THE Build_System SHALL MavenPublication을 Central_Portal에 업로드할 수 있는 Publishing_Plugin을 적용한다.
2. THE Build_System SHALL Central_Portal 자격증명(사용자명, 토큰/비밀번호)을 Gradle 프로퍼티 또는 환경 변수에서 읽으며, git으로 추적되는 어떤 파일에도 하드코딩하지 않는다.
3. WHEN 프로젝트 버전 문자열이 `-SNAPSHOT`으로 끝나면, THE Build_System SHALL 해당 버전을 Central_Portal의 SNAPSHOT 저장소에 게시하도록 구성되어 있다.
4. IF 자격증명이 구성되지 않은 상태에서 Central_Portal 게시 태스크가 실행되면, THEN THE Build_System SHALL 어떤 아티팩트도 업로드하기 전에 실패한다 — 부분적이거나 인증되지 않은 업로드는 발생하지 않는다.
5. WHEN GPG 서명이 활성화되어 있으면(Requirement 6), THE Build_System SHALL Central_Portal에 업로드하는 게시 태스크보다 서명 태스크가 먼저 완료되도록 보장한다.

---

### Requirement 8: 회귀 방지 — 전체 빌드/테스트 그린 유지

**User Story:** As a 메인테이너, I want Phase 3의 모든 변경 후에도 전체 테스트 스위트와 전체 빌드가 여전히 성공하는지 확인하는 단일 명령, so that 브랜치가 머지되기 전에 회귀를 바로 잡아낼 수 있다.

#### Acceptance Criteria

1. WHEN Phase 3의 모든 변경을 적용한 뒤 `./gradlew clean test`를 실행하면, THE Build_System SHALL 기존 테스트 메서드를 모두 통과시키며 빌드를 성공적으로 완료한다.
2. WHEN Phase 3의 모든 변경을 적용한 뒤 `./gradlew build`를 실행하면, THE Build_System SHALL 오류 없이 빌드를 성공적으로 완료한다.
3. THE Build_System SHALL Phase 2에서 확립한 Java 25 toolchain 설정을 유지한다.
4. IF `./gradlew dependencies`가 이 스펙의 변경으로 생긴 미해결 버전 충돌을 드러내면, THEN THE Build_System SHALL 명시적인 해결(`resolutionStrategy` 또는 `constraints` 항목)을 선언하여 단일 버전으로 수렴시킨다.

---

## Constraints

*이 스펙에서만 적용되는 범위 제한이다. 프로젝트 전역에 걸쳐 항상 성립해야 할 규칙(자격증명 비커밋, 테스트 전용 의존성 scope 규율 등)은 여기 적지 않고 tasks.md의 마지막 태스크에서 baseline 승격 대상으로 남긴다 — 이 프로젝트에는 아직 `specs/_baseline.md`가 없다.*

| ID | 제약 |
| -- | ---- |
| C-1 | 이 스펙에서는 Gradle 버전(9.7.0)을 변경하지 않는다. 버전 업이 필요해지면 별도 스펙에서 다룬다. |
| C-2 | 이 스펙에서는 `build.gradle.kts`의 DSL을 Kotlin에서 Groovy로 전환하지 않는다. |
| C-3 | 이 스펙은 배포 파이프라인의 "구성과 로컬 검증"(서명, `--dry-run` 등)까지만 다룬다. Central Portal에 대한 실제 아티팩트 업로드 및 정식 배포 실행은 Phase 6(별도 스펙)에서 수행한다. |
