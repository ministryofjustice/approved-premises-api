package uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer

import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ApprovedPremisesUser
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ApprovedPremisesUserRole
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ProfileResponse
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ServiceName
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.TemporaryAccommodationUser
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.TemporaryAccommodationUserRole
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.UserWithWorkload
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserPermission
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserQualification
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserQualificationAssignmentEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserRole
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserRoleAssignmentEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.GetUserResponse
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.UserWorkload
import uk.gov.justice.digital.hmpps.approvedpremisesapi.problem.InternalServerErrorProblem
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ApprovedPremisesUserPermission as ApiUserPermission
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.UserQualification as ApiUserQualification

@Component
class UserTransformer(
  private val probationRegionTransformer: ProbationRegionTransformer,
  private val probationDeliveryUnitTransformer: ProbationDeliveryUnitTransformer,
  private val apAreaTransformer: ApAreaTransformer,
) {

  fun transformJpaToAPIUserWithWorkload(jpa: UserEntity, userWorkload: UserWorkload): UserWithWorkload {
    return UserWithWorkload(
      id = jpa.id,
      deliusUsername = jpa.deliusUsername,
      email = jpa.email,
      name = jpa.name,
      telephoneNumber = jpa.telephoneNumber,
      isActive = jpa.isActive,
      region = probationRegionTransformer.transformJpaToApi(jpa.probationRegion),
      service = ServiceName.approvedPremises.value,
      numTasksPending = userWorkload.numTasksPending,
      numTasksCompleted7Days = userWorkload.numTasksCompleted7Days,
      numTasksCompleted30Days = userWorkload.numTasksCompleted30Days,
      qualifications = jpa.qualifications.distinctBy { it.qualification }.map(::transformQualificationToApi),
      roles = jpa.roles.distinctBy { it.role }.mapNotNull(::transformApprovedPremisesRoleToApi),
      apArea = jpa.apArea?.let { apAreaTransformer.transformJpaToApi(it) },
    )
  }

  fun transformJpaToApi(jpa: UserEntity, serviceName: ServiceName) = when (serviceName) {
    ServiceName.approvedPremises -> ApprovedPremisesUser(
      id = jpa.id,
      deliusUsername = jpa.deliusUsername,
      roles = jpa.roles.distinctBy { it.role }.mapNotNull(::transformApprovedPremisesRoleToApi),
      email = jpa.email,
      name = jpa.name,
      telephoneNumber = jpa.telephoneNumber,
      isActive = jpa.isActive,
      qualifications = jpa.qualifications.map(::transformQualificationToApi),
      permissions = jpa.roles.distinctBy { it.role }.mapNotNull(::transformApprovedPremisesRoleToPermissionApi).flatten().distinct(),
      region = probationRegionTransformer.transformJpaToApi(jpa.probationRegion),
      service = "CAS1",
      apArea = jpa.apArea?.let { apAreaTransformer.transformJpaToApi(it) } ?: throw InternalServerErrorProblem("CAS1 user ${jpa.id} should have AP Area Set"),
    )
    ServiceName.temporaryAccommodation -> TemporaryAccommodationUser(
      id = jpa.id,
      deliusUsername = jpa.deliusUsername,
      email = jpa.email,
      name = jpa.name,
      telephoneNumber = jpa.telephoneNumber,
      isActive = jpa.isActive,
      roles = jpa.roles.distinctBy { it.role }.mapNotNull(::transformTemporaryAccommodationRoleToApi),
      region = probationRegionTransformer.transformJpaToApi(jpa.probationRegion),
      probationDeliveryUnit = jpa.probationDeliveryUnit?.let { probationDeliveryUnitTransformer.transformJpaToApi(it) },
      service = "CAS3",
    )
    ServiceName.cas2 -> throw RuntimeException("CAS2 not supported")
  }

  fun transformProfileResponseToApi(userName: String, userResponse: GetUserResponse, xServiceName: ServiceName): ProfileResponse {
    if (!userResponse.staffRecordFound) {
      return ProfileResponse(userName, ProfileResponse.LoadError.staffRecordNotFound, user = null)
    }
    return ProfileResponse(userName, loadError = null, transformJpaToApi(userResponse.user!!, xServiceName))
  }

  private fun transformApprovedPremisesRoleToApi(userRole: UserRoleAssignmentEntity): ApprovedPremisesUserRole? =
    when (userRole.role.service) {
      ServiceName.approvedPremises -> userRole.role.cas1ApiValue!!
      else -> null
    }

  private fun transformTemporaryAccommodationRoleToApi(userRole: UserRoleAssignmentEntity): TemporaryAccommodationUserRole? = when (userRole.role) {
    UserRole.CAS3_ASSESSOR -> TemporaryAccommodationUserRole.assessor
    UserRole.CAS3_REFERRER -> TemporaryAccommodationUserRole.referrer
    UserRole.CAS3_REPORTER -> TemporaryAccommodationUserRole.reporter
    else -> null
  }

  private fun transformQualificationToApi(userQualification: UserQualificationAssignmentEntity): ApiUserQualification = when (userQualification.qualification) {
    UserQualification.PIPE -> ApiUserQualification.pipe
    UserQualification.WOMENS -> ApiUserQualification.womens
    UserQualification.LAO -> ApiUserQualification.lao
    UserQualification.ESAP -> ApiUserQualification.esap
    UserQualification.EMERGENCY -> ApiUserQualification.emergency
    UserQualification.MENTAL_HEALTH_SPECIALIST -> ApiUserQualification.mentalHealthSpecialist
    UserQualification.RECOVERY_FOCUSED -> ApiUserQualification.recoveryFocused
  }

  private fun transformApprovedPremisesRoleToPermissionApi(userRole: UserRoleAssignmentEntity): List<ApiUserPermission> {
    return userRole.role.permissions.map {
      when (it) {
        UserPermission.CAS1_VIEW_ASSIGNED_ASSESSMENTS -> ApiUserPermission.viewAssignedAssessments
        UserPermission.CAS1_PROCESS_AN_APPEAL -> ApiUserPermission.processAnAppeal
        UserPermission.CAS1_ASSESS_PLACEMENT_APPLICATION -> ApiUserPermission.assessPlacementApplication
        UserPermission.CAS1_ASSESS_PLACEMENT_REQUEST -> ApiUserPermission.assessPlacementRequest
        UserPermission.CAS1_ASSESS_APPLICATION -> ApiUserPermission.assessApplication
        UserPermission.CAS1_ASSESS_APPEALED_APPLICATION -> ApiUserPermission.assessAppealedApplication
      }
    }
  }
}
