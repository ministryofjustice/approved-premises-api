package uk.gov.justice.digital.hmpps.approvedpremisesapi.seed.cas1

import org.slf4j.LoggerFactory
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.Cas1CruManagementAreaRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.seed.SeedJob
import java.util.UUID

@Component
class Cas1CruManagementAreaSeedJob(
  private val cas1CruManagementAreaRepository: Cas1CruManagementAreaRepository,
) : SeedJob<CruManagementAreaSeedCsvRow>(
  requiredHeaders = setOf(
    "id",
    "current_name",
    "email_address",
    "assessment_auto_allocation_username",
  ),
) {
  private val log = LoggerFactory.getLogger(this::class.java)

  override fun deserializeRow(columns: Map<String, String>) = CruManagementAreaSeedCsvRow(
    id = UUID.fromString(columns["id"]!!.trim()),
    currentName = columns["current_name"]!!.trim(),
    emailAddress = columns["email_address"]!!.trim(),
    assessmentAutoAllocationUsername = columns["assessment_auto_allocation_username"]!!.trim(),
  )

  override fun processRow(row: CruManagementAreaSeedCsvRow) {
    val id = row.id
    val managementArea = cas1CruManagementAreaRepository.findByIdOrNull(id)
      ?: error("CRU Management Area with id '$id' does not exist")

    val currentName = row.currentName
    if (managementArea.name != currentName) {
      error("Not updating entry for '$id' as current name '$currentName' in seed file doesn't match actual name '${managementArea.name}'")
    }

    val emailAddress = row.emailAddress.ifBlank { null }
    val autoAlloc = row.assessmentAutoAllocationUsername.ifBlank { null }

    val before = managementArea.copy()

    managementArea.emailAddress = emailAddress
    managementArea.assessmentAutoAllocationUsername = autoAlloc
    cas1CruManagementAreaRepository.save(managementArea)

    log.info("Management are was $before, is now $managementArea")
  }
}

data class CruManagementAreaSeedCsvRow(
  val id: UUID,
  val currentName: String,
  val emailAddress: String,
  val assessmentAutoAllocationUsername: String,
)
