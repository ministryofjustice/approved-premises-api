package uk.gov.justice.digital.hmpps.approvedpremisesapi.controller.cas2bail

import com.fasterxml.jackson.databind.ObjectMapper
import jakarta.transaction.Transactional
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.cas2bail.ApplicationsCas2bailDelegate
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Application
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.NewApplication
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.SortDirection
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.UpdateApplication
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.cas2bail.Cas2BailApplicationEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.cas2bail.Cas2BailApplicationSummaryEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.problem.BadRequestProblem
import uk.gov.justice.digital.hmpps.approvedpremisesapi.problem.ConflictProblem
import uk.gov.justice.digital.hmpps.approvedpremisesapi.problem.ForbiddenProblem
import uk.gov.justice.digital.hmpps.approvedpremisesapi.problem.NotFoundProblem
import uk.gov.justice.digital.hmpps.approvedpremisesapi.results.AuthorisableActionResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.results.ValidatableActionResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.NomisUserService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas2.OffenderService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas2bail.Cas2BailApplicationService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.cas2bail.Cas2BailApplicationsTransformer
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.PageCriteria
import java.net.URI
import java.util.UUID
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas2ApplicationSummary as ModelCas2ApplicationSummary

@Service(
  "Cas2BailApplicationsController",
)
class Cas2BailApplicationController(
  private val cas2BailApplicationService: Cas2BailApplicationService,
  private val cas2BailApplicationsTransformer: Cas2BailApplicationsTransformer,
  private val objectMapper: ObjectMapper,
  private val offenderService: OffenderService,
  private val userService: NomisUserService,
) : ApplicationsCas2bailDelegate {

  override fun applicationsGet(
    isSubmitted: Boolean?,
    page: Int?,
    prisonCode: String?,
  ): ResponseEntity<List<ModelCas2ApplicationSummary>> {
    /*This gets a NomisUser. Toby and Gareth discussed creating a third user service
     * The third user service will return a Cas2BailUser. That user will have two nullable fields/properties
     * 1. DeliusUser
     * 2. NomisUser
     * We discussed using transformers if we need any specific data about the user eg their name.
     *
     * In the Cas2BailApplicationEntity the created_by field would be of type Cas2BailUser
     * */
    val user = userService.getUserForRequest()

    prisonCode?.let { if (prisonCode != user.activeCaseloadId) throw ForbiddenProblem() }

    val pageCriteria = PageCriteria("createdAt", SortDirection.desc, page)

    val (applications, metadata) = cas2BailApplicationService.getCas2BailApplications(prisonCode, isSubmitted, user, pageCriteria)

    return ResponseEntity.ok().headers(
      metadata?.toHeaders(),
    ).body(getPersonNamesAndTransformToSummaries(applications))
  }

  override fun applicationsApplicationIdGet(applicationId: UUID): ResponseEntity<Application> {
    val user = userService.getUserForRequest()

    val application = when (
      val applicationResult = cas2BailApplicationService
        .getCas2BailApplicationForUser(
          applicationId,
          user,
        )

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
  override fun applicationsPost(body: NewApplication): ResponseEntity<Application> {
    val user = userService.getUserForRequest()

    val personInfo = offenderService.getFullInfoForPersonOrThrow(body.crn)

    val applicationResult = cas2BailApplicationService.createCas2BailApplication(
      body.crn,
      user,
    )

    val application = when (applicationResult) {
      is ValidatableActionResult.GeneralValidationError -> throw BadRequestProblem(errorDetail = applicationResult.message)
      is ValidatableActionResult.FieldValidationError -> throw BadRequestProblem(invalidParams = applicationResult.validationMessages)
      is ValidatableActionResult.ConflictError -> throw ConflictProblem(id = applicationResult.conflictingEntityId, conflictReason = applicationResult.message)
      is ValidatableActionResult.Success -> applicationResult.entity
    }

    return ResponseEntity
      .created(URI.create("/cas2/applications/${application.id}"))
      .body(cas2BailApplicationsTransformer.transformJpaToApi(application, personInfo))
  }

  @Transactional
  override fun applicationsApplicationIdPut(
    applicationId: UUID,
    body: UpdateApplication,
  ): ResponseEntity<Application> {
    val user = userService.getUserForRequest()

    val serializedData = objectMapper.writeValueAsString(body.data)

    val applicationResult = cas2BailApplicationService.updateCas2BailApplication(
      applicationId =
      applicationId,
      data = serializedData,
      user,
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

  @Transactional
  override fun applicationsApplicationIdAbandonPut(applicationId: UUID): ResponseEntity<Unit> {
    val user = userService.getUserForRequest()

    val validationResult = when (val applicationResult = cas2BailApplicationService.abandonCas2BailApplication(applicationId, user)) {
      is AuthorisableActionResult.NotFound -> throw NotFoundProblem(applicationId, "Application")
      is AuthorisableActionResult.Unauthorised -> throw ForbiddenProblem()
      is AuthorisableActionResult.Success -> applicationResult.entity
    }

    when (validationResult) {
      is ValidatableActionResult.GeneralValidationError -> throw BadRequestProblem(errorDetail = validationResult.message)
      is ValidatableActionResult.FieldValidationError -> throw BadRequestProblem(invalidParams = validationResult.validationMessages)
      is ValidatableActionResult.ConflictError -> throw ConflictProblem(id = validationResult.conflictingEntityId, conflictReason = validationResult.message)
      is ValidatableActionResult.Success -> validationResult.entity
    }

    return ResponseEntity.ok(Unit)
  }

  private fun getPersonNamesAndTransformToSummaries(
    applicationSummaries: List<Cas2BailApplicationSummaryEntity>,
  ): List<ModelCas2ApplicationSummary> {
    val crns = applicationSummaries.map { it.crn }

    val personNamesMap = offenderService.getMapOfPersonNamesAndCrns(crns)

    return applicationSummaries.map { application ->
      cas2BailApplicationsTransformer.transformJpaSummaryToSummary(application, personNamesMap[application.crn]!!)
    }
  }

  private fun getPersonDetailAndTransform(
    application: Cas2BailApplicationEntity,
  ): Application {
    val personInfo = offenderService.getFullInfoForPersonOrThrow(application.crn)

    return cas2BailApplicationsTransformer.transformJpaToApi(application, personInfo)
  }
}
