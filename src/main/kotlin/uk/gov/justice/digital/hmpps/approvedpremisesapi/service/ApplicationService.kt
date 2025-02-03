package uk.gov.justice.digital.hmpps.approvedpremisesapi.service

import com.fasterxml.jackson.databind.ObjectMapper
import jakarta.transaction.Transactional
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ApType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ApplicationSortField
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas1ApplicationTimelinessCategory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas1ApplicationUserDetails
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ServiceName
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.SortDirection
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.SubmitApprovedPremisesApplication
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.SubmitTemporaryAccommodationApplication
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.WithdrawalReason
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.ApDeliusContextApiClient
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.ClientResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApAreaRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApplicationEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApplicationRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApplicationSummary
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApplicationTeamCodeEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApplicationTeamCodeRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApprovedPremisesApplicationEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApprovedPremisesApplicationJsonSchemaEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApprovedPremisesApplicationSummary
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.Cas1ApplicationUserDetailsEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.Cas1ApplicationUserDetailsRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.Cas1CruManagementAreaEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.Cas1CruManagementAreaRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.LockableApplicationRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.OfflineApplicationEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.OfflineApplicationRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.PlacementApplicationAutomaticEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.PlacementApplicationAutomaticRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ProbationDeliveryUnitRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.TemporaryAccommodationApplicationEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.TemporaryAccommodationApplicationJsonSchemaEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserRole
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.listeners.ApplicationListener
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.ApprovedPremisesApplicationStatus
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.ApprovedPremisesType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.PaginationMetadata
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.PersonInfoResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.PersonRisks
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.ValidationErrors
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.asApprovedPremisesType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.community.OffenderDetailSummary
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.prisonsapi.InmateStatus
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.validatedCasResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.problem.InternalServerErrorProblem
import uk.gov.justice.digital.hmpps.approvedpremisesapi.results.AuthorisableActionResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.results.CasResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.results.ValidatableActionResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1.Cas1ApplicationDomainEventService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1.Cas1ApplicationEmailService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1.WithdrawableState
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.getMetadata
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.getPageable
import java.time.Clock
import java.time.LocalDate
import java.time.LocalTime
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.util.UUID

@Service
@Suppress(
  "LongParameterList",
  "TooManyFunctions",
  "ReturnCount",
  "ThrowsCount",
  "TooGenericExceptionThrown",
)
class ApplicationService(
  private val userRepository: UserRepository,
  private val applicationRepository: ApplicationRepository,
  private val jsonSchemaService: JsonSchemaService,
  private val offenderService: OffenderService,
  private val userService: UserService,
  private val assessmentService: AssessmentService,
  private val offlineApplicationRepository: OfflineApplicationRepository,
  private val cas3DomainEventService: uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas3.DomainEventService,
  private val apDeliusContextApiClient: ApDeliusContextApiClient,
  private val applicationTeamCodeRepository: ApplicationTeamCodeRepository,
  private val userAccessService: UserAccessService,
  private val objectMapper: ObjectMapper,
  private val apAreaRepository: ApAreaRepository,
  private val cas1ApplicationDomainEventService: Cas1ApplicationDomainEventService,
  private val cas1ApplicationUserDetailsRepository: Cas1ApplicationUserDetailsRepository,
  private val cas1ApplicationEmailService: Cas1ApplicationEmailService,
  private val placementApplicationAutomaticRepository: PlacementApplicationAutomaticRepository,
  private val applicationListener: ApplicationListener,
  private val clock: Clock,
  private val lockableApplicationRepository: LockableApplicationRepository,
  private val probationDeliveryUnitRepository: ProbationDeliveryUnitRepository,
  private val cas1CruManagementAreaRepository: Cas1CruManagementAreaRepository,
) {
  fun getApplication(applicationId: UUID) = applicationRepository.findByIdOrNull(applicationId)

  fun getAllApplicationsForUsername(userEntity: UserEntity, serviceName: ServiceName): List<ApplicationSummary> {
    return when (serviceName) {
      ServiceName.approvedPremises -> getAllApprovedPremisesApplicationsForUser(userEntity)
      ServiceName.cas2 -> throw RuntimeException("CAS2 applications now require NomisUser")
      ServiceName.cas2v2 -> throw RuntimeException("CAS2v2 applications now require Cas2v2User")
      ServiceName.temporaryAccommodation -> getAllTemporaryAccommodationApplicationsForUser(userEntity)
    }
  }

  fun getAllApprovedPremisesApplications(
    page: Int?,
    crnOrName: String?,
    sortDirection: SortDirection?,
    status: List<ApprovedPremisesApplicationStatus>,
    sortBy: ApplicationSortField?,
    apAreaId: UUID?,
    releaseType: String?,
    pageSize: Int? = 10,
  ): Pair<List<ApprovedPremisesApplicationSummary>, PaginationMetadata?> {
    val sortField = when (sortBy) {
      ApplicationSortField.arrivalDate -> "arrivalDate"
      ApplicationSortField.createdAt -> "a.created_at"
      ApplicationSortField.tier -> "tier"
      ApplicationSortField.releaseType -> "releaseType"
      else -> "a.created_at"
    }
    val pageable = getPageable(sortField, sortDirection, page, pageSize)

    val statusNames = status.map { it.name }

    val response = applicationRepository.findAllApprovedPremisesSummaries(
      pageable = pageable,
      crnOrName = crnOrName,
      statusProvided = statusNames.isNotEmpty(),
      status = statusNames,
      apAreaId = apAreaId,
      releaseType,
    )

    return Pair(response.content, getMetadata(response, page, pageSize))
  }

  private fun getAllApprovedPremisesApplicationsForUser(user: UserEntity) =
    applicationRepository.findNonWithdrawnApprovedPremisesSummariesForUser(user.id)

  private fun getAllTemporaryAccommodationApplicationsForUser(user: UserEntity): List<ApplicationSummary> =
    applicationRepository.findAllTemporaryAccommodationSummariesCreatedByUser(user.id)

  fun getApplicationForUsername(
    applicationId: UUID,
    userDistinguishedName: String,
  ): CasResult<ApplicationEntity> {
    val applicationEntity = applicationRepository.findByIdOrNull(applicationId)
      ?: return CasResult.NotFound("Application", applicationId.toString())

    if (applicationEntity is TemporaryAccommodationApplicationEntity && applicationEntity.deletedAt != null) {
      return CasResult.NotFound("Application", applicationId.toString())
    }

    val userEntity = userRepository.findByDeliusUsername(userDistinguishedName)
      ?: throw RuntimeException("Could not get user")

    val canAccess = userAccessService.userCanViewApplication(userEntity, applicationEntity)

    return if (canAccess) {
      CasResult.Success(jsonSchemaService.checkSchemaOutdated(applicationEntity))
    } else {
      CasResult.Unauthorised()
    }
  }

  fun getOfflineApplicationForUsername(
    applicationId: UUID,
    deliusUsername: String,
  ): CasResult<OfflineApplicationEntity> {
    val applicationEntity = offlineApplicationRepository.findByIdOrNull(applicationId)
      ?: return CasResult.NotFound("Application", applicationId.toString())

    val userEntity = userRepository.findByDeliusUsername(deliusUsername)
      ?: throw RuntimeException("Could not get user")

    if (userEntity.hasAnyRole(
        UserRole.CAS1_WORKFLOW_MANAGER,
        UserRole.CAS1_ASSESSOR,
        UserRole.CAS1_MATCHER,
        UserRole.CAS1_FUTURE_MANAGER,
      ) &&
      offenderService.canAccessOffender(deliusUsername, applicationEntity.crn)
    ) {
      return CasResult.Success(applicationEntity)
    }

    return CasResult.Unauthorised()
  }

  fun createApprovedPremisesApplication(
    offenderDetails: OffenderDetailSummary,
    user: UserEntity,
    convictionId: Long?,
    deliusEventNumber: String?,
    offenceId: String?,
    createWithRisks: Boolean? = true,
  ) = validatedCasResult<ApprovedPremisesApplicationEntity> {
    val crn = offenderDetails.otherIds.crn

    val managingTeamCodes = when (val managingTeamsResult = apDeliusContextApiClient.getTeamsManagingCase(crn)) {
      is ClientResult.Success -> managingTeamsResult.body.teamCodes
      is ClientResult.Failure -> managingTeamsResult.throwException()
    }

    if (convictionId == null) {
      "$.convictionId" hasValidationError "empty"
    }

    if (deliusEventNumber == null) {
      "$.deliusEventNumber" hasValidationError "empty"
    }

    if (offenceId == null) {
      "$.offenceId" hasValidationError "empty"
    }

    if (validationErrors.any()) {
      return fieldValidationError
    }

    var riskRatings: PersonRisks? = null

    if (createWithRisks == true) {
      val riskRatingsResult = offenderService.getRiskByCrn(crn, user.deliusUsername)

      riskRatings = when (riskRatingsResult) {
        is AuthorisableActionResult.NotFound -> return "$.crn" hasSingleValidationError "doesNotExist"
        is AuthorisableActionResult.Unauthorised -> return "$.crn" hasSingleValidationError "userPermission"
        is AuthorisableActionResult.Success -> riskRatingsResult.entity
      }
    }

    val createdApplication = applicationRepository.saveAndFlush(
      createApprovedPremisesApplicationEntity(
        crn,
        user,
        convictionId,
        deliusEventNumber,
        offenceId,
        riskRatings,
        offenderDetails,
      ),
    )

    managingTeamCodes.forEach {
      createdApplication.teamCodes += applicationTeamCodeRepository.save(
        ApplicationTeamCodeEntity(
          id = UUID.randomUUID(),
          application = createdApplication,
          teamCode = it,
        ),
      )
    }

    return success(createdApplication.apply { schemaUpToDate = true })
  }

  fun createApprovedPremisesApplicationEntity(
    crn: String,
    user: UserEntity,
    convictionId: Long?,
    deliusEventNumber: String?,
    offenceId: String?,
    riskRatings: PersonRisks?,
    offenderDetails: OffenderDetailSummary,
  ): ApprovedPremisesApplicationEntity {
    return ApprovedPremisesApplicationEntity(
      id = UUID.randomUUID(),
      crn = crn,
      createdByUser = user,
      data = null,
      document = null,
      schemaVersion = jsonSchemaService.getNewestSchema(ApprovedPremisesApplicationJsonSchemaEntity::class.java),
      createdAt = OffsetDateTime.now(),
      submittedAt = null,
      deletedAt = null,
      isWomensApplication = null,
      isEmergencyApplication = null,
      apType = ApprovedPremisesType.NORMAL,
      convictionId = convictionId!!,
      eventNumber = deliusEventNumber!!,
      offenceId = offenceId!!,
      schemaUpToDate = true,
      riskRatings = riskRatings,
      assessments = mutableListOf(),
      teamCodes = mutableListOf(),
      placementRequests = mutableListOf(),
      releaseType = null,
      arrivalDate = null,
      isInapplicable = null,
      isWithdrawn = false,
      withdrawalReason = null,
      otherWithdrawalReason = null,
      nomsNumber = offenderDetails.otherIds.nomsNumber,
      name = "${offenderDetails.firstName.uppercase()} ${offenderDetails.surname.uppercase()}",
      targetLocation = null,
      status = ApprovedPremisesApplicationStatus.STARTED,
      sentenceType = null,
      situation = null,
      inmateInOutStatusOnSubmission = null,
      apArea = null,
      cruManagementArea = null,
      applicantUserDetails = null,
      caseManagerIsNotApplicant = null,
      caseManagerUserDetails = null,
      noticeType = null,
      licenceExpiryDate = null,
    )
  }

  fun createOfflineApplication(offlineApplication: OfflineApplicationEntity) =
    offlineApplicationRepository.save(offlineApplication)

  fun createTemporaryAccommodationApplication(
    crn: String,
    user: UserEntity,
    convictionId: Long?,
    deliusEventNumber: String?,
    offenceId: String?,
    createWithRisks: Boolean? = true,
    personInfo: PersonInfoResult.Success.Full,
  ): CasResult<TemporaryAccommodationApplicationEntity> {
    if (!user.hasRole(UserRole.CAS3_REFERRER)) {
      return CasResult.Unauthorised()
    }

    return validatedCasResult {
      val offenderDetails =
        when (
          val offenderDetailsResult = offenderService.getOffenderByCrn(crn, user.deliusUsername)
        ) {
          is AuthorisableActionResult.NotFound -> return@validatedCasResult "$.crn" hasSingleValidationError "doesNotExist"
          is AuthorisableActionResult.Unauthorised ->
            return@validatedCasResult "$.crn" hasSingleValidationError "userPermission"

          is AuthorisableActionResult.Success -> offenderDetailsResult.entity
        }

      if (convictionId == null) {
        "$.convictionId" hasValidationError "empty"
      }

      if (deliusEventNumber == null) {
        "$.deliusEventNumber" hasValidationError "empty"
      }

      if (offenceId == null) {
        "$.offenceId" hasValidationError "empty"
      }

      if (validationErrors.any()) {
        return@validatedCasResult fieldValidationError
      }

      var riskRatings: PersonRisks? = null

      if (createWithRisks == true) {
        val riskRatingsResult = offenderService.getRiskByCrn(crn, user.deliusUsername)

        riskRatings = when (riskRatingsResult) {
          is AuthorisableActionResult.NotFound ->
            return@validatedCasResult "$.crn" hasSingleValidationError "doesNotExist"

          is AuthorisableActionResult.Unauthorised ->
            return@validatedCasResult "$.crn" hasSingleValidationError "userPermission"

          is AuthorisableActionResult.Success -> riskRatingsResult.entity
        }
      }

      val prisonName = getPrisonName(personInfo)

      val createdApplication = applicationRepository.save(
        createTemporaryAccommodationApplicationEntity(
          crn,
          user,
          convictionId,
          deliusEventNumber,
          offenceId,
          riskRatings,
          offenderDetails,
          prisonName,
        ),
      )

      success(createdApplication.apply { schemaUpToDate = true })
    }
  }

  private fun createTemporaryAccommodationApplicationEntity(
    crn: String,
    user: UserEntity,
    convictionId: Long?,
    deliusEventNumber: String?,
    offenceId: String?,
    riskRatings: PersonRisks?,
    offenderDetails: OffenderDetailSummary,
    prisonName: String?,
  ): TemporaryAccommodationApplicationEntity {
    return TemporaryAccommodationApplicationEntity(
      id = UUID.randomUUID(),
      crn = crn,
      createdByUser = user,
      data = null,
      document = null,
      schemaVersion = jsonSchemaService.getNewestSchema(
        TemporaryAccommodationApplicationJsonSchemaEntity::class.java,
      ),
      createdAt = OffsetDateTime.now(),
      submittedAt = null,
      deletedAt = null,
      convictionId = convictionId!!,
      eventNumber = deliusEventNumber!!,
      offenceId = offenceId!!,
      schemaUpToDate = true,
      riskRatings = riskRatings,
      assessments = mutableListOf(),
      probationRegion = user.probationRegion,
      nomsNumber = offenderDetails.otherIds.nomsNumber,
      arrivalDate = null,
      isRegisteredSexOffender = null,
      needsAccessibleProperty = null,
      hasHistoryOfArson = null,
      isDutyToReferSubmitted = null,
      dutyToReferSubmissionDate = null,
      isEligible = null,
      eligibilityReason = null,
      dutyToReferLocalAuthorityAreaName = null,
      dutyToReferOutcome = null,
      prisonNameOnCreation = prisonName,
      personReleaseDate = null,
      pdu = null,
      name = "${offenderDetails.firstName} ${offenderDetails.surname}",
      isHistoryOfSexualOffence = null,
      isConcerningSexualBehaviour = null,
      isConcerningArsonBehaviour = null,
      prisonReleaseTypes = null,
      probationDeliveryUnit = null,
    )
  }

  data class Cas1ApplicationUpdateFields(
    val isWomensApplication: Boolean?,
    @Deprecated("use apType")
    val isPipeApplication: Boolean?,
    @Deprecated("use noticeType")
    val isEmergencyApplication: Boolean?,
    @Deprecated("use apType")
    val isEsapApplication: Boolean?,
    val apType: ApType?,
    val releaseType: String?,
    val arrivalDate: LocalDate?,
    val data: String,
    val isInapplicable: Boolean?,
    val noticeType: Cas1ApplicationTimelinessCategory?,
  )

  @Transactional
  fun updateApprovedPremisesApplication(
    applicationId: UUID,
    updateFields: Cas1ApplicationUpdateFields,
    userForRequest: UserEntity,
  ): CasResult<ApprovedPremisesApplicationEntity> {
    val application = applicationRepository.findByIdOrNull(applicationId)?.let(jsonSchemaService::checkSchemaOutdated)
      ?: return CasResult.NotFound("Application", applicationId.toString())

    if (application !is ApprovedPremisesApplicationEntity) {
      return CasResult.GeneralValidationError("onlyCas1Supported")
    }

    if (updateFields.isUsingLegacyApTypeFields && updateFields.isUsingNewApTypeField) {
      return CasResult.GeneralValidationError(
        "`isPipeApplication`/`isEsapApplication` should not be used in conjunction with `apType`",
      )
    }

    if (application.createdByUser.id != userForRequest.id) {
      return CasResult.Unauthorised()
    }

    if (!application.schemaUpToDate) {
      return CasResult.GeneralValidationError("The schema version is outdated")
    }

    if (application.submittedAt != null) {
      return CasResult.GeneralValidationError("This application has already been submitted")
    }

    application.apply {
      this.isInapplicable = updateFields.isInapplicable
      this.isWomensApplication = updateFields.isWomensApplication
      this.isEmergencyApplication = updateFields.isEmergencyApplication
      this.apType = updateFields.deriveApType()
      this.releaseType = updateFields.releaseType
      this.arrivalDate = if (updateFields.arrivalDate !== null) {
        OffsetDateTime.of(updateFields.arrivalDate, LocalTime.MIDNIGHT, ZoneOffset.UTC)
      } else {
        null
      }
      this.data = updateFields.data
      this.noticeType = getNoticeType(updateFields.noticeType, updateFields.isEmergencyApplication, this)
    }

    applicationListener.preUpdate(application)
    val savedApplication = applicationRepository.save(application)

    return CasResult.Success(savedApplication)
  }

  private fun upsertCas1ApplicationUserDetails(
    existingEntry: Cas1ApplicationUserDetailsEntity?,
    updatedValues: Cas1ApplicationUserDetails?,
  ): Cas1ApplicationUserDetailsEntity? {
    if (updatedValues == null) {
      existingEntry?.let {
        cas1ApplicationUserDetailsRepository.delete(it)
      }
      return null
    }

    return cas1ApplicationUserDetailsRepository.save(
      Cas1ApplicationUserDetailsEntity(
        id = existingEntry?.id ?: UUID.randomUUID(),
        name = updatedValues.name,
        email = updatedValues.email,
        telephoneNumber = updatedValues.telephoneNumber,
      ),
    )
  }

  fun updateApprovedPremisesApplicationStatus(applicationId: UUID, status: ApprovedPremisesApplicationStatus) {
    applicationRepository.updateStatus(applicationId, status)
  }

  /**
   * This function should not be called directly. Instead, use [WithdrawableService.withdrawApplication] that
   * will indirectly invoke this function. It will also ensure that:
   *
   * 1. The entity is withdrawable, and error if not
   * 2. The user is allowed to withdraw it, and error if not
   * 3. If withdrawn, all descdents entities are withdrawn, where applicable
   */
  @Transactional
  fun withdrawApprovedPremisesApplication(
    applicationId: UUID,
    user: UserEntity,
    withdrawalReason: String,
    otherReason: String?,
  ): CasResult<Unit> {
    val application = applicationRepository.findByIdOrNull(applicationId)
      ?: return CasResult.NotFound(entityType = "application", applicationId.toString())

    if (application !is ApprovedPremisesApplicationEntity) {
      return CasResult.GeneralValidationError("onlyCas1Supported")
    }

    if (application.isWithdrawn) {
      return CasResult.Success(Unit)
    }

    val updatedApplication = application.apply {
      this.isWithdrawn = true
      this.withdrawalReason = withdrawalReason
      this.otherWithdrawalReason = if (withdrawalReason == WithdrawalReason.other.value) {
        otherReason
      } else {
        null
      }
    }
    applicationListener.preUpdate(updatedApplication)

    applicationRepository.save(updatedApplication)

    cas1ApplicationDomainEventService.applicationWithdrawn(updatedApplication, withdrawingUser = user)
    cas1ApplicationEmailService.applicationWithdrawn(updatedApplication, user)

    updatedApplication.assessments.map {
      assessmentService.updateCas1AssessmentWithdrawn(it.id, user)
    }

    return CasResult.Success(Unit)
  }

  fun getWithdrawableState(application: ApprovedPremisesApplicationEntity, user: UserEntity): WithdrawableState {
    return WithdrawableState(
      withdrawable = !application.isWithdrawn,
      withdrawn = application.isWithdrawn,
      userMayDirectlyWithdraw = userAccessService.userMayWithdrawApplication(user, application),
    )
  }

  fun updateTemporaryAccommodationApplication(
    applicationId: UUID,
    data: String,
  ): CasResult<ApplicationEntity> {
    val application = applicationRepository.findByIdOrNull(applicationId)?.let(jsonSchemaService::checkSchemaOutdated)
      ?: return CasResult.NotFound("Application", applicationId.toString())

    if (application !is TemporaryAccommodationApplicationEntity) {
      return CasResult.GeneralValidationError("onlyCas3Supported")
    }

    val user = userService.getUserForRequest()

    if (application.createdByUser != user) {
      return CasResult.Unauthorised()
    }

    if (!application.schemaUpToDate) {
      return CasResult.GeneralValidationError("The schema version is outdated")
    }

    if (application.deletedAt != null) {
      return CasResult.GeneralValidationError("This application has already been deleted")
    }

    if (application.submittedAt != null) {
      return CasResult.GeneralValidationError("This application has already been submitted")
    }

    application.apply {
      this.data = data
    }

    val savedApplication = applicationRepository.save(application)

    return CasResult.Success(savedApplication)
  }

  @SuppressWarnings("CyclomaticComplexMethod")
  @Transactional
  fun submitApprovedPremisesApplication(
    applicationId: UUID,
    submitApplication: SubmitApprovedPremisesApplication,
    user: UserEntity,
    apAreaId: UUID,
  ): AuthorisableActionResult<ValidatableActionResult<ApplicationEntity>> {
    lockableApplicationRepository.acquirePessimisticLock(applicationId)

    var application = applicationRepository.findByIdOrNull(
      applicationId,
    )?.let(jsonSchemaService::checkSchemaOutdated)
      ?: return AuthorisableActionResult.NotFound()

    val serializedTranslatedDocument = objectMapper.writeValueAsString(submitApplication.translatedDocument)

    if (application.createdByUser != user) {
      return AuthorisableActionResult.Unauthorised()
    }

    if (application !is ApprovedPremisesApplicationEntity) {
      return AuthorisableActionResult.Success(
        ValidatableActionResult.GeneralValidationError("onlyCas1Supported"),
      )
    }

    if (submitApplication.isUsingLegacyApTypeFields && submitApplication.isUsingNewApTypeField) {
      return AuthorisableActionResult.Success(
        ValidatableActionResult.GeneralValidationError(
          "`isPipeApplication`/`isEsapApplication` should not be used in conjunction with `apType`",
        ),
      )
    }

    val apType = when {
      submitApplication.isUsingLegacyApTypeFields -> when {
        submitApplication.isPipeApplication == true -> ApprovedPremisesType.PIPE
        submitApplication.isEsapApplication == true -> ApprovedPremisesType.ESAP
        else -> ApprovedPremisesType.NORMAL
      }

      submitApplication.isUsingNewApTypeField -> submitApplication.apType!!.asApprovedPremisesType()
      else -> ApprovedPremisesType.NORMAL
    }

    if (application.submittedAt != null) {
      return AuthorisableActionResult.Success(
        ValidatableActionResult.GeneralValidationError("This application has already been submitted"),
      )
    }

    if (submitApplication.caseManagerIsNotApplicant == true && submitApplication.caseManagerUserDetails == null) {
      return AuthorisableActionResult.Success(
        ValidatableActionResult.GeneralValidationError("caseManagerUserDetails must be provided if caseManagerIsNotApplicant is true"),
      )
    }

    if (!application.schemaUpToDate) {
      return AuthorisableActionResult.Success(
        ValidatableActionResult.GeneralValidationError("The schema version is outdated"),
      )
    }

    val validationErrors = ValidationErrors()
    val applicationData = application.data

    if (applicationData == null) {
      validationErrors["$.data"] = "empty"
    } else if (!jsonSchemaService.validate(application.schemaVersion, applicationData)) {
      validationErrors["$.data"] = "invalid"
    }

    if (validationErrors.any()) {
      return AuthorisableActionResult.Success(
        ValidatableActionResult.FieldValidationError(validationErrors),
      )
    }

    val inmateDetails = application.nomsNumber?.let { nomsNumber ->
      when (val inmateDetailsResult = offenderService.getInmateDetailByNomsNumber(application.crn, nomsNumber)) {
        is AuthorisableActionResult.Success -> inmateDetailsResult.entity
        else -> null
      }
    }

    val now = OffsetDateTime.now(clock)

    val apArea = apAreaRepository.findByIdOrNull(apAreaId)!!
    application.apply {
      isWomensApplication = submitApplication.isWomensApplication
      this.isEmergencyApplication = isEmergencyApplication
      this.apType = apType
      submittedAt = now
      document = serializedTranslatedDocument
      releaseType = submitApplication.releaseType.toString()
      targetLocation = submitApplication.targetLocation
      arrivalDate = getArrivalDate(submitApplication.arrivalDate)
      sentenceType = submitApplication.sentenceType.toString()
      situation = submitApplication.situation?.toString()
      inmateInOutStatusOnSubmission = inmateDetails?.custodyStatus?.name
      this.apArea = apArea
      this.cruManagementArea = if (submitApplication.isWomensApplication == true) {
        cas1CruManagementAreaRepository.findByIdOrNull(Cas1CruManagementAreaEntity.WOMENS_ESTATE_ID)
          ?: throw InternalServerErrorProblem("Could not find women's estate CRU Management Area Entity with ID ${Cas1CruManagementAreaEntity.WOMENS_ESTATE_ID}")
      } else {
        apArea.defaultCruManagementArea
      }
      this.applicantUserDetails = upsertCas1ApplicationUserDetails(this.applicantUserDetails, submitApplication.applicantUserDetails)
      this.caseManagerIsNotApplicant = submitApplication.caseManagerIsNotApplicant
      this.caseManagerUserDetails = upsertCas1ApplicationUserDetails(
        existingEntry = this.caseManagerUserDetails,
        updatedValues = if (submitApplication.caseManagerIsNotApplicant == true) {
          submitApplication.caseManagerUserDetails
        } else {
          null
        },
      )
      this.noticeType = getNoticeType(submitApplication.noticeType, submitApplication.isEmergencyApplication, this)
      this.licenceExpiryDate = submitApplication.licenseExpiryDate
    }

    cas1ApplicationDomainEventService.applicationSubmitted(application, submitApplication, user.deliusUsername)
    assessmentService.createApprovedPremisesAssessment(application)

    applicationListener.preUpdate(application)
    application = applicationRepository.save(application)

    cas1ApplicationEmailService.applicationSubmitted(application)

    if (application.arrivalDate != null) {
      placementApplicationAutomaticRepository.save(
        PlacementApplicationAutomaticEntity(
          id = UUID.randomUUID(),
          application = application,
          submittedAt = application.submittedAt!!,
          expectedArrivalDate = application.arrivalDate!!,
        ),
      )
    }

    return AuthorisableActionResult.Success(
      ValidatableActionResult.Success(application),
    )
  }

  private fun getNoticeType(noticeType: Cas1ApplicationTimelinessCategory?, isEmergencyApplication: Boolean?, application: ApprovedPremisesApplicationEntity) = noticeType
    ?: if (isEmergencyApplication == true) {
      Cas1ApplicationTimelinessCategory.emergency
    } else if (application.isShortNoticeApplication() == true) {
      Cas1ApplicationTimelinessCategory.shortNotice
    } else {
      Cas1ApplicationTimelinessCategory.standard
    }

  fun getArrivalDate(arrivalDate: LocalDate?): OffsetDateTime? {
    if (arrivalDate !== null) {
      return OffsetDateTime.of(arrivalDate, LocalTime.MIDNIGHT, ZoneOffset.UTC)
    }
    return null
  }

  @Transactional
  fun submitTemporaryAccommodationApplication(
    applicationId: UUID,
    submitApplication: SubmitTemporaryAccommodationApplication,
  ): AuthorisableActionResult<ValidatableActionResult<ApplicationEntity>> {
    lockableApplicationRepository.acquirePessimisticLock(applicationId)
    var application =
      applicationRepository.findByIdOrNull(
        applicationId,
      )?.let(jsonSchemaService::checkSchemaOutdated)
        ?: return AuthorisableActionResult.NotFound()

    if (application.deletedAt != null) {
      return AuthorisableActionResult.Success(
        ValidatableActionResult.GeneralValidationError("This application has already been deleted"),
      )
    }

    val serializedTranslatedDocument = objectMapper.writeValueAsString(submitApplication.translatedDocument)

    val user = userService.getUserForRequest()

    if (application.createdByUser != user) {
      return AuthorisableActionResult.Unauthorised()
    }

    if (application !is TemporaryAccommodationApplicationEntity) {
      return AuthorisableActionResult.Success(
        ValidatableActionResult.GeneralValidationError("onlyCas3Supported"),
      )
    }

    if (application.submittedAt != null) {
      return AuthorisableActionResult.Success(
        ValidatableActionResult.GeneralValidationError("This application has already been submitted"),
      )
    }

    if (!application.schemaUpToDate) {
      return AuthorisableActionResult.Success(
        ValidatableActionResult.GeneralValidationError("The schema version is outdated"),
      )
    }

    val validationErrors = ValidationErrors()
    val applicationData = application.data

    if (applicationData == null) {
      validationErrors["$.data"] = "empty"
    } else if (!jsonSchemaService.validate(application.schemaVersion, applicationData)) {
      validationErrors["$.data"] = "invalid"
    }

    if (validationErrors.any()) {
      return AuthorisableActionResult.Success(
        ValidatableActionResult.FieldValidationError(validationErrors),
      )
    }

    application.apply {
      submittedAt = OffsetDateTime.now()
      document = serializedTranslatedDocument
      arrivalDate = OffsetDateTime.of(submitApplication.arrivalDate, LocalTime.MIDNIGHT, ZoneOffset.UTC)
      isRegisteredSexOffender = submitApplication.isRegisteredSexOffender
      isHistoryOfSexualOffence = submitApplication.isHistoryOfSexualOffence
      isConcerningSexualBehaviour = submitApplication.isConcerningSexualBehaviour
      needsAccessibleProperty = submitApplication.needsAccessibleProperty
      hasHistoryOfArson = submitApplication.hasHistoryOfArson
      isConcerningArsonBehaviour = submitApplication.isConcerningArsonBehaviour
      isDutyToReferSubmitted = submitApplication.isDutyToReferSubmitted
      dutyToReferSubmissionDate = submitApplication.dutyToReferSubmissionDate
      dutyToReferOutcome = submitApplication.dutyToReferOutcome
      isEligible = submitApplication.isApplicationEligible
      eligibilityReason = submitApplication.eligibilityReason
      dutyToReferLocalAuthorityAreaName = submitApplication.dutyToReferLocalAuthorityAreaName
      personReleaseDate = submitApplication.personReleaseDate
      pdu = submitApplication.pdu
      prisonReleaseTypes = submitApplication.prisonReleaseTypes?.joinToString(",")
      probationDeliveryUnit = submitApplication.probationDeliveryUnitId?.let {
        probationDeliveryUnitRepository.findByIdOrNull(it)
      }
    }

    assessmentService.createTemporaryAccommodationAssessment(application, submitApplication.summaryData!!)

    application = applicationRepository.save(application)

    cas3DomainEventService.saveReferralSubmittedEvent(application)

    return AuthorisableActionResult.Success(
      ValidatableActionResult.Success(application),
    )
  }

  fun getApplicationsForCrn(crn: String, serviceName: ServiceName): List<ApplicationEntity> {
    val entityType = if (serviceName == ServiceName.approvedPremises) {
      ApprovedPremisesApplicationEntity::class.java
    } else {
      TemporaryAccommodationApplicationEntity::class.java
    }

    return applicationRepository.findByCrn(crn, entityType)
  }

  fun getOfflineApplicationsForCrn(crn: String, serviceName: ServiceName) =
    offlineApplicationRepository.findAllByServiceAndCrn(serviceName.value, crn)

  private fun getPrisonName(personInfo: PersonInfoResult.Success.Full): String? {
    val prisonName = when (personInfo.inmateDetail?.custodyStatus) {
      InmateStatus.IN,
      InmateStatus.TRN,
      -> {
        personInfo.inmateDetail?.assignedLivingUnit?.agencyName ?: personInfo.inmateDetail?.assignedLivingUnit?.agencyId
      }
      else -> null
    }
    return prisonName
  }

  private val Cas1ApplicationUpdateFields.isUsingLegacyApTypeFields: Boolean
    get() = isPipeApplication != null || isEsapApplication != null

  private val Cas1ApplicationUpdateFields.isUsingNewApTypeField: Boolean
    get() = apType != null

  private fun Cas1ApplicationUpdateFields.deriveApType() = when {
    this.isUsingLegacyApTypeFields -> when {
      this.isPipeApplication == true -> ApprovedPremisesType.PIPE
      this.isEsapApplication == true -> ApprovedPremisesType.ESAP
      else -> ApprovedPremisesType.NORMAL
    }
    this.isUsingNewApTypeField -> this.apType!!.asApprovedPremisesType()
    else -> ApprovedPremisesType.NORMAL
  }

  private val SubmitApprovedPremisesApplication.isUsingLegacyApTypeFields: Boolean
    get() = isPipeApplication != null || isEsapApplication != null

  private val SubmitApprovedPremisesApplication.isUsingNewApTypeField: Boolean
    get() = apType != null
}
