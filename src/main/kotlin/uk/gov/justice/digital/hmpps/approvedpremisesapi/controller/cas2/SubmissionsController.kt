package uk.gov.justice.digital.hmpps.approvedpremisesapi.controller.cas2

import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Service
import org.zalando.problem.AbstractThrowableProblem
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.cas2.SubmissionsCas2Delegate
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas2ApplicationStatusUpdate
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas2SubmittedApplication
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas2SubmittedApplicationSummary
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.SubmitCas2Application
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.Cas2ApplicationEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.Cas2ApplicationSummary
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.Cas2StatusUpdateEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.problem.BadRequestProblem
import uk.gov.justice.digital.hmpps.approvedpremisesapi.problem.ConflictProblem
import uk.gov.justice.digital.hmpps.approvedpremisesapi.problem.ForbiddenProblem
import uk.gov.justice.digital.hmpps.approvedpremisesapi.problem.NotFoundProblem
import uk.gov.justice.digital.hmpps.approvedpremisesapi.results.AuthorisableActionResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.results.ValidatableActionResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.ExternalUserService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.HttpAuthService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas2.ApplicationService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas2.OffenderService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas2.StatusUpdateService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.cas2.SubmittedApplicationTransformer
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.cas2.getFullInfoForPersonOrThrow
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.cas2.getInfoForPersonOrThrowInternalServerError
import java.util.UUID

@Service("Cas2SubmissionsController")
class SubmissionsController(
  private val httpAuthService: HttpAuthService,
  private val applicationService: ApplicationService,
  private val applicationTransformer: SubmittedApplicationTransformer,
  private val offenderService: OffenderService,
  private val externalUserService: ExternalUserService,
  private val statusUpdateService: StatusUpdateService,
) : SubmissionsCas2Delegate {

  override fun submissionsGet(): ResponseEntity<List<Cas2SubmittedApplicationSummary>> {
    httpAuthService.getCas2AuthenticatedPrincipalOrThrow()
    ensureExternalUserPersisted()
    val applications = applicationService.getAllSubmittedApplicationsForAssessor()
    return ResponseEntity.ok(applications.map { getPersonDetailAndTransformToSummary(it) })
  }

  override fun submissionsApplicationIdGet(applicationId: UUID): ResponseEntity<Cas2SubmittedApplication> {
    httpAuthService.getCas2AuthenticatedPrincipalOrThrow()

    val application = when (
      val applicationResult = applicationService.getSubmittedApplicationForAssessor(applicationId)
    ) {
      is AuthorisableActionResult.NotFound -> null
      is AuthorisableActionResult.Unauthorised -> throw ForbiddenProblem()
      is AuthorisableActionResult.Success -> applicationResult.entity
    }

    if (application != null) {
      return ResponseEntity.ok(getPersonDetailAndTransform(application))
    }
    throw NotFoundProblem(applicationId, "Application")
  }

  override fun submissionsPost(
    submitApplication: SubmitCas2Application,
  ): ResponseEntity<Unit> {
    httpAuthService.getNomisPrincipalOrThrow()
    val submitResult = applicationService.submitApplication(submitApplication)

    val validationResult = when (submitResult) {
      is AuthorisableActionResult.NotFound -> throw NotFoundProblem(submitApplication.applicationId, "Application")
      is AuthorisableActionResult.Unauthorised -> throw ForbiddenProblem()
      is AuthorisableActionResult.Success -> submitResult.entity
    }

    when (validationResult) {
      is ValidatableActionResult.GeneralValidationError -> throw BadRequestProblem(errorDetail = validationResult.message)
      is ValidatableActionResult.FieldValidationError -> throw BadRequestProblem(invalidParams = validationResult.validationMessages)
      is ValidatableActionResult.ConflictError -> throw ConflictProblem(id = validationResult.conflictingEntityId, conflictReason = validationResult.message)
      is ValidatableActionResult.Success -> Unit
    }

    return ResponseEntity(HttpStatus.OK)
  }

  override fun submissionsApplicationIdStatusUpdatesPost(
    applicationId: UUID,
    statusUpdate: Cas2ApplicationStatusUpdate,
  ): ResponseEntity<Unit> {
    val result = statusUpdateService.create(
      applicationId = applicationId,
      statusUpdate = statusUpdate,
      assessor = externalUserService.getUserForRequest(),
    )

    processAuthorisationFor(applicationId, result)
      .run { processValidation(this as ValidatableActionResult<Cas2StatusUpdateEntity>) }

    return ResponseEntity(HttpStatus.CREATED)
  }

  private fun processAuthorisationFor(
    applicationId: UUID,
    result: AuthorisableActionResult<ValidatableActionResult<Cas2StatusUpdateEntity>>,
  ): Any {
    return when (result) {
      is AuthorisableActionResult.NotFound -> throwProblem(NotFoundProblem(applicationId, "Cas2Application"))
      is AuthorisableActionResult.Unauthorised -> throwProblem(ForbiddenProblem())
      is AuthorisableActionResult.Success -> result.entity
    }
  }

  private fun processValidation(validationResult: ValidatableActionResult<Cas2StatusUpdateEntity>) {
    return when (validationResult) {
      is ValidatableActionResult.GeneralValidationError -> throwProblem(BadRequestProblem(errorDetail = validationResult.message))
      is ValidatableActionResult.FieldValidationError -> throwProblem(BadRequestProblem(invalidParams = validationResult.validationMessages))
      is ValidatableActionResult.ConflictError -> throwProblem(ConflictProblem(id = validationResult.conflictingEntityId, conflictReason = validationResult.message))
      is ValidatableActionResult.Success -> Unit
    }
  }

  private fun throwProblem(problem: AbstractThrowableProblem) {
    throw problem
  }

  private fun ensureExternalUserPersisted() {
    externalUserService.getUserForRequest()
  }

  private fun getPersonDetailAndTransformToSummary(
    application: Cas2ApplicationSummary,
  ):
    Cas2SubmittedApplicationSummary {
    val personInfo = offenderService.getInfoForPersonOrThrowInternalServerError(application.getCrn())

    return applicationTransformer.transformJpaSummaryToApiRepresentation(
      application,
      personInfo,
    )
  }

  private fun getPersonDetailAndTransform(
    application: Cas2ApplicationEntity,
  ): Cas2SubmittedApplication {
    val personInfo = offenderService.getFullInfoForPersonOrThrow(application.crn)

    return applicationTransformer.transformJpaToApiRepresentation(
      application,
      personInfo,
    )
  }
}
