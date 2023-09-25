package uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer

import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ApprovedPremisesUser
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.AssessmentTask
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.PlacementApplicationTask
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.PlacementDates
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.PlacementRequestTask
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ServiceName
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.TaskStatus
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.TaskType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.AssessmentEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.PlacementApplicationEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.PlacementRequestEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.PlacementType as ApiPlacementType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.PlacementType as JpaPlacementType

@Component
class TaskTransformer(
  private val userTransformer: UserTransformer,
  private val risksTransformer: RisksTransformer,
  private val placementRequestTransformer: PlacementRequestTransformer,
) {
  fun transformAssessmentToTask(assessment: AssessmentEntity, personName: String) = AssessmentTask(
    id = assessment.id,
    applicationId = assessment.application.id,
    personName = personName,
    crn = assessment.application.crn,
    dueDate = assessment.createdAt.plusDays(10).toLocalDate(),
    allocatedToStaffMember = transformUserOrNull(assessment.allocatedToUser),
    status = getAssessmentStatus(assessment),
    taskType = TaskType.assessment,
  )

  fun transformPlacementRequestToTask(placementRequest: PlacementRequestEntity, personName: String) = PlacementRequestTask(
    id = placementRequest.id,
    applicationId = placementRequest.application.id,
    personName = personName,
    crn = placementRequest.application.crn,
    dueDate = placementRequest.createdAt.plusDays(10).toLocalDate(),
    allocatedToStaffMember = transformUserOrNull(placementRequest.allocatedToUser),
    status = getPlacementRequestStatus(placementRequest),
    taskType = TaskType.placementRequest,
    tier = risksTransformer.transformTierDomainToApi(placementRequest.application.riskRatings!!.tier),
    expectedArrival = placementRequest.expectedArrival,
    duration = placementRequest.duration,
    placementRequestStatus = placementRequestTransformer.getStatus(placementRequest),
    releaseType = placementRequestTransformer.getReleaseType(placementRequest.application.releaseType),
  )

  fun transformPlacementApplicationToTask(placementApplication: PlacementApplicationEntity, personName: String) = PlacementApplicationTask(
    id = placementApplication.id,
    applicationId = placementApplication.application.id,
    personName = personName,
    crn = placementApplication.application.crn,
    dueDate = placementApplication.createdAt.plusDays(10).toLocalDate(),
    allocatedToStaffMember = transformUserOrNull(placementApplication.allocatedToUser),
    status = getPlacementApplicationStatus(placementApplication),
    taskType = TaskType.placementApplication,
    tier = risksTransformer.transformTierDomainToApi(placementApplication.application.riskRatings!!.tier),
    placementDates = placementApplication.placementDates.map {
      PlacementDates(
        expectedArrival = it.expectedArrival,
        duration = it.duration,
      )
    },
    releaseType = placementRequestTransformer.getReleaseType(placementApplication.application.releaseType),
    placementType = getPlacementType(placementApplication.placementType!!),
  )

  private fun getPlacementType(placementType: JpaPlacementType): ApiPlacementType = when (placementType) {
    JpaPlacementType.ROTL -> ApiPlacementType.rotl
    JpaPlacementType.ADDITIONAL_PLACEMENT -> ApiPlacementType.additionalPlacement
    JpaPlacementType.RELEASE_FOLLOWING_DECISION -> ApiPlacementType.releaseFollowingDecision
  }

  private fun getPlacementApplicationStatus(entity: PlacementApplicationEntity): TaskStatus = when {
    entity.data.isNullOrEmpty() -> TaskStatus.notStarted
    entity.decision !== null -> TaskStatus.complete
    else -> TaskStatus.inProgress
  }

  private fun getAssessmentStatus(entity: AssessmentEntity): TaskStatus = when {
    entity.data.isNullOrEmpty() -> TaskStatus.notStarted
    entity.decision !== null -> TaskStatus.complete
    else -> TaskStatus.inProgress
  }

  private fun getPlacementRequestStatus(entity: PlacementRequestEntity): TaskStatus = when {
    entity.booking !== null -> TaskStatus.complete
    else -> TaskStatus.notStarted
  }

  private fun transformUserOrNull(userEntity: UserEntity?) = userEntity?.let {
    userTransformer.transformJpaToApi(it, ServiceName.approvedPremises) as ApprovedPremisesUser
  }
}
