package uk.gov.justice.digital.hmpps.approvedpremisesapi.controller

import com.fasterxml.jackson.databind.ObjectMapper
import jakarta.transaction.Transactional
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.ApplicationsApiDelegate
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Appeal
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Application
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ApplicationSortField
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ApplicationSummary
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ApplicationTimelineNote
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ApprovedPremisesApplicationStatus
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Assessment
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Document
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.NewAppeal
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.NewApplication
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.NewApplicationTimelineNote
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.NewWithdrawal
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ReleaseTypeOption
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.RequestForPlacement
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ServiceName
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.SortDirection
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.SubmitApplication
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.SubmitApprovedPremisesApplication
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.SubmitTemporaryAccommodationApplication
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.TimelineEvent
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.UpdateApplication
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.UpdateApprovedPremisesApplication
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.UpdateTemporaryAccommodationApplication
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Withdrawable
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Withdrawables
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApplicationEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApprovedPremisesApplicationEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.OfflineApplicationEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserQualification
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.PersonInfoResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.deliuscontext.APDeliusDocument
import uk.gov.justice.digital.hmpps.approvedpremisesapi.problem.BadRequestProblem
import uk.gov.justice.digital.hmpps.approvedpremisesapi.problem.ConflictProblem
import uk.gov.justice.digital.hmpps.approvedpremisesapi.problem.ForbiddenProblem
import uk.gov.justice.digital.hmpps.approvedpremisesapi.problem.NotFoundProblem
import uk.gov.justice.digital.hmpps.approvedpremisesapi.problem.NotImplementedProblem
import uk.gov.justice.digital.hmpps.approvedpremisesapi.results.AuthorisableActionResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.results.CasResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.results.ValidatableActionResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.AppealService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.ApplicationService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.ApplicationTimelineNoteService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.AssessmentService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.HttpAuthService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.OffenderService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.UserService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1.Cas1RequestForPlacementService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1.Cas1WithdrawableService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1.WithdrawableEntitiesWithNotes
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.AppealTransformer
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.ApplicationTimelineNoteTransformer
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.ApplicationsTransformer
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.AssessmentTransformer
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.DocumentTransformer
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.WithdrawableTransformer
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.extractEntityFromCasResult
import java.net.URI
import java.util.UUID
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApplicationSummary as JPAApplicationSummary
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.ApprovedPremisesApplicationStatus.Companion as DomainApprovedPremisesApplicationStatus

@Service
@Suppress("LongParameterList", "TooManyFunctions")
class ApplicationsController(
  private val httpAuthService: HttpAuthService,
  private val applicationService: ApplicationService,
  private val applicationsTransformer: ApplicationsTransformer,
  private val assessmentTransformer: AssessmentTransformer,
  private val objectMapper: ObjectMapper,
  private val offenderService: OffenderService,
  private val documentTransformer: DocumentTransformer,
  private val assessmentService: AssessmentService,
  private val userService: UserService,
  private val cas1WithdrawableService: Cas1WithdrawableService,
  private val appealService: AppealService,
  private val appealTransformer: AppealTransformer,
  private val cas1RequestForPlacementService: Cas1RequestForPlacementService,
  private val withdrawableTransformer: WithdrawableTransformer,
  private val applicationTimelineNoteService: ApplicationTimelineNoteService,
  private val applicationTimelineNoteTransformer: ApplicationTimelineNoteTransformer,
) : ApplicationsApiDelegate {

  override fun applicationsGet(xServiceName: ServiceName?): ResponseEntity<List<ApplicationSummary>> {
    val serviceName = xServiceName ?: ServiceName.approvedPremises

    val user = userService.getUserForRequest()

    val applications = applicationService.getAllApplicationsForUsername(user.deliusUsername, serviceName)

      /*
      This code is inefficient:

      getPersonDetailAndTransformToSummary will retrieve/check user access (via the call to offenderService),
      but the prior call to getAllApplicationsForUsername has already retrieved this information
      and filtered out applications that the user cannot access. This leads to duplicate calls being made.

      This check should be moved into getPersonDetailAndTransformToSummary (or a custom version of it to
      avoid breaking behaviour for other callers), where we filter out any response from
      offenderService.getInfoForPerson of type PersonInfoResult.Restricted. This will most likely have
      to be optional as to not 'break' other functions using getPersonDetailAndTransformToSummary
       */
    return ResponseEntity.ok(getPersonDetailAndTransformToSummary(applications, user))
  }

  override fun applicationsAllGet(
    xServiceName: ServiceName,
    page: Int?,
    crnOrName: String?,
    sortDirection: SortDirection?,
    status: List<ApprovedPremisesApplicationStatus>?,
    sortBy: ApplicationSortField?,
    apAreaId: UUID?,
    releaseType: ReleaseTypeOption?,
  ): ResponseEntity<List<ApplicationSummary>> {
    if (xServiceName != ServiceName.approvedPremises) {
      throw ForbiddenProblem()
    }
    val user = userService.getUserForRequest()
    val statusTransformed = status?.map { DomainApprovedPremisesApplicationStatus.valueOf(it) } ?: emptyList()

    val (applications, metadata) =
      applicationService.getAllApprovedPremisesApplications(
        page,
        crnOrName,
        sortDirection,
        statusTransformed,
        sortBy,
        apAreaId,
        releaseType?.name,
      )

    return ResponseEntity.ok().headers(
      metadata?.toHeaders(),
    ).body(
      getPersonDetailAndTransformToSummary(
        applications,
        user = user,
        ignoreLaoRestrictions = user.hasQualification(UserQualification.LAO),
      ),
    )
  }

  override fun applicationsApplicationIdGet(applicationId: UUID): ResponseEntity<Application> {
    val user = userService.getUserForRequest()

    val applicationResult = applicationService.getApplicationForUsername(applicationId, user.deliusUsername)
    // check for offlineApplication if not found
    if (applicationResult !is CasResult.NotFound) {
      val application = extractEntityFromCasResult(applicationResult)
      return ResponseEntity.ok(
        getPersonDetailAndTransform(
          application = application,
          user = user,
          ignoreLaoRestrictions = application is ApprovedPremisesApplicationEntity && user.hasQualification(
            UserQualification.LAO,
          ),
        ),
      )
    } else {
      val offlineApplication = extractEntityFromCasResult(
        applicationService.getOfflineApplicationForUsername(applicationId, user.deliusUsername),
      )

      return ResponseEntity.ok(
        getPersonDetailAndTransform(
          offlineApplication = offlineApplication,
          user = user,
          ignoreLaoRestrictions = user.hasQualification(UserQualification.LAO),
        ),
      )
    }
  }

  @Transactional
  override fun applicationsPost(body: NewApplication, xServiceName: ServiceName?, createWithRisks: Boolean?): ResponseEntity<Application> {
    val user = userService.getUserForRequest()

    val personInfo =
      when (val personInfoResult = offenderService.getPersonInfoResult(body.crn, user.deliusUsername, false)) {
        is PersonInfoResult.NotFound, is PersonInfoResult.Unknown -> throw NotFoundProblem(
          personInfoResult.crn,
          "Offender",
        )

        is PersonInfoResult.Success.Restricted -> throw ForbiddenProblem()
        is PersonInfoResult.Success.Full -> personInfoResult
      }

    val applicationResult = createApplication(
      xServiceName ?: ServiceName.approvedPremises,
      personInfo,
      user,
      body,
      createWithRisks,
    )

    val application = when (applicationResult) {
      is ValidatableActionResult.GeneralValidationError ->
        throw BadRequestProblem(errorDetail = applicationResult.message)

      is ValidatableActionResult.FieldValidationError ->
        throw BadRequestProblem(invalidParams = applicationResult.validationMessages)

      is ValidatableActionResult.ConflictError ->
        throw ConflictProblem(id = applicationResult.conflictingEntityId, conflictReason = applicationResult.message)

      is ValidatableActionResult.Success -> applicationResult.entity
    }

    return ResponseEntity
      .created(URI.create("/applications/${application.id}"))
      .body(applicationsTransformer.transformJpaToApi(application, personInfo))
  }

  private fun createApplication(
    serviceName: ServiceName,
    personInfo: PersonInfoResult.Success.Full,
    user: UserEntity,
    body: NewApplication,
    createWithRisks: Boolean?,
  ): ValidatableActionResult<ApplicationEntity> = when (serviceName) {
    ServiceName.approvedPremises ->
      applicationService.createApprovedPremisesApplication(
        personInfo.offenderDetailSummary,
        user,
        body.convictionId,
        body.deliusEventNumber,
        body.offenceId,
        createWithRisks,
      )

    ServiceName.temporaryAccommodation -> {
      when (
        val actionResult =
          applicationService.createTemporaryAccommodationApplication(
            body.crn,
            user,
            body.convictionId,
            body.deliusEventNumber,
            body.offenceId,
            createWithRisks,
            personInfo,
          )
      ) {
        is AuthorisableActionResult.NotFound -> throw NotFoundProblem(actionResult.id!!, actionResult.entityType!!)
        is AuthorisableActionResult.Unauthorised -> throw ForbiddenProblem()
        is AuthorisableActionResult.Success -> actionResult.entity
      }
    }

    ServiceName.cas2 -> throw RuntimeException(
      "CAS2 now has its own " +
        "Cas2ApplicationsController",
    )
  }

  @Transactional
  override fun applicationsApplicationIdPut(applicationId: UUID, body: UpdateApplication): ResponseEntity<Application> {
    val user = userService.getUserForRequest()

    val serializedData = objectMapper.writeValueAsString(body.data)

    val applicationResult = when (body) {
      is UpdateApprovedPremisesApplication -> applicationService.updateApprovedPremisesApplication(
        applicationId = applicationId,
        ApplicationService.Cas1ApplicationUpdateFields(
          data = serializedData,
          isWomensApplication = body.isWomensApplication,
          isPipeApplication = body.isPipeApplication,
          isEmergencyApplication = body.isEmergencyApplication,
          isEsapApplication = body.isEsapApplication,
          apType = body.apType,
          releaseType = body.releaseType?.name,
          arrivalDate = body.arrivalDate,
          isInapplicable = body.isInapplicable,
          noticeType = body.noticeType,
        ),
        userForRequest = user,
      )

      is UpdateTemporaryAccommodationApplication -> applicationService.updateTemporaryAccommodationApplication(
        applicationId = applicationId,
        data = serializedData,
      )

      else -> throw RuntimeException("Unsupported UpdateApplication type: ${body::class.qualifiedName}")
    }

    val updatedApplication = extractEntityFromCasResult(applicationResult)

    return ResponseEntity.ok(getPersonDetailAndTransform(updatedApplication, user))
  }

  override fun applicationsApplicationIdNotesPost(
    applicationId: UUID,
    body: NewApplicationTimelineNote,
  ): ResponseEntity<ApplicationTimelineNote> {
    val user = userService.getUserForRequest()
    val savedNote = applicationTimelineNoteService.saveApplicationTimelineNote(applicationId, body.note, user)

    return ResponseEntity.ok(applicationTimelineNoteTransformer.transformJpaToApi(savedNote))
  }

  override fun applicationsApplicationIdWithdrawalPost(applicationId: UUID, body: NewWithdrawal): ResponseEntity<Unit> {
    val user = userService.getUserForRequest()

    return ResponseEntity.ok(
      extractEntityFromCasResult(
        cas1WithdrawableService.withdrawApplication(
          applicationId = applicationId,
          user = user,
          withdrawalReason = body.reason.value,
          otherReason = body.otherReason,
        ),
      ),
    )
  }

  override fun applicationsApplicationIdTimelineGet(applicationId: UUID, xServiceName: ServiceName): ResponseEntity<List<TimelineEvent>> {
    if (xServiceName != ServiceName.approvedPremises) {
      throw NotImplementedProblem("Timeline is only supported for Approved Premises applications")
    }
    val events = applicationService.getApplicationTimeline(applicationId)
    return ResponseEntity(events, HttpStatus.OK)
  }

  override fun applicationsApplicationIdRequestsForPlacementGet(applicationId: UUID): ResponseEntity<List<RequestForPlacement>> {
    return ResponseEntity.ok(
      extractEntityFromCasResult(cas1RequestForPlacementService.getRequestsForPlacementByApplication(applicationId, userService.getUserForRequest())),
    )
  }

  override fun applicationsApplicationIdRequestsForPlacementRequestForPlacementIdGet(
    applicationId: UUID,
    requestForPlacementId: UUID,
  ): ResponseEntity<RequestForPlacement> {
    val application = applicationService.getApplication(applicationId) ?: throw NotFoundProblem(applicationId, "Application")

    return ResponseEntity.ok(
      extractEntityFromCasResult(cas1RequestForPlacementService.getRequestForPlacement(application, requestForPlacementId, userService.getUserForRequest())),
    )
  }

  override fun applicationsApplicationIdSubmissionPost(
    applicationId: UUID,
    submitApplication: SubmitApplication,
  ): ResponseEntity<Unit> {
    val deliusPrincipal = httpAuthService.getDeliusPrincipalOrThrow()
    val username = deliusPrincipal.name

    val submitResult = when (submitApplication) {
      is SubmitApprovedPremisesApplication -> {
        var apAreaId = submitApplication.apAreaId

        if (apAreaId == null) {
          val user = userService.getUserForRequest()
          apAreaId = user.apArea!!.id
        }
        applicationService.submitApprovedPremisesApplication(
          applicationId,
          submitApplication,
          username,
          apAreaId,
        )
      }

      is SubmitTemporaryAccommodationApplication ->
        applicationService.submitTemporaryAccommodationApplication(applicationId, submitApplication)

      else -> throw RuntimeException("Unsupported SubmitApplication type: ${submitApplication::class.qualifiedName}")
    }

    val validationResult = when (submitResult) {
      is AuthorisableActionResult.NotFound -> throw NotFoundProblem(applicationId, "Application")
      is AuthorisableActionResult.Unauthorised -> throw ForbiddenProblem()
      is AuthorisableActionResult.Success -> submitResult.entity
    }

    when (validationResult) {
      is ValidatableActionResult.GeneralValidationError ->
        throw BadRequestProblem(errorDetail = validationResult.message)

      is ValidatableActionResult.FieldValidationError ->
        throw BadRequestProblem(invalidParams = validationResult.validationMessages)

      is ValidatableActionResult.ConflictError ->
        throw ConflictProblem(id = validationResult.conflictingEntityId, conflictReason = validationResult.message)

      is ValidatableActionResult.Success -> Unit
    }

    return ResponseEntity(HttpStatus.OK)
  }

  override fun applicationsApplicationIdDocumentsGet(applicationId: UUID): ResponseEntity<List<Document>> {
    val deliusPrincipal = httpAuthService.getDeliusPrincipalOrThrow()
    val username = deliusPrincipal.name
    val application = extractEntityFromCasResult(applicationService.getApplicationForUsername(applicationId, username))

    val documentsResult = getDocuments(application.crn)
    val apiDocuments = documentTransformer.transformToApi(documentsResult)

    return ResponseEntity(apiDocuments, HttpStatus.OK)
  }

  private fun getDocuments(crn: String): List<APDeliusDocument> {
    return when (val result = offenderService.getDocumentsFromApDeliusApi(crn)) {
      is AuthorisableActionResult.NotFound -> throw NotFoundProblem(crn, "Documents")
      is AuthorisableActionResult.Unauthorised -> throw ForbiddenProblem()
      is AuthorisableActionResult.Success -> result.entity
    }
  }

  override fun applicationsApplicationIdAppealsAppealIdGet(
    applicationId: UUID,
    appealId: UUID,
  ): ResponseEntity<Appeal> {
    val user = userService.getUserForRequest()
    val application =
      extractEntityFromCasResult(applicationService.getApplicationForUsername(applicationId, user.deliusUsername))

    val appeal = when (val getAppealResult = appealService.getAppeal(appealId, application)) {
      is AuthorisableActionResult.NotFound -> throw NotFoundProblem(appealId, "Appeal")
      is AuthorisableActionResult.Unauthorised -> throw ForbiddenProblem()
      is AuthorisableActionResult.Success -> getAppealResult.entity
    }
    return ResponseEntity.ok(appealTransformer.transformJpaToApi(appeal))
  }

  override fun applicationsApplicationIdAppealsPost(applicationId: UUID, body: NewAppeal): ResponseEntity<Appeal> {
    val user = userService.getUserForRequest()
    val application =
      extractEntityFromCasResult(applicationService.getApplicationForUsername(applicationId, user.deliusUsername))

    val assessment = application.getLatestAssessment()
      ?: throw ConflictProblem(
        applicationId,
        "An appeal cannot be created when the application does not have an assessment",
      )

    val createAppealResult = appealService.createAppeal(
      appealDate = body.appealDate,
      appealDetail = body.appealDetail,
      decision = body.decision,
      decisionDetail = body.decisionDetail,
      application = application,
      assessment = assessment,
      createdBy = user,
    )

    val validationResult = when (createAppealResult) {
      is AuthorisableActionResult.NotFound -> throw NotFoundProblem(applicationId, "Application")
      is AuthorisableActionResult.Unauthorised -> throw ForbiddenProblem()
      is AuthorisableActionResult.Success -> createAppealResult.entity
    }

    val appeal = when (validationResult) {
      is ValidatableActionResult.GeneralValidationError -> throw BadRequestProblem(errorDetail = validationResult.message)
      is ValidatableActionResult.FieldValidationError -> throw BadRequestProblem(invalidParams = validationResult.validationMessages)
      is ValidatableActionResult.ConflictError -> throw ConflictProblem(id = validationResult.conflictingEntityId, conflictReason = validationResult.message)
      is ValidatableActionResult.Success -> validationResult.entity
    }

    return ResponseEntity
      .created(URI.create("/applications/${application.id}/appeals/${appeal.id}"))
      .body(appealTransformer.transformJpaToApi(appeal))
  }

  override fun applicationsApplicationIdAssessmentGet(applicationId: UUID): ResponseEntity<Assessment> {
    val user = userService.getUserForRequest()

    val assessment = when (
      val applicationResult =
        assessmentService.getAssessmentForUserAndApplication(
          user,
          applicationId,
        )
    ) {
      is AuthorisableActionResult.NotFound -> throw NotFoundProblem(applicationId, "Assessment")
      is AuthorisableActionResult.Unauthorised -> throw ForbiddenProblem()
      is AuthorisableActionResult.Success -> applicationResult.entity
    }

    val personInfo = offenderService.getPersonInfoResult(assessment.application.crn, user.deliusUsername, false)

    return ResponseEntity.ok(assessmentTransformer.transformJpaToApi(assessment, personInfo))
  }

  override fun applicationsApplicationIdWithdrawablesWithNotesGet(
    applicationId: UUID,
    xServiceName: ServiceName,
  ): ResponseEntity<Withdrawables> {
    val result = getWithdrawables(applicationId, xServiceName)

    return ResponseEntity.ok(
      Withdrawables(
        notes = result.notes,
        withdrawables = result.withdrawables.map { withdrawableTransformer.toApi(it) },
      ),
    )
  }

  override fun applicationsApplicationIdWithdrawablesGet(
    applicationId: UUID,
    xServiceName: ServiceName,
  ): ResponseEntity<List<Withdrawable>> {
    val withdrawables = getWithdrawables(applicationId, xServiceName).withdrawables

    return ResponseEntity.ok(withdrawables.map { withdrawableTransformer.toApi(it) })
  }

  @SuppressWarnings("ThrowsCount")
  private fun getWithdrawables(applicationId: UUID, xServiceName: ServiceName): WithdrawableEntitiesWithNotes {
    if (xServiceName != ServiceName.approvedPremises) {
      throw ForbiddenProblem()
    }

    val user = userService.getUserForRequest()
    val application =
      extractEntityFromCasResult(applicationService.getApplicationForUsername(applicationId, user.deliusUsername))

    if (application !is ApprovedPremisesApplicationEntity) {
      throw RuntimeException("Unsupported Application type: ${application::class.qualifiedName}")
    }

    return cas1WithdrawableService.allDirectlyWithdrawables(application, user)
  }

  private fun getPersonDetailAndTransform(
    application: ApplicationEntity,
    user: UserEntity,
    ignoreLaoRestrictions: Boolean = false,
  ): Application {
    val personInfo = offenderService.getPersonInfoResult(application.crn, user.deliusUsername, ignoreLaoRestrictions)

    return applicationsTransformer.transformJpaToApi(application, personInfo)
  }

  private fun getPersonDetailAndTransformToSummary(
    applications: List<JPAApplicationSummary>,
    user: UserEntity,
    ignoreLaoRestrictions: Boolean = false,
  ): List<ApplicationSummary> {
    val crns = applications.map { it.getCrn() }
    val personInfoResults = offenderService.getPersonInfoResults(crns.toSet(), user.deliusUsername, ignoreLaoRestrictions)

    return applications.map {
      val crn = it.getCrn()
      applicationsTransformer.transformDomainToApiSummary(
        it,
        personInfoResults.firstOrNull { it.crn == crn } ?: PersonInfoResult.Unknown(crn),
      )
    }
  }

  private fun getPersonDetailAndTransform(
    offlineApplication: OfflineApplicationEntity,
    user: UserEntity,
    ignoreLaoRestrictions: Boolean = false,
  ): Application {
    val personInfo = offenderService.getPersonInfoResult(offlineApplication.crn, user.deliusUsername, ignoreLaoRestrictions)

    return applicationsTransformer.transformJpaToApi(offlineApplication, personInfo)
  }
}
