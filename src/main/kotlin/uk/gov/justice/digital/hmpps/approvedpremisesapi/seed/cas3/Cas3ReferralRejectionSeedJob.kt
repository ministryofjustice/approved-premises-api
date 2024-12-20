package uk.gov.justice.digital.hmpps.approvedpremisesapi.seed.cas3

import org.slf4j.LoggerFactory
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ServiceName
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.AssessmentDecision
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.AssessmentEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.AssessmentReferralHistoryNoteRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.AssessmentReferralHistorySystemNoteEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.AssessmentRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ReferralHistorySystemNoteType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ReferralRejectionReasonRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.TemporaryAccommodationAssessmentEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.seed.SeedJob
import java.time.OffsetDateTime
import java.util.UUID

@Component
class Cas3ReferralRejectionSeedJob(
  private val assessmentRepository: AssessmentRepository,
  private val referralRejectionReasonRepository: ReferralRejectionReasonRepository,
  private val assessmentReferralHistoryNoteRepository: AssessmentReferralHistoryNoteRepository,
  private val userRepository: UserRepository,
) : SeedJob<Cas3ReferralRejectionSeedCsvRow>(
  requiredHeaders = setOf(
    "assessment_id",
    "rejection_reason",
    "rejection_reason_detail",
    "is_withdrawn",
    "delius_username",
  ),
  runInTransaction = false,
) {
  private val log = LoggerFactory.getLogger(this::class.java)

  override fun deserializeRow(columns: Map<String, String>) = Cas3ReferralRejectionSeedCsvRow(
    assessmentId = UUID.fromString(columns["assessment_id"]!!.trim()),
    rejectionReason = columns["rejection_reason"]!!.trim(),
    rejectionReasonDetail = columns["rejection_reason_detail"]!!.trim(),
    isWithdrawn = columns["is_withdrawn"]!!.trim().equals("true", ignoreCase = true),
    deliusUsername = columns["delius_username"]!!.trim(),
  )

  override fun processRow(row: Cas3ReferralRejectionSeedCsvRow) {
    rejectAssessment(row)
  }

  @SuppressWarnings("TooGenericExceptionCaught")
  private fun rejectAssessment(row: Cas3ReferralRejectionSeedCsvRow) {
    val userRequest = userRepository.findByDeliusUsername(row.deliusUsername.uppercase()) ?: error("User with delius username ${row.deliusUsername} not found")

    val assessment =
      assessmentRepository.findByIdOrNull(row.assessmentId) ?: error("Assessment with id ${row.assessmentId} not found")

    if (assessment.reallocatedAt != null) {
      error("The application has been reallocated, this assessment is read only")
    }

    if (assessment is TemporaryAccommodationAssessmentEntity) {
      val rejectionReason = referralRejectionReasonRepository.findByNameAndActive(
        row.rejectionReason,
        ServiceName.temporaryAccommodation.value,
      ) ?: error("Rejection reason ${row.rejectionReason} not found")

      try {
        assessment.submittedAt = OffsetDateTime.now()
        assessment.decision = AssessmentDecision.REJECTED
        assessment.completedAt = null
        assessment.referralRejectionReason = rejectionReason
        assessment.referralRejectionReasonDetail = row.rejectionReasonDetail
        assessment.isWithdrawn = row.isWithdrawn

        val savedAssessment = assessmentRepository.save(assessment)
        savedAssessment.addSystemNote(userRequest, ReferralHistorySystemNoteType.REJECTED)
      } catch (e: Throwable) {
        log.error("Failed to update assessment with id ${row.assessmentId}", e)
        error("Failed to update assessment with id ${row.assessmentId}")
      }

      log.info("Assessment with id ${row.assessmentId} has been successfully rejected")
    } else {
      error("Assessment with id ${row.assessmentId} is not a temporary accommodation assessment")
    }
  }

  fun AssessmentEntity.addSystemNote(user: UserEntity, type: ReferralHistorySystemNoteType) {
    this.referralHistoryNotes += assessmentReferralHistoryNoteRepository.save(
      AssessmentReferralHistorySystemNoteEntity(
        id = UUID.randomUUID(),
        assessment = this,
        createdAt = OffsetDateTime.now(),
        message = "",
        createdByUser = user,
        type = type,
      ),
    )
  }
}

data class Cas3ReferralRejectionSeedCsvRow(
  val assessmentId: UUID,
  val rejectionReason: String,
  val rejectionReasonDetail: String?,
  val isWithdrawn: Boolean,
  val deliusUsername: String,
)
