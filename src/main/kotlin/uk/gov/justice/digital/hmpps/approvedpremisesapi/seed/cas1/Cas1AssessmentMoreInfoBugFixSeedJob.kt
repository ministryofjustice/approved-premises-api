package uk.gov.justice.digital.hmpps.approvedpremisesapi.seed.cas1

import org.slf4j.LoggerFactory
import org.springframework.data.repository.findByIdOrNull
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.AssessmentRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.seed.SeedJob
import java.util.UUID

class Cas1FurtherInfoBugFixSeedJob(
  fileName: String,
  private val assessmentRepository: AssessmentRepository,
) : SeedJob<Cas1FurtherInfoBugFixSeedCsvRow>(
  id = UUID.randomUUID(),
  fileName = fileName,
  requiredHeaders = setOf(
    "assessment_id",
  ),
) {
  private val log = LoggerFactory.getLogger(this::class.java)

  override fun deserializeRow(columns: Map<String, String>) = Cas1FurtherInfoBugFixSeedCsvRow(
    assessmentId = columns["assessment_id"]!!.trim(),
  )

  override fun processRow(row: Cas1FurtherInfoBugFixSeedCsvRow) {
    val assessment = assessmentRepository.findByIdOrNull(UUID.fromString(row.assessmentId))
      ?: error("Assessment with identifier '${row.assessmentId}' does not exist")

    val updatedJson = assessment.data?.replace(
      Regex("sufficient-information-confirm\"\\s*:\\s*\\{\\s*\"confirm\"\\s*:\\s*\"no\"\\s*}"),
      "sufficient-information-confirm\":{\"confirm\":\"yes\"}",
    )

    if (updatedJson != null) {
      assessmentRepository.updateData(assessment.id, updatedJson)
    }

    log.info("Updated JSON for assessment ${assessment.id}")
  }
}

data class Cas1FurtherInfoBugFixSeedCsvRow(
  val assessmentId: String,
)
