package uk.gov.justice.digital.hmpps.approvedpremisesapi.controller.cas2v2

import jakarta.transaction.Transactional
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.cas2v2.SubmissionsCas2v2Delegate
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.*
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.cas2v2.Cas2v2ApplicationEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.cas2v2.Cas2v2ApplicationSummaryEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.problem.BadRequestProblem
import uk.gov.justice.digital.hmpps.approvedpremisesapi.problem.ConflictProblem
import uk.gov.justice.digital.hmpps.approvedpremisesapi.problem.ForbiddenProblem
import uk.gov.justice.digital.hmpps.approvedpremisesapi.problem.NotFoundProblem
import uk.gov.justice.digital.hmpps.approvedpremisesapi.results.AuthorisableActionResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.results.CasResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.ExternalUserService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.HttpAuthService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.NomisUserService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas2.OffenderService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas2v2.Cas2v2ApplicationService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.cas2v2.Cas2v2SubmissionsTransformer
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.PageCriteria
import java.util.UUID

@Service("Cas2v2SubmissionsController")
class Cas2v2SubmissionsController(
  private val httpAuthService: HttpAuthService,
  private val cas2v2ApplicationService: Cas2v2ApplicationService,
  private val cas2v2SubmissionsTransformer: Cas2v2SubmissionsTransformer,
  private val offenderService: OffenderService,
  private val externalUserService: ExternalUserService,
  private val nomisUserService: NomisUserService,
) : SubmissionsCas2v2Delegate {

  override fun submissionsGet(page: Int?): ResponseEntity<List<Cas2v2SubmittedApplicationSummary>> {
    val principal = httpAuthService.getCas2v2AuthenticatedPrincipalOrThrow()
    if (principal.isExternalUser()) {
      ensureExternalUserPersisted()
    } else {
      ensureNomisUserPersisted()
    }

    val sortDirection = SortDirection.asc
    val sortBy = "submittedAt"

    val (applications, metadata) = cas2v2ApplicationService.getAllSubmittedCas2v2ApplicationsForAssessor(PageCriteria(sortBy, sortDirection, page))

    return ResponseEntity.ok().headers(
      metadata?.toHeaders(),
    ).body(getPersonNamesAndTransformToSummaries(applications))
  }

  override fun submissionsApplicationIdGet(applicationId: UUID): ResponseEntity<Cas2v2SubmittedApplication> {
    val principal = httpAuthService.getCas2v2AuthenticatedPrincipalOrThrow()
    if (principal.isExternalUser()) {
      ensureExternalUserPersisted()
    } else {
      ensureNomisUserPersisted()
    }

    val application = when (
      val applicationResult = cas2v2ApplicationService.getSubmittedCas2v2ApplicationForAssessor(applicationId)
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
    submitCas2v2Application: SubmitCas2v2Application,
  ): ResponseEntity<Unit> {
    val user = nomisUserService.getUserForRequest()
    val submitResult = cas2v2ApplicationService.submitCas2v2Application(submitCas2v2Application, user)

    when (submitResult) {
      is CasResult.NotFound -> throw NotFoundProblem(submitCas2v2Application.applicationId, "Application")
      is CasResult.Unauthorised -> throw ForbiddenProblem()
      is CasResult.ConflictError -> throw ConflictProblem(id = submitResult.conflictingEntityId, conflictReason = submitResult.message)
      is CasResult.FieldValidationError -> throw BadRequestProblem(invalidParams = submitResult.validationMessages)
      is CasResult.GeneralValidationError -> throw BadRequestProblem(errorDetail = submitResult.message)
      is CasResult.Success -> return ResponseEntity(HttpStatus.OK)
    }

  }

  private fun ensureExternalUserPersisted() {
    externalUserService.getUserForRequest()
  }

  private fun ensureNomisUserPersisted() {
    nomisUserService.getUserForRequest()
  }

  private fun getPersonNamesAndTransformToSummaries(
    applicationSummaries: List<Cas2v2ApplicationSummaryEntity>
  ): List<Cas2v2SubmittedApplicationSummary> {
    val crns = applicationSummaries.map { it.crn }

    val personNamesMap = offenderService.getMapOfPersonNamesAndCrns(crns)

    return applicationSummaries.map { application ->
      cas2v2SubmissionsTransformer.transformJpaSummaryToApiRepresentation(application, personNamesMap[application.crn]!!)
    }
  }

  private fun getPersonDetailAndTransform(
    application: Cas2v2ApplicationEntity,
  ): Cas2v2SubmittedApplication {
    val personInfo = offenderService.getFullInfoForPersonOrThrow(application.crn)

    return cas2v2SubmissionsTransformer.transformJpaToApiRepresentation(
      application,
      personInfo,
    )
  }
}
