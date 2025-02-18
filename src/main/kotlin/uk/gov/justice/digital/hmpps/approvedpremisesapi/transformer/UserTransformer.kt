package uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer

import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ApprovedPremisesUser
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ApprovedPremisesUserRole
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.NamedId
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ProfileResponse
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ServiceName
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.TemporaryAccommodationUser
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.TemporaryAccommodationUserRole
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.UserSummary
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.UserWithWorkload
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.Cas1CruManagementAreaEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserPermission
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserQualification
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserQualificationAssignmentEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserRole
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserRoleAssignmentEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.UserWorkload
import uk.gov.justice.digital.hmpps.approvedpremisesapi.problem.InternalServerErrorProblem
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.UserService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ApprovedPremisesUserPermission as ApiUserPermission
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.UserQualification as ApiUserQualification

@Component
class UserTransformer(
  private val probationRegionTransformer: ProbationRegionTransformer,
  private val probationDeliveryUnitTransformer: ProbationDeliveryUnitTransformer,
  private val apAreaTransformer: ApAreaTransformer,
) {

  fun transformJpaToAPIUserWithWorkload(jpa: UserEntity, userWorkload: UserWorkload): UserWithWorkload = UserWithWorkload(
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
    cruManagementArea = jpa.cruManagementArea?.toNamedId(),
  )

  fun transformJpaToSummaryApi(jpa: UserEntity) = UserSummary(
    id = jpa.id,
    name = jpa.name,
  )

  @Suppress("TooGenericExceptionThrown")
  fun transformJpaToApi(jpa: UserEntity, serviceName: ServiceName) = when (serviceName) {
    ServiceName.approvedPremises -> transformCas1JpaToApi(jpa)
    ServiceName.temporaryAccommodation -> transformCas3JpatoApi(jpa)
    ServiceName.cas2 -> throw RuntimeException("CAS2 not supported")
    ServiceName.cas2v2 -> throw RuntimeException("CAS2v2 not supported")
  }

  fun transformCas1JpaToApi(jpa: UserEntity): ApprovedPremisesUser {
    val apArea = jpa.apArea ?: throw InternalServerErrorProblem("CAS1 user ${jpa.id} should have AP Area Set")
    val cruManagementArea = jpa.cruManagementArea ?: throw InternalServerErrorProblem("CAS1 user ${jpa.id} should have CRU Management Area Set")
    return ApprovedPremisesUser(
      id = jpa.id,
      deliusUsername = jpa.deliusUsername,
      roles = jpa.roles.distinctBy { it.role }.mapNotNull(::transformApprovedPremisesRoleToApi),
      email = jpa.email,
      name = jpa.name,
      telephoneNumber = jpa.telephoneNumber,
      isActive = jpa.isActive,
      qualifications = jpa.qualifications.map(::transformQualificationToApi),
      permissions = jpa.roles.distinctBy { it.role }.map(::transformApprovedPremisesRoleToPermissionApi).flatten().distinct(),
      region = probationRegionTransformer.transformJpaToApi(jpa.probationRegion),
      service = "CAS1",
      apArea = apArea.let { apAreaTransformer.transformJpaToApi(it) },
      cruManagementArea = cruManagementArea.toNamedId(),
      cruManagementAreaDefault = apArea.defaultCruManagementArea.toNamedId(),
      cruManagementAreaOverride = jpa.cruManagementAreaOverride?.toNamedId(),
      version = UserEntity.getVersionHashCode((jpa.roles.map { it.role })),
    )
  }

  fun transformCas3JpatoApi(jpa: UserEntity) = TemporaryAccommodationUser(
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

  fun Cas1CruManagementAreaEntity.toNamedId() = NamedId(id, name)

  fun transformProfileResponseToApi(userName: String, userResponse: UserService.GetUserResponse, xServiceName: ServiceName): ProfileResponse = when (userResponse) {
    UserService.GetUserResponse.StaffRecordNotFound -> ProfileResponse(userName, ProfileResponse.LoadError.staffRecordNotFound)
    is UserService.GetUserResponse.Success -> ProfileResponse(userName, user = transformJpaToApi(userResponse.user, xServiceName))
  }

  private fun transformApprovedPremisesRoleToApi(userRole: UserRoleAssignmentEntity): ApprovedPremisesUserRole? = when (userRole.role.service) {
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
    UserQualification.LAO -> ApiUserQualification.lao
    UserQualification.ESAP -> ApiUserQualification.esap
    UserQualification.EMERGENCY -> ApiUserQualification.emergency
    UserQualification.MENTAL_HEALTH_SPECIALIST -> ApiUserQualification.mentalHealthSpecialist
    UserQualification.RECOVERY_FOCUSED -> ApiUserQualification.recoveryFocused
  }

  @SuppressWarnings("CyclomaticComplexMethod")
  private fun transformApprovedPremisesRoleToPermissionApi(userRole: UserRoleAssignmentEntity): List<ApiUserPermission> = userRole.role.permissions.map {
    when (it) {
      UserPermission.CAS1_ADHOC_BOOKING_CREATE -> ApiUserPermission.adhocBookingCreate
      UserPermission.CAS1_ASSESS_APPEALED_APPLICATION -> ApiUserPermission.assessAppealedApplication
      UserPermission.CAS1_ASSESS_APPLICATION -> ApiUserPermission.assessApplication
      UserPermission.CAS1_ASSESS_PLACEMENT_APPLICATION -> ApiUserPermission.assessPlacementApplication
      UserPermission.CAS1_ASSESS_PLACEMENT_REQUEST -> ApiUserPermission.assessPlacementRequest
      UserPermission.CAS1_BOOKING_CREATE -> ApiUserPermission.bookingCreate
      UserPermission.CAS1_BOOKING_CHANGE_DATES -> ApiUserPermission.bookingChangeDates
      UserPermission.CAS1_BOOKING_WITHDRAW -> ApiUserPermission.bookingWithdraw
      UserPermission.CAS1_OUT_OF_SERVICE_BED_CREATE -> ApiUserPermission.outOfServiceBedCreate
      UserPermission.CAS1_PROCESS_AN_APPEAL -> ApiUserPermission.processAnAppeal
      UserPermission.CAS1_USER_LIST -> ApiUserPermission.userList
      UserPermission.CAS1_USER_MANAGEMENT -> ApiUserPermission.userManagement
      UserPermission.CAS1_VIEW_ASSIGNED_ASSESSMENTS -> ApiUserPermission.viewAssignedAssessments
      UserPermission.CAS1_VIEW_CRU_DASHBOARD -> ApiUserPermission.viewCruDashboard
      UserPermission.CAS1_VIEW_MANAGE_TASKS -> ApiUserPermission.viewManageTasks
      UserPermission.CAS1_VIEW_OUT_OF_SERVICE_BEDS -> ApiUserPermission.viewOutOfServiceBeds
      UserPermission.CAS1_SPACE_BOOKING_CREATE -> ApiUserPermission.spaceBookingCreate
      UserPermission.CAS1_SPACE_BOOKING_LIST -> ApiUserPermission.spaceBookingList
      UserPermission.CAS1_SPACE_BOOKING_RECORD_ARRIVAL -> ApiUserPermission.spaceBookingRecordArrival
      UserPermission.CAS1_SPACE_BOOKING_RECORD_DEPARTURE -> ApiUserPermission.spaceBookingRecordDeparture
      UserPermission.CAS1_SPACE_BOOKING_RECORD_NON_ARRIVAL -> ApiUserPermission.spaceBookingRecordNonArrival
      UserPermission.CAS1_SPACE_BOOKING_RECORD_KEYWORKER -> ApiUserPermission.spaceBookingRecordKeyworker
      UserPermission.CAS1_SPACE_BOOKING_VIEW -> ApiUserPermission.spaceBookingView
      UserPermission.CAS1_SPACE_BOOKING_WITHDRAW -> ApiUserPermission.spaceBookingWithdraw
      UserPermission.CAS1_PREMISES_VIEW -> ApiUserPermission.premisesView
      UserPermission.CAS1_PREMISES_MANAGE -> ApiUserPermission.premisesManage
      UserPermission.CAS1_APPLICATION_WITHDRAW_OTHERS -> ApiUserPermission.applicationWithdrawOthers
      UserPermission.CAS1_REQUEST_FOR_PLACEMENT_WITHDRAW_OTHERS -> ApiUserPermission.requestForPlacementWithdrawOthers
      UserPermission.CAS1_REPORTS_VIEW -> ApiUserPermission.reportsView
      UserPermission.CAS1_REPORTS_VIEW_WITH_PII -> ApiUserPermission.reportsViewWithPii
    }
  }
}
