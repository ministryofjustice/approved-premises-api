package uk.gov.justice.digital.hmpps.approvedpremisesapi.service

import com.fasterxml.jackson.databind.ObjectMapper
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas1.model.ApplicationAssessed
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas1.model.ApplicationAssessedAssessedBy
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas1.model.ApplicationAssessedEnvelope
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas1.model.Cru
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas1.model.EventType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas1.model.PersonReference
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas1.model.ProbationArea
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ApType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.AssessmentSortField
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas1ApplicationTimelinessCategory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.PlacementDates
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.PlacementRequirements
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ServiceName
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.ApDeliusContextApiClient
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.ClientResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApprovedPremisesApplicationEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApprovedPremisesAssessmentEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApprovedPremisesAssessmentJsonSchemaEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.AssessmentClarificationNoteEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.AssessmentClarificationNoteRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.AssessmentDecision
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.AssessmentEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.AssessmentReferralHistoryNoteRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.AssessmentReferralHistorySystemNoteEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.AssessmentReferralHistoryUserNoteEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.AssessmentRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.DomainAssessmentSummary
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.DomainAssessmentSummaryStatus
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.LockableAssessmentRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ReferralHistorySystemNoteType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ReferralRejectionReasonRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.TemporaryAccommodationApplicationEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.TemporaryAccommodationAssessmentEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.TemporaryAccommodationAssessmentJsonSchemaEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserPermission
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserQualification
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserRole
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.listeners.AssessmentClarificationNoteListener
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.listeners.AssessmentListener
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.DomainEvent
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.PaginationMetadata
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.ValidationErrors
import uk.gov.justice.digital.hmpps.approvedpremisesapi.problem.InternalServerErrorProblem
import uk.gov.justice.digital.hmpps.approvedpremisesapi.results.AuthorisableActionResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.results.CasResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.results.ValidatableActionResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1.Cas1AssessmentDomainEventService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1.Cas1AssessmentEmailService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1.Cas1DomainEventService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1.Cas1PlacementRequestEmailService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1.allocations.UserAllocator
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.PageCriteria
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.UrlTemplate
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.getMetadata
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.getPageableOrAllPages
import java.time.Clock
import java.time.LocalDate
import java.time.OffsetDateTime
import java.util.UUID

@Service
@Suppress("ReturnCount", "CyclomaticComplexMethod")
class AssessmentService(
  private val userService: UserService,
  private val userAccessService: UserAccessService,
  private val assessmentRepository: AssessmentRepository,
  private val assessmentClarificationNoteRepository: AssessmentClarificationNoteRepository,
  private val assessmentReferralHistoryNoteRepository: AssessmentReferralHistoryNoteRepository,
  private val referralRejectionReasonRepository: ReferralRejectionReasonRepository,
  private val jsonSchemaService: JsonSchemaService,
  private val domainEventService: Cas1DomainEventService,
  private val offenderService: OffenderService,
  private val apDeliusContextApiClient: ApDeliusContextApiClient,
  private val placementRequestService: PlacementRequestService,
  private val placementRequirementsService: PlacementRequirementsService,
  private val userAllocator: UserAllocator,
  private val objectMapper: ObjectMapper,
  @Value("\${url-templates.frontend.application}") private val applicationUrlTemplate: UrlTemplate,
  private val taskDeadlineService: TaskDeadlineService,
  private val cas1AssessmentEmailService: Cas1AssessmentEmailService,
  private val cas1AssessmentDomainEventService: Cas1AssessmentDomainEventService,
  private val cas1PlacementRequestEmailService: Cas1PlacementRequestEmailService,
  private val assessmentListener: AssessmentListener,
  private val assessmentClarificationNoteListener: AssessmentClarificationNoteListener,
  private val clock: Clock,
  private val lockableAssessmentRepository: LockableAssessmentRepository,
) {
  private val log = LoggerFactory.getLogger(this::class.java)

  fun getVisibleAssessmentSummariesForUserCAS1(
    user: UserEntity,
    statuses: List<DomainAssessmentSummaryStatus>,
    pageCriteria: PageCriteria<AssessmentSortField>,
  ): Pair<List<DomainAssessmentSummary>, PaginationMetadata?> {
    val pageable = buildPageable(pageCriteria)

    val response = assessmentRepository.findAllApprovedPremisesAssessmentSummariesNotReallocated(
      user.id.toString(),
      statuses.map { it.name },
      pageable,
    )

    return Pair(response.content, getMetadata(response, pageCriteria))
  }

  private fun buildPageable(pageCriteria: PageCriteria<AssessmentSortField>) = pageCriteria.toPageableOrAllPages(
    sortByConverter = when (pageCriteria.sortBy) {
      AssessmentSortField.assessmentStatus -> "status"
      AssessmentSortField.assessmentArrivalDate -> "arrivalDate"
      AssessmentSortField.assessmentCreatedAt -> "createdAt"
      AssessmentSortField.assessmentDueAt -> "dueAt"
      AssessmentSortField.personCrn -> "crn"
      AssessmentSortField.personName -> "personName"
      AssessmentSortField.applicationProbationDeliveryUnitName -> "probationDeliveryUnit"
    },
  )

  fun getAssessmentSummariesForUserCAS3(
    user: UserEntity,
    crnOrName: String?,
    serviceName: ServiceName,
    statuses: List<DomainAssessmentSummaryStatus>,
    pageCriteria: PageCriteria<AssessmentSortField>,
  ): Pair<List<DomainAssessmentSummary>, PaginationMetadata?> {
    when (serviceName) {
      ServiceName.temporaryAccommodation -> {
        val sortFieldString = when (pageCriteria.sortBy) {
          AssessmentSortField.assessmentStatus -> "status"
          AssessmentSortField.assessmentArrivalDate -> "arrivalDate"
          AssessmentSortField.assessmentCreatedAt -> "createdAt"
          AssessmentSortField.personCrn -> "crn"
          AssessmentSortField.applicationProbationDeliveryUnitName -> "probationDeliveryUnitName"
          else -> "arrivalDate"
        }
        val response = assessmentRepository.findTemporaryAccommodationAssessmentSummariesForRegionAndCrnAndStatus(
          user.probationRegion.id,
          crnOrName,
          statuses.map { it.name },
          getPageableOrAllPages(pageCriteria.withSortBy(sortFieldString)),
        )

        return Pair(response.content, getMetadata(response, pageCriteria))
      }
      else -> throw RuntimeException("Only CAS3 assessments are currently supported")
    }
  }

  fun getAssessmentAndValidate(
    user: UserEntity,
    assessmentId: UUID,
    forTimeline: Boolean = false,
  ): CasResult<AssessmentEntity> {
    val assessment = assessmentRepository.findByIdOrNull(assessmentId)
      ?: return CasResult.NotFound(AssessmentEntity::class.simpleName, assessmentId.toString())

    val latestSchema = when (assessment) {
      is ApprovedPremisesAssessmentEntity -> jsonSchemaService.getNewestSchema(
        ApprovedPremisesAssessmentJsonSchemaEntity::class.java,
      )

      is TemporaryAccommodationAssessmentEntity -> jsonSchemaService.getNewestSchema(
        TemporaryAccommodationAssessmentJsonSchemaEntity::class.java,
      )

      else -> throw RuntimeException("Assessment type '${assessment::class.qualifiedName}' is not currently supported")
    }

    val isAuthorised = userAccessService.userCanViewAssessment(user, assessment) || (forTimeline && userAccessService.userCanViewApplication(user, assessment.application))

    if (!isAuthorised) {
      return CasResult.Unauthorised()
    }

    assessment.schemaUpToDate = assessment.schemaVersion.id == latestSchema.id

    val offenderResult = offenderService.getOffenderByCrn(
      assessment.application.crn,
      user.deliusUsername,
      user.hasQualification(UserQualification.LAO),
    )

    if (offenderResult !is AuthorisableActionResult.Success) {
      return CasResult.Unauthorised()
    }

    return CasResult.Success(assessment)
  }

  fun getAssessmentForUserAndApplication(
    user: UserEntity,
    applicationID: UUID,
  ): AuthorisableActionResult<AssessmentEntity> {
    val latestSchema = jsonSchemaService.getNewestSchema(ApprovedPremisesAssessmentJsonSchemaEntity::class.java)

    val assessment = assessmentRepository.findByApplicationIdAndReallocatedAtNull(applicationID)
      ?: return AuthorisableActionResult.NotFound()

    if (!user.hasRole(UserRole.CAS1_WORKFLOW_MANAGER) && assessment.allocatedToUser != user) {
      return AuthorisableActionResult.Unauthorised()
    }

    assessment.schemaUpToDate = assessment.schemaVersion.id == latestSchema.id

    return AuthorisableActionResult.Success(assessment)
  }

  fun createApprovedPremisesAssessment(application: ApprovedPremisesApplicationEntity, createdFromAppeal: Boolean = false): ApprovedPremisesAssessmentEntity {
    val dateTimeNow = OffsetDateTime.now(clock)

    var assessment = ApprovedPremisesAssessmentEntity(
      id = UUID.randomUUID(),
      application = application,
      data = null,
      document = null,
      schemaVersion = jsonSchemaService.getNewestSchema(ApprovedPremisesAssessmentJsonSchemaEntity::class.java),
      allocatedToUser = null,
      allocatedAt = dateTimeNow,
      reallocatedAt = null,
      createdAt = dateTimeNow,
      submittedAt = null,
      decision = null,
      schemaUpToDate = true,
      rejectionRationale = null,
      clarificationNotes = mutableListOf(),
      referralHistoryNotes = mutableListOf(),
      isWithdrawn = false,
      createdFromAppeal = createdFromAppeal,
      dueAt = null,
    )

    assessment.dueAt = taskDeadlineService.getDeadline(assessment)

    val allocatedUser = userAllocator.getUserForAssessmentAllocation(assessment)
    assessment.allocatedToUser = allocatedUser

    prePersistAssessment(assessment)
    assessment = assessmentRepository.save(assessment)

    if (allocatedUser != null) {
      if (createdFromAppeal) {
        cas1AssessmentEmailService.appealedAssessmentAllocated(allocatedUser, assessment.id, application)
      } else {
        cas1AssessmentEmailService.assessmentAllocated(allocatedUser, assessment.id, application, assessment.dueAt, application.noticeType == Cas1ApplicationTimelinessCategory.emergency)
      }
      cas1AssessmentDomainEventService.assessmentAllocated(assessment, allocatedUser, allocatingUser = null)
    }

    return assessment
  }

  fun createTemporaryAccommodationAssessment(
    application: TemporaryAccommodationApplicationEntity,
    summaryData: Any,
  ): TemporaryAccommodationAssessmentEntity {
    val dateTimeNow = OffsetDateTime.now()

    val assessment = assessmentRepository.save(
      TemporaryAccommodationAssessmentEntity(
        id = UUID.randomUUID(),
        application = application,
        data = null,
        document = null,
        schemaVersion = jsonSchemaService.getNewestSchema(TemporaryAccommodationAssessmentJsonSchemaEntity::class.java),
        allocatedToUser = null,
        allocatedAt = dateTimeNow,
        reallocatedAt = null,
        createdAt = dateTimeNow,
        submittedAt = null,
        decision = null,
        schemaUpToDate = true,
        rejectionRationale = null,
        clarificationNotes = mutableListOf(),
        referralHistoryNotes = mutableListOf(),
        completedAt = null,
        summaryData = objectMapper.writeValueAsString(summaryData),
        isWithdrawn = false,
        referralRejectionReason = null,
        referralRejectionReasonDetail = null,
        dueAt = null,
        releaseDate = null,
        accommodationRequiredFromDate = null,
      ),
    )

    assessment.addSystemNote(userService.getUserForRequest(), ReferralHistorySystemNoteType.SUBMITTED)

    return assessment
  }

  fun updateAssessment(
    user: UserEntity,
    assessmentId: UUID,
    data: String?,
  ): CasResult<AssessmentEntity> {
    val assessmentResult = getAssessmentAndValidate(user, assessmentId)

    val assessment = when (assessmentResult) {
      is CasResult.Success -> assessmentResult.value
      else -> return assessmentResult
    }

    if (assessment.isWithdrawn) {
      return CasResult.GeneralValidationError("The application has been withdrawn.")
    }

    if (!assessment.schemaUpToDate) {
      return CasResult.GeneralValidationError("The schema version is outdated")
    }

    if (assessment.submittedAt != null) {
      return CasResult.GeneralValidationError("A decision has already been taken on this assessment")
    }

    if (assessment.reallocatedAt != null) {
      return CasResult.GeneralValidationError("The assessment has been reallocated, this assessment is read only")
    }

    assessment.data = data

    preUpdateAssessment(assessment)
    val savedAssessment = assessmentRepository.save(assessment)

    return CasResult.Success(savedAssessment)
  }

  fun acceptAssessment(
    user: UserEntity,
    assessmentId: UUID,
    document: String?,
    placementRequirements: PlacementRequirements?,
    placementDates: PlacementDates?,
    apType: ApType?,
    notes: String?,
  ): CasResult<AssessmentEntity> {
    val acceptedAt = OffsetDateTime.now(clock)
    val createPlacementRequest = placementDates != null

    val assessment = when (val assessmentResult = getAssessmentAndValidate(user, assessmentId)) {
      is CasResult.Success -> assessmentResult.value
      is CasResult.Error -> return assessmentResult
    }

    if (!assessment.schemaUpToDate) {
      return CasResult.GeneralValidationError("The schema version is outdated")
    }

    if (assessment is ApprovedPremisesAssessmentEntity && assessment.submittedAt != null) {
      return CasResult.GeneralValidationError("A decision has already been taken on this assessment")
    }

    if (assessment.reallocatedAt != null) {
      return CasResult.GeneralValidationError("The application has been reallocated, this assessment is read only")
    }

    val validationErrors = ValidationErrors()
    val assessmentData = assessment.data

    if (assessment is ApprovedPremisesAssessmentEntity) {
      if (assessmentData == null) {
        validationErrors["$.data"] = "empty"
      } else if (!jsonSchemaService.validate(assessment.schemaVersion, assessmentData)) {
        validationErrors["$.data"] = "invalid"
      }

      if (placementRequirements == null) {
        validationErrors["$.requirements"] = "empty"
      }
    }

    if (validationErrors.any()) {
      return CasResult.FieldValidationError(validationErrors)
    }

    assessment.document = document
    assessment.submittedAt = acceptedAt
    assessment.decision = AssessmentDecision.ACCEPTED

    if (assessment is TemporaryAccommodationAssessmentEntity) {
      assessment.completedAt = null
    }

    preUpdateAssessment(assessment)
    val savedAssessment = assessmentRepository.save(assessment)

    if (savedAssessment is TemporaryAccommodationAssessmentEntity) {
      savedAssessment.addSystemNote(userService.getUserForRequest(), ReferralHistorySystemNoteType.READY_TO_PLACE)
    }

    if (assessment is ApprovedPremisesAssessmentEntity) {
      val placementRequirementsResult =
        when (
          val result =
            placementRequirementsService.createPlacementRequirements(assessment, placementRequirements!!)
        ) {
          is CasResult.Success -> result.value
          is CasResult.Error -> return result.reviseType()
        }

      if (createPlacementRequest) {
        placementRequestService.createPlacementRequest(
          PlacementRequestSource.ASSESSMENT_OF_APPLICATION,
          placementRequirementsResult,
          placementDates!!,
          notes,
          false,
          null,
        )
      }
    }

    val application = savedAssessment.application

    val offenderDetails =
      when (val offenderDetailsResult = offenderService.getOffenderByCrn(application.crn, user.deliusUsername, true)) {
        is AuthorisableActionResult.Success -> offenderDetailsResult.entity
        is AuthorisableActionResult.Unauthorised -> throw RuntimeException("Unable to get Offender Details when creating Application Assessed Domain Event: Unauthorised")
        is AuthorisableActionResult.NotFound -> throw RuntimeException("Unable to get Offender Details when creating Application Assessed Domain Event: Not Found")
      }

    if (application is ApprovedPremisesApplicationEntity) {
      cas1AssessmentDomainEventService.assessmentAccepted(application, assessment, offenderDetails, placementDates, apType, userService.getUserForRequest())
      cas1AssessmentEmailService.assessmentAccepted(application)

      if (createPlacementRequest) {
        cas1PlacementRequestEmailService.placementRequestSubmitted(application)
      }
    }

    return CasResult.Success(savedAssessment)
  }

  fun rejectAssessment(
    user: UserEntity,
    assessmentId: UUID,
    document: String?,
    rejectionRationale: String,
    referralRejectionReasonId: UUID? = null,
    referralRejectionReasonDetail: String? = null,
    isWithdrawn: Boolean? = null,
  ): CasResult<AssessmentEntity> {
    val domainEventId = UUID.randomUUID()
    val rejectedAt = OffsetDateTime.now(clock)

    val assessmentResult = getAssessmentAndValidate(user, assessmentId)
    val assessment = when (assessmentResult) {
      is CasResult.Success -> assessmentResult.value
      else -> return assessmentResult
    }

    if (!assessment.schemaUpToDate) {
      return CasResult.GeneralValidationError("The schema version is outdated")
    }

    if (assessment is ApprovedPremisesAssessmentEntity && assessment.submittedAt != null) {
      return CasResult.GeneralValidationError("A decision has already been taken on this assessment")
    }

    if (assessment.reallocatedAt != null) {
      return CasResult.GeneralValidationError("The application has been reallocated, this assessment is read only")
    }

    val validationErrors = ValidationErrors()
    val assessmentData = assessment.data

    if (assessment is ApprovedPremisesAssessmentEntity) {
      if (assessmentData == null) {
        validationErrors["$.data"] = "empty"
      } else if (!jsonSchemaService.validate(assessment.schemaVersion, assessmentData)) {
        validationErrors["$.data"] = "invalid"
      }
    }

    if (validationErrors.any()) {
      return CasResult.FieldValidationError(validationErrors)
    }

    assessment.document = document
    assessment.submittedAt = rejectedAt
    assessment.decision = AssessmentDecision.REJECTED
    assessment.rejectionRationale = rejectionRationale

    if (assessment is TemporaryAccommodationAssessmentEntity) {
      val referralRejectionReason = referralRejectionReasonRepository.findByIdOrNull(referralRejectionReasonId)
        ?: throw InternalServerErrorProblem("No Referral Rejection Reason with an ID of $referralRejectionReasonId could be found")

      assessment.completedAt = null
      assessment.referralRejectionReason = referralRejectionReason
      assessment.referralRejectionReasonDetail = referralRejectionReasonDetail
      assessment.isWithdrawn = isWithdrawn!!
    }

    preUpdateAssessment(assessment)
    val savedAssessment = assessmentRepository.save(assessment)

    if (savedAssessment is TemporaryAccommodationAssessmentEntity) {
      savedAssessment.addSystemNote(userService.getUserForRequest(), ReferralHistorySystemNoteType.REJECTED)
    }

    val application = savedAssessment.application

    val offenderDetails =
      when (val offenderDetailsResult = offenderService.getOffenderByCrn(application.crn, user.deliusUsername, true)) {
        is AuthorisableActionResult.Success -> offenderDetailsResult.entity
        else -> null
      }

    val staffDetails = when (val staffDetailsResult = apDeliusContextApiClient.getStaffDetail(user.deliusUsername)) {
      is ClientResult.Success -> staffDetailsResult.body
      is ClientResult.Failure -> staffDetailsResult.throwException()
    }

    if (application is ApprovedPremisesApplicationEntity) {
      domainEventService.saveApplicationAssessedDomainEvent(
        DomainEvent(
          id = domainEventId,
          applicationId = application.id,
          assessmentId = assessment.id,
          crn = application.crn,
          nomsNumber = offenderDetails?.otherIds?.nomsNumber,
          occurredAt = rejectedAt.toInstant(),
          data = ApplicationAssessedEnvelope(
            id = domainEventId,
            timestamp = rejectedAt.toInstant(),
            eventType = EventType.applicationAssessed,
            eventDetails = ApplicationAssessed(
              applicationId = application.id,
              applicationUrl = applicationUrlTemplate
                .resolve("id", application.id.toString()),
              personReference = PersonReference(
                crn = assessment.application.crn,
                noms = offenderDetails?.otherIds?.nomsNumber ?: "Unknown NOMS Number",
              ),
              deliusEventNumber = application.eventNumber,
              assessedAt = rejectedAt.toInstant(),
              assessedBy = ApplicationAssessedAssessedBy(
                staffMember = staffDetails.toStaffMember(),
                probationArea = ProbationArea(
                  code = staffDetails.probationArea.code,
                  name = staffDetails.probationArea.description,
                ),
                cru = Cru(
                  name = user.apArea?.name ?: "Unknown CRU",
                ),
              ),
              decision = assessment.decision.toString(),
              decisionRationale = assessment.rejectionRationale,
              arrivalDate = null,
            ),
          ),
        ),
      )

      cas1AssessmentEmailService.assessmentRejected(application)
    }

    return CasResult.Success(savedAssessment)
  }

  fun closeAssessment(
    user: UserEntity,
    assessmentId: UUID,
  ): CasResult<AssessmentEntity> {
    val assessment = when (val assessmentResult = getAssessmentAndValidate(user, assessmentId)) {
      is CasResult.Success -> assessmentResult.value
      else -> return assessmentResult
    }

    if (assessment !is TemporaryAccommodationAssessmentEntity) {
      throw RuntimeException("Only CAS3 is currently supported")
    }

    if (!assessment.schemaUpToDate) {
      return CasResult.GeneralValidationError("The schema version is outdated")
    }

    if (assessment.completedAt != null) {
      log.info("User: ${user.id} attempted to close assessment: $assessmentId. This assessment has already been closed.")
      return CasResult.Success(assessment)
    }

    assessment.completedAt = OffsetDateTime.now()

    val savedAssessment = assessmentRepository.save(assessment)
    savedAssessment.addSystemNote(userService.getUserForRequest(), ReferralHistorySystemNoteType.COMPLETED)

    return CasResult.Success(savedAssessment)
  }

  fun reallocateAssessment(
    assigneeUser: UserEntity,
    id: UUID,
  ): AuthorisableActionResult<ValidatableActionResult<AssessmentEntity>> {
    lockableAssessmentRepository.acquirePessimisticLock(id)

    val currentAssessment = assessmentRepository.findByIdOrNull(id)
      ?: return AuthorisableActionResult.NotFound()

    if (currentAssessment.reallocatedAt != null) {
      return AuthorisableActionResult.Success(
        ValidatableActionResult.ConflictError(
          currentAssessment.id,
          "This assessment has already been reallocated",
        ),
      )
    }

    return when (currentAssessment) {
      is ApprovedPremisesAssessmentEntity -> reallocateApprovedPremisesAssessment(assigneeUser, currentAssessment)
      is TemporaryAccommodationAssessmentEntity -> reallocateTemporaryAccommodationAssessment(
        assigneeUser,
        currentAssessment,
      )

      else -> throw RuntimeException("Reallocating an assessment of type '${currentAssessment::class.qualifiedName}' has not been implemented.")
    }
  }

  private fun reallocateApprovedPremisesAssessment(
    assigneeUser: UserEntity,
    currentAssessment: ApprovedPremisesAssessmentEntity,
  ): AuthorisableActionResult<ValidatableActionResult<AssessmentEntity>> {
    if (currentAssessment.submittedAt != null) {
      return AuthorisableActionResult.Success(
        ValidatableActionResult.GeneralValidationError("A decision has already been taken on this assessment"),
      )
    }

    val application = currentAssessment.application
    val requiredQualifications = application.getRequiredQualifications()

    if (!canUserAssessPlacement(assigneeUser, currentAssessment)) {
      return AuthorisableActionResult.Success(
        ValidatableActionResult.FieldValidationError(
          ValidationErrors().apply {
            this["$.userId"] = "lacking assess application or assess appealed application permission"
          },
        ),
      )
    }

    if (!assigneeUser.hasAllQualifications(requiredQualifications)) {
      return AuthorisableActionResult.Success(
        ValidatableActionResult.FieldValidationError(
          ValidationErrors().apply {
            this["$.userId"] = "lackingQualifications"
          },
        ),
      )
    }

    val dateTimeNow = OffsetDateTime.now(clock)
    currentAssessment.reallocatedAt = dateTimeNow

    preUpdateAssessment(currentAssessment)
    assessmentRepository.save(currentAssessment)

    val newAssessment =
      ApprovedPremisesAssessmentEntity(
        id = UUID.randomUUID(),
        application = application,
        data = null,
        document = null,
        schemaVersion = jsonSchemaService.getNewestSchema(ApprovedPremisesAssessmentJsonSchemaEntity::class.java),
        allocatedToUser = assigneeUser,
        allocatedAt = dateTimeNow,
        reallocatedAt = null,
        createdAt = dateTimeNow,
        submittedAt = null,
        decision = null,
        schemaUpToDate = true,
        rejectionRationale = null,
        clarificationNotes = mutableListOf(),
        referralHistoryNotes = mutableListOf(),
        isWithdrawn = false,
        createdFromAppeal = currentAssessment.createdFromAppeal,
        dueAt = null,
      )

    newAssessment.dueAt = taskDeadlineService.getDeadline(newAssessment)

    prePersistAssessment(newAssessment)
    assessmentRepository.save(newAssessment)

    if (application is ApprovedPremisesApplicationEntity) {
      cas1AssessmentEmailService.assessmentAllocated(assigneeUser, newAssessment.id, application, newAssessment.dueAt, application.noticeType == Cas1ApplicationTimelinessCategory.emergency)
      val allocatedToUser = currentAssessment.allocatedToUser
      if (allocatedToUser != null) {
        cas1AssessmentEmailService.assessmentDeallocated(allocatedToUser, newAssessment.id, application)
      }
      cas1AssessmentDomainEventService.assessmentAllocated(newAssessment, assigneeUser, userService.getUserForRequest())
    }

    return AuthorisableActionResult.Success(
      ValidatableActionResult.Success(
        newAssessment,
      ),
    )
  }

  private fun canUserAssessPlacement(user: UserEntity, assessment: ApprovedPremisesAssessmentEntity): Boolean {
    val assigneeUsersPermissions = (user.roles.map { it.role.permissions }).flatten().distinct()

    return (
      assigneeUsersPermissions.contains(UserPermission.CAS1_ASSESS_APPLICATION) ||
        (assessment.createdFromAppeal && assigneeUsersPermissions.contains(UserPermission.CAS1_ASSESS_APPEALED_APPLICATION))
      )
  }

  private fun reallocateTemporaryAccommodationAssessment(
    assigneeUser: UserEntity,
    currentAssessment: TemporaryAccommodationAssessmentEntity,
  ): AuthorisableActionResult<ValidatableActionResult<AssessmentEntity>> {
    if (!assigneeUser.hasRole(UserRole.CAS3_ASSESSOR)) {
      return AuthorisableActionResult.Success(
        ValidatableActionResult.FieldValidationError(
          ValidationErrors().apply {
            this["$.userId"] = "lackingAssessorRole"
          },
        ),
      )
    }

    currentAssessment.allocatedToUser = assigneeUser
    currentAssessment.allocatedAt = OffsetDateTime.now()
    currentAssessment.decision = null

    val savedAssessment = assessmentRepository.save(currentAssessment)
    savedAssessment.addSystemNote(userService.getUserForRequest(), ReferralHistorySystemNoteType.IN_REVIEW)

    return AuthorisableActionResult.Success(
      ValidatableActionResult.Success(
        savedAssessment,
      ),
    )
  }

  fun deallocateAssessment(id: UUID): AuthorisableActionResult<ValidatableActionResult<AssessmentEntity>> {
    val currentAssessment = assessmentRepository.findByIdOrNull(id)
      ?: return AuthorisableActionResult.NotFound()

    if (currentAssessment !is TemporaryAccommodationAssessmentEntity) {
      throw RuntimeException("Only CAS3 Assessments are currently supported")
    }

    currentAssessment.allocatedToUser = null
    currentAssessment.allocatedAt = null
    currentAssessment.decision = null
    currentAssessment.submittedAt = null

    val savedAssessment = assessmentRepository.save(currentAssessment)
    savedAssessment.addSystemNote(userService.getUserForRequest(), ReferralHistorySystemNoteType.UNALLOCATED)

    return AuthorisableActionResult.Success(
      ValidatableActionResult.Success(
        savedAssessment,
      ),
    )
  }

  fun addAssessmentClarificationNote(
    user: UserEntity,
    assessmentId: UUID,
    text: String,
  ): CasResult<AssessmentClarificationNoteEntity> {
    val assessment = when (val assessmentResult = getAssessmentAndValidate(user, assessmentId)) {
      is CasResult.Success -> assessmentResult.value
      is CasResult.Error -> return assessmentResult.reviseType()
    }

    val clarificationNoteToSave = AssessmentClarificationNoteEntity(
      id = UUID.randomUUID(),
      assessment = assessment,
      createdByUser = user,
      createdAt = OffsetDateTime.now(clock),
      query = text,
      response = null,
      responseReceivedOn = null,
    )
    prePersistClarificationNote(clarificationNoteToSave)
    val clarificationNoteEntity = assessmentClarificationNoteRepository.save(clarificationNoteToSave)

    cas1AssessmentDomainEventService.furtherInformationRequested(assessment, clarificationNoteEntity)

    return CasResult.Success(clarificationNoteEntity)
  }

  fun updateAssessmentClarificationNote(
    user: UserEntity,
    assessmentId: UUID,
    id: UUID,
    response: String,
    responseReceivedOn: LocalDate,
  ): CasResult<AssessmentClarificationNoteEntity> {
    val assessment = when (val assessmentResult = getAssessmentAndValidate(user, assessmentId)) {
      is CasResult.Success -> assessmentResult.value
      is CasResult.Error -> return assessmentResult.reviseType()
    }

    val clarificationNoteEntity = assessmentClarificationNoteRepository.findByAssessmentIdAndId(assessment.id, id)

    if (clarificationNoteEntity === null) {
      return CasResult.NotFound()
    }

    if (clarificationNoteEntity.createdByUser.id !== user.id) {
      return CasResult.Unauthorised()
    }

    if (clarificationNoteEntity.response !== null) {
      return CasResult.GeneralValidationError("A response has already been added to this note")
    }

    clarificationNoteEntity.response = response
    clarificationNoteEntity.responseReceivedOn = responseReceivedOn

    preUpdateClarificationNote(clarificationNoteEntity)
    val savedNote = assessmentClarificationNoteRepository.save(clarificationNoteEntity)
    // We need to save the assessment here to update the Application's status

    val assessmentToUpdate = clarificationNoteEntity.assessment
    preUpdateAssessment(assessmentToUpdate)
    assessmentRepository.save(assessmentToUpdate)

    return CasResult.Success(savedNote)
  }

  fun addAssessmentReferralHistoryUserNote(
    user: UserEntity,
    assessmentId: UUID,
    text: String,
  ): CasResult<AssessmentReferralHistoryUserNoteEntity> {
    val assessment = when (val assessmentResult = getAssessmentAndValidate(user, assessmentId)) {
      is CasResult.Success -> assessmentResult.value
      is CasResult.Error -> return assessmentResult.reviseType()
    }

    val referralHistoryNoteEntity = assessmentReferralHistoryNoteRepository.save(
      AssessmentReferralHistoryUserNoteEntity(
        id = UUID.randomUUID(),
        assessment = assessment,
        createdAt = OffsetDateTime.now(),
        message = text,
        createdByUser = user,
      ),
    )

    return CasResult.Success(referralHistoryNoteEntity)
  }

  fun updateCas1AssessmentWithdrawn(assessmentId: UUID, withdrawingUser: UserEntity) {
    val assessment = assessmentRepository.findByIdOrNull(assessmentId)
    if (assessment is ApprovedPremisesAssessmentEntity) {
      val isPendingAssessment = assessment.isPendingAssessment()

      assessment.isWithdrawn = true

      preUpdateAssessment(assessment)
      assessmentRepository.save(assessment)

      cas1AssessmentEmailService.assessmentWithdrawn(
        assessment = assessment,
        isAssessmentPending = isPendingAssessment,
        withdrawingUser = withdrawingUser,
        application = assessment.application as ApprovedPremisesApplicationEntity,
      )
    }
  }

  private fun AssessmentEntity.addSystemNote(user: UserEntity, type: ReferralHistorySystemNoteType) {
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

  private fun prePersistClarificationNote(note: AssessmentClarificationNoteEntity) {
    assessmentClarificationNoteListener.prePersist(note)
  }

  private fun preUpdateClarificationNote(note: AssessmentClarificationNoteEntity) {
    assessmentClarificationNoteListener.preUpdate(note)
  }

  private fun prePersistAssessment(assessment: AssessmentEntity) {
    if (assessment is ApprovedPremisesAssessmentEntity) {
      assessmentListener.prePersist(assessment)
    }
  }

  private fun preUpdateAssessment(assessment: AssessmentEntity) {
    if (assessment is ApprovedPremisesAssessmentEntity) {
      assessmentListener.preUpdate(assessment)
    }
  }
}
