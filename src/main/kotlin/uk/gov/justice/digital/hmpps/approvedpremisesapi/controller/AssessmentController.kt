package uk.gov.justice.digital.hmpps.approvedpremisesapi.controller

import com.fasterxml.jackson.databind.ObjectMapper
import jakarta.transaction.Transactional
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.AssessmentsApiDelegate
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Assessment
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.AssessmentAcceptance
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.AssessmentRejection
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.AssessmentSortField
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.AssessmentStatus
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.AssessmentSummary
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ClarificationNote
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.NewClarificationNote
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.NewReferralHistoryUserNote
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ReferralHistoryNote
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ServiceName
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.SortDirection
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.UpdateAssessment
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.UpdatedClarificationNote
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApprovedPremisesApplicationEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.DomainAssessmentSummary
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserQualification
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.PersonInfoResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.problem.BadRequestProblem
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.AssessmentService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.OffenderService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.UserService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas3.Cas3AssessmentService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas3.DomainEventService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.AssessmentClarificationNoteTransformer
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.AssessmentReferralHistoryNoteTransformer
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.AssessmentTransformer
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.PageCriteria
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.extractEntityFromCasResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.sortByName
import java.util.UUID

@Service
class AssessmentController(
  private val objectMapper: ObjectMapper,
  private val assessmentService: AssessmentService,
  private val userService: UserService,
  private val offenderService: OffenderService,
  private val assessmentTransformer: AssessmentTransformer,
  private val assessmentClarificationNoteTransformer: AssessmentClarificationNoteTransformer,
  private val assessmentReferralHistoryNoteTransformer: AssessmentReferralHistoryNoteTransformer,
  private val cas3AssessmentService: Cas3AssessmentService,
  private val domainEventService: DomainEventService,
) : AssessmentsApiDelegate {

  override fun assessmentsGet(
    xServiceName: ServiceName,
    sortDirection: SortDirection?,
    sortBy: AssessmentSortField?,
    statuses: List<AssessmentStatus>?,
    crnOrName: String?,
    page: Int?,
    perPage: Int?,
  ): ResponseEntity<List<AssessmentSummary>> {
    val user = userService.getUserForRequest()
    val resolvedSortDirection = sortDirection ?: SortDirection.asc
    val resolvedSortBy = sortBy ?: AssessmentSortField.assessmentArrivalDate
    val domainSummaryStatuses = statuses?.map { assessmentTransformer.transformApiStatusToDomainSummaryState(it) } ?: emptyList()

    val (summaries, metadata) = when (xServiceName) {
      ServiceName.cas2 -> throw UnsupportedOperationException("CAS2 not supported")
      ServiceName.temporaryAccommodation -> {
        val (summaries, metadata) = assessmentService.getAssessmentSummariesForUserCAS3(
          user,
          crnOrName,
          xServiceName,
          domainSummaryStatuses,
          PageCriteria(resolvedSortBy, resolvedSortDirection, page, perPage),
        )
        val transformSummaries = when (sortBy) {
          AssessmentSortField.assessmentDueAt -> throw BadRequestProblem(errorDetail = "Sorting by due date is not supported for CAS3")
          AssessmentSortField.personName -> transformDomainToApi(user, summaries, user.hasQualification(UserQualification.LAO)).sortByName(resolvedSortDirection)
          else -> transformDomainToApi(user, summaries)
        }
        Pair(transformSummaries, metadata)
      }
      else -> {
        val (summaries, metadata) = assessmentService.getVisibleAssessmentSummariesForUserCAS1(user, domainSummaryStatuses, PageCriteria(resolvedSortBy, resolvedSortDirection, page, perPage))
        Pair(transformDomainToApi(user, summaries, user.hasQualification(UserQualification.LAO)), metadata)
      }
    }

    return ResponseEntity.ok()
      .headers(metadata?.toHeaders())
      .body(summaries)
  }

  private fun transformDomainToApi(user: UserEntity, summaries: List<DomainAssessmentSummary>, ignoreLaoRestrictions: Boolean = false): List<AssessmentSummary> {
    val crns = summaries.map { it.crn }
    val personInfoResults = offenderService.getPersonInfoResults(crns.toSet(), user.deliusUsername, ignoreLaoRestrictions)

    return summaries.map {
      val crn = it.crn
      assessmentTransformer.transformDomainToApiSummary(
        it,
        personInfoResults.firstOrNull { it.crn == crn } ?: PersonInfoResult.Unknown(crn),
      )
    }
  }

  override fun assessmentsAssessmentIdGet(assessmentId: UUID): ResponseEntity<Assessment> {
    val user = userService.getUserForRequest()

    val assessment = extractEntityFromCasResult(assessmentService.getAssessmentAndValidate(user, assessmentId))

    val ignoreLaoRestrictions = (assessment.application is ApprovedPremisesApplicationEntity) && user.hasQualification(UserQualification.LAO)

    val personInfo = offenderService.getPersonInfoResult(assessment.application.crn, user.deliusUsername, ignoreLaoRestrictions)

    val assessmentUpdatedDomainEvents = domainEventService.getAssessmentUpdatedEvents(assessmentId = assessment.id)

    val transformedResponse = assessmentTransformer.transformJpaToApi(assessment, personInfo, assessmentUpdatedDomainEvents)

    return ResponseEntity.ok(transformedResponse)
  }

  @Transactional
  override fun assessmentsAssessmentIdPut(
    assessmentId: UUID,
    updateAssessment: UpdateAssessment,
    xServiceName: ServiceName?,
  ): ResponseEntity<Assessment> {
    val user = userService.getUserForRequest()
    val assessment =
      when (xServiceName) {
        ServiceName.temporaryAccommodation -> {
          extractEntityFromCasResult(cas3AssessmentService.updateAssessment(user, assessmentId, updateAssessment))
        }

        else -> {
          extractEntityFromCasResult(
            assessmentService
              .updateAssessment(
                user,
                assessmentId,
                objectMapper.writeValueAsString(updateAssessment.data),
              ),
          )
        }
      }

    val ignoreLao =
      (assessment.application is ApprovedPremisesApplicationEntity) && user.hasQualification(UserQualification.LAO)

    val personInfo = offenderService.getPersonInfoResult(assessment.application.crn, user.deliusUsername, ignoreLao)

    return ResponseEntity.ok(
      assessmentTransformer.transformJpaToApi(assessment, personInfo),
    )
  }

  @Transactional
  override fun assessmentsAssessmentIdAcceptancePost(assessmentId: UUID, assessmentAcceptance: AssessmentAcceptance): ResponseEntity<Unit> {
    val user = userService.getUserForRequest()

    val serializedData = objectMapper.writeValueAsString(assessmentAcceptance.document)

    val assessmentAuthResult = assessmentService.acceptAssessment(
      user = user,
      assessmentId = assessmentId,
      document = serializedData,
      placementRequirements = assessmentAcceptance.requirements,
      placementDates = assessmentAcceptance.placementDates,
      apType = assessmentAcceptance.apType,
      notes = assessmentAcceptance.notes,
    )

    extractEntityFromCasResult(assessmentAuthResult)

    return ResponseEntity(HttpStatus.OK)
  }

  @Transactional
  override fun assessmentsAssessmentIdRejectionPost(assessmentId: UUID, assessmentRejection: AssessmentRejection): ResponseEntity<Unit> {
    val user = userService.getUserForRequest()

    val serializedData = objectMapper.writeValueAsString(assessmentRejection.document)

    val assessmentAuthResult =
      assessmentService.rejectAssessment(
        user,
        assessmentId,
        serializedData,
        assessmentRejection.rejectionRationale,
        assessmentRejection.referralRejectionReasonId,
        assessmentRejection.referralRejectionReasonDetail,
        assessmentRejection.isWithdrawn,
      )

    extractEntityFromCasResult(assessmentAuthResult)

    return ResponseEntity(HttpStatus.OK)
  }

  override fun assessmentsAssessmentIdClosurePost(assessmentId: UUID): ResponseEntity<Unit> {
    val user = userService.getUserForRequest()
    val assessmentAuthResult = assessmentService.closeAssessment(user, assessmentId)
    extractEntityFromCasResult(assessmentAuthResult)
    return ResponseEntity(HttpStatus.OK)
  }

  override fun assessmentsAssessmentIdNotesPost(
    assessmentId: UUID,
    newClarificationNote: NewClarificationNote,
  ): ResponseEntity<ClarificationNote> {
    val user = userService.getUserForRequest()

    val clarificationNoteResult = assessmentService.addAssessmentClarificationNote(user, assessmentId, newClarificationNote.query)

    return ResponseEntity.ok(
      assessmentClarificationNoteTransformer.transformJpaToApi(extractEntityFromCasResult(clarificationNoteResult)),
    )
  }

  override fun assessmentsAssessmentIdNotesNoteIdPut(
    assessmentId: UUID,
    noteId: UUID,
    updatedClarificationNote: UpdatedClarificationNote,
  ): ResponseEntity<ClarificationNote> {
    val user = userService.getUserForRequest()
    val clarificationNoteResult = assessmentService.updateAssessmentClarificationNote(
      user,
      assessmentId,
      noteId,
      updatedClarificationNote.response,
      updatedClarificationNote.responseReceivedOn,
    )

    return ResponseEntity.ok(
      assessmentClarificationNoteTransformer.transformJpaToApi(extractEntityFromCasResult(clarificationNoteResult)),
    )
  }

  override fun assessmentsAssessmentIdReferralHistoryNotesPost(
    assessmentId: UUID,
    newReferralHistoryUserNote: NewReferralHistoryUserNote,
  ): ResponseEntity<ReferralHistoryNote> {
    val user = userService.getUserForRequest()

    val referralHistoryUserNoteResult =
      assessmentService.addAssessmentReferralHistoryUserNote(user, assessmentId, newReferralHistoryUserNote.message)

    return ResponseEntity.ok(
      assessmentReferralHistoryNoteTransformer.transformJpaToApi(
        extractEntityFromCasResult(
          referralHistoryUserNoteResult,
        ),
      ),
    )
  }
}
