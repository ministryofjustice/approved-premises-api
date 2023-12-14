package uk.gov.justice.digital.hmpps.approvedpremisesapi.controller.cas2

import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.cas2.ApplicationsCas2Delegate
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Application
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ApplicationSummary
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas2NewApplication
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.UpdateApplication
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.Cas2ApplicationEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.problem.BadRequestProblem
import uk.gov.justice.digital.hmpps.approvedpremisesapi.problem.ConflictProblem
import uk.gov.justice.digital.hmpps.approvedpremisesapi.problem.ForbiddenProblem
import uk.gov.justice.digital.hmpps.approvedpremisesapi.problem.NotFoundProblem
import uk.gov.justice.digital.hmpps.approvedpremisesapi.results.AuthorisableActionResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.results.ValidatableActionResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.HttpAuthService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.NomisUserService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas2.ApplicationService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.cas2.ApplicationsTransformer
import java.net.URI
import java.util.UUID
import javax.transaction.Transactional

@Service(
  "uk.gov.justice.digital.hmpps.approvedpremisesapi.controller.cas2" +
    ".ApplicationsController",
)
class ApplicationsController(
  private val httpAuthService: HttpAuthService,
  private val applicationService: ApplicationService,
  private val applicationsTransformer: ApplicationsTransformer,
  private val objectMapper: ObjectMapper,
  private val userService: NomisUserService,
) : ApplicationsCas2Delegate {

  override fun applicationsGet(): ResponseEntity<List<ApplicationSummary>> {
    val user = userService.getUserForRequest()

    val applications = applicationService.getAllApplicationsForUser(user)

    return ResponseEntity.ok(applications.map { getPersonDetailAndTransformToSummary(it) })
  }

  override fun applicationsApplicationIdGet(applicationId: UUID):
    ResponseEntity<Application> {
    val user = userService.getUserForRequest()

    val application = when (
      val applicationResult = applicationService
        .getApplicationForUsername(applicationId, user.nomisUsername)
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

  @Transactional
  override fun applicationsPost(body: Cas2NewApplication):
    ResponseEntity<Application> {
    val nomisPrincipal = httpAuthService.getNomisPrincipalOrThrow()
    val user = userService.getUserForRequest()

    val applicationResult = applicationService.createApplication(
      body,
      user,
      nomisPrincipal.token.tokenValue,
    )

    val application = when (applicationResult) {
      is ValidatableActionResult.GeneralValidationError -> throw BadRequestProblem(errorDetail = applicationResult.message)
      is ValidatableActionResult.FieldValidationError -> throw BadRequestProblem(invalidParams = applicationResult.validationMessages)
      is ValidatableActionResult.ConflictError -> throw ConflictProblem(id = applicationResult.conflictingEntityId, conflictReason = applicationResult.message)
      is ValidatableActionResult.Success -> applicationResult.entity
    }

    return ResponseEntity
      .created(URI.create("/cas2/applications/${application.id}"))
      .body(applicationsTransformer.transformJpaToApi(application))
  }

  @Transactional
  override fun applicationsApplicationIdPut(
    applicationId: UUID,
    body:
      UpdateApplication,
  ): ResponseEntity<Application> {
    val user = userService.getUserForRequest()

    val serializedData = objectMapper.writeValueAsString(body.data)

    val applicationResult = applicationService.updateApplication(
      applicationId =
      applicationId,
      data = serializedData,
      username = user.nomisUsername,
    )

    val validationResult = when (applicationResult) {
      is AuthorisableActionResult.NotFound -> throw NotFoundProblem(applicationId, "Application")
      is AuthorisableActionResult.Unauthorised -> throw ForbiddenProblem()
      is AuthorisableActionResult.Success -> applicationResult.entity
    }

    val updatedApplication = when (validationResult) {
      is ValidatableActionResult.GeneralValidationError -> throw BadRequestProblem(errorDetail = validationResult.message)
      is ValidatableActionResult.FieldValidationError -> throw BadRequestProblem(invalidParams = validationResult.validationMessages)
      is ValidatableActionResult.ConflictError -> throw ConflictProblem(id = validationResult.conflictingEntityId, conflictReason = validationResult.message)
      is ValidatableActionResult.Success -> validationResult.entity
    }

    return ResponseEntity.ok(getPersonDetailAndTransform(updatedApplication))
  }

  private fun getPersonDetailAndTransformToSummary(
    application: uk.gov.justice.digital
    .hmpps.approvedpremisesapi.jpa.entity.Cas2ApplicationSummary,
  ):
    ApplicationSummary {
    return applicationsTransformer.transformJpaSummaryToSummary(application)
  }

  private fun getPersonDetailAndTransform(
    application: Cas2ApplicationEntity,
  ): Application {
    return applicationsTransformer.transformJpaToApi(application)
  }
}
