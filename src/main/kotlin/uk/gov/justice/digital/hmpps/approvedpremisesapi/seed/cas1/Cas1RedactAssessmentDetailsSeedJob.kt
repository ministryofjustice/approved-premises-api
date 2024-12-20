package uk.gov.justice.digital.hmpps.approvedpremisesapi.seed.cas1

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import org.slf4j.LoggerFactory
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.AssessmentRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.seed.SeedJob
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.ApplicationTimelineNoteService
import java.util.UUID

@Component
class Cas1RemoveAssessmentDetailsSeedJob(
  private val assessmentRepository: AssessmentRepository,
  private val objectMapper: ObjectMapper,
  private val applicationTimelineNoteService: ApplicationTimelineNoteService,
) : SeedJob<Cas1RemoveAssessmentDetailsSeedCsvRow>(
  requiredHeaders = setOf(
    "assessment_id",
  ),
) {
  private val log = LoggerFactory.getLogger(this::class.java)

  override fun deserializeRow(columns: Map<String, String>) = Cas1RemoveAssessmentDetailsSeedCsvRow(
    assessmentId = columns["assessment_id"]!!.trim(),
  )

  override fun processRow(row: Cas1RemoveAssessmentDetailsSeedCsvRow) {
    val assessment = assessmentRepository.findByIdOrNull(UUID.fromString(row.assessmentId))
      ?: error("Assessment with identifier '${row.assessmentId}' does not exist")

    assessment.data = removeAllButSufficientInformation(assessment.data)
    assessment.document = removeAllButSufficientInformation(assessment.document)

    assessmentRepository.save(assessment)

    if (assessment.data != null || assessment.document != null) {
      applicationTimelineNoteService.saveApplicationTimelineNote(
        applicationId = assessment.application.id,
        note = "Assessment details redacted",
        user = null,
      )
    }

    log.info("Updated JSON for assessment ${assessment.id}")
  }

  fun removeAllButSufficientInformation(json: String?): String? {
    if (json == null) {
      return null
    }

    val dataModel: JsonNode = objectMapper.readTree(json)

    dataModel.removeAll {
      it.isObject && !it.has("sufficient-information")
    }

    return objectMapper.writeValueAsString(dataModel)
  }
}

data class Cas1RemoveAssessmentDetailsSeedCsvRow(
  val assessmentId: String,
)
