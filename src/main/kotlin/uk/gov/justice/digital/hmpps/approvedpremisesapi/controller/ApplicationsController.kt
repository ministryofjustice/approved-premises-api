package uk.gov.justice.digital.hmpps.approvedpremisesapi.controller

import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.ApplicationsApiDelegate
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Application
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ApplicationSortField
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ApplicationSummary
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ApplicationTimelineNote
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Assessment
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Document
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.NewApplication
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.NewWithdrawal
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.PlacementApplication
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ServiceName
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.SortDirection
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.SubmitApplication
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.SubmitApprovedPremisesApplication
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.SubmitTemporaryAccommodationApplication
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.TimelineEvent
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.UpdateApplication
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.UpdateApprovedPremisesApplication
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.UpdateTemporaryAccommodationApplication
import uk.gov.justice.digital.hmpps.approvedpremisesapi.config.AuthAwareAuthenticationToken
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApplicationEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApprovedPremisesApplicationEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.OfflineApplicationEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.TemporaryAccommodationApplicationEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserRole
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.PersonInfoResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.problem.BadRequestProblem
import uk.gov.justice.digital.hmpps.approvedpremisesapi.problem.ConflictProblem
import uk.gov.justice.digital.hmpps.approvedpremisesapi.problem.ForbiddenProblem
import uk.gov.justice.digital.hmpps.approvedpremisesapi.problem.NotFoundProblem
import uk.gov.justice.digital.hmpps.approvedpremisesapi.results.AuthorisableActionResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.results.ValidatableActionResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.ApplicationService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.AssessmentService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.HttpAuthService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.OffenderService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.PlacementApplicationService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.UserService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.ApplicationsTransformer
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.AssessmentTransformer
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.DocumentTransformer
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.PlacementApplicationTransformer
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.extractEntityFromNestedAuthorisableValidatableActionResult
import java.net.URI
import java.util.UUID
import javax.transaction.Transactional
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApplicationSummary as JPAApplicationSummary

@Service
@Suppress("LongParameterList", "TooManyFunctions")
class ApplicationsController(
  private val httpAuthService: HttpAuthService,
  private val applicationService: ApplicationService,
  private val placementApplicationService: PlacementApplicationService,
  private val applicationsTransformer: ApplicationsTransformer,
  private val assessmentTransformer: AssessmentTransformer,
  private val placementApplicationTransformer: PlacementApplicationTransformer,
  private val objectMapper: ObjectMapper,
  private val offenderService: OffenderService,
  private val documentTransformer: DocumentTransformer,
  private val assessmentService: AssessmentService,
  private val userService: UserService,
) : ApplicationsApiDelegate {

  override fun applicationsGet(xServiceName: ServiceName?): ResponseEntity<List<ApplicationSummary>> {
    val serviceName = xServiceName ?: ServiceName.approvedPremises

    val user = userService.getUserForRequest()

    val applications = applicationService.getAllApplicationsForUsername(user.deliusUsername, serviceName)

    return ResponseEntity.ok(applications.map { getPersonDetailAndTransformToSummary(it, user) })
  }

  override fun applicationsAllGet(
    xServiceName: ServiceName,
    page: Int?,
    crn: String?,
    sortDirection: SortDirection?,
    applicationSortField: ApplicationSortField?,
  ): ResponseEntity<List<ApplicationSummary>> {
    if (xServiceName != ServiceName.approvedPremises) {
      throw ForbiddenProblem()
    }
    val user = userService.getUserForRequest()
    val (applications, metadata) =
      applicationService.getAllApprovedPremisesApplications(page, crn, sortDirection, applicationSortField)

    return ResponseEntity.ok().headers(
      metadata?.toHeaders(),
    ).body(
      applications.map {
        getPersonDetailAndTransformToSummary(it, user)
      },
    )
  }

  override fun applicationsApplicationIdGet(applicationId: UUID): ResponseEntity<Application> {
    val user = userService.getUserForRequest()

    val application = when (
      val applicationResult =
        applicationService.getApplicationForUsername(applicationId, user.deliusUsername)
    ) {
      is AuthorisableActionResult.NotFound -> null
      is AuthorisableActionResult.Unauthorised -> throw ForbiddenProblem()
      is AuthorisableActionResult.Success -> applicationResult.entity
    }

    if (application != null) {
      return ResponseEntity.ok(getPersonDetailAndTransform(application, user))
    }

    val offlineApplication = when (
      val offlineApplicationResult =
        applicationService.getOfflineApplicationForUsername(applicationId, user.deliusUsername)
    ) {
      is AuthorisableActionResult.Unauthorised -> throw ForbiddenProblem()
      is AuthorisableActionResult.NotFound -> throw NotFoundProblem(applicationId, "Application")
      is AuthorisableActionResult.Success -> offlineApplicationResult.entity
    }

    return ResponseEntity.ok(getPersonDetailAndTransform(offlineApplication, user))
  }

  @Transactional
  override fun applicationsPost(body: NewApplication, xServiceName: ServiceName?, createWithRisks: Boolean?):
    ResponseEntity<Application> {
    val deliusPrincipal = httpAuthService.getDeliusPrincipalOrThrow()
    val user = userService.getUserForRequest()

    val personInfo = when (val personInfoResult = offenderService.getInfoForPerson(body.crn, user.deliusUsername, false)) {
      is PersonInfoResult.NotFound, is PersonInfoResult.Unknown -> throw NotFoundProblem(personInfoResult.crn, "Offender")
      is PersonInfoResult.Success.Restricted -> throw ForbiddenProblem()
      is PersonInfoResult.Success.Full -> personInfoResult
    }

    val applicationResult = createApplication(
      xServiceName ?: ServiceName.approvedPremises,
      personInfo,
      user,
      deliusPrincipal,
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
    deliusPrincipal: AuthAwareAuthenticationToken,
    body: NewApplication,
    createWithRisks: Boolean?,
  ): ValidatableActionResult<ApplicationEntity> = when (serviceName) {
    ServiceName.approvedPremises ->
      applicationService.createApprovedPremisesApplication(
        personInfo.offenderDetailSummary,
        user,
        deliusPrincipal.token.tokenValue,
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
            deliusPrincipal.token.tokenValue,
            body.convictionId,
            body.deliusEventNumber,
            body.offenceId,
            createWithRisks,
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
        data = serializedData,
        isWomensApplication = body.isWomensApplication,
        isPipeApplication = body.isPipeApplication,
        isEmergencyApplication = body.isEmergencyApplication,
        isEsapApplication = body.isEsapApplication,
        releaseType = body.releaseType?.name,
        arrivalDate = body.arrivalDate,
        isInapplicable = body.isInapplicable,
      )
      is UpdateTemporaryAccommodationApplication -> applicationService.updateTemporaryAccommodationApplication(
        applicationId = applicationId,
        data = serializedData,
      )
      else -> throw RuntimeException("Unsupported UpdateApplication type: ${body::class.qualifiedName}")
    }

    val validationResult = when (applicationResult) {
      is AuthorisableActionResult.NotFound -> throw NotFoundProblem(applicationId, "Application")
      is AuthorisableActionResult.Unauthorised -> throw ForbiddenProblem()
      is AuthorisableActionResult.Success -> applicationResult.entity
    }

    val updatedApplication = when (validationResult) {
      is ValidatableActionResult.GeneralValidationError ->
        throw BadRequestProblem(errorDetail = validationResult.message)
      is ValidatableActionResult.FieldValidationError ->
        throw BadRequestProblem(invalidParams = validationResult.validationMessages)
      is ValidatableActionResult.ConflictError ->
        throw ConflictProblem(id = validationResult.conflictingEntityId, conflictReason = validationResult.message)
      is ValidatableActionResult.Success -> validationResult.entity
    }

    return ResponseEntity.ok(getPersonDetailAndTransform(updatedApplication, user))
  }

  override fun applicationsApplicationIdNotesPost(
    applicationId: UUID,
    body: ApplicationTimelineNote,
  ): ResponseEntity<ApplicationTimelineNote> {
    val savedNote = applicationService.addNoteToApplication(applicationId, body)
    return ResponseEntity.ok(savedNote)
  }

  override fun applicationsApplicationIdWithdrawalPost(applicationId: UUID, body: NewWithdrawal): ResponseEntity<Unit> {
    val user = userService.getUserForRequest()

    extractEntityFromNestedAuthorisableValidatableActionResult(
      applicationService.withdrawApprovedPremisesApplication(
        applicationId = applicationId,
        user = user,
        withdrawalReason = body.reason.value,
        otherReason = body.otherReason,
      ),
    )

    return ResponseEntity.ok(Unit)
  }

  override fun applicationsApplicationIdTimelineGet(applicationId: UUID, xServiceName: ServiceName):
    ResponseEntity<List<TimelineEvent>> {
    val user = userService.getUserForRequest()
    if (xServiceName != ServiceName.approvedPremises ||
      !user.hasAnyRole(UserRole.CAS1_ADMIN, UserRole.CAS1_WORKFLOW_MANAGER)
    ) {
      throw ForbiddenProblem()
    }
    val events = applicationService.getApplicationTimeline(applicationId)
    return ResponseEntity(events, HttpStatus.OK)
  }

  override fun applicationsApplicationIdSubmissionPost(
    applicationId: UUID,
    submitApplication: SubmitApplication,
  ): ResponseEntity<Unit> {
    val deliusPrincipal = httpAuthService.getDeliusPrincipalOrThrow()
    val username = deliusPrincipal.name

    val submitResult = when (submitApplication) {
      is SubmitApprovedPremisesApplication ->
        applicationService.submitApprovedPremisesApplication(
          applicationId,
          submitApplication,
          username,
          deliusPrincipal.token.tokenValue,
        )
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

    val application = when (
      val applicationResult =
        applicationService.getApplicationForUsername(applicationId, username)
    ) {
      is AuthorisableActionResult.NotFound -> throw NotFoundProblem(applicationId, "Application")
      is AuthorisableActionResult.Unauthorised -> throw ForbiddenProblem()
      is AuthorisableActionResult.Success -> applicationResult.entity
    }

    val convictionId = when (application) {
      is ApprovedPremisesApplicationEntity -> application.convictionId
      is TemporaryAccommodationApplicationEntity -> application.convictionId
      else -> throw RuntimeException("Unsupported Application type: ${application::class.qualifiedName}")
    }

    val documents = when (val documentsResult = offenderService.getDocuments(application.crn)) {
      is AuthorisableActionResult.NotFound -> throw NotFoundProblem(application.crn, "Person")
      is AuthorisableActionResult.Unauthorised -> throw ForbiddenProblem()
      is AuthorisableActionResult.Success -> documentsResult.entity
    }

    return ResponseEntity(documentTransformer.transformToApi(documents, convictionId), HttpStatus.OK)
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

    val personInfo = offenderService.getInfoForPerson(assessment.application.crn, user.deliusUsername, false)

    return ResponseEntity.ok(assessmentTransformer.transformJpaToApi(assessment, personInfo))
  }

  override fun applicationsApplicationIdPlacementApplicationsGet(
    applicationId: UUID,
    xServiceName: ServiceName,
  ): ResponseEntity<List<PlacementApplication>> {
    if (xServiceName != ServiceName.approvedPremises) {
      throw ForbiddenProblem()
    }
    val placementApplicationEntities =
      placementApplicationService.getAllPlacementApplicationEntitiesForApplicationId(applicationId)
    val placementApplications = placementApplicationEntities.map {
      placementApplicationTransformer.transformJpaToApi(it)
    }

    return ResponseEntity.ok(placementApplications)
  }

  private fun getPersonDetailAndTransform(application: ApplicationEntity, user: UserEntity): Application {
    val personInfo = offenderService.getInfoForPerson(application.crn, user.deliusUsername, false)

    return applicationsTransformer.transformJpaToApi(application, personInfo)
  }

  private fun getPersonDetailAndTransformToSummary(
    application: JPAApplicationSummary,
    user: UserEntity,
  ): ApplicationSummary {
    val personInfo = offenderService.getInfoForPerson(application.getCrn(), user.deliusUsername, false)

    return applicationsTransformer.transformDomainToApiSummary(application, personInfo)
  }

  private fun getPersonDetailAndTransform(offlineApplication: OfflineApplicationEntity, user: UserEntity): Application {
    val personInfo = offenderService.getInfoForPerson(offlineApplication.crn, user.deliusUsername, false)

    return applicationsTransformer.transformJpaToApi(offlineApplication, personInfo)
  }
}
