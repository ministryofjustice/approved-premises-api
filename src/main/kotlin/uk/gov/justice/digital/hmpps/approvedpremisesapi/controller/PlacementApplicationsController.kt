package uk.gov.justice.digital.hmpps.approvedpremisesapi.controller

import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.PlacementApplicationsApiDelegate
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.NewPlacementApplication
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.PlacementApplication
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.PlacementApplicationDecisionEnvelope
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.SubmitPlacementApplication
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.UpdatePlacementApplication
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.WithdrawPlacementApplication
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.WithdrawPlacementRequestReason
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApprovedPremisesApplicationEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.PlacementApplicationWithdrawalReason
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserQualification
import uk.gov.justice.digital.hmpps.approvedpremisesapi.problem.ForbiddenProblem
import uk.gov.justice.digital.hmpps.approvedpremisesapi.problem.NotAllowedProblem
import uk.gov.justice.digital.hmpps.approvedpremisesapi.results.AuthorisableActionResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.ApplicationService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.OffenderService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.PlacementApplicationService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.UserService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1.Cas1WithdrawableService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.PlacementApplicationTransformer
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.extractEntityFromCasResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.extractEntityFromValidatableActionResult
import java.util.UUID

@Service
class PlacementApplicationsController(
  private val userService: UserService,
  private val applicationService: ApplicationService,
  private val offenderService: OffenderService,
  private val placementApplicationService: PlacementApplicationService,
  private val placementApplicationTransformer: PlacementApplicationTransformer,
  private val objectMapper: ObjectMapper,
  private val withdrawalService: Cas1WithdrawableService,
) : PlacementApplicationsApiDelegate {
  override fun placementApplicationsPost(newPlacementApplication: NewPlacementApplication): ResponseEntity<PlacementApplication> {
    val user = userService.getUserForRequest()

    val application = extractEntityFromCasResult(
      applicationService.getApplicationForUsername(newPlacementApplication.applicationId, user.deliusUsername),
    )

    if (application !is ApprovedPremisesApplicationEntity) {
      throw RuntimeException("Only CAS1 Applications are currently supported")
    }

    val placementApplication = extractEntityFromValidatableActionResult(
      placementApplicationService.createPlacementApplication(application, user),
    )

    return ResponseEntity.ok(placementApplicationTransformer.transformJpaToApi(placementApplication))
  }

  override fun placementApplicationsIdGet(id: UUID): ResponseEntity<PlacementApplication> {
    val user = userService.getUserForRequest()

    val result = placementApplicationService.getApplication(id)
    val placementApplication = extractEntityFromCasResult(result)

    val offenderResult = offenderService.getOffenderByCrn(placementApplication.application.crn, user.deliusUsername, user.hasQualification(UserQualification.LAO))

    if (offenderResult is AuthorisableActionResult.Unauthorised) {
      throw ForbiddenProblem()
    }

    return ResponseEntity.ok(placementApplicationTransformer.transformJpaToApi(placementApplication))
  }

  override fun placementApplicationsIdPut(
    id: UUID,
    updatePlacementApplication: UpdatePlacementApplication,
  ): ResponseEntity<PlacementApplication> {
    val serializedData = objectMapper.writeValueAsString(updatePlacementApplication.data)

    val result = placementApplicationService.updateApplication(id, serializedData)

    val placementApplication = extractEntityFromCasResult(result)

    return ResponseEntity.ok(placementApplicationTransformer.transformJpaToApi(placementApplication))
  }

  override fun placementApplicationsIdSubmissionPost(
    id: UUID,
    submitPlacementApplication: SubmitPlacementApplication,
  ): ResponseEntity<List<PlacementApplication>> {
    val serializedData = objectMapper.writeValueAsString(submitPlacementApplication.translatedDocument)

    val result = placementApplicationService.submitApplication(id, serializedData, submitPlacementApplication.placementType, submitPlacementApplication.placementDates)

    val placementApplications = extractEntityFromCasResult(result)

    return ResponseEntity.ok(placementApplications.map { placementApplicationTransformer.transformJpaToApi(it) })
  }

  override fun placementApplicationsIdDecisionPost(
    id: UUID,
    placementApplicationDecisionEnvelope: PlacementApplicationDecisionEnvelope,
  ): ResponseEntity<PlacementApplication> {
    val result = placementApplicationService.recordDecision(id, placementApplicationDecisionEnvelope)

    val placementApplication = extractEntityFromCasResult(result)

    return ResponseEntity.ok(placementApplicationTransformer.transformJpaToApi(placementApplication))
  }

  override fun placementApplicationsIdWithdrawPost(
    id: UUID,
    withdrawPlacementApplication: WithdrawPlacementApplication?,
  ): ResponseEntity<PlacementApplication> {
    val withdrawalReason = when (withdrawPlacementApplication?.reason) {
      WithdrawPlacementRequestReason.duplicatePlacementRequest -> PlacementApplicationWithdrawalReason.DUPLICATE_PLACEMENT_REQUEST
      WithdrawPlacementRequestReason.alternativeProvisionIdentified -> PlacementApplicationWithdrawalReason.ALTERNATIVE_PROVISION_IDENTIFIED
      WithdrawPlacementRequestReason.changeInCircumstances -> PlacementApplicationWithdrawalReason.CHANGE_IN_CIRCUMSTANCES
      WithdrawPlacementRequestReason.changeInReleaseDecision -> PlacementApplicationWithdrawalReason.CHANGE_IN_RELEASE_DECISION
      WithdrawPlacementRequestReason.noCapacityDueToLostBed -> PlacementApplicationWithdrawalReason.NO_CAPACITY_DUE_TO_LOST_BED
      WithdrawPlacementRequestReason.noCapacityDueToPlacementPrioritisation -> PlacementApplicationWithdrawalReason.NO_CAPACITY_DUE_TO_PLACEMENT_PRIORITISATION
      WithdrawPlacementRequestReason.noCapacity -> PlacementApplicationWithdrawalReason.NO_CAPACITY
      WithdrawPlacementRequestReason.errorInPlacementRequest -> PlacementApplicationWithdrawalReason.ERROR_IN_PLACEMENT_REQUEST
      WithdrawPlacementRequestReason.withdrawnByPP -> throw NotAllowedProblem("Withdrawal reason is reserved for internal use")
      WithdrawPlacementRequestReason.relatedApplicationWithdrawn -> throw NotAllowedProblem("Withdrawal reason is reserved for internal use")
      WithdrawPlacementRequestReason.relatedPlacementRequestWithdrawn -> throw NotAllowedProblem("Withdrawal reason is reserved for internal use")
      WithdrawPlacementRequestReason.relatedPlacementApplicationWithdrawn -> throw NotAllowedProblem("Withdrawal reason is reserved for internal use")
      null -> null
    }

    val result = withdrawalService.withdrawPlacementApplication(
      id,
      userService.getUserForRequest(),
      withdrawalReason,
    )

    return ResponseEntity.ok(
      placementApplicationTransformer.transformJpaToApi(extractEntityFromCasResult(result)),
    )
  }
}
