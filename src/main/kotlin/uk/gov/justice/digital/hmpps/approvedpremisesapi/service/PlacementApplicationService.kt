package uk.gov.justice.digital.hmpps.approvedpremisesapi.service

import arrow.core.Either
import jakarta.transaction.Transactional
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.PlacementApplicationDecisionEnvelope
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApprovedPremisesApplicationEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApprovedPremisesPlacementApplicationJsonSchemaEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.AssessmentDecision
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.LockablePlacementApplicationRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.PlacementApplicationEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.PlacementApplicationRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.PlacementApplicationWithdrawalReason
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.PlacementDateEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.PlacementDateRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.PlacementType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserPermission
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.ApprovedPremisesApplicationStatus
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.ValidationErrors
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.validated
import uk.gov.justice.digital.hmpps.approvedpremisesapi.problem.InternalServerErrorProblem
import uk.gov.justice.digital.hmpps.approvedpremisesapi.results.CasResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1.Cas1PlacementApplicationDomainEventService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1.Cas1PlacementApplicationEmailService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1.PlacementRequestService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1.WithdrawableEntityType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1.WithdrawableState
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1.WithdrawalContext
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1.allocations.UserAllocator
import java.time.Clock
import java.time.OffsetDateTime
import java.util.UUID
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.PlacementApplicationDecision as ApiPlacementApplicationDecision
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.PlacementDates as ApiPlacementDates
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.PlacementType as ApiPlacementType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.PlacementApplicationDecision as JpaPlacementApplicationDecision

@Service
@Suppress("ReturnCount")
class PlacementApplicationService(
  private val placementApplicationRepository: PlacementApplicationRepository,
  private val jsonSchemaService: JsonSchemaService,
  private val userService: UserService,
  private val placementDateRepository: PlacementDateRepository,
  private val placementRequestService: PlacementRequestService,
  private val userAllocator: UserAllocator,
  private val userAccessService: UserAccessService,
  private val cas1PlacementApplicationEmailService: Cas1PlacementApplicationEmailService,
  private val cas1PlacementApplicationDomainEventService: Cas1PlacementApplicationDomainEventService,
  private val taskDeadlineService: TaskDeadlineService,
  private val clock: Clock,
  private val lockablePlacementApplicationRepository: LockablePlacementApplicationRepository,
) {

  var log: Logger = LoggerFactory.getLogger(this::class.java)

  fun getAllSubmittedNonReallocatedApplications(applicationId: UUID): List<PlacementApplicationEntity> {
    return placementApplicationRepository.findAllSubmittedNonReallocatedApplicationsForApplicationId(applicationId)
  }

  fun createPlacementApplication(
    application: ApprovedPremisesApplicationEntity,
    user: UserEntity,
  ) = validated<PlacementApplicationEntity> {
    val assessment = application.getLatestAssessment()

    if (assessment?.decision !== AssessmentDecision.ACCEPTED) {
      return generalError("You cannot request a placement request for an application that has not been approved")
    }

    if (application.status == ApprovedPremisesApplicationStatus.WITHDRAWN) {
      return generalError("You cannot request a placement request for an application that has been withdrawn")
    }

    if (application.status == ApprovedPremisesApplicationStatus.EXPIRED) {
      return generalError("Placement requests cannot be made for an expired application")
    }

    val placementApplication = placementApplicationRepository.save(
      PlacementApplicationEntity(
        id = UUID.randomUUID(),
        application = application,
        createdByUser = user,
        schemaVersion = jsonSchemaService.getNewestSchema(ApprovedPremisesPlacementApplicationJsonSchemaEntity::class.java),
        schemaUpToDate = true,
        data = null,
        document = null,
        createdAt = OffsetDateTime.now(),
        submittedAt = null,
        allocatedToUser = null,
        allocatedAt = null,
        reallocatedAt = null,
        decision = null,
        decisionMadeAt = null,
        placementType = null,
        placementDates = mutableListOf(),
        placementRequests = mutableListOf(),
        withdrawalReason = null,
        dueAt = null,
        submissionGroupId = UUID.randomUUID(),
      ),
    )

    val createdApplication = placementApplicationRepository.save(placementApplication)

    return success(createdApplication.apply { schemaUpToDate = true })
  }

  fun getApplication(id: UUID): CasResult<PlacementApplicationEntity> {
    val placementApplication =
      placementApplicationRepository.findByIdOrNull(id) ?: return CasResult.NotFound("placement application", id.toString())

    return CasResult.Success(setSchemaUpToDate(placementApplication))
  }

  fun getApplicationOrNull(id: UUID) = placementApplicationRepository.findByIdOrNull(id)

  fun reallocateApplication(
    assigneeUser: UserEntity,
    id: UUID,
  ): CasResult<PlacementApplicationEntity> {
    lockablePlacementApplicationRepository.acquirePessimisticLock(id)

    val currentPlacementApplication = placementApplicationRepository.findByIdOrNull(id)
      ?: return CasResult.NotFound("placement application", id.toString())

    if (currentPlacementApplication.reallocatedAt != null) {
      return CasResult.ConflictError(
        currentPlacementApplication.id,
        "This placement application has already been reallocated",
      )
    }

    if (currentPlacementApplication.decision != null) {
      return CasResult.GeneralValidationError("This placement application has already been completed")
    }

    if (!assigneeUser.hasPermission(UserPermission.CAS1_ASSESS_PLACEMENT_APPLICATION)) {
      return CasResult.FieldValidationError(
        ValidationErrors().apply {
          this["$.userId"] = "lackingMatcherRole"
        },
      )
    }

    currentPlacementApplication.reallocatedAt = OffsetDateTime.now()
    placementApplicationRepository.save(currentPlacementApplication)

    // Make the timestamp precision less precise, so we don't have any issues with microsecond resolution in tests
    val dateTimeNow = OffsetDateTime.now().withNano(0)

    val newPlacementApplication = currentPlacementApplication.copy(
      id = UUID.randomUUID(),
      reallocatedAt = null,
      allocatedAt = currentPlacementApplication.reallocatedAt,
      allocatedToUser = assigneeUser,
      createdAt = dateTimeNow,
      dueAt = null,
    )

    newPlacementApplication.dueAt = taskDeadlineService.getDeadline(newPlacementApplication)

    placementApplicationRepository.save(newPlacementApplication)
    cas1PlacementApplicationDomainEventService.placementApplicationAllocated(
      placementApplication = newPlacementApplication,
      allocatedByUser = userService.getUserForRequest(),
    )

    val newPlacementDates = placementDateRepository.saveAll(
      currentPlacementApplication.placementDates.map {
        PlacementDateEntity(
          id = UUID.randomUUID(),
          expectedArrival = it.expectedArrival,
          duration = it.duration,
          placementApplication = newPlacementApplication,
          createdAt = dateTimeNow,
        )
      },
    )

    val applicationWasntPreviouslyAllocated = currentPlacementApplication.allocatedToUser == null
    if (applicationWasntPreviouslyAllocated) {
      cas1PlacementApplicationEmailService.placementApplicationAllocated(newPlacementApplication)
    }

    newPlacementApplication.placementDates = newPlacementDates

    return CasResult.Success(newPlacementApplication)
  }

  fun getWithdrawableState(placementApplication: PlacementApplicationEntity, user: UserEntity): WithdrawableState {
    return WithdrawableState(
      withdrawable = placementApplication.isInWithdrawableState(),
      withdrawn = placementApplication.isWithdrawn,
      userMayDirectlyWithdraw = userAccessService.userMayWithdrawPlacementApplication(user, placementApplication),
    )
  }

  /**
   * This function should not be called directly. Instead, use [WithdrawableService.withdrawPlacementApplication] that
   * will indirectly invoke this function. It will also ensure that:
   *
   * 1. The entity is withdrawable, and error if not
   * 2. The user is allowed to withdraw it, and error if not
   * 3. If withdrawn, all descendents entities are withdrawn, where applicable
   */
  @SuppressWarnings("ThrowsCount")
  @Transactional
  fun withdrawPlacementApplication(
    id: UUID,
    userProvidedReason: PlacementApplicationWithdrawalReason?,
    withdrawalContext: WithdrawalContext,
  ): CasResult<PlacementApplicationEntity> {
    val placementApplication =
      placementApplicationRepository.findByIdOrNull(id) ?: return CasResult.NotFound(
        entityType = "PlacementApplication",
        id = id.toString(),
      )

    if (placementApplication.isWithdrawn) {
      return CasResult.Success(placementApplication)
    }

    val wasBeingAssessedBy = if (placementApplication.isBeingAssessed()) {
      placementApplication.allocatedToUser
    } else {
      null
    }

    placementApplication.isWithdrawn = true
    placementApplication.withdrawalReason = when (withdrawalContext.triggeringEntityType) {
      WithdrawableEntityType.Application -> PlacementApplicationWithdrawalReason.RELATED_APPLICATION_WITHDRAWN
      WithdrawableEntityType.PlacementApplication -> userProvidedReason
      WithdrawableEntityType.PlacementRequest -> throw InternalServerErrorProblem("Withdrawing a PlacementRequest should not cascade to PlacementApplications")
      WithdrawableEntityType.Booking -> throw InternalServerErrorProblem("Withdrawing a Booking should not cascade to PlacementApplications")
      WithdrawableEntityType.SpaceBooking -> throw InternalServerErrorProblem("Withdrawing a SpaceBooking should not cascade to PlacementApplications")
    }

    val savedApplication = placementApplicationRepository.save(placementApplication)

    cas1PlacementApplicationDomainEventService.placementApplicationWithdrawn(placementApplication, withdrawalContext)
    cas1PlacementApplicationEmailService.placementApplicationWithdrawn(
      placementApplication = placementApplication,
      wasBeingAssessedBy = wasBeingAssessedBy,
      withdrawalTriggeredBy = withdrawalContext.withdrawalTriggeredBy,
    )

    return CasResult.Success(savedApplication)
  }

  fun updateApplication(
    id: UUID,
    data: String,
  ): CasResult<PlacementApplicationEntity> {
    val placementApplicationAuthorisationResult = getApplicationForUpdateOrSubmit<PlacementApplicationEntity>(id)

    if (placementApplicationAuthorisationResult is Either.Left) {
      return placementApplicationAuthorisationResult.value
    }

    val placementApplicationEntity = (placementApplicationAuthorisationResult as Either.Right).value

    if (placementApplicationEntity.application.status == ApprovedPremisesApplicationStatus.EXPIRED) {
      return CasResult.GeneralValidationError("Placement requests cannot be made for an expired application")
    }

    placementApplicationEntity.data = data

    val savedApplication = placementApplicationRepository.save(placementApplicationEntity)

    return CasResult.Success(savedApplication)
  }

  @Transactional
  fun submitApplication(
    id: UUID,
    translatedDocument: String,
    apiPlacementType: ApiPlacementType,
    apiPlacementDates: List<ApiPlacementDates>,
  ): CasResult<List<PlacementApplicationEntity>> {
    val placementApplicationAuthorisationResult = getApplicationForUpdateOrSubmit<List<PlacementApplicationEntity>>(id)

    if (placementApplicationAuthorisationResult is Either.Left) {
      return placementApplicationAuthorisationResult.value
    }

    if (apiPlacementDates.isEmpty()) {
      return CasResult.GeneralValidationError("At least one placement date is required")
    }

    val submittedPlacementApplication = (placementApplicationAuthorisationResult as Either.Right).value

    if (submittedPlacementApplication.application.status == ApprovedPremisesApplicationStatus.EXPIRED) {
      return CasResult.GeneralValidationError("Placement requests cannot be made for an expired application")
    }

    val allocatedUser = userAllocator.getUserForPlacementApplicationAllocation(submittedPlacementApplication)

    val now = OffsetDateTime.now(clock)

    submittedPlacementApplication.apply {
      document = translatedDocument
      allocatedToUser = allocatedUser
      submittedAt = now
      allocatedAt = now
      placementType = getPlacementType(apiPlacementType)
      submissionGroupId = UUID.randomUUID()
    }

    submittedPlacementApplication.dueAt = taskDeadlineService.getDeadline(submittedPlacementApplication)

    val baselinePlacementApplication = placementApplicationRepository.save(submittedPlacementApplication)

    val placementApplicationsWithDates = saveDatesOnSubmissionToAnAppPerDate(baselinePlacementApplication, apiPlacementDates)

    placementApplicationsWithDates.forEach { placementApplication ->
      cas1PlacementApplicationDomainEventService.placementApplicationSubmitted(
        placementApplication,
        userService.getDeliusUserNameForRequest(),
      )
      cas1PlacementApplicationEmailService.placementApplicationSubmitted(placementApplication)
      if (baselinePlacementApplication.allocatedToUser != null) {
        cas1PlacementApplicationEmailService.placementApplicationAllocated(placementApplication)
        cas1PlacementApplicationDomainEventService.placementApplicationAllocated(
          placementApplication = placementApplication,
          allocatedByUser = null,
        )
      }
    }

    return CasResult.Success(placementApplicationsWithDates)
  }

  private fun saveDatesOnSubmissionToAnAppPerDate(
    baselinePlacementApplication: PlacementApplicationEntity,
    apiPlacementDates: List<ApiPlacementDates>,
  ): List<PlacementApplicationEntity> {
    val additionalPlacementApps = List(apiPlacementDates.size - 1) {
      placementApplicationRepository.save(
        baselinePlacementApplication.copy(id = UUID.randomUUID()),
      )
    }

    val allPlacementApps = listOf(baselinePlacementApplication) + additionalPlacementApps

    allPlacementApps.zip(apiPlacementDates) { placementApp, apiDate ->
      val placementDate = placementDateRepository.save(
        PlacementDateEntity(
          id = UUID.randomUUID(),
          expectedArrival = apiDate.expectedArrival,
          duration = apiDate.duration,
          placementApplication = placementApp,
          createdAt = OffsetDateTime.now(),
        ),
      )

      placementApp.placementDates = mutableListOf(placementDate)
    }

    return allPlacementApps
  }

  @Transactional
  fun recordDecision(
    id: UUID,
    placementApplicationDecisionEnvelope: PlacementApplicationDecisionEnvelope,
  ): CasResult<PlacementApplicationEntity> {
    val user = userService.getUserForRequest()
    val placementApplicationEntity =
      placementApplicationRepository.findByIdOrNull(id) ?: return CasResult.NotFound(
        entityType = "PlacementApplication",
        id = id.toString(),
      )

    if (placementApplicationEntity.allocatedToUser != user) {
      return CasResult.Unauthorised()
    }

    if (placementApplicationEntity.decision != null) {
      return CasResult.GeneralValidationError("This application has already had a decision set")
    }

    if (placementApplicationDecisionEnvelope.decision == ApiPlacementApplicationDecision.accepted) {
      val placementRequestResult =
        placementRequestService.createPlacementRequestsFromPlacementApplication(
          placementApplicationEntity,
          placementApplicationDecisionEnvelope.decisionSummary,
        )

      if (placementRequestResult is CasResult.Error) {
        return placementRequestResult.reviseType()
      }
    }

    placementApplicationEntity.apply {
      decision = JpaPlacementApplicationDecision.valueOf(placementApplicationDecisionEnvelope.decision)
      decisionMadeAt = OffsetDateTime.now(clock)
    }

    val savedApplication = placementApplicationRepository.save(placementApplicationEntity)

    when (placementApplicationDecisionEnvelope.decision) {
      ApiPlacementApplicationDecision.accepted -> cas1PlacementApplicationEmailService.placementApplicationAccepted(placementApplicationEntity)
      ApiPlacementApplicationDecision.rejected -> cas1PlacementApplicationEmailService.placementApplicationRejected(placementApplicationEntity)
      ApiPlacementApplicationDecision.withdraw -> cas1PlacementApplicationEmailService.placementApplicationRejected(placementApplicationEntity)
      ApiPlacementApplicationDecision.withdrawnByPp -> cas1PlacementApplicationEmailService.placementApplicationRejected(placementApplicationEntity)
    }

    cas1PlacementApplicationDomainEventService.placementApplicationAssessed(
      savedApplication,
      user,
      placementApplicationDecisionEnvelope,
    )

    return CasResult.Success(savedApplication)
  }

  private fun getPlacementType(apiPlacementType: ApiPlacementType): PlacementType {
    return when (apiPlacementType) {
      ApiPlacementType.additionalPlacement -> PlacementType.ADDITIONAL_PLACEMENT
      ApiPlacementType.rotl -> PlacementType.ROTL
      ApiPlacementType.releaseFollowingDecision -> PlacementType.RELEASE_FOLLOWING_DECISION
    }
  }

  private fun setSchemaUpToDate(placementApplicationEntity: PlacementApplicationEntity): PlacementApplicationEntity {
    val latestSchema = jsonSchemaService.getNewestSchema(
      ApprovedPremisesPlacementApplicationJsonSchemaEntity::class.java,
    )

    placementApplicationEntity.schemaUpToDate = placementApplicationEntity.schemaVersion.id == latestSchema.id

    return placementApplicationEntity
  }

  private fun <T> getApplicationForUpdateOrSubmit(id: UUID): Either<CasResult<T>, PlacementApplicationEntity> {
    val placementApplication = placementApplicationRepository.findByIdOrNull(id)
      ?: return Either.Left(
        CasResult.NotFound(
          entityType = "PlacementApplication",
          id = id.toString(),
        ),
      )
    val user = userService.getUserForRequest()

    if (placementApplication.createdByUser != user) {
      return Either.Left(CasResult.Unauthorised())
    }

    val validationError = confirmApplicationCanBeUpdatedOrSubmitted<T>(placementApplication)
    if (validationError != null) {
      return Either.Left(validationError)
    }

    return Either.Right(placementApplication)
  }

  private fun <T> confirmApplicationCanBeUpdatedOrSubmitted(
    placementApplicationEntity: PlacementApplicationEntity,
  ): CasResult<T>? {
    val latestSchema = jsonSchemaService.getNewestSchema(
      ApprovedPremisesPlacementApplicationJsonSchemaEntity::class.java,
    )
    placementApplicationEntity.schemaUpToDate = placementApplicationEntity.schemaVersion.id == latestSchema.id

    if (!placementApplicationEntity.schemaUpToDate) {
      return CasResult.GeneralValidationError("The schema version is outdated")
    }

    if (placementApplicationEntity.submittedAt != null) {
      return CasResult.GeneralValidationError("This application has already been submitted")
    }

    return null
  }
}
