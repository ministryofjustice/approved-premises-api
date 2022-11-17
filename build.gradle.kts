plugins {
  id("uk.gov.justice.hmpps.gradle-spring-boot") version "4.5.5"
  kotlin("plugin.spring") version "1.6.21"

  id("org.openapi.generator") version "5.4.0"
  id("org.jetbrains.kotlin.plugin.jpa") version "1.7.21"
}

configurations {
  testImplementation { exclude(group = "org.junit.vintage") }
}

val springDocVersion = "1.6.12"
val sentryVersion = "6.7.1"

dependencies {
  implementation("org.springframework.boot:spring-boot-starter-webflux")
  implementation("org.springframework.boot:spring-boot-starter-data-jpa")
  implementation("com.vladmihalcea:hibernate-types-55:2.20.0")
  implementation("org.flywaydb:flyway-core")
  implementation("org.springframework.boot:spring-boot-starter-data-redis")
  implementation("org.springframework.boot:spring-boot-starter-cache")

  runtimeOnly("org.postgresql:postgresql:42.5.0")

  implementation("org.springdoc:springdoc-openapi-webmvc-core:$springDocVersion")
  implementation("org.springdoc:springdoc-openapi-ui:$springDocVersion")
  implementation("org.springdoc:springdoc-openapi-kotlin:$springDocVersion")
  implementation("org.springdoc:springdoc-openapi-data-rest:$springDocVersion")

  implementation("org.zalando:problem-spring-web-starter:0.27.0")

  implementation("org.springframework.boot:spring-boot-starter-security")
  implementation("org.springframework.boot:spring-boot-starter-oauth2-resource-server")
  implementation("org.springframework.boot:spring-boot-starter-oauth2-client")

  implementation("io.sentry:sentry-spring-boot-starter:$sentryVersion")
  implementation("io.sentry:sentry-logback:$sentryVersion")

  implementation(kotlin("reflect"))

  implementation("com.networknt:json-schema-validator:1.0.73")
  implementation("io.github.jamsesso:json-logic-java:1.0.7")

  testImplementation("io.github.bluegroundltd:kfactory:1.0.0")
  testImplementation("io.mockk:mockk:1.13.2")
  testImplementation("io.jsonwebtoken:jjwt-api:0.11.5")
  testImplementation("com.github.tomakehurst:wiremock-standalone:2.27.2")
  testRuntimeOnly("io.jsonwebtoken:jjwt-impl:0.11.5")
  testRuntimeOnly("io.jsonwebtoken:jjwt-jackson:0.11.5")

  implementation("com.fasterxml.jackson.core:jackson-databind") {
    version { strictly("2.14.0-rc2") }
  }

  testRuntimeOnly("com.fasterxml.jackson.core:jackson-databind") {
    version { strictly("2.14.0-rc2") }
  }

  implementation("com.fasterxml.jackson.core:jackson-annotations") {
    version { strictly("2.14.0-rc2") }
  }

  testRuntimeOnly("com.fasterxml.jackson.core:jackson-annotations") {
    version { strictly("2.14.0-rc2") }
  }

  implementation("com.fasterxml.jackson.core:jackson-core") {
    version { strictly("2.14.0-rc2") }
  }

  testRuntimeOnly("com.fasterxml.jackson.core:jackson-core") {
    version { strictly("2.14.0-rc2") }
  }

  implementation("org.apache.commons:commons-text") {
    version { strictly("1.10.0") }
  }

  testRuntimeOnly("org.apache.commons:commons-text") {
    version { strictly("1.10.0") }
  }
}

java {
  toolchain.languageVersion.set(JavaLanguageVersion.of(17))
}

tasks {
  withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
    kotlinOptions {
      jvmTarget = "17"
    }

    kotlin.sourceSets["main"].kotlin.srcDir("$buildDir/generated/src/main")
    dependsOn("openApiGenerate")
  }
}

tasks.register("bootRunLocal") {
  group = "application"
  description = "Runs this project as a Spring Boot application with the local profile"
  doFirst {
    tasks.bootRun.configure {
      systemProperty("spring.profiles.active", "local")
    }
  }
  finalizedBy("bootRun")
}

openApiGenerate {
  generatorName.set("kotlin-spring")
  inputSpec.set("$rootDir/src/main/resources/static/api.yml")
  outputDir.set("$buildDir/generated")
  apiPackage.set("uk.gov.justice.digital.hmpps.approvedpremisesapi.api")
  modelPackage.set("uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model")
  configOptions.apply {
    put("basePackage", "uk.gov.justice.digital.hmpps.approvedpremisesapi")
    put("delegatePattern", "true")
    put("gradleBuildFile", "false")
    put("exceptionHandler", "false")
    put("useBeanValidation", "false")
  }
}

ktlint {
  filter {
    exclude { it.file.path.contains("$buildDir${File.separator}generated${File.separator}") }
  }
}

allOpen {
  annotations("javax.persistence.Entity")
}
