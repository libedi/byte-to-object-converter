# v2.0 로드맵: Java 25 + Gradle 9.7.0 전환

`2.0.0`을 breaking major로 확정하고, 빌드 대상 Java를 25로 올리며 Maven → Gradle 9.7.0으로
빌드 시스템 자체를 전환한다. 범위가 크므로 단계별로 진행하고, 각 단계가 끝나면 실제로
빌드/테스트/publish 파이프라인이 살아있는 상태를 유지한다.

## 현재 상태 (2026-08-22 기준)

- 로컬 빌드 JDK: OpenJDK Temurin 25.0.3
- 현재 `pom.xml`: `java.version=1.8`(source/target), Maven 빌드 (아직 존재 — 제거 전)
- v2.0 기능(양방향 변환, `DataAlignment`, `Ignorable`, 예외 세부 타입 분리)은 이미 구현·커밋됨
- Sonatype OSSRH → Central Publishing 플러그인(`central-publishing-maven-plugin`) 마이그레이션 진행 중
  - `publishingServerId=central`에 맞춰 `~/.m2/settings.xml`에 `central` 서버(User Token) 등록 완료
  - 플러그인 버전 0.4.0 → 0.7.0으로 올려 `-SNAPSHOT` publish 지원 확보, `snapshotRepository` 추가
  - `lombok` 1.18.28 → 1.18.38로 올려 JDK 25 컴파일 오류(`TypeTag :: UNKNOWN`) 해소
- SNAPSHOT dry-run 도중 발견된 문제 중 테스트 관련 2건은 **해결·커밋 완료** (커밋 `2fe1e61`, 상세는 Phase 4 및 하단 세션 로그 참고)
- 아직 남은 미해결 문제:
  - `maven-javadoc-plugin 3.3.1`이 JDK 25 javadoc 툴에서 인코딩 깨짐 + `{@link}`/`@see` 참조 실패 (미조사)
  - `maven-source-plugin`, `maven-gpg-plugin`, `maven-compiler-plugin`, `maven-enforcer-plugin`, `maven-surefire-plugin` 모두 2020~2023년 버전으로 노후화 (Gradle 전환하면서 자연히 재검토됨 → Phase 3)

## 방향

라이브러리의 "legacy 시스템 byte array 처리"라는 용도 자체는 유지하되, **2.0부터는 최신 Java(25)를 최소
요구 버전으로 못박는 명시적 breaking change**로 간다. 기존 Java 8 사용자는 1.x 라인에 남는다.

## 단계

### Phase 1 — Gradle 전환 (뼈대)
- [x] `gradlew`/`gradlew.bat`/`gradle/wrapper` 생성 (Gradle 9.7.0)
- [x] `settings.gradle(.kts)` 작성 — Kotlin DSL, `rootProject.name = "byte-to-object-converter"`
- [x] `build.gradle.kts` 작성 — 2026-08-23, `pom.xml`의 dependency 목록을 그대로 이식.
  - `commons-lang3:3.14.0`은 라이브러리 소비자에게도 노출되어야 하므로 `implementation`이 아니라 `api`로 선언 (Maven의 기본 compile scope와 동치)
  - `lombok`은 `src/main`에서는 사용되지 않고 테스트 코드에서만 쓰이는 걸 확인(`grep`)하여 `testCompileOnly`/`testAnnotationProcessor`로만 선언 (main 쪽엔 아예 추가 안 함 — pom.xml에도 lombok은 test scope뿐이었음)
  - Gradle 9는 `junit-platform-launcher`를 테스트 런타임 클래스패스에 명시적으로 요구함 — 처음 `./gradlew test` 실행 시 `TestFrameworkNotAvailableException`으로 실패했고, `testRuntimeOnly("org.junit.platform:junit-platform-launcher")` 추가로 해결 (pom.xml엔 없던 항목, Gradle 9 고유 요구사항)
  - Javadoc/GPG/Central Publishing 플러그인은 Phase 3 범위라 아직 미포함
  - `./gradlew test` 8/8 통과 확인 (Maven 결과와 동일)
- [x] 기존 `mvnw`/`mvnw.cmd`/`pom.xml`/`.mvn/` 제거 — 2026-08-23, `git rm`으로 삭제(스테이징만, 아직 커밋 안 됨). 삭제 후 `./gradlew clean test`로 Gradle 단독 상태에서도 재확인 완료.

### Phase 2 — Java 25 베이스라인
- [ ] `build.gradle`의 `sourceCompatibility`/`toolchain`을 Java 25로 설정
- [ ] JDK 25 기준으로 컴파일 확인 (lombok 최신 버전은 이미 1.18.38로 확보됨 — Gradle 쪽에도 동일 버전 적용)
- [ ] 소스 코드 중 Java 8 시절 관례(예: 구식 리플렉션 접근 패턴)를 최신 API로 정리할 부분 있는지 점검

### Phase 3 — 의존성/플러그인 전면 최신화
- [ ] 런타임: `commons-lang3` 최신 버전 확인
- [ ] 테스트: JUnit5, Mockito, AssertJ, AutoParams, Lombok(test), **`spring-test 5.0.0.RELEASE`(2017년산, 특히 노후)** 최신화 또는 대체 검토
- [ ] Javadoc/소스잭/GPG 서명에 해당하는 Gradle 플러그인 구성 (`java-library`, `maven-publish`, `signing`)
- [ ] Sonatype Central Publishing용 Gradle 플러그인 결정 (예: Central Portal 공식 Gradle 플러그인) 및 인증 연결

### Phase 4 — 테스트 정상화
- [x] JPMS `opens` 문제 해결 — 2026-08-22, Maven 기준으로 조기 해결(순서상 Phase 1보다 먼저 처리, 커밋 `2fe1e61`). `--add-opens` 등 실행 설정으로 우회하는 대신, `DeconversionHelper.getListSize()`/`deconvertElement()`가 `MethodUtils.invokeMethod(list, true, "size"/"get")`로 리스트의 실제 런타임 클래스에 강제 리플렉션 접근하던 걸 `List` 캐스팅 후 직접 호출로 변경. 테스트가 `subList()`로 만드는 JDK 내부 클래스(`ArrayList$SubList`)를 넘길 때 JDK 16+ 모듈 시스템이 `setAccessible`을 막던 게 근본 원인이었음. Gradle 전환 이후에도 이 수정은 그대로 유효.
- [x] `testConvertInputStream_DirectMethod` assertion 실패(`expected 15 but was 10`) 원인 조사 및 수정 — 2026-08-22, 커밋 `2fe1e61`. JDK 버전과 무관한 순수 테스트 버그였음: `convertInputStream()`은 결과를 trim해서 반환하도록 설계되어 있는데, 테스트는 trim된 문자열 길이가 trim 전 길이(15)와 같아야 한다고 assert하고 있어서 `int` 값이 우연히 15자리가 아닌 이상(사실상 항상) 실패하는 구조였음. 실제 값/다음 읽기 위치를 검증하도록 assertion을 고침.
- [x] 전체 테스트 그린 상태 확보 — Maven 기준 `mvn test` 8/8 통과 확인(2026-08-22). **단, Gradle 전환 후 `./gradlew test`로 재확인 필요** (Phase 1 마무리 항목).

### Phase 5 — 문서화
- [ ] `CLAUDE.md` 갱신: Java 25 요구사항, Gradle 빌드/테스트 명령어로 교체
- [ ] `README` 갱신: 설치/빌드 안내, 최소 Java 버전 명시, 1.x와의 breaking change 안내

### Phase 6 — Publish 파이프라인 재검증 & 실제 릴리스
- [ ] Gradle 기준으로 `2.0.0-SNAPSHOT` 배포 dry-run (서명 + Sonatype 인증 + 업로드 확인)
- [ ] 전체 그린 상태에서 `2.0.0` 정식 배포
- [ ] GitHub 릴리스/태그 정리

## 메모

- 각 Phase 종료 시점마다 빌드 가능한 상태를 유지하는 것을 목표로 한다 (중간에 오래 깨진 상태로 두지 않기).
- Phase 순서는 유동적으로 조정 가능 — 진행하면서 막히는 지점이 있으면 이 문서를 갱신한다.

## 세션 로그

### 2026-08-23

- Phase 1 마무리: `build.gradle.kts` 작성 → `./gradlew test` 그린(8/8) 확인 → `pom.xml`/`mvnw`/`mvnw.cmd`/`.mvn/` `git rm`으로 제거 → `./gradlew clean test`로 재확인.
- 아직 커밋 안 됨 (다음 커밋에 다음을 함께 묶을 것): `build.gradle.kts`(신규), `gradlew`/`gradlew.bat`/`gradle/`(신규), `settings.gradle.kts`(신규), `pom.xml`/`mvnw`/`mvnw.cmd`/`.mvn/`(삭제), `docs/backlog.md`(갱신).
- **다음 세션 시작점**: 위 변경을 한 커밋으로 정리(커밋 메시지에 Maven→Gradle 전환 명시) → Phase 2(Java 25 베이스라인)로 진행.

### 2026-08-22

- **작업 순서**: "미해결 Maven 이슈부터 조사" → Phase 4 항목(JPMS, assertion 버그) 조기 해결·커밋(`2fe1e61`) → Phase 1(Gradle 뼈대) 착수.
- **결정 사항**:
  - Gradle 빌드 스크립트는 **Kotlin DSL**(`build.gradle.kts`, `settings.gradle.kts`)로 작성.
  - `mvnw`/`mvnw.cmd`/`pom.xml`/`.mvn/`은 과도기 병행 없이 **Phase 1에서 바로 삭제**하기로 결정 (아직 미실행).
- **커밋된 것** (`2fe1e61`): `DeconversionHelper.java`, `ByteToObjectConverterTest.java` 수정, `pom.xml`(lombok 1.18.38, snapshotRepository), `docs/backlog.md` 최초 추가, `.vscode/settings.json` 추가.
- **커밋 안 된 것 (untracked, 다음 세션에서 이어갈 것)**:
  - `settings.gradle.kts` — 작성 완료 (최소 버전)
  - `gradlew`, `gradlew.bat`, `gradle/wrapper/gradle-wrapper.jar`, `gradle/wrapper/gradle-wrapper.properties` — Gradle 9.7.0으로 생성 완료
  - `build.gradle.kts` — **아직 파일로 존재하지 않음**. Phase 1 체크리스트 항목에 적어둔 초안 방향대로 작성부터 시작할 것.
- **다음 세션 시작점**:
  1. `build.gradle.kts` 작성 (Phase 1 체크리스트의 초안 참고)
  2. `./gradlew test`로 전체 그린 확인
  3. `pom.xml`/`mvnw`/`mvnw.cmd`/`.mvn/` 삭제
  4. Gradle 관련 파일 + pom.xml 삭제를 한 커밋으로 정리
  5. 이후 Phase 2(Java 25 베이스라인)로 진행
