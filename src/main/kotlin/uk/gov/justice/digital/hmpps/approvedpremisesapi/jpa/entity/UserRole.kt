package uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity

import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ApprovedPremisesUserRole
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ServiceName
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.Constants.commonCruMemberPermissions

private object Constants {
  val commonCruMemberPermissions = listOf(
    UserPermission.CAS1_ADHOC_BOOKING_CREATE,
    UserPermission.CAS1_APPLICATION_WITHDRAW_OTHERS,
    UserPermission.CAS1_BOOKING_CHANGE_DATES,
    UserPermission.CAS1_BOOKING_WITHDRAW,
    UserPermission.CAS1_OUT_OF_SERVICE_BED_CREATE,
    UserPermission.CAS1_PREMISES_VIEW,
    UserPermission.CAS1_REQUEST_FOR_PLACEMENT_WITHDRAW_OTHERS,
    UserPermission.CAS1_SPACE_BOOKING_LIST,
    UserPermission.CAS1_SPACE_BOOKING_RECORD_ARRIVAL,
    UserPermission.CAS1_SPACE_BOOKING_RECORD_DEPARTURE,
    UserPermission.CAS1_SPACE_BOOKING_RECORD_KEYWORKER,
    UserPermission.CAS1_SPACE_BOOKING_VIEW,
    UserPermission.CAS1_SPACE_BOOKING_WITHDRAW,
    UserPermission.CAS1_USER_LIST,
    UserPermission.CAS1_VIEW_CRU_DASHBOARD,
    UserPermission.CAS1_VIEW_MANAGE_TASKS,
    UserPermission.CAS1_VIEW_OUT_OF_SERVICE_BEDS,
  )
}

enum class UserRole(val service: ServiceName, val cas1ApiValue: ApprovedPremisesUserRole?, val permissions: List<UserPermission> = emptyList()) {

  CAS1_ASSESSOR(
    ServiceName.approvedPremises,
    ApprovedPremisesUserRole.assessor,
    listOf(
      UserPermission.CAS1_ASSESS_APPEALED_APPLICATION,
      UserPermission.CAS1_ASSESS_APPLICATION,
      UserPermission.CAS1_ASSESS_PLACEMENT_APPLICATION,
      UserPermission.CAS1_VIEW_ASSIGNED_ASSESSMENTS,
    ),
  ),

  @Deprecated("This role will be removed in the future. Superseded by CRU_MEMBER")
  CAS1_MATCHER(
    ServiceName.approvedPremises,
    ApprovedPremisesUserRole.matcher,
    listOf(
      UserPermission.CAS1_ASSESS_PLACEMENT_APPLICATION,
      UserPermission.CAS1_ASSESS_PLACEMENT_REQUEST,
    ),
  ),

  CAS1_FUTURE_MANAGER(
    ServiceName.approvedPremises,
    ApprovedPremisesUserRole.futureManager,
    listOf(
      UserPermission.CAS1_OUT_OF_SERVICE_BED_CREATE,
      UserPermission.CAS1_PREMISES_VIEW,
      UserPermission.CAS1_PREMISES_MANAGE,
      UserPermission.CAS1_SPACE_BOOKING_LIST,
      UserPermission.CAS1_SPACE_BOOKING_RECORD_ARRIVAL,
      UserPermission.CAS1_SPACE_BOOKING_RECORD_DEPARTURE,
      UserPermission.CAS1_SPACE_BOOKING_RECORD_NON_ARRIVAL,
      UserPermission.CAS1_SPACE_BOOKING_RECORD_KEYWORKER,
      UserPermission.CAS1_SPACE_BOOKING_VIEW,
      UserPermission.CAS1_VIEW_OUT_OF_SERVICE_BEDS,
    ),
  ),

  @Deprecated("This role will be removed in the future. It will be superseded by Assessor, CRU Member and Future Manager")
  CAS1_WORKFLOW_MANAGER(
    ServiceName.approvedPremises,
    ApprovedPremisesUserRole.workflowManager,
    listOf(
      UserPermission.CAS1_ADHOC_BOOKING_CREATE,
      UserPermission.CAS1_APPLICATION_WITHDRAW_OTHERS,
      UserPermission.CAS1_BOOKING_CHANGE_DATES,
      UserPermission.CAS1_BOOKING_CREATE,
      UserPermission.CAS1_BOOKING_WITHDRAW,
      UserPermission.CAS1_PREMISES_VIEW,
      UserPermission.CAS1_REQUEST_FOR_PLACEMENT_WITHDRAW_OTHERS,
      UserPermission.CAS1_USER_LIST,
      UserPermission.CAS1_VIEW_CRU_DASHBOARD,
      UserPermission.CAS1_VIEW_MANAGE_TASKS,
    ),
  ),

  CAS1_CRU_MEMBER(
    ServiceName.approvedPremises,
    ApprovedPremisesUserRole.cruMember,
    permissions = commonCruMemberPermissions + listOf(UserPermission.CAS1_BOOKING_CREATE),
  ),

  /**
   * A temporary role used while rolling out Find and Booking functionality
   */
  CAS1_CRU_MEMBER_FIND_AND_BOOK_BETA(
    ServiceName.approvedPremises,
    ApprovedPremisesUserRole.cruMemberFindAndBookBeta,
    permissions = commonCruMemberPermissions + listOf(UserPermission.CAS1_SPACE_BOOKING_CREATE),
  ),

  /**
   * A temporary role used while rolling out OOSB functionality
   */
  CAS1_CRU_MEMBER_ENABLE_OUT_OF_SERVICE_BEDS(
    ServiceName.approvedPremises,
    ApprovedPremisesUserRole.cruMemberEnableOutOfServiceBeds,
    permissions = listOf(
      UserPermission.CAS1_OUT_OF_SERVICE_BED_CREATE,
      UserPermission.CAS1_VIEW_OUT_OF_SERVICE_BEDS,
    ),
  ),

  CAS1_APPLICANT(ServiceName.approvedPremises, ApprovedPremisesUserRole.applicant),

  CAS1_REPORT_VIEWER(
    ServiceName.approvedPremises,
    ApprovedPremisesUserRole.reportViewer,
    listOf(
      UserPermission.CAS1_REPORTS_VIEW,
    ),
  ),
  CAS1_EXCLUDED_FROM_ASSESS_ALLOCATION(
    ServiceName.approvedPremises,
    ApprovedPremisesUserRole.excludedFromAssessAllocation,
  ),
  CAS1_EXCLUDED_FROM_MATCH_ALLOCATION(
    ServiceName.approvedPremises,
    ApprovedPremisesUserRole.excludedFromMatchAllocation,
  ),
  CAS1_EXCLUDED_FROM_PLACEMENT_APPLICATION_ALLOCATION(
    ServiceName.approvedPremises,
    ApprovedPremisesUserRole.excludedFromPlacementApplicationAllocation,
  ),
  CAS1_APPEALS_MANAGER(
    ServiceName.approvedPremises,
    ApprovedPremisesUserRole.appealsManager,
    listOf(
      UserPermission.CAS1_ASSESS_APPEALED_APPLICATION,
      UserPermission.CAS1_PROCESS_AN_APPEAL,
      UserPermission.CAS1_VIEW_ASSIGNED_ASSESSMENTS,
    ),
  ),
  CAS1_JANITOR(
    ServiceName.approvedPremises,
    ApprovedPremisesUserRole.janitor,
    UserPermission.entries.toList(),
  ),
  CAS1_USER_MANAGER(
    ServiceName.approvedPremises,
    ApprovedPremisesUserRole.userManager,
    listOf(
      UserPermission.CAS1_USER_LIST,
      UserPermission.CAS1_USER_MANAGEMENT,
    ),
  ),
  CAS3_ASSESSOR(ServiceName.temporaryAccommodation, null),
  CAS3_REFERRER(ServiceName.temporaryAccommodation, null),
  CAS3_REPORTER(ServiceName.temporaryAccommodation, null),
  ;

  fun hasPermission(permission: UserPermission) = permissions.contains(permission)

  companion object {
    fun getAllRolesForService(service: ServiceName) = UserRole.values().filter { it.service == service }

    fun valueOf(apiValue: ApprovedPremisesUserRole) = UserRole.entries.first { it.cas1ApiValue == apiValue }

    fun getAllRolesForPermission(permission: UserPermission) = UserRole.values().filter { it.permissions.contains(permission) }
  }
}
