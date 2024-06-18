package uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ApprovedPremisesApplication
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ApprovedPremisesAssessment
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ApprovedPremisesAssessmentStatus
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ApprovedPremisesAssessmentSummary
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ApprovedPremisesUser
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.AssessmentStatus
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.AssessmentSummary
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ServiceName
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.TemporaryAccommodationApplication
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.TemporaryAccommodationAssessment
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.TemporaryAccommodationAssessmentStatus
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.TemporaryAccommodationAssessmentSummary
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.TemporaryAccommodationUser
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApprovedPremisesAssessmentEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.AssessmentDecision
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.AssessmentEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.AssessmentReferralHistorySystemNoteEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.DomainAssessmentSummary
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.DomainAssessmentSummaryStatus
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ReferralHistorySystemNoteType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.TemporaryAccommodationApplicationEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.TemporaryAccommodationAssessmentEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.PersonInfoResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.PersonRisks
import java.time.LocalDate
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.AssessmentDecision as ApiAssessmentDecision
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.AssessmentDecision as JpaAssessmentDecision

@Component
class AssessmentTransformer(
  private val objectMapper: ObjectMapper,
  private val applicationsTransformer: ApplicationsTransformer,
  private val assessmentClarificationNoteTransformer: AssessmentClarificationNoteTransformer,
  private val assessmentReferralHistoryNoteTransformer: AssessmentReferralHistoryNoteTransformer,
  private val userTransformer: UserTransformer,
  private val personTransformer: PersonTransformer,
  private val risksTransformer: RisksTransformer,
) {
  fun transformJpaToApi(jpa: AssessmentEntity, personInfo: PersonInfoResult) = when (jpa) {
    is ApprovedPremisesAssessmentEntity -> ApprovedPremisesAssessment(
      id = jpa.id,
      application = applicationsTransformer.transformJpaToApi(
        jpa.application,
        personInfo,
      ) as ApprovedPremisesApplication,
      schemaVersion = jpa.schemaVersion.id,
      outdatedSchema = jpa.schemaUpToDate,
      createdAt = jpa.createdAt.toInstant(),
      allocatedAt = jpa.allocatedAt?.toInstant(),
      data = if (jpa.data != null) objectMapper.readTree(jpa.data) else null,
      clarificationNotes = jpa.clarificationNotes.map(assessmentClarificationNoteTransformer::transformJpaToApi),
      referralHistoryNotes = jpa.referralHistoryNotes.map(assessmentReferralHistoryNoteTransformer::transformJpaToApi),
      allocatedToStaffMember = jpa.allocatedToUser?.let {
        userTransformer.transformJpaToApi(it, ServiceName.approvedPremises) as ApprovedPremisesUser
      },
      submittedAt = jpa.submittedAt?.toInstant(),
      decision = transformJpaDecisionToApi(jpa.decision),
      rejectionRationale = jpa.rejectionRationale,
      status = getStatusForApprovedPremisesAssessment(jpa),
      service = "CAS1",
      createdFromAppeal = jpa.createdFromAppeal,
    )

    is TemporaryAccommodationAssessmentEntity -> {
      val lastReferralRejectedHistoryNote =
        jpa.referralHistoryNotes.filter { it is AssessmentReferralHistorySystemNoteEntity && it.type == ReferralHistorySystemNoteType.REJECTED }
          .maxByOrNull { it.createdAt }

      val application = applicationsTransformer.transformJpaToApi(
        jpa.application,
        personInfo,
      ) as TemporaryAccommodationApplication

      TemporaryAccommodationAssessment(
        id = jpa.id,
        application = application,
        schemaVersion = jpa.schemaVersion.id,
        outdatedSchema = jpa.schemaUpToDate,
        createdAt = jpa.createdAt.toInstant(),
        allocatedAt = jpa.allocatedAt?.toInstant(),
        data = if (jpa.data != null) objectMapper.readTree(jpa.data) else null,
        clarificationNotes = jpa.clarificationNotes.map(assessmentClarificationNoteTransformer::transformJpaToApi),
        referralHistoryNotes = jpa.referralHistoryNotes.map {
          if (it.id == lastReferralRejectedHistoryNote?.id) {
            assessmentReferralHistoryNoteTransformer.transformJpaToApi(it, jpa)
          } else {
            assessmentReferralHistoryNoteTransformer.transformJpaToApi(it)
          }
        },
        allocatedToStaffMember = jpa.allocatedToUser?.let {
          userTransformer.transformJpaToApi(
            it,
            ServiceName.temporaryAccommodation,
          ) as TemporaryAccommodationUser
        },
        submittedAt = jpa.submittedAt?.toInstant(),
        decision = transformJpaDecisionToApi(jpa.decision),
        rejectionRationale = jpa.rejectionRationale,
        status = getStatusForTemporaryAccommodationAssessment(jpa),
        summaryData = objectMapper.readTree(jpa.summaryData),
        service = "CAS3",
        releaseDate = jpa.releaseDate ?: LocalDate.from((jpa.application as TemporaryAccommodationApplicationEntity).arrivalDate),
        accommodationRequiredFromDate = jpa.accommodationRequiredFromDate ?: (jpa.application as TemporaryAccommodationApplicationEntity).personReleaseDate,
      )
    }

    else -> throw RuntimeException("Unsupported Application type when transforming Assessment: ${jpa.application::class.qualifiedName}")
  }

  fun transformDomainToApiSummary(ase: DomainAssessmentSummary, personInfo: PersonInfoResult): AssessmentSummary =
    when (ase.type) {
      "approved-premises" -> ApprovedPremisesAssessmentSummary(
        type = "CAS1",
        id = ase.id,
        applicationId = ase.applicationId,
        createdAt = ase.createdAt.toInstant(),
        arrivalDate = ase.arrivalDate?.toInstant(),
        status = getStatusForApprovedPremisesAssessment(ase),
        decision = transformDomainSummaryDecisionToApi(ase.decision),
        risks = ase.riskRatings?.let {
          risksTransformer.transformDomainToApi(
            objectMapper.readValue<PersonRisks>(it),
            ase.crn,
          )
        },
        person = personTransformer.transformModelToPersonApi(personInfo),
        dueAt = ase.dueAt!!.toInstant(),
      )

      "temporary-accommodation" -> TemporaryAccommodationAssessmentSummary(
        type = "CAS3",
        id = ase.id,
        applicationId = ase.applicationId,
        createdAt = ase.createdAt.toInstant(),
        arrivalDate = ase.arrivalDate?.toInstant(),
        status = getStatusForTemporaryAccommodationAssessment(ase),
        decision = transformDomainSummaryDecisionToApi(ase.decision),
        risks = ase.riskRatings?.let {
          risksTransformer.transformDomainToApi(
            objectMapper.readValue<PersonRisks>(it),
            ase.crn,
          )
        },
        person = personTransformer.transformModelToPersonApi(personInfo),
      )

      else -> throw RuntimeException("Unsupported type: ${ase.type}")
    }

  fun transformJpaDecisionToApi(decision: JpaAssessmentDecision?) = when (decision) {
    JpaAssessmentDecision.ACCEPTED -> ApiAssessmentDecision.accepted
    JpaAssessmentDecision.REJECTED -> ApiAssessmentDecision.rejected
    null -> null
  }

  fun transformApiStatusToDomainSummaryState(status: AssessmentStatus) = when (status) {
    AssessmentStatus.cas1Completed -> DomainAssessmentSummaryStatus.COMPLETED
    AssessmentStatus.cas1AwaitingResponse -> DomainAssessmentSummaryStatus.AWAITING_RESPONSE
    AssessmentStatus.cas1InProgress -> DomainAssessmentSummaryStatus.IN_PROGRESS
    AssessmentStatus.cas1NotStarted -> DomainAssessmentSummaryStatus.NOT_STARTED
    AssessmentStatus.cas1Reallocated -> DomainAssessmentSummaryStatus.REALLOCATED
    AssessmentStatus.cas3InReview -> DomainAssessmentSummaryStatus.IN_REVIEW
    AssessmentStatus.cas3Unallocated -> DomainAssessmentSummaryStatus.UNALLOCATED
    AssessmentStatus.cas3Rejected -> DomainAssessmentSummaryStatus.REJECTED
    AssessmentStatus.cas3Closed -> DomainAssessmentSummaryStatus.CLOSED
    AssessmentStatus.cas3ReadyToPlace -> DomainAssessmentSummaryStatus.READY_TO_PLACE
  }

  private fun transformDomainSummaryDecisionToApi(decision: String?) = when (decision) {
    "ACCEPTED" -> ApiAssessmentDecision.accepted
    "REJECTED" -> ApiAssessmentDecision.rejected
    else -> null
  }

  /**
   * Note that the logic to determine assessment status is duplicated in
   * [uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.AssessmentRepository.findAllApprovedPremisesAssessmentSummariesNotReallocated]
   * and as such changes should be synchronized
   */
  private fun getStatusForApprovedPremisesAssessment(entity: AssessmentEntity) = when {
    entity.decision !== null -> ApprovedPremisesAssessmentStatus.completed
    entity.clarificationNotes.any { it.response == null } -> ApprovedPremisesAssessmentStatus.awaitingResponse
    entity.reallocatedAt != null -> ApprovedPremisesAssessmentStatus.reallocated
    entity.data != null -> ApprovedPremisesAssessmentStatus.inProgress
    else -> ApprovedPremisesAssessmentStatus.notStarted
  }

  private fun getStatusForApprovedPremisesAssessment(ase: DomainAssessmentSummary): ApprovedPremisesAssessmentStatus {
    return when (ase.status) {
      DomainAssessmentSummaryStatus.COMPLETED -> ApprovedPremisesAssessmentStatus.completed
      DomainAssessmentSummaryStatus.AWAITING_RESPONSE -> ApprovedPremisesAssessmentStatus.awaitingResponse
      DomainAssessmentSummaryStatus.IN_PROGRESS -> ApprovedPremisesAssessmentStatus.inProgress
      DomainAssessmentSummaryStatus.REALLOCATED -> ApprovedPremisesAssessmentStatus.reallocated
      else -> ApprovedPremisesAssessmentStatus.notStarted
    }
  }

  private fun getStatusForTemporaryAccommodationAssessment(entity: AssessmentEntity) = when {
    entity.decision == AssessmentDecision.REJECTED -> TemporaryAccommodationAssessmentStatus.rejected
    entity.decision == AssessmentDecision.ACCEPTED && (entity as TemporaryAccommodationAssessmentEntity).completedAt != null ->
      TemporaryAccommodationAssessmentStatus.closed

    entity.decision == AssessmentDecision.ACCEPTED -> TemporaryAccommodationAssessmentStatus.readyToPlace
    entity.allocatedToUser != null -> TemporaryAccommodationAssessmentStatus.inReview
    else -> TemporaryAccommodationAssessmentStatus.unallocated
  }

  private fun getStatusForTemporaryAccommodationAssessment(ase: DomainAssessmentSummary) = when {
    ase.decision == "REJECTED" -> TemporaryAccommodationAssessmentStatus.rejected
    ase.decision == "ACCEPTED" && ase.completed -> TemporaryAccommodationAssessmentStatus.closed
    ase.decision == "ACCEPTED" -> TemporaryAccommodationAssessmentStatus.readyToPlace
    ase.allocated -> TemporaryAccommodationAssessmentStatus.inReview
    else -> TemporaryAccommodationAssessmentStatus.unallocated
  }
}
