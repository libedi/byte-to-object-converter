# v2.0 로드맵: Java 25 + Gradle 9.7.0 전환

`2.0.0`을 breaking major로 확정하고, 빌드 대상 Java를 25로 올리며 Maven → Gradle 9.7.0으로
빌드 시스템 자체를 전환한다. 범위가 크므로 단계별로 진행하고, 각 단계가 끝나면 실제로
빌드/테스트/publish 파이프라인이 살아있는 상태를 유지한다.

## 현재 상태 (2026-08-18 기준)

- 로컬 빌드 JDK: OpenJDK Temurin 25.0.3
- 현재 `pom.xml`: `java.version=1.8`(source/target), Maven 빌드
- v2.0 기능(양방향 변환, `DataAlignment`, `Ignorable`, 예외 세부 타입 분리)은 이미 구현·커밋됨
- Sonatype OSSRH → Central Publishing 플러그인(`central-publishing-maven-plugin`) 마이그레이션 진행 중
  - `publishingServerId=central`에 맞춰 `~/.m2/settings.xml`에 `central` 서버(User Token) 등록 완료
  - 플러그인 버전 0.4.0 → 0.7.0으로 올려 `-SNAPSHOT` publish 지원 확보, `snapshotRepository` 추가
  - `lombok` 1.18.28 → 1.18.38로 올려 JDK 25 컴파일 오류(`TypeTag :: UNKNOWN`) 해소
- SNAPSHOT dry-run 도중 추가로 발견된 문제 (미해결):
  - `maven-javadoc-plugin 3.3.1`이 JDK 25 javadoc 툴에서 인코딩 깨짐 + `{@link}`/`@see` 참조 실패
  - 테스트 4건이 JPMS 리플렉션 차단으로 에러: `module java.base does not "opens java.util"`
  - 테스트 1건 assertion 실패: `testConvertInputStream_DirectMethod` — `expected: 15 but was: 10` (원인 미조사)
  - `maven-source-plugin`, `maven-gpg-plugin`, `maven-compiler-plugin`, `maven-enforcer-plugin`, `maven-surefire-plugin` 모두 2020~2023년 버전으로 노후화

## 방향

라이브러리의 "legacy 시스템 byte array 처리"라는 용도 자체는 유지하되, **2.0부터는 최신 Java(25)를 최소
요구 버전으로 못박는 명시적 breaking change**로 간다. 기존 Java 8 사용자는 1.x 라인에 남는다.

## 단계

### Phase 1 — Gradle 전환 (뼈대)
- [ ] `gradlew`/`gradlew.bat`/`gradle/wrapper` 생성 (Gradle 9.7.0)
- [ ] `build.gradle(.kts)` 작성: 현재 `pom.xml`의 dependency/plugin 목록을 Gradle 구성으로 이식
- [ ] `settings.gradle(.kts)` 작성 (프로젝트명 등)
- [ ] 기존 `mvnw`/`mvnw.cmd`/`pom.xml` 제거 여부 결정 (완전 전환 vs 과도기 병행)

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
- [ ] JPMS `opens` 문제 해결: 필요한 모듈 접근을 테스트 실행 설정(`--add-opens` 등)으로 열거나, 리플렉션 의존 코드/테스트 자체를 재검토
- [ ] `testConvertInputStream_DirectMethod` assertion 실패(`expected 15 but was 10`) 원인 조사 및 수정
- [ ] 전체 테스트 그린 상태 확보

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
