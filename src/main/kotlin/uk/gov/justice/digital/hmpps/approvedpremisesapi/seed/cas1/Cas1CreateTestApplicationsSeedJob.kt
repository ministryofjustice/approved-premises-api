package uk.gov.justice.digital.hmpps.approvedpremisesapi.seed.cas1

import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.approvedpremisesapi.seed.SeedColumns
import uk.gov.justice.digital.hmpps.approvedpremisesapi.seed.SeedJob

@Service
class Cas1CreateTestApplicationsSeedJob(
  val cas1ApplicationSeedService: Cas1ApplicationSeedService,
) : SeedJob<Cas1CreateTestApplicationsSeedCsvRow>(
  requiredHeaders = setOf(
    "creator_username",
    "crn",
    "count",
  ),
  runInTransaction = true,
) {

  private val log = LoggerFactory.getLogger(this::class.java)

  override fun deserializeRow(columns: Map<String, String>): Cas1CreateTestApplicationsSeedCsvRow {
    val seedColumns = SeedColumns(columns)

    return Cas1CreateTestApplicationsSeedCsvRow(
      creatorUsername = seedColumns.getStringOrNull("creator_username")!!,
      crn = seedColumns.getStringOrNull("crn")!!,
      count = seedColumns.getIntOrNull("count")!!,
    )
  }

  override fun processRow(row: Cas1CreateTestApplicationsSeedCsvRow) {
    val deliusUsername = row.creatorUsername
    val crn = row.crn
    val count = row.count

    log.info("Creating $count applications for CRN $crn using creator username $deliusUsername")

    repeat(row.count) {
      cas1ApplicationSeedService.createApplication(
        deliusUserName = deliusUsername,
        crn = crn,
        createIfExistingApplicationForCrn = true,
      )
    }
  }
}

data class Cas1CreateTestApplicationsSeedCsvRow(
  val creatorUsername: String,
  val crn: String,
  val count: Int,
)
