package uk.gov.justice.digital.hmpps.approvedpremisesapi.seed.cas2

import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ExternalUserEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ExternalUserRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.seed.SeedJob
import java.util.UUID

@Component
class ExternalUsersSeedJob(
  private val repository: ExternalUserRepository,
) : SeedJob<ExternalUserSeedCsvRow>(
  requiredHeaders = setOf("username", "name", "email"),
) {
  private val log = LoggerFactory.getLogger(this::class.java)

  override fun deserializeRow(columns: Map<String, String>) = ExternalUserSeedCsvRow(
    username = columns["username"]!!.trim().uppercase(),
    name = columns["name"]!!.trim(),
    email = columns["email"]!!.trim(),
  )

  override fun processRow(row: ExternalUserSeedCsvRow) {
    log.info("Setting up ${row.username}")

    if (repository.findByUsername(row.username) !== null) {
      return log.info("Skipping ${row.username}: already seeded")
    }

    try {
      createExternalUser(row)
    } catch (exception: Exception) {
      throw RuntimeException("Could not create external user ${row.username}", exception)
    }
  }

  private fun createExternalUser(row: ExternalUserSeedCsvRow) {
    repository.save(
      ExternalUserEntity(
        id = UUID.randomUUID(),
        name = row.name,
        username = row.username,
        origin = "NACRO",
        email = row.email,
        isEnabled = true,
      ),
    )
  }
}

data class ExternalUserSeedCsvRow(
  val username: String,
  val name: String,
  val email: String,
)
