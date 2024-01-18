package uk.gov.justice.digital.hmpps.approvedpremisesapi.controller

import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.beans.factory.annotation.Value
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
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.DomainAssessmentSummary
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.PaginationMetadata
import uk.gov.justice.digital.hmpps.approvedpremisesapi.problem.BadRequestProblem
import uk.gov.justice.digital.hmpps.approvedpremisesapi.problem.ConflictProblem
import uk.gov.justice.digital.hmpps.approvedpremisesapi.problem.ForbiddenProblem
import uk.gov.justice.digital.hmpps.approvedpremisesapi.problem.InternalServerErrorProblem
import uk.gov.justice.digital.hmpps.approvedpremisesapi.problem.NotFoundProblem
import uk.gov.justice.digital.hmpps.approvedpremisesapi.results.AuthorisableActionResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.results.ValidatableActionResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.AssessmentService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.OffenderService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.UserService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.AssessmentClarificationNoteTransformer
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.AssessmentReferralHistoryNoteTransformer
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.AssessmentTransformer
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.PageCriteria
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.filterByStatuses
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.sort
import java.util.UUID
import javax.transaction.Transactional

@Service
class AssessmentController(
  private val objectMapper: ObjectMapper,
  private val assessmentService: AssessmentService,
  private val userService: UserService,
  private val offenderService: OffenderService,
  private val assessmentTransformer: AssessmentTransformer,
  private val assessmentClarificationNoteTransformer: AssessmentClarificationNoteTransformer,
  private val assessmentReferralHistoryNoteTransformer: AssessmentReferralHistoryNoteTransformer,
  @Value("\${pagination.default-page-size}") private val defaultPageSize: Int,
) : AssessmentsApiDelegate {

  override fun assessmentsGet(
    xServiceName: ServiceName,
    sortDirection: SortDirection?,
    sortBy: AssessmentSortField?,
    statuses: List<AssessmentStatus>?,
    crn: String?,
    page: Int?,
    perPage: Int?,
  ): ResponseEntity<List<AssessmentSummary>> {
    val user = userService.getUserForRequest()
    val resolvedSortDirection = sortDirection ?: SortDirection.asc
    val resolvedSortBy = sortBy ?: AssessmentSortField.assessmentArrivalDate

    val (summaries, metadata) = when (xServiceName) {
      ServiceName.cas2 -> throw UnsupportedOperationException("CAS2 not supported")
      ServiceName.temporaryAccommodation -> {
        val summaries = if (crn != null) {
          assessmentService.getAssessmentSummariesByCrnForUserCAS3(user, crn, xServiceName)
        } else {
          assessmentService.getVisibleAssessmentSummariesForUserCAS3(user)
        }

        var filteredSummaries = transformDomainToApi(user, summaries)
          .sort(resolvedSortDirection, resolvedSortBy)
          .filterByStatuses(statuses)

        var metadata: PaginationMetadata? = null

        if (page !== null && filteredSummaries.isNotEmpty()) {
          val pageSize = perPage ?: defaultPageSize
          val chunkedSummaries = filteredSummaries.chunked(pageSize)
          val pageIndex = page - 1

          metadata = PaginationMetadata(page, chunkedSummaries.size, filteredSummaries.size.toLong(), pageSize)
          filteredSummaries = chunkedSummaries[pageIndex]
        }

        Pair(filteredSummaries, metadata)
      }
      else -> {
        val domainSummaryStatuses = statuses?.map { assessmentTransformer.transformApiStatusToDomainSummaryState(it) } ?: emptyList()
        val (summaries, metadata) = assessmentService.getVisibleAssessmentSummariesForUserCAS1(user, domainSummaryStatuses, PageCriteria(resolvedSortBy, resolvedSortDirection, page, perPage))
        Pair(transformDomainToApi(user, summaries), metadata)
      }
    }

    return ResponseEntity.ok()
      .headers(metadata?.toHeaders())
      .body(summaries)
  }

  private fun transformDomainToApi(user: UserEntity, summaries: List<DomainAssessmentSummary>) = summaries.map {
    val personInfo = offenderService.getInfoForPerson(it.crn, user.deliusUsername, false)

    assessmentTransformer.transformDomainToApiSummary(
      it,
      personInfo,
    )
  }

  override fun assessmentsAssessmentIdGet(assessmentId: UUID): ResponseEntity<Assessment> {
    val user = userService.getUserForRequest()

    val assessmentResult = assessmentService.getAssessmentForUser(user, assessmentId)
    val assessment = when (assessmentResult) {
      is AuthorisableActionResult.Success -> assessmentResult.entity
      is AuthorisableActionResult.NotFound -> throw NotFoundProblem(assessmentId, "Assessment")
      is AuthorisableActionResult.Unauthorised -> throw ForbiddenProblem()
    }

    val personInfo = offenderService.getInfoForPerson(assessment.application.crn, user.deliusUsername, false)

    return ResponseEntity.ok(
      assessmentTransformer.transformJpaToApi(assessment, personInfo),
    )
  }

  @Transactional
  override fun assessmentsAssessmentIdPut(assessmentId: UUID, updateAssessment: UpdateAssessment): ResponseEntity<Assessment> {
    val user = userService.getUserForRequest()

    val serializedData = objectMapper.writeValueAsString(updateAssessment.data)

    val assessmentAuthResult = assessmentService.updateAssessment(user, assessmentId, serializedData)
    val assessmentValidationResult = when (assessmentAuthResult) {
      is AuthorisableActionResult.Success -> assessmentAuthResult.entity
      is AuthorisableActionResult.NotFound -> throw NotFoundProblem(assessmentId, "Assessment")
      is AuthorisableActionResult.Unauthorised -> throw ForbiddenProblem()
    }

    val assessment = when (assessmentValidationResult) {
      is ValidatableActionResult.GeneralValidationError -> throw BadRequestProblem(errorDetail = assessmentValidationResult.message)
      is ValidatableActionResult.FieldValidationError -> throw BadRequestProblem(invalidParams = assessmentValidationResult.validationMessages)
      is ValidatableActionResult.ConflictError -> throw ConflictProblem(id = assessmentValidationResult.conflictingEntityId, conflictReason = assessmentValidationResult.message)
      is ValidatableActionResult.Success -> assessmentValidationResult.entity
    }

    val personInfo = offenderService.getInfoForPerson(assessment.application.crn, user.deliusUsername, false)

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
      notes = assessmentAcceptance.notes,
    )

    val assessmentValidationResult = when (assessmentAuthResult) {
      is AuthorisableActionResult.Success -> assessmentAuthResult.entity
      is AuthorisableActionResult.NotFound -> throw NotFoundProblem(assessmentId, "Assessment")
      is AuthorisableActionResult.Unauthorised -> throw ForbiddenProblem()
    }

    when (assessmentValidationResult) {
      is ValidatableActionResult.GeneralValidationError -> throw BadRequestProblem(errorDetail = assessmentValidationResult.message)
      is ValidatableActionResult.FieldValidationError -> throw BadRequestProblem(invalidParams = assessmentValidationResult.validationMessages)
      is ValidatableActionResult.ConflictError -> throw ConflictProblem(id = assessmentValidationResult.conflictingEntityId, conflictReason = assessmentValidationResult.message)
      is ValidatableActionResult.Success -> assessmentValidationResult.entity
    }

    return ResponseEntity(HttpStatus.OK)
  }

  @Transactional
  override fun assessmentsAssessmentIdRejectionPost(assessmentId: UUID, assessmentRejection: AssessmentRejection): ResponseEntity<Unit> {
    val user = userService.getUserForRequest()

    val serializedData = objectMapper.writeValueAsString(assessmentRejection.document)

    val assessmentAuthResult = assessmentService.rejectAssessment(user, assessmentId, serializedData, assessmentRejection.rejectionRationale)

    val assessmentValidationResult = when (assessmentAuthResult) {
      is AuthorisableActionResult.Success -> assessmentAuthResult.entity
      is AuthorisableActionResult.NotFound -> throw NotFoundProblem(assessmentId, "Assessment")
      is AuthorisableActionResult.Unauthorised -> throw ForbiddenProblem()
    }

    when (assessmentValidationResult) {
      is ValidatableActionResult.GeneralValidationError -> throw BadRequestProblem(errorDetail = assessmentValidationResult.message)
      is ValidatableActionResult.FieldValidationError -> throw BadRequestProblem(invalidParams = assessmentValidationResult.validationMessages)
      is ValidatableActionResult.ConflictError -> throw ConflictProblem(id = assessmentValidationResult.conflictingEntityId, conflictReason = assessmentValidationResult.message)
      is ValidatableActionResult.Success -> Unit
    }

    return ResponseEntity(HttpStatus.OK)
  }

  override fun assessmentsAssessmentIdClosurePost(assessmentId: UUID): ResponseEntity<Unit> {
    val user = userService.getUserForRequest()

    val assessmentValidationResult = when (val assessmentAuthResult = assessmentService.closeAssessment(user, assessmentId)) {
      is AuthorisableActionResult.Success -> assessmentAuthResult.entity
      is AuthorisableActionResult.NotFound -> throw NotFoundProblem(assessmentId, "Assessment")
      is AuthorisableActionResult.Unauthorised -> throw ForbiddenProblem()
    }

    when (assessmentValidationResult) {
      is ValidatableActionResult.GeneralValidationError -> throw BadRequestProblem(errorDetail = assessmentValidationResult.message)
      is ValidatableActionResult.FieldValidationError -> throw BadRequestProblem(invalidParams = assessmentValidationResult.validationMessages)
      is ValidatableActionResult.ConflictError -> throw ConflictProblem(id = assessmentValidationResult.conflictingEntityId, conflictReason = assessmentValidationResult.message)
      is ValidatableActionResult.Success -> Unit
    }

    return ResponseEntity(HttpStatus.OK)
  }

  override fun assessmentsAssessmentIdNotesPost(
    assessmentId: UUID,
    newClarificationNote: NewClarificationNote,
  ): ResponseEntity<ClarificationNote> {
    val user = userService.getUserForRequest()

    val clarificationNoteResult = assessmentService.addAssessmentClarificationNote(user, assessmentId, newClarificationNote.query)
    val clarificationNote = when (clarificationNoteResult) {
      is AuthorisableActionResult.Success -> clarificationNoteResult.entity
      is AuthorisableActionResult.NotFound -> throw NotFoundProblem(assessmentId, "Assessment")
      is AuthorisableActionResult.Unauthorised -> throw ForbiddenProblem()
    }

    return ResponseEntity.ok(
      assessmentClarificationNoteTransformer.transformJpaToApi(clarificationNote),
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

    val clarificationNoteEntityResult = when (clarificationNoteResult) {
      is AuthorisableActionResult.Success -> clarificationNoteResult.entity
      is AuthorisableActionResult.NotFound -> throw NotFoundProblem(assessmentId, "Assessment")
      is AuthorisableActionResult.Unauthorised -> throw ForbiddenProblem()
    }

    val clarificationNoteForResponse = when (clarificationNoteEntityResult) {
      is ValidatableActionResult.Success -> clarificationNoteEntityResult.entity
      else -> throw InternalServerErrorProblem("You must provide a response")
    }

    return ResponseEntity.ok(
      assessmentClarificationNoteTransformer.transformJpaToApi(clarificationNoteForResponse),
    )
  }

  override fun assessmentsAssessmentIdReferralHistoryNotesPost(
    assessmentId: UUID,
    newReferralHistoryUserNote: NewReferralHistoryUserNote,
  ): ResponseEntity<ReferralHistoryNote> {
    val user = userService.getUserForRequest()

    val referralHistoryUserNote = when (val referralHistoryUserNoteResult = assessmentService.addAssessmentReferralHistoryUserNote(user, assessmentId, newReferralHistoryUserNote.message)) {
      is AuthorisableActionResult.Success -> referralHistoryUserNoteResult.entity
      is AuthorisableActionResult.NotFound -> throw NotFoundProblem(assessmentId, "Assessment")
      is AuthorisableActionResult.Unauthorised -> throw ForbiddenProblem()
    }

    return ResponseEntity.ok(
      assessmentReferralHistoryNoteTransformer.transformJpaToApi(referralHistoryUserNote),
    )
  }
}
