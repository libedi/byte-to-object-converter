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
