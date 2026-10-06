plugins {
    kotlin("jvm")
    `java-library`
    `maven-publish`
    id("io.spring.dependency-management")
    id("org.jlleitschuh.gradle.ktlint")
}

description = "sigil-client"

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(25)
    }
    withSourcesJar()
}

dependencyManagement {
    imports {
        mavenBom("org.springframework.boot:spring-boot-dependencies:4.1.1")
    }
}

dependencies {
    // Exposed API: IndieAuthError.Code references HttpStatus, and the client
    // facade is Spring-based (RestClient).
    api("org.springframework:spring-web")

    implementation("org.jetbrains.kotlin:kotlin-reflect")
    implementation("tools.jackson.core:jackson-databind")
    implementation("tools.jackson.module:jackson-module-kotlin")
    implementation("com.fasterxml.jackson.core:jackson-annotations")
    implementation("com.github.ben-manes.caffeine:caffeine")
    implementation("io.github.oshai:kotlin-logging-jvm:7.0.3")

    // Optional Spring Boot integration (auto-configuration); consumers that are
    // not Spring Boot apps get the plain client classes.
    compileOnly("org.springframework.boot:spring-boot-autoconfigure")

    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.mockito.kotlin:mockito-kotlin:6.2.0")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

kotlin {
    compilerOptions {
        freeCompilerArgs.addAll("-Xjsr305=strict", "-Xannotation-default-target=param-property")
    }
}

tasks.withType<Test> {
    useJUnitPlatform()
}

ktlint {
    version.set("1.8.0")
}

publishing {
    publications {
        create<MavenPublication>("maven") {
            from(components["java"])
            artifactId = "sigil-client"
        }
    }
    repositories {
        maven {
            name = "GitHubPackages"
            url = uri("https://maven.pkg.github.com/marchland/sigil")
            credentials {
                username = System.getenv("GITHUB_ACTOR") ?: (project.findProperty("gpr.user") as String?)
                password = System.getenv("GITHUB_TOKEN") ?: (project.findProperty("gpr.token") as String?)
            }
        }
    }
}
