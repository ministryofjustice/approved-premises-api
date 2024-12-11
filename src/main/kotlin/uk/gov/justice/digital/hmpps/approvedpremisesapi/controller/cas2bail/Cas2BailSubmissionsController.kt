package uk.gov.justice.digital.hmpps.approvedpremisesapi.controller.cas2bail

import jakarta.transaction.Transactional
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.cas2bail.SubmissionsCas2bailDelegate
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas2SubmittedApplication
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas2SubmittedApplicationSummary
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.SortDirection
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.SubmitCas2Application
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.Cas2ApplicationEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.Cas2ApplicationSummaryEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.problem.BadRequestProblem
import uk.gov.justice.digital.hmpps.approvedpremisesapi.problem.ConflictProblem
import uk.gov.justice.digital.hmpps.approvedpremisesapi.problem.ForbiddenProblem
import uk.gov.justice.digital.hmpps.approvedpremisesapi.problem.NotFoundProblem
import uk.gov.justice.digital.hmpps.approvedpremisesapi.results.AuthorisableActionResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.results.ValidatableActionResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.ExternalUserService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.HttpAuthService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.NomisUserService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas2.ApplicationService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas2.OffenderService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.cas2.SubmissionsTransformer
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.PageCriteria
import java.util.UUID

@Service("Cas2BailSubmissionsController")
class Cas2BailSubmissionsController(
  private val httpAuthService: HttpAuthService,
  private val applicationService: ApplicationService,
  private val submissionsTransformer: SubmissionsTransformer,
  private val offenderService: OffenderService,
  private val externalUserService: ExternalUserService,
  private val nomisUserService: NomisUserService,
) : SubmissionsCas2bailDelegate {

  override fun submissionsGet(page: Int?): ResponseEntity<List<Cas2SubmittedApplicationSummary>> {
    val principal = httpAuthService.getCas2BailAuthenticatedPrincipalOrThrow()
    if (principal.isExternalUser()) {
      ensureExternalUserPersisted()
    } else {
      ensureNomisUserPersisted()
    }

    val sortDirection = SortDirection.asc
    val sortBy = "submittedAt"

    val (applications, metadata) = applicationService.getAllSubmittedApplicationsForAssessor(PageCriteria(sortBy, sortDirection, page))

    return ResponseEntity.ok().headers(
      metadata?.toHeaders(),
    ).body(getPersonNamesAndTransformToSummaries(applications))
  }

  override fun submissionsApplicationIdGet(applicationId: UUID): ResponseEntity<Cas2SubmittedApplication> {
    val principal = httpAuthService.getCas2BailAuthenticatedPrincipalOrThrow()
    if (principal.isExternalUser()) {
      ensureExternalUserPersisted()
    } else {
      ensureNomisUserPersisted()
    }

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

  @Transactional
  override fun submissionsPost(
    submitCas2BailApplication: SubmitCas2Application,
  ): ResponseEntity<Unit> {
    val user = nomisUserService.getUserForRequest()
    val submitResult = applicationService.submitApplication(submitCas2BailApplication, user)

    val validationResult = when (submitResult) {
      is AuthorisableActionResult.NotFound -> throw NotFoundProblem(submitCas2BailApplication.applicationId, "Application")
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

  private fun ensureExternalUserPersisted() {
    externalUserService.getUserForRequest()
  }

  private fun ensureNomisUserPersisted() {
    nomisUserService.getUserForRequest()
  }

  private fun getPersonNamesAndTransformToSummaries(applicationSummaries: List<Cas2ApplicationSummaryEntity>): List<Cas2SubmittedApplicationSummary> {
    val crns = applicationSummaries.map { it.crn }

    val personNamesMap = offenderService.getMapOfPersonNamesAndCrns(crns)

    return applicationSummaries.map { application ->
      submissionsTransformer.transformJpaSummaryToApiRepresentation(application, personNamesMap[application.crn]!!)
    }
  }

  private fun getPersonDetailAndTransform(
    application: Cas2ApplicationEntity,
  ): Cas2SubmittedApplication {
    val personInfo = offenderService.getFullInfoForPersonOrThrow(application.crn)

    return submissionsTransformer.transformJpaToApiRepresentation(
      application,
      personInfo,
    )
  }
}
