package uk.gov.justice.digital.hmpps.approvedpremisesapi.seed.cas2

import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.NomisUserEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.NomisUserRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.seed.SeedJob
import java.util.UUID
import kotlin.random.Random

@Component
class NomisUsersSeedJob(
  private val repository: NomisUserRepository,
) : SeedJob<NomisUserSeedCsvRow>(
  requiredHeaders = setOf("nomisUsername", "name", "email"),
) {
  private val log = LoggerFactory.getLogger(this::class.java)

  override fun deserializeRow(columns: Map<String, String>) = NomisUserSeedCsvRow(
    nomisUsername = columns["nomisUsername"]!!.trim().uppercase(),
    name = columns["name"]!!.trim(),
    email = columns["email"]!!.trim(),
  )

  override fun processRow(row: NomisUserSeedCsvRow) {
    log.info("Setting up ${row.nomisUsername}")

    if (repository.findByNomisUsername(row.nomisUsername) !== null) {
      return log.info("Skipping ${row.nomisUsername}: already seeded")
    }

    try {
      createNomisUser(row)
    } catch (exception: Exception) {
      throw RuntimeException("Could not create user ${row.nomisUsername}", exception)
    }
  }

  private fun createNomisUser(row: NomisUserSeedCsvRow) {
    repository.save(
      NomisUserEntity(
        id = UUID.randomUUID(),
        name = row.name,
        nomisUsername = row.nomisUsername,
        nomisStaffId = randomId(),
        accountType = "GENERAL",
        email = row.email,
        isEnabled = true,
        isActive = true,
        applications = mutableListOf(),
      ),
    )
  }

  private fun randomId(): Long {
    return Random.nextInt(100000, 900000).toLong()
  }
}

data class NomisUserSeedCsvRow(
  val nomisUsername: String,
  val name: String,
  val email: String,
)
