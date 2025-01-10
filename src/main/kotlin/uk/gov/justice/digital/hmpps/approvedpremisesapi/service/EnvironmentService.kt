package uk.gov.justice.digital.hmpps.approvedpremisesapi.service

import jakarta.annotation.PostConstruct
import org.slf4j.LoggerFactory
import org.springframework.core.env.Environment
import org.springframework.stereotype.Service

@Service
class EnvironmentService(
  val environment: Environment,
) {
  private val log = LoggerFactory.getLogger(this::class.java)

  @PostConstruct
  fun logProfiles() {
    log.info("Active profiles are ${environment.activeProfiles}")
  }

  fun isLocal() = profileActive("local")
  fun isDev() = profileActive("dev")
  fun isIntegrationTest() = profileActive("test")
  fun isProd() = profileActive("prod")
  fun isPreProd() = profileActive("preprod")

  fun isNotATestEnvironment() = isProd() || isPreProd() || (!isLocal() && !isDev() && !isIntegrationTest())

  fun profileActive(name: String) = environment.activeProfiles.any { it.equals(name, ignoreCase = true) }
}
