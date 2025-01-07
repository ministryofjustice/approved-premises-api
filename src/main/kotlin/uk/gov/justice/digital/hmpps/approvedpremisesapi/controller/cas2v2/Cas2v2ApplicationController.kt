package uk.gov.justice.digital.hmpps.approvedpremisesapi.controller.cas2v2

import com.fasterxml.jackson.databind.ObjectMapper
import jakarta.transaction.Transactional
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.cas2v2.ApplicationsCas2v2Delegate
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Application
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.NewApplication
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.SortDirection
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.UpdateApplication
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.cas2v2.Cas2v2ApplicationEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.cas2v2.Cas2v2ApplicationSummaryEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.problem.BadRequestProblem
import uk.gov.justice.digital.hmpps.approvedpremisesapi.problem.ConflictProblem
import uk.gov.justice.digital.hmpps.approvedpremisesapi.problem.ForbiddenProblem
import uk.gov.justice.digital.hmpps.approvedpremisesapi.problem.NotFoundProblem
import uk.gov.justice.digital.hmpps.approvedpremisesapi.results.AuthorisableActionResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.results.ValidatableActionResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.NomisUserService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas2.OffenderService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas2v2.Cas2v2ApplicationService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.cas2v2.Cas2v2ApplicationsTransformer
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.PageCriteria
import java.net.URI
import java.util.UUID
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas2v2ApplicationSummary as ModelCas2v2ApplicationSummary

@Service(
  "Cas2v2ApplicationController",
)
class Cas2v2ApplicationController(
  private val cas2v2ApplicationService: Cas2v2ApplicationService,
  private val cas2v2ApplicationsTransformer: Cas2v2ApplicationsTransformer,
  private val objectMapper: ObjectMapper,
  private val offenderService: OffenderService,
  private val userService: NomisUserService,
) : ApplicationsCas2v2Delegate {

  override fun applicationsGet(
    isSubmitted: Boolean?,
    page: Int?,
    prisonCode: String?,
  ): ResponseEntity<List<ModelCas2v2ApplicationSummary>> {
    /*This gets a NomisUser. Toby and Gareth discussed creating a third user service
     * The third user service will return a Cas2v2User. That user will have two nullable fields/properties
     * 1. DeliusUser
     * 2. NomisUser
     * We discussed using transformers if we need any specific data about the user eg their name.
     *
     * In the Cas2v2ApplicationEntity the created_by field would be of type Cas2v2User
     * */
    val user = userService.getUserForRequest()

    prisonCode?.let { if (prisonCode != user.activeCaseloadId) throw ForbiddenProblem() }

    val pageCriteria = PageCriteria("createdAt", SortDirection.desc, page)

    val (applications, metadata) = cas2v2ApplicationService.getCas2v2Applications(prisonCode, isSubmitted, user, pageCriteria)

    return ResponseEntity.ok().headers(
      metadata?.toHeaders(),
    ).body(getPersonNamesAndTransformToSummaries(applications))
  }

  override fun applicationsApplicationIdGet(applicationId: UUID): ResponseEntity<Application> {
    val user = userService.getUserForRequest()

    val application = when (
      val applicationResult = cas2v2ApplicationService
        .getCas2v2ApplicationForUser(
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

    val applicationResult = cas2v2ApplicationService.createCas2v2Application(
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
      .created(URI.create("/cas2v2/applications/${application.id}"))
      .body(cas2v2ApplicationsTransformer.transformJpaToApi(application, personInfo))
  }

  @Transactional
  override fun applicationsApplicationIdPut(
    applicationId: UUID,
    body: UpdateApplication,
  ): ResponseEntity<Application> {
    val user = userService.getUserForRequest()

    val serializedData = objectMapper.writeValueAsString(body.data)

    val applicationResult = cas2v2ApplicationService.updateCas2v2Application(
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

    val validationResult = when (val applicationResult = cas2v2ApplicationService.abandonCas2v2Application(applicationId, user)) {
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
    applicationSummaries: List<Cas2v2ApplicationSummaryEntity>,
  ): List<ModelCas2v2ApplicationSummary> {
    val crns = applicationSummaries.map { it.crn }

    val personNamesMap = offenderService.getMapOfPersonNamesAndCrns(crns)

    return applicationSummaries.map { application ->
      cas2v2ApplicationsTransformer.transformJpaSummaryToSummary(application, personNamesMap[application.crn]!!)
    }
  }

  private fun getPersonDetailAndTransform(
    application: Cas2v2ApplicationEntity,
  ): Application {
    val personInfo = offenderService.getFullInfoForPersonOrThrow(application.crn)

    return cas2v2ApplicationsTransformer.transformJpaToApi(application, personInfo)
  }
}
