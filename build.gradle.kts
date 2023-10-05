import org.apache.commons.io.FileUtils

plugins {
  id("uk.gov.justice.hmpps.gradle-spring-boot") version "4.12.0"
  kotlin("plugin.spring") version "1.9.10"
  id("org.openapi.generator") version "5.4.0"
  id("org.jetbrains.kotlin.plugin.jpa") version "1.9.10"
  id("io.gatling.gradle") version "3.9.5.6"
}

configurations {
  testImplementation { exclude(group = "org.junit.vintage") }
}

val springDocVersion = "1.7.0"
val sentryVersion = "6.30.0"

dependencies {
  implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.3")
  implementation("org.springframework.boot:spring-boot-starter-webflux")
  implementation("org.springframework.boot:spring-boot-starter-data-jpa")
  implementation("com.vladmihalcea:hibernate-types-55:2.21.1")
  implementation("org.locationtech.jts:jts-core:1.19.0")
  implementation("org.hibernate:hibernate-spatial")
  implementation("org.flywaydb:flyway-core")
  implementation("org.springframework.boot:spring-boot-starter-data-redis")
  implementation("org.springframework.boot:spring-boot-starter-cache")
  implementation("com.github.ben-manes.caffeine:caffeine")

  runtimeOnly("org.postgresql:postgresql:42.6.0")

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

  implementation("com.networknt:json-schema-validator:1.0.87")

  implementation("com.github.doyaaaaaken:kotlin-csv-jvm:1.9.2")

  implementation("org.jetbrains.kotlinx:dataframe:0.11.1")

  implementation("io.arrow-kt:arrow-core:1.2.1")
  implementation("io.github.s-sathish:redlock-java:1.0.4")

  testImplementation("io.github.bluegroundltd:kfactory:1.0.0")
  testImplementation("io.mockk:mockk:1.13.8")
  testImplementation("io.jsonwebtoken:jjwt-api:0.11.5")
  testImplementation("com.github.tomakehurst:wiremock-standalone:3.0.1")
  testRuntimeOnly("io.jsonwebtoken:jjwt-impl:0.12.1")
  testRuntimeOnly("io.jsonwebtoken:jjwt-jackson:0.11.5")

  testImplementation("org.springframework.boot:spring-boot-starter-test") {
    exclude(module = "mockito-core")
  }

  testImplementation("com.ninja-squad:springmockk:4.0.2")

  implementation("uk.gov.justice.service.hmpps:hmpps-sqs-spring-boot-starter:1.3.1")

  implementation("uk.gov.service.notify:notifications-java-client:4.1.0-RELEASE")

  gatlingImplementation("org.springframework.boot:spring-boot-starter-webflux")
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

tasks.withType<Test> {
  jvmArgs("--add-opens", "java.base/java.lang.reflect=ALL-UNNAMED", "--add-opens", "java.base/java.time=ALL-UNNAMED")

  if (environment["GITHUB_ACTION"] != null) {
    maxParallelForks = Runtime.getRuntime().availableProcessors()
    println("Running on GitHub Actions - setting max test processes to number of processors: $maxParallelForks")
  } else {
    maxParallelForks = (Runtime.getRuntime().availableProcessors() / 2).takeIf { it > 0 } ?: 1
    println("Setting max test processes to recommended half of available: $maxParallelForks")
  }
}

tasks.register<Test>("integrationTest") {
  group = "verification"

  useJUnitPlatform {
    includeTags("integration")
  }
}

tasks.register<Test>("unitTest") {
  group = "verification"

  useJUnitPlatform {
    excludeTags("integration")
  }
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
    put("dateLibrary", "custom")
  }
  typeMappings.put("DateTime", "Instant")
  importMappings.put("Instant", "java.time.Instant")
}

tasks.register<org.openapitools.generator.gradle.plugin.tasks.GenerateTask>("openApiGenerateDomainEvents") {
  generatorName.set("kotlin-spring")
  inputSpec.set("$rootDir/src/main/resources/static/domain-events-api.yml")
  outputDir.set("$buildDir/generated")
  apiPackage.set("uk.gov.justice.digital.hmpps.approvedpremisesapi.api")
  modelPackage.set("uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.model")
  configOptions.apply {
    put("basePackage", "uk.gov.justice.digital.hmpps.approvedpremisesapi")
    put("delegatePattern", "true")
    put("gradleBuildFile", "false")
    put("exceptionHandler", "false")
    put("useBeanValidation", "false")
    put("dateLibrary", "custom")
  }
  typeMappings.put("DateTime", "Instant")
  importMappings.put("Instant", "java.time.Instant")
}

tasks.get("openApiGenerate").dependsOn("openApiGenerateDomainEvents")

tasks.get("openApiGenerate").doLast {
  // This is a workaround to allow us to have the `/documents/{crn}/{documentId}` endpoint specified in api.yml but not use
  // OpenAPI Generate for the scaffolding.  This is because we need to properly stream the files
  // (rather than loading whole file into memory first) which the OpenAPI generated controller does not support.

  File("$rootDir/build/generated/src/main/kotlin/uk/gov/justice/digital/hmpps/approvedpremisesapi/api/DocumentsApi.kt").delete()
  File("$rootDir/build/generated/src/main/kotlin/uk/gov/justice/digital/hmpps/approvedpremisesapi/api/DocumentsApiController.kt").delete()
  File("$rootDir/build/generated/src/main/kotlin/uk/gov/justice/digital/hmpps/approvedpremisesapi/api/DocumentsApiDelegate.kt").delete()

  // This is a workaround for an issue where we end up with duplicate keys in output JSON because we declare properties both in the discriminator
  // and as a regular property in the OpenAPI spec.  The Typescript generator does not support just the discriminator so there is no alternative.
  File("$rootDir/build/generated/src/main/kotlin/uk/gov/justice/digital/hmpps/approvedpremisesapi/api/model").walk().forEach {
    if (it.isFile && it.extension == "kt") {
      val replacedFileContents = FileUtils.readFileToString(it, "UTF-8")
        .replace("include = JsonTypeInfo.As.PROPERTY", "include = JsonTypeInfo.As.EXISTING_PROPERTY")
      FileUtils.writeStringToFile(it, replacedFileContents, "UTF-8")
    }
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

tasks {
  withType<JavaExec> {
    jvmArgs!!.plus("--add-opens")
    jvmArgs!!.plus("java.base/java.lang=ALL-UNNAMED")
  }
}

tasks.getByName("runKtlintCheckOverMainSourceSet").dependsOn("openApiGenerate", "openApiGenerateDomainEvents")

gatling {
  // WARNING: options below only work when logback config file isn't provided
  logLevel = "WARN" // logback root level
  logHttp = io.gatling.gradle.LogHttp.NONE // set to 'ALL' for all HTTP traffic in TRACE, 'FAILURES' for failed HTTP traffic in DEBUG
}
