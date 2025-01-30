package uk.gov.justice.digital.hmpps.approvedpremisesapi.service

import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.data.domain.Sort
import org.springframework.data.repository.findByIdOrNull
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ApprovedPremisesUserRole
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ServiceName
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.SortDirection
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.UserSortField
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.ApDeliusContextApiClient
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.ClientResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.Cas1CruManagementAreaRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ProbationAreaProbationRegionMappingRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ProbationDeliveryUnitEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ProbationDeliveryUnitRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ProbationRegionEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ProbationRegionRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserPermission
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserQualification
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserQualificationAssignmentEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserQualificationAssignmentRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserRole
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserRoleAssignmentEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserRoleAssignmentRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.specification.hasQualificationsAndRoles
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.PaginationMetadata
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.UserWorkload
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.deliuscontext.StaffDetail
import uk.gov.justice.digital.hmpps.approvedpremisesapi.problem.InternalServerErrorProblem
import uk.gov.justice.digital.hmpps.approvedpremisesapi.results.AuthorisableActionResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.results.CasResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.results.CasSimpleResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1.Cas1ApAreaMappingService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.getMetadata
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.getPageable
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.transformQualifications
import java.time.OffsetDateTime
import java.util.UUID
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.UserQualification as APIUserQualification

@Service
class UserServiceConfig(
  @Value("\${assign-default-region-to-users-with-unknown-region}") val assignDefaultRegionToUsersWithUnknownRegion: Boolean,
)

@Service
class UserService(
  private val userServiceConfig: UserServiceConfig,
  private val requestContextService: RequestContextService,
  private val httpAuthService: HttpAuthService,
  private val offenderService: OffenderService,
  private val userRepository: UserRepository,
  private val userRoleAssignmentRepository: UserRoleAssignmentRepository,
  private val userQualificationAssignmentRepository: UserQualificationAssignmentRepository,
  private val probationRegionRepository: ProbationRegionRepository,
  private val probationAreaProbationRegionMappingRepository: ProbationAreaProbationRegionMappingRepository,
  private val cas1ApAreaMappingService: Cas1ApAreaMappingService,
  private val probationDeliveryUnitRepository: ProbationDeliveryUnitRepository,
  private val apDeliusContextApiClient: ApDeliusContextApiClient,
  private val cas1CruManagementAreaRepository: Cas1CruManagementAreaRepository,
) {
  private val log = LoggerFactory.getLogger(this::class.java)

  fun findByIdOrNull(id: UUID) = userRepository.findByIdOrNull(id)

  fun getUsersByPartialName(name: String): List<UserEntity> {
    return userRepository.findByNameContainingIgnoreCase(name)
  }
  fun getDeliusUserNameForRequest() = httpAuthService.getDeliusPrincipalOrThrow().name

  fun getUserForRequest(): UserEntity {
    val username = getDeliusUserNameForRequest()
    val user = when (val result = getExistingUserOrCreate(username)) {
      GetUserResponse.StaffRecordNotFound -> throw InternalServerErrorProblem("Could not find staff record for user $username")
      is GetUserResponse.Success -> result.user
    }
    ensureCas3UserHasCas3ReferrerRole(user)

    return user
  }

  fun getUserForProfile(username: String): GetUserResponse {
    val userResponse = getExistingUserOrCreate(username)
    if (userResponse is GetUserResponse.Success) {
      ensureCas3UserHasCas3ReferrerRole(userResponse.user)
    }
    return userResponse
  }

  fun getDeliusUserNameForRequestOrNull(): String? =
    httpAuthService.getDeliusPrincipalOrNull()?.name

  fun getUserForRequestOrNull(): UserEntity? {
    val username = getDeliusUserNameForRequestOrNull() ?: return null

    return userRepository.findByDeliusUsername(username.uppercase())
  }

  fun getUserForRequestVersionInfo(): UserVersionInfo? {
    return httpAuthService.getDeliusPrincipalOrNull()?.let { deliusPrincipal ->
      val roleAssignments = userRepository.findRoleAssignmentByUsername(deliusPrincipal.name.uppercase())
      if (roleAssignments.isEmpty()) {
        return null
      }

      val userId = roleAssignments.first().userId
      val roles = roleAssignments
        .filter { it.roleName != null }
        .map { UserRole.valueOf(it.roleName!!) }

      return UserVersionInfo(
        userId = userId,
        version = UserEntity.getVersionHashCode(roles),
      )
    }
  }

  data class UserVersionInfo(
    val userId: UUID,
    val version: Int,
  )

  fun getUsers(
    qualifications: List<UserQualification>?,
    roles: List<UserRole>?,
    sortBy: UserSortField?,
    sortDirection: SortDirection?,
    page: Int?,
    region: UUID?,
    apArea: UUID?,
    cruManagementAreaId: UUID?,
  ): Pair<List<UserEntity>, PaginationMetadata?> {
    var metadata: PaginationMetadata? = null
    val users: List<UserEntity>

    val pageable = getPageable(sortBy?.value ?: "name", sortDirection, page)

    if (pageable == null) {
      users = userRepository.findAll(
        hasQualificationsAndRoles(qualifications, roles, region, apArea, cruManagementAreaId, true),
        Sort.by(Sort.Direction.ASC, "name"),
      )
    } else {
      val response = userRepository.findAll(
        hasQualificationsAndRoles(qualifications, roles, region, apArea, cruManagementAreaId, true),
        pageable,
      )

      users = response.content
      metadata = getMetadata(response, page)
    }

    return Pair(users, metadata)
  }

  private fun ensureCas3UserHasCas3ReferrerRole(user: UserEntity) {
    val serviceForRequest = requestContextService.getServiceForRequest()
    if ((serviceForRequest == ServiceName.temporaryAccommodation) &&
      (!user.hasAnyRole(*UserRole.getAllRolesForService(ServiceName.temporaryAccommodation).toTypedArray()))
    ) {
      user.roles += userRoleAssignmentRepository.save(
        UserRoleAssignmentEntity(
          id = UUID.randomUUID(),
          user = user,
          role = UserRole.CAS3_REFERRER,
        ),
      )
    }
  }

  fun getAllocatableUsersForAllocationType(
    crn: String,
    qualifications: List<UserQualification>,
    permission: UserPermission,
  ): List<UserEntity> {
    val userQualifications = mutableListOf<UserQualification>()
    userQualifications.addAll(qualifications)

    if (offenderService.isLao(crn)) {
      userQualifications.add(UserQualification.LAO)
    }

    val requiredRole = UserRole.getAllRolesForPermission(permission)

    var users = userRepository.findActiveUsersWithAtLeastOneRole(requiredRole)

    userQualifications.forEach { qualification ->
      users = users.filter { it.hasQualification(qualification) }
    }

    return users
  }

  fun deleteUser(id: UUID) {
    val user = userRepository.findByIdOrNull(id) ?: return
    user.isActive = false
    userRepository.save(user)
  }

  fun updateUser(
    id: UUID,
    roles: List<ApprovedPremisesUserRole>,
    qualifications: List<APIUserQualification>,
    cruManagementAreaOverrideId: UUID?,
  ): CasResult<UserEntity> {
    val user = userRepository.findByIdOrNull(id) ?: return CasResult.NotFound(
      entityType = "User",
      id = id.toString(),
    )

    user.isActive = true

    if (cruManagementAreaOverrideId != null) {
      val override = cas1CruManagementAreaRepository.findByIdOrNull(cruManagementAreaOverrideId)
        ?: return CasResult.NotFound(entityType = "Cas1CruManagementArea", id = cruManagementAreaOverrideId.toString())
      user.cruManagementArea = override
      user.cruManagementAreaOverride = override
    } else {
      user.cruManagementArea = user.apArea!!.defaultCruManagementArea
      user.cruManagementAreaOverride = null
    }

    userRepository.save(user)

    updateUserRolesAndQualificationsForUser(
      user = user,
      roles = roles,
      qualifications = qualifications,
    )

    return CasResult.Success(user)
  }

  private fun updateUserRolesAndQualificationsForUser(
    user: UserEntity,
    roles: List<ApprovedPremisesUserRole>,
    qualifications: List<APIUserQualification>,
  ) {
    clearQualifications(user)
    clearRolesForService(user, ServiceName.approvedPremises)

    roles.forEach {
      this.addRoleToUser(user, UserRole.valueOf(it))
    }

    qualifications.forEach {
      this.addQualificationToUser(user, transformQualifications(it))
    }
  }

  fun updateUserFromDelius(
    id: UUID,
    forService: ServiceName,
  ): CasResult<GetUserResponse> {
    val user = userRepository.findByIdOrNull(id) ?: return CasResult.NotFound(entityType = "User", id = id.toString())
    return CasResult.Success(updateUserFromDelius(user, forService))
  }

  fun updateUserFromDelius(
    user: UserEntity,
    forService: ServiceName,
  ) = when (val clientResult = apDeliusContextApiClient.getStaffDetail(user.deliusUsername)) {
    is ClientResult.Failure.StatusCode -> {
      if (clientResult.status == HttpStatus.NOT_FOUND) {
        GetUserResponse.StaffRecordNotFound
      } else {
        clientResult.throwException()
      }
    }

    is ClientResult.Failure -> clientResult.throwException()
    is ClientResult.Success -> {
      val staffDetail = clientResult.body
      GetUserResponse.Success(updateUserEntity(user, staffDetail, forService, user.deliusUsername))
    }
  }

  @SuppressWarnings("TooGenericExceptionThrown")
  fun updateUserPduById(id: UUID): AuthorisableActionResult<UserEntity> {
    val user = userRepository.findByIdOrNull(id) ?: return AuthorisableActionResult.NotFound()

    val deliusUser = when (val staffUserDetailsResponse = apDeliusContextApiClient.getStaffDetail(user.deliusUsername)) {
      is ClientResult.Success -> staffUserDetailsResponse.body
      is ClientResult.Failure -> staffUserDetailsResponse.throwException()
    }

    when (val pduResult = findDeliusUserLastPdu(deliusUser)) {
      is CasSimpleResult.Failure -> {
        throw Exception(pduResult.message)
      }

      is CasSimpleResult.Success -> {
        if (user.probationDeliveryUnit?.id != pduResult.value.id) {
          user.probationDeliveryUnit = pduResult.value
          userRepository.save(user)
        }
      }
    }

    return AuthorisableActionResult.Success(user)
  }

  fun updateUserEntity(
    user: UserEntity,
    staffDetail: StaffDetail,
    forService: ServiceName,
    username: String,
  ): UserEntity {
    user.name = staffDetail.name.deliusName()
    user.email = staffDetail.email
    user.telephoneNumber = staffDetail.telephoneNumber
    user.deliusStaffCode = staffDetail.code
    user.teamCodes = staffDetail.teamCodes()

    staffDetail.probationArea.let { probationArea ->
      findProbationRegionFromArea(probationArea.code)?.let { probationRegion ->
        user.probationRegion = probationRegion
      }
    }

    val pduResult = findDeliusUserLastPdu(staffDetail)
    if (pduResult is CasSimpleResult.Success) {
      user.probationDeliveryUnit = pduResult.value
    }

    if (forService == ServiceName.approvedPremises) {
      val apArea = cas1ApAreaMappingService.determineApArea(user.probationRegion, staffDetail.teamCodes(), username)
      user.apArea = apArea
      if (user.cruManagementAreaOverride == null) {
        user.cruManagementArea = apArea.defaultCruManagementArea
      }
    }

    return userRepository.save(user)
  }

  fun getUserWorkloads(userIds: List<UUID>): Map<UUID, UserWorkload> {
    return userRepository.findWorkloadForUserIds(userIds).associate {
      it.getUserId() to UserWorkload(
        numTasksPending = listOf(
          it.getPendingAssessments(),
          it.getPendingPlacementRequests(),
          it.getPendingPlacementApplications(),
        ).sum(),
        numTasksCompleted7Days = listOf(
          it.getCompletedAssessmentsInTheLastSevenDays(),
          it.getCompletedPlacementApplicationsInTheLastSevenDays(),
          it.getCompletedPlacementRequestsInTheLastSevenDays(),
        ).sum(),
        numTasksCompleted30Days = listOf(
          it.getCompletedAssessmentsInTheLastThirtyDays(),
          it.getCompletedPlacementApplicationsInTheLastThirtyDays(),
          it.getCompletedPlacementRequestsInTheLastThirtyDays(),
        ).sum(),
      )
    }
  }

  @SuppressWarnings("TooGenericExceptionThrown")
  fun getExistingUserOrCreate(username: String): GetUserResponse {
    val normalisedUsername = username.uppercase()

    val existingUser = userRepository.findByDeliusUsername(normalisedUsername)
    if (existingUser != null) return GetUserResponse.Success(existingUser)

    val staffUserDetailsResponse = apDeliusContextApiClient.getStaffDetail(normalisedUsername)

    val staffUserDetails = when (staffUserDetailsResponse) {
      is ClientResult.Success -> staffUserDetailsResponse.body
      is ClientResult.Failure.StatusCode -> {
        if (staffUserDetailsResponse.status == HttpStatus.NOT_FOUND) {
          return GetUserResponse.StaffRecordNotFound
        } else {
          staffUserDetailsResponse.throwException()
        }
      }

      is ClientResult.Failure -> staffUserDetailsResponse.throwException()
    }

    var staffProbationRegion = findProbationRegionFromArea(staffUserDetails.probationArea.code)

    if (staffProbationRegion == null) {
      if (userServiceConfig.assignDefaultRegionToUsersWithUnknownRegion) {
        log.warn("Unknown probation region code '${staffUserDetails.probationArea.code}' for user '$normalisedUsername', assigning a default region of 'North West'.")
        staffProbationRegion = probationRegionRepository.findByName("North West")!!
      } else {
        throw RuntimeException("Unknown probation region code '${staffUserDetails.probationArea.code}' for user '$normalisedUsername'")
      }
    }

    val pduResult = findDeliusUserLastPdu(staffUserDetails)

    val apArea = cas1ApAreaMappingService.determineApArea(staffProbationRegion, staffUserDetails, normalisedUsername)

    val savedUser = userRepository.save(
      UserEntity(
        id = UUID.randomUUID(),
        name = staffUserDetails.name.deliusName(),
        deliusUsername = normalisedUsername,
        deliusStaffCode = staffUserDetails.code,
        email = staffUserDetails.email,
        telephoneNumber = staffUserDetails.telephoneNumber,
        applications = mutableListOf(),
        roles = mutableListOf(),
        qualifications = mutableListOf(),
        probationRegion = staffProbationRegion,
        probationDeliveryUnit = when (pduResult) {
          is CasSimpleResult.Failure -> null
          is CasSimpleResult.Success -> pduResult.value
        },
        isActive = true,
        apArea = apArea,
        cruManagementArea = apArea.defaultCruManagementArea,
        cruManagementAreaOverride = null,
        teamCodes = staffUserDetails.teamCodes(),
        createdAt = OffsetDateTime.now(),
        updatedAt = null,
      ),
    )
    return GetUserResponse.Success(savedUser, createdOnGet = true)
  }

  private fun findProbationRegionFromArea(code: String): ProbationRegionEntity? {
    return probationAreaProbationRegionMappingRepository
      .findByProbationAreaDeliusCode(code)?.probationRegion
  }

  private fun findDeliusUserLastPdu(staffDetail: StaffDetail): CasSimpleResult<ProbationDeliveryUnitEntity> {
    val activeTeamsNewestFirst = staffDetail.activeTeamsNewestFirst()
    activeTeamsNewestFirst.forEach { team ->
      val pdu =
        team.borough?.let { probationDeliveryUnitRepository.findByDeliusCode(it.code) }

      if (pdu != null) {
        return CasSimpleResult.Success(pdu)
      }
    }

    val teamsToLog = activeTeamsNewestFirst.joinToString(",") {
      " ${it.name} (${it.code}) with borough ${it.borough?.description} (${it.borough?.code})"
    }

    return CasSimpleResult.Failure(
      "PDU could not be determined for user ${staffDetail.username}. " +
        "Considered ${activeTeamsNewestFirst.size} teams$teamsToLog",
    )
  }

  fun addRoleToUser(user: UserEntity, role: UserRole) {
    if (user.hasRole(role)) return

    user.roles.add(
      userRoleAssignmentRepository.save(
        UserRoleAssignmentEntity(
          id = UUID.randomUUID(),
          user = user,
          role = role,
        ),
      ),
    )
  }

  fun removeRoleFromUser(user: UserEntity, role: UserRole) {
    user.roles.filter { it.role == role }.forEach { roleAssignment ->
      user.roles.remove(roleAssignment)
      userRoleAssignmentRepository.delete(roleAssignment)
    }
  }

  fun addQualificationToUser(user: UserEntity, qualification: UserQualification) {
    if (user.hasQualification(qualification)) return

    user.qualifications.add(
      userQualificationAssignmentRepository.save(
        UserQualificationAssignmentEntity(
          id = UUID.randomUUID(),
          user = user,
          qualification = qualification,
        ),
      ),
    )
  }

  fun clearRolesForService(user: UserEntity, service: ServiceName) {
    val rolesToClear = UserRole.getAllRolesForService(service)
    user.roles.filter { rolesToClear.contains(it.role) }.forEach {
      userRoleAssignmentRepository.delete(it)
      user.roles.remove(it)
    }
  }

  fun clearQualifications(user: UserEntity) {
    userQualificationAssignmentRepository.deleteAllById(user.qualifications.map(UserQualificationAssignmentEntity::id))
    user.qualifications.clear()
  }

  sealed interface GetUserResponse {
    data object StaffRecordNotFound : GetUserResponse
    data class Success(val user: UserEntity, val createdOnGet: Boolean = false) : GetUserResponse
  }
}
