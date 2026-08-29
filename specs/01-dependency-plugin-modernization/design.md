# Design Document: dependency-plugin-modernization

## Overview

이 설계는 승인된 requirements.md의 8개 Requirement를 세 갈래로 묶어 구현한다.

1. 런타임·테스트 의존성을 Gradle Version Catalog(`gradle/libs.versions.toml`)로 관리하며 현재 안정 버전으로 올리고, `spring-test`를 제거한다(Requirements 1~4).
2. Javadoc/소스 jar/GPG 서명 파이프라인을 `java-library` + `maven-publish` + `signing` 플러그인으로 구성한다(Requirement 5, 6).
3. Central Portal에 게시할 Gradle 플러그인을 선정하고 자격증명 연결 방식을 정한다(Requirement 7).

회귀 방지(Requirement 8)는 별도 컴포넌트가 아니라 각 단계의 검증 절차와 Testing Strategy로 다룬다.

---

## Architecture

```text
byte-to-object-converter/
├── gradlew, gradlew.bat                              ← 기존 파일(Phase 1에서 생성), 변경 없음 — 아래 모든 검증 명령의 실행 진입점
├── gradle/
│   ├── wrapper/                                      ← 기존, 변경 없음
│   └── libs.versions.toml                            ← 신규 생성. 의존성 좌표·버전을 한 곳에 모으는 Version Catalog
├── build.gradle.kts                                  ← 플러그인·퍼블리싱·서명 설정. 의존성 선언은 libs.* 참조로 대체
├── .gitignore                                        ← `gradle.properties` 추가 (로컬에 실수로 만들어져도 커밋되지 않도록)
├── docs/backlog.md                                   ← 각 Requirement의 IF...THEN 폴백이 실제로 발동하면 사유 기록
└── src/test/java/.../ByteToObjectConverterTest.java  ← ReflectionTestUtils → FieldUtils 교체
```

소스 코드 변경은 `ByteToObjectConverterTest.java`의 리플렉션 유틸리티 교체 한 곳뿐이다. GPG/Central Portal 자격증명 값은 이 저장소 안 어떤 파일에도 두지 않는다 — Component 5·6 참고. `gradle.properties` 자체가 금지되는 것은 아니다(비밀이 아닌 Gradle 설정이라면 프로젝트에 커밋해도 된다) — 다만 이 스펙에서는 그런 설정이 필요 없어서 새로 만들지 않는다.

---

## Components and Interfaces

### Component 1: Gradle Version Catalog 도입 (Requirements 1, 2, 4)

의존성 좌표·버전 문자열을 `build.gradle.kts`에 직접 쓰지 않고 `gradle/libs.versions.toml`에 모은다. Gradle 9.7.0은 이 파일이 `gradle/libs.versions.toml` 경로에 있으면 별도 설정 없이 자동으로 `libs` 카탈로그로 인식한다.

```toml
[versions]
commons-lang3 = "3.20.0"
junit-bom = "5.14.4"
mockito = "5.22.0"
assertj = "3.27.7"
lombok = "1.18.38"
autoparams = "11.3.2"
nmcp = "1.4.4"

[libraries]
commons-lang3 = { module = "org.apache.commons:commons-lang3", version.ref = "commons-lang3" }
junit-bom = { module = "org.junit:junit-bom", version.ref = "junit-bom" }
junit-jupiter = { module = "org.junit.jupiter:junit-jupiter" }
junit-platform-launcher = { module = "org.junit.platform:junit-platform-launcher" }
mockito-core = { module = "org.mockito:mockito-core", version.ref = "mockito" }
mockito-junit-jupiter = { module = "org.mockito:mockito-junit-jupiter", version.ref = "mockito" }
assertj-core = { module = "org.assertj:assertj-core", version.ref = "assertj" }
autoparams = { module = "io.github.autoparams:autoparams", version.ref = "autoparams" }
autoparams-lombok = { module = "io.github.autoparams:autoparams-lombok", version.ref = "autoparams" }
lombok = { module = "org.projectlombok:lombok", version.ref = "lombok" }

[plugins]
nmcp = { id = "com.gradleup.nmcp", version.ref = "nmcp" }
```

**왜 이렇게 했는가:** 버전 문자열이 `build.gradle.kts` 곳곳에 흩어져 있으면 Requirement 8 AC 4(버전 충돌을 명시적으로 해결)를 지키기 어렵다. 카탈로그 한 곳만 보면 전체 버전 목록을 확인할 수 있고, `libs.commons.lang3`처럼 타입-세이프 접근자를 쓰므로 오타로 인한 좌표 실수도 컴파일 시점에 잡힌다.

의존성 선언(Requirements 1, 2)과 스코프는 아래와 같다.

| 의존성 | 현재 | 목표 | 컨피규레이션 |
| ---- | ---- | ---- | ---- |
| `commons-lang3` | `3.14.0` | `3.20.0` | `implementation` |
| `junit-bom` | `5.9.3` | `5.14.4` | `testImplementation` (platform) |
| `mockito-core` / `mockito-junit-jupiter` | `5.3.1` | `5.22.0` | `testImplementation` |
| `assertj-core` | `3.24.2` | `3.27.7` | `testImplementation` |
| `lombok`(test) | `1.18.38` | `1.18.38` (이미 최신) | `testCompileOnly` + `testAnnotationProcessor` |

`commons-lang3`는 `src/main`의 `ByteToObjectConverter`(public 파사드)와 `DataAlignment`(public enum)의 public/protected 시그니처를 확인한 결과 `InputStream`/`Class<?>`/`String`/`Object`/`DataAlignment` 등 표준 JDK 타입만 노출하고, `StringUtils`/`ArrayUtils`/`FieldUtils`/`ClassUtils`/`MethodUtils`는 전부 package-private인 `ConversionHelper`/`DeconversionHelper`/`AbstractCommonHelper` 내부에서만 쓰인다 — 즉 commons-lang3 타입이 이 라이브러리의 공개 API 어디에도 등장하지 않는다. 따라서 Requirement 1 AC 1이 요구하는 "컴파일 타임 비노출·런타임만 포함"을 만족시키려면 `api`가 아니라 `implementation`으로 충분하다.

`junit-bom`/`mockito`/`assertj`는 테스트 전용이므로 `testImplementation`으로, `lombok`은 Requirement 2 AC 4(런타임 아티팩트 비포함)를 만족시키기 위해 `testCompileOnly` + `testAnnotationProcessor`로 선언한다. `implementation` 스코프로 선언된 `commons-lang3`는 Gradle이 `testImplementation`을 `implementation`을 확장하도록 구성해두므로 테스트 코드(Component 2)에서도 별도 선언 없이 그대로 쓸 수 있다.

---

### Component 2: `spring-test` 제거 → `FieldUtils` 대체 (Requirement 3)

`ByteToObjectConverterTest.java`에서 `ReflectionTestUtils.setField(...)` 호출 8곳을 `FieldUtils.writeField(..., true)`로 교체한다. `commons-lang3`는 Component 1에서 이미 (테스트 코드에서도 보이는) 의존성으로 선언되므로 Requirement 3 AC 2("새 의존성 추가 없이")를 그대로 만족한다.

```java
// 변경 전
import org.springframework.test.util.ReflectionTestUtils;
...
ReflectionTestUtils.setField(expected, "dateTimeValue", newValue);
ReflectionTestUtils.setField(expected.getNestedLoopValue(), "count", expected.getNestedLoopValue().list.size());
ReflectionTestUtils.setField(expected, "voList", expected.getVoList().subList(0, 2));
ReflectionTestUtils.setField(expected, "ignorable", null);
```

```java
// 변경 후
import org.apache.commons.lang3.reflect.FieldUtils;
...
FieldUtils.writeField(expected, "dateTimeValue", newValue, true);
FieldUtils.writeField(expected.getNestedLoopValue(), "count", expected.getNestedLoopValue().list.size(), true);
FieldUtils.writeField(expected, "voList", expected.getVoList().subList(0, 2), true);
FieldUtils.writeField(expected, "ignorable", null, true);
```

`FieldUtils.writeField(Object target, String fieldName, Object value, boolean forceAccess)`의 네 번째 인자 `true`가 `private` 필드 강제 접근에 해당한다.

**왜 이렇게 했는가:** `ReflectionTestUtils`가 하던 일(임의 `private` 필드에 값 주입)은 이미 프로젝트가 의존성으로 갖고 있는 `commons-lang3`의 `FieldUtils`로 1:1 대체된다 — 새 라이브러리를 데려올 이유가 없다. Requirement 3 AC 4의 폴백(표준 Java Reflection API로 직접 처리)은 `FieldUtils.writeField()`가 `IllegalAccessException`을 던지는 등 예외적인 경우에만 개별 호출 단위로 적용한다.

---

### Component 3: `autoparams` / `autoparams-lombok` 버전 결정 (Requirement 4)

목표 버전: `11.3.2` (Maven Central 조회 기준, 2026-03-31 릴리스 — 실제 구현 시점에 재확인). 카탈로그의 `versions.autoparams` 하나를 두 라이브러리 항목이 함께 참조하도록 해 Requirement 4 AC 1(두 좌표의 버전 일치)을 구조적으로 보장한다.

현재 테스트가 쓰는 API(`@AutoSource`, `@Customization`, `BuilderCustomizer`)는 major 버전이 올라가도 이름이 유지되는 것으로 확인되어, 기계적 이름 변경 없이 컴파일될 가능성이 높다. 만약 컴파일이 깨지면 Requirement 4 AC 4에 따라 호환되는 직전 major 버전(예: `10.x`)으로 낮추고 `docs/backlog.md`에 기록한다.

**왜 이렇게 했는가:** `1.1.1` → `11.3.2`는 10개 major 버전 차이이므로 breaking change 가능성이 실재한다(라이브러리 저자 본인이 `10.0.0`에서 일부 breaking change를 공지한 바 있다). 그래서 requirements.md에 이미 "컴파일·테스트 실패 시 직전 호환 major로 폴백"이라는 조건부 AC를 넣어뒀고, 이 컴포넌트는 그 조건을 실행 시점에 어떻게 판단할지(컴파일 시도 → 실패하면 낮춘다)를 정한다.

---

### Component 4: Javadoc·소스 jar 파이프라인 (Requirement 5)

```kotlin
java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(25)
    }
    withSourcesJar()
    withJavadocJar()
}

tasks.withType<Javadoc> {
    options.encoding = "UTF-8"
    (options as StandardJavadocDocletOptions).apply {
        charSet = "UTF-8"
        docEncoding = "UTF-8"
        addBooleanOption("Xdoclint:none", true)
    }
}
```

`Xdoclint:none`은 Requirement 5 AC 4(`{@link}`/`@see` 참조 문제가 빌드를 실패시키지 않아야 함)를 만족시키는 메커니즘이다. 인코딩을 세 곳(`options.encoding`, `charSet`, `docEncoding`) 모두 `UTF-8`로 맞추는 이유는 JDK 25 javadoc 툴이 플랫폼 기본 인코딩을 따라가려는 경향이 있어 하나라도 빠지면 한글 Javadoc 주석에서 깨짐이 재발할 수 있기 때문이다.

POM 메타데이터(Requirement 5 AC 5)는 삭제 전 `pom.xml`에 있던 실제 값을 그대로 옮긴다(새로 지어내지 않는다):

```kotlin
publishing {
    publications {
        create<MavenPublication>("mavenJava") {
            from(components["java"])
            pom {
                name.set("byte-to-object-converter")
                description.set("Byte To Object Converter")
                url.set("https://github.com/libedi/byte-to-object-converter")
                licenses {
                    license {
                        name.set("Apache-2.0")
                        url.set("https://opensource.org/licenses/Apache-2.0")
                        distribution.set("repo")
                    }
                }
                developers {
                    developer {
                        id.set("libedi")
                        name.set("Sangjun, Park")
                        email.set("libedi@gmail.com")
                    }
                }
                scm {
                    connection.set("https://github.com/libedi/byte-to-object-converter.git")
                    developerConnection.set("https://github.com/libedi/byte-to-object-converter.git")
                    url.set("https://github.com/libedi/byte-to-object-converter")
                }
            }
        }
    }
}
```

---

### Component 5: GPG 서명 (Requirement 6)

```kotlin
signing {
    isRequired = project.hasProperty("signing.required")
    sign(publishing.publications["mavenJava"])
}
```

`isRequired`는 `signing.required` 프로퍼티의 "존재 여부"로 결정된다. 이 프로퍼티와 실제 키 정보(`signing.keyId`/`signing.password`/`signing.secretKeyRingFile`)는 **이 저장소 안 어떤 파일에도 두지 않는다** — 개발자 각자의 `~/.gradle/gradle.properties`(사용자 홈, git 추적 밖)에 설정하거나, CI에서는 `ORG_GRADLE_PROJECT_signing.required` 같은 환경 변수로 주입한다. Gradle은 프로젝트 루트의 `gradle.properties`와 사용자 홈의 `~/.gradle/gradle.properties`를 자동으로 병합해서 읽으므로, `build.gradle.kts` 코드는 값의 출처를 몰라도 된다. 프로젝트 루트 `gradle.properties` 자체가 금지되는 것은 아니다 — 비밀이 아닌 Gradle 설정(예: `org.gradle.parallel`)을 담는 용도라면 저장소에 커밋해도 된다. 다만 이 스펙은 그런 설정이 필요 없어 새로 만들지 않으며, 개발자가 로컬 편의상 여기에 서명 키 값 같은 자격증명을 실수로 적어 넣는 경우에 대비해 `.gitignore`에 `gradle.properties`를 추가해 안전장치를 하나 더 둔다.

GPG 키가 없는 로컬 환경에서는 `signing.required`가 어디에도 설정되어 있지 않으므로 `./gradlew build`는 서명을 시도하지 않고 성공한다(Requirement 6 AC 3). `sign(publishing.publications["mavenJava"])` 선언만으로 Gradle이 sign 태스크를 대응하는 publish 태스크보다 먼저 실행하도록 태스크 그래프를 자동 구성한다 — 이 의존 순서는 Requirement 7 AC 5에서도 재사용된다.

**왜 이렇게 했는가:** 값이 아니라 프로퍼티의 "존재 여부"만으로 필수 여부를 판단하고, 그 프로퍼티 자체를 저장소 밖에 두면 자격증명이 실수로 커밋될 경로 자체가 없어진다.

---

### Component 6: Central Portal 배포 플러그인 (Requirement 7)

**플러그인 선택**: `com.gradleup.nmcp` (카탈로그의 `libs.plugins.nmcp`, 버전 `1.4.4` — 실제 구현 시점에 최신 패치 재확인).

**왜 이렇게 했는가:** 2025-06-30 OSSRH sunset 이후 Sonatype 공식 Gradle 플러그인은 아직 없다("추후 지원 예정"으로만 공지됨). `com.gradleup.nmcp`는 Central Portal REST API를 직접 사용하고 SNAPSHOT 배포(Requirement 7 AC 3)를 기본 지원하는 커뮤니티 플러그인 중 활발히 유지보수되는 쪽이라 선택했다. `gradle-nexus/publish-plugin`은 OSSRH 방식 기반이라 sunset 이후 미선정.

```kotlin
nmcp {
    publishAllPublicationsToCentralPortal {
        username = providers.gradleProperty("centralPortalUsername").orElse("")
        password = providers.gradleProperty("centralPortalPassword").orElse("")
        publishingType = "USER_MANAGED"
    }
}
```

`centralPortalUsername`/`centralPortalPassword`도 Component 5와 동일한 원칙을 따른다 — 이 저장소에는 값도, 값을 담는 템플릿 파일도 두지 않는다. 개발자는 자신의 `~/.gradle/gradle.properties`에 아래처럼 적어 둔다(예시일 뿐, 이 파일은 저장소 밖에 있다):

```properties
centralPortalUsername=<Central Portal User Token의 username>
centralPortalPassword=<Central Portal User Token의 password>
```

`username`/`password`가 비어 있으면(`providers.gradleProperty(...).orElse("")`가 빈 문자열로 폴백) Central Portal이 인증 단계에서 거부하므로 Requirement 7 AC 4(자격증명 없으면 업로드 전에 실패)가 별도 코드 없이 만족된다.

`publishingType = "USER_MANAGED"`를 선택한 이유는 Constraints의 C-3(이 스펙은 실제 업로드·정식 배포까지 다루지 않음)과 맞물린다 — 업로드 후 Central Portal UI에서 수동으로 "Publish"를 눌러야 최종 릴리스되므로, Phase 3에서 실수로 되돌릴 수 없는 정식 배포가 발생할 위험이 없다.

---

## Data Models

### `gradle/libs.versions.toml`

Component 1에 전체 내용이 있다.

### `build.gradle.kts` 목표 상태

```kotlin
plugins {
    `java-library`
    `maven-publish`
    signing
    alias(libs.plugins.nmcp)
}

group = "io.github.libedi"
version = "2.0.0"

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(25)
    }
    withSourcesJar()
    withJavadocJar()
}

repositories {
    mavenCentral()
}

dependencies {
    implementation(libs.commons.lang3)

    testImplementation(platform(libs.junit.bom))
    testImplementation(libs.junit.jupiter)
    testImplementation(libs.mockito.core)
    testImplementation(libs.mockito.junit.jupiter)
    testImplementation(libs.assertj.core)
    testImplementation(libs.autoparams)
    testImplementation(libs.autoparams.lombok)

    testCompileOnly(libs.lombok)
    testAnnotationProcessor(libs.lombok)

    testRuntimeOnly(libs.junit.platform.launcher)
}

tasks.test {
    useJUnitPlatform()
}

tasks.withType<Javadoc> {
    options.encoding = "UTF-8"
    (options as StandardJavadocDocletOptions).apply {
        charSet = "UTF-8"
        docEncoding = "UTF-8"
        addBooleanOption("Xdoclint:none", true)
    }
}

publishing {
    publications {
        create<MavenPublication>("mavenJava") {
            from(components["java"])
            pom {
                name.set("byte-to-object-converter")
                description.set("Byte To Object Converter")
                url.set("https://github.com/libedi/byte-to-object-converter")
                licenses {
                    license {
                        name.set("Apache-2.0")
                        url.set("https://opensource.org/licenses/Apache-2.0")
                        distribution.set("repo")
                    }
                }
                developers {
                    developer {
                        id.set("libedi")
                        name.set("Sangjun, Park")
                        email.set("libedi@gmail.com")
                    }
                }
                scm {
                    connection.set("https://github.com/libedi/byte-to-object-converter.git")
                    developerConnection.set("https://github.com/libedi/byte-to-object-converter.git")
                    url.set("https://github.com/libedi/byte-to-object-converter")
                }
            }
        }
    }
}

signing {
    isRequired = project.hasProperty("signing.required")
    sign(publishing.publications["mavenJava"])
}

nmcp {
    publishAllPublicationsToCentralPortal {
        username = providers.gradleProperty("centralPortalUsername").orElse("")
        password = providers.gradleProperty("centralPortalPassword").orElse("")
        publishingType = "USER_MANAGED"
    }
}
```

이 스펙은 저장소에 `gradle.properties`를 추가하지 않는다 — 서명·Central Portal 자격증명 값은 전부 개발자 개인의 `~/.gradle/gradle.properties` 또는 CI 환경 변수에서 온다(Component 5, 6 참고). `gradle.properties` 자체가 금지된 것은 아니며, 비밀이 아닌 Gradle 설정이 필요해지면 그때 커밋된 `gradle.properties`를 추가할 수 있다.

---

## Error Handling

| 시나리오 | 대응 전략 |
| ---- | ---- |
| `commons-lang3` 3.20.0가 프로젝트가 쓰는 API를 깬다 (Requirement 1 AC 3) | 컴파일 성공하는 최신 버전으로 낮추고 사유를 `docs/backlog.md`에 기록 |
| junit/mockito/assertj/lombok 중 하나의 갱신이 실패한다 (Requirement 2 AC 6) | 실패 직전 최신 버전 사용, 사유 기록 |
| `FieldUtils.writeField()`가 `IllegalAccessException`을 던진다 (Requirement 3 AC 4) | 해당 호출만 표준 `java.lang.reflect`(`Field#setAccessible(true)` + `Field#set(...)`)로 개별 폴백, 사유 기록 |
| `autoparams` 11.x에서 컴파일 오류 발생 (Requirement 4 AC 4) | 호환되는 직전 major(예: `10.x`)로 다운그레이드, 사유 기록 |
| `{@link}`/`@see` 참조 실패로 javadoc 태스크가 실패할 뻔함 (Requirement 5 AC 4) | `Xdoclint:none` 적용 확인 |
| 로컬에 GPG 키 없음 (Requirement 6 AC 3) | `signing.required` 프로퍼티 미설정 상태 유지 → 서명 스킵, 빌드는 계속 진행 |
| Central Portal 자격증명 미설정 상태에서 배포 태스크 실행 (Requirement 7 AC 4) | `username`/`password`가 빈 문자열 → 업로드 시도 시 인증 실패로 중단, 부분 업로드 없음 |
| `./gradlew dependencies`에서 버전 충돌 발견 (Requirement 8 AC 4) | `dependencies { constraints { ... } }` 또는 `resolutionStrategy.force(...)`로 명시적 해결 |

---

## Correctness Properties

이 스펙의 Acceptance Criteria는 대부분 정적 의존성 선언 확인, 빌드 산출물(jar/서명 파일) 존재 확인, 외부 서비스(Central Portal) 연동이다. 입력을 다양화해도 결과가 달라지지 않는 종류(같은 `build.gradle.kts`를 100번 빌드해도 같은 파일이 나온다)이거나, 외부 서비스와의 계약을 테스트하는 것이라 property로 표현해도 얻는 게 없다. Component 2의 `FieldUtils` 치환도 이미 검증된 서드파티 라이브러리(Apache Commons Lang3)의 동작을 재확인하는 것뿐이라 별도 property가 필요하지 않다. 전부 example-based test(기존 8개 테스트 메서드 통과 여부)로 충분하다.

---

## Testing Strategy

### Step 1: Version Catalog 도입 + 의존성 갱신 + `spring-test` 제거 검증

```bash
./gradlew compileTestJava     # spring-test 제거 후 컴파일 오류 없음, libs.* 접근자 정상 해석
./gradlew clean test          # 기존 8개 테스트 전부 통과 (BUILD SUCCESSFUL, 종료 코드 0)
./gradlew dependencies        # 버전 충돌 없음, commons-lang3가 implementation으로 분류되는지 확인
```

### Step 2: Javadoc/소스 jar 파이프라인 검증

```bash
./gradlew build
ls build/libs/
# byte-to-object-converter-2.0.0.jar
# byte-to-object-converter-2.0.0-sources.jar
# byte-to-object-converter-2.0.0-javadoc.jar
```

javadoc 로그에 인코딩 오류·`{@link}` 실패로 인한 빌드 실패가 없는지 확인한다.

### Step 3: 서명 + 배포 파이프라인 로컬 검증 (C-3: 실제 업로드는 하지 않음)

```bash
# 로컬 ~/.gradle/gradle.properties에 signing.required=true와 GPG 키 정보가 있는 경우
./gradlew signMavenJavaPublication
ls build/libs/*.asc   # jar/sources/javadoc/pom 4개 .asc 파일

# 자격증명 없이 --dry-run으로 task 그래프만 확인 (실제 업로드 안 함)
./gradlew publishAllPublicationsToCentralPortal --dry-run
# sign 관련 태스크가 publish 태스크보다 먼저 실행 순서에 나오는지 확인
```

### Final: 전체 회귀 확인 (Requirement 8)

```bash
./gradlew clean test   # BUILD SUCCESSFUL, 종료 코드 0, 8개 테스트 전부 통과
./gradlew build        # BUILD SUCCESSFUL, 종료 코드 0
```
