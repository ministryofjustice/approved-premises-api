package uk.gov.justice.digital.hmpps.approvedpremisesapi.unit.service

import io.mockk.Called
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import org.springframework.data.repository.findByIdOrNull
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ApprovedPremisesUserRole
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ServiceName
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.ApDeliusContextApiClient
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.ClientResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.config.AuthAwareAuthenticationToken
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.ApAreaEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.ProbationAreaProbationRegionMapping
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.ProbationDeliveryUnitEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.ProbationRegionEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.StaffDetailFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.TeamFactoryDeliusContext
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.UserEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.UserQualificationAssignmentEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.UserRoleAssignmentEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.cas1.Cas1CruManagementAreaEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.Cas1CruManagementAreaRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ProbationAreaProbationRegionMappingRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ProbationDeliveryUnitRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ProbationRegionRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserPermission
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserQualification
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserQualificationAssignmentEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserQualificationAssignmentRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserRepository.RoleAssignmentByUsername
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserRole
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserRoleAssignmentEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserRoleAssignmentRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.deliuscontext.Borough
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.deliuscontext.PersonName
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.deliuscontext.ProbationArea
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.deliuscontext.StaffDetail
import uk.gov.justice.digital.hmpps.approvedpremisesapi.results.CasResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.HttpAuthService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.OffenderService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.RequestContextService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.UserService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.UserService.GetUserResponse
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.UserServiceConfig
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1.Cas1ApAreaMappingService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.isWithinTheLastMinute
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.randomStringMultiCaseWithNumbers
import java.time.LocalDate
import java.util.UUID
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.UserQualification as APIUserQualification

class UserServiceTest {
  private val mockUserServiceConfig = mockk<UserServiceConfig>()
  private val mockRequestContextService = mockk<RequestContextService>()
  private val mockHttpAuthService = mockk<HttpAuthService>()
  private val mockOffenderService = mockk<OffenderService>()
  private val mockUserRepository = mockk<UserRepository>()
  private val mockUserRoleAssignmentRepository = mockk<UserRoleAssignmentRepository>()
  private val mockUserQualificationAssignmentRepository = mockk<UserQualificationAssignmentRepository>()
  private val mockProbationRegionRepository = mockk<ProbationRegionRepository>()
  private val mockProbationAreaProbationRegionMappingRepository = mockk<ProbationAreaProbationRegionMappingRepository>()
  private val mockProbationDeliveryUnitRepository = mockk<ProbationDeliveryUnitRepository>()
  private val mockCas1ApAreaMappingService = mockk<Cas1ApAreaMappingService>()
  private val mockApDeliusContextApiClient = mockk<ApDeliusContextApiClient>()
  private val mockCas1CruManagementAreaRepository = mockk<Cas1CruManagementAreaRepository>()

  private val userService = UserService(
    mockUserServiceConfig,
    mockRequestContextService,
    mockHttpAuthService,
    mockOffenderService,
    mockUserRepository,
    mockUserRoleAssignmentRepository,
    mockUserQualificationAssignmentRepository,
    mockProbationRegionRepository,
    mockProbationAreaProbationRegionMappingRepository,
    mockCas1ApAreaMappingService,
    mockProbationDeliveryUnitRepository,
    mockApDeliusContextApiClient,
    mockCas1CruManagementAreaRepository,
  )

  @BeforeEach
  fun setup() {
    every { mockUserServiceConfig.assignDefaultRegionToUsersWithUnknownRegion } returns false
  }

  @Nested
  inner class GetExistingUserOrCreate {

    @Test
    fun `getExistingUserOrCreate when user has no delius staff record`() {
      val username = "SOMEPERSON"

      every { mockUserRepository.findByDeliusUsername(username) } returns null
      every { mockApDeliusContextApiClient.getStaffDetail(username) } returns ClientResult.Failure.StatusCode(
        HttpMethod.GET,
        "/secure/staff/username",
        HttpStatus.NOT_FOUND,
        body = null,
      )

      val result = userService.getExistingUserOrCreate(username)

      assertThat(result).isInstanceOf(GetUserResponse.StaffRecordNotFound::class.java)
    }

    @Test
    fun `getExistingUserOrCreate when clientResult is failure throws error`() {
      val username = "SOMEPERSON"

      every { mockUserRepository.findByDeliusUsername(username) } returns null
      every { mockApDeliusContextApiClient.getStaffDetail(username) } returns ClientResult.Failure.PreemptiveCacheTimeout("", "", 0)

      assertThrows<RuntimeException> { userService.getExistingUserOrCreate(username) }
    }

    @Test
    fun `getExistingUserOrCreate returns existing user`() {
      val username = "SOMEPERSON"

      val user = UserEntityFactory()
        .withDefaults()
        .produce()

      every { mockUserRepository.findByDeliusUsername(username) } returns user

      val result = userService.getExistingUserOrCreate(username)

      assertThat(result).isInstanceOf(GetUserResponse.Success::class.java)
      assertThat((result as GetUserResponse.Success).user).isEqualTo(user)

      verify(exactly = 0) { mockUserRepository.save(any()) }
    }

    @Test
    fun `getExistingUserOrCreate creates new user`() {
      val username = "SOMEPERSON"
      val pduDeliusCode = randomStringMultiCaseWithNumbers(7)

      val borough = Borough(code = pduDeliusCode, description = randomStringMultiCaseWithNumbers(10))
      every { mockUserRepository.findByDeliusUsername(username) } returns null
      every { mockUserRepository.save(any()) } answers { it.invocation.args[0] as UserEntity }

      val deliusUser = StaffDetailFactory.staffDetail(
        deliusUsername = username,
        name = PersonName("Jim", "Jimmerson"),
        probationArea = ProbationArea(code = "AREACODE", description = "description"),
        teams = listOf(
          TeamFactoryDeliusContext.team(code = "TC1", borough = borough),
          TeamFactoryDeliusContext.team(code = "TC2", borough = borough),
        ),
      )

      every { mockApDeliusContextApiClient.getStaffDetail(username) } returns ClientResult.Success(
        HttpStatus.OK,
        deliusUser,
      )

      val probationRegion = ProbationRegionEntityFactory()
        .withApArea(ApAreaEntityFactory().produce())
        .produce()

      every { mockProbationAreaProbationRegionMappingRepository.findByProbationAreaDeliusCode("AREACODE") } returns ProbationAreaProbationRegionMapping(
        probationRegion = probationRegion,
        probationAreaDeliusCode = "AREACODE",
      ).toEntity()

      val apAreaDefaultCruManagementArea = Cas1CruManagementAreaEntityFactory().produce()
      val apArea = ApAreaEntityFactory().withDefaultCruManagementArea(apAreaDefaultCruManagementArea).produce()

      every { mockCas1ApAreaMappingService.determineApArea(probationRegion, deliusUser, username) } returns apArea

      every { mockProbationDeliveryUnitRepository.findByDeliusCode(pduDeliusCode) } returns ProbationDeliveryUnitEntityFactory()
        .withProbationRegion(probationRegion)
        .withDeliusCode(pduDeliusCode)
        .produce()

      val result = userService.getExistingUserOrCreate(username)

      assertThat(result).isInstanceOf(GetUserResponse.Success::class.java)
      result as GetUserResponse.Success

      assertThat(result.createdOnGet).isEqualTo(true)

      assertThat(result.user.name).isEqualTo("Jim Jimmerson")
      assertThat(result.user.teamCodes).isEqualTo(listOf("TC1", "TC2"))
      assertThat(result.user.apArea).isEqualTo(apArea)
      assertThat(result.user.cruManagementArea).isEqualTo(apAreaDefaultCruManagementArea)
      assertThat(result.user.probationDeliveryUnit?.deliusCode).isEqualTo(pduDeliusCode)
      assertThat(result.user.createdAt).isWithinTheLastMinute()

      verify(exactly = 1) { mockApDeliusContextApiClient.getStaffDetail(username) }
      verify(exactly = 1) { mockUserRepository.save(any()) }
      verify(exactly = 1) { mockProbationAreaProbationRegionMappingRepository.findByProbationAreaDeliusCode(any()) }
    }

    @Test
    fun `getExistingUserOrCreate throws internal server error problem if can't resolve region`() {
      val username = "SOMEPERSON"
      val pduDeliusCode = randomStringMultiCaseWithNumbers(7)
      val borough = Borough(code = pduDeliusCode, description = randomStringMultiCaseWithNumbers(10))

      every { mockUserRepository.findByDeliusUsername(username) } returns null
      every { mockUserRepository.save(any()) } answers { it.invocation.args[0] as UserEntity }

      val deliusUser = StaffDetailFactory.staffDetail(
        deliusUsername = username,
        name = PersonName("Jim", "Jimmerson"),
        probationArea = ProbationArea(code = "AREACODE", description = "Description"),
        teams = listOf(
          TeamFactoryDeliusContext.team(code = "TC1", borough = borough),
          TeamFactoryDeliusContext.team(code = "TC2", borough = borough),
        ),
      )

      every { mockApDeliusContextApiClient.getStaffDetail(username) } returns ClientResult.Success(
        HttpStatus.OK,
        deliusUser,
      )

      every { mockProbationAreaProbationRegionMappingRepository.findByProbationAreaDeliusCode("AREACODE") } returns null

      assertThatThrownBy {
        userService.getExistingUserOrCreate(username)
      }
        .hasMessage("Unknown probation region code 'AREACODE' for user 'SOMEPERSON'")
        .isInstanceOf(RuntimeException::class.java)
    }
  }

  @Nested
  inner class GetUserForRequest {

    @Test
    fun `getUserForRequest returns existing User when exists`() {
      val username = "SOMEPERSON"
      val mockPrincipal = mockk<AuthAwareAuthenticationToken>()

      every { mockRequestContextService.getServiceForRequest() } returns ServiceName.approvedPremises

      every { mockHttpAuthService.getDeliusPrincipalOrThrow() } returns mockPrincipal
      every { mockPrincipal.name } returns username

      val user = UserEntityFactory()
        .withDefaultProbationRegion()
        .produce()

      every { mockUserRepository.findByDeliusUsername(username) } returns user

      assertThat(userService.getUserForRequest()).isEqualTo(user)

      verify(exactly = 0) { mockApDeliusContextApiClient.getStaffDetail(username) }
      verify(exactly = 0) { mockUserRepository.save(any()) }
    }

    @Test
    fun `getUserForRequest returns new persisted User when one does not already exist`() {
      val username = "SOMEPERSON"
      val mockPrincipal = mockk<AuthAwareAuthenticationToken>()

      every { mockRequestContextService.getServiceForRequest() } returns ServiceName.approvedPremises

      every { mockHttpAuthService.getDeliusPrincipalOrThrow() } returns mockPrincipal
      every { mockPrincipal.name } returns username

      every { mockUserRepository.findByDeliusUsername(username) } returns null
      every { mockUserRepository.save(any()) } answers { it.invocation.args[0] as UserEntity }

      val deliusUser = StaffDetailFactory.staffDetail(
        deliusUsername = username,
        name = PersonName("Jim", "Jimmerson"),
        probationArea = ProbationArea(code = "AREACODE", description = "AREADESCRIPTION"),
        teams = emptyList(),
      )

      every { mockApDeliusContextApiClient.getStaffDetail(username) } returns ClientResult.Success(
        HttpStatus.OK,
        deliusUser,
      )

      val probationRegion = ProbationRegionEntityFactory()
        .withApArea(ApAreaEntityFactory().produce())
        .produce()

      every { mockProbationAreaProbationRegionMappingRepository.findByProbationAreaDeliusCode("AREACODE") } returns ProbationAreaProbationRegionMapping(
        probationRegion = probationRegion,
        probationAreaDeliusCode = "AREACODE",
      ).toEntity()

      val apArea = ApAreaEntityFactory().produce()

      every { mockCas1ApAreaMappingService.determineApArea(probationRegion, deliusUser, username) } returns apArea

      assertThat(userService.getUserForRequest()).matches {
        it.name == "Jim Jimmerson"
      }

      verify(exactly = 1) { mockApDeliusContextApiClient.getStaffDetail(username) }
      verify(exactly = 1) { mockUserRepository.save(any()) }
      verify(exactly = 1) { mockProbationAreaProbationRegionMappingRepository.findByProbationAreaDeliusCode(any()) }
    }

    @Test
    fun `getUserForRequest assigns a default role of CAS3_REFERRER for CAS3 if the user has no Temporary Accommodation roles`() {
      val username = "SOMEPERSON"
      val mockPrincipal = mockk<AuthAwareAuthenticationToken>()

      every { mockRequestContextService.getServiceForRequest() } returns ServiceName.temporaryAccommodation

      every { mockHttpAuthService.getDeliusPrincipalOrThrow() } returns mockPrincipal
      every { mockPrincipal.name } returns username

      val expectedUser = UserEntityFactory()
        .withDefaults()
        .produce()

      val roleAssignment = UserRoleAssignmentEntityFactory()
        .withUser(expectedUser)
        .withRole(UserRole.CAS1_WORKFLOW_MANAGER)
        .produce()

      expectedUser.roles += roleAssignment

      every { mockUserRepository.findByDeliusUsername(username) } returns expectedUser

      every { mockUserRoleAssignmentRepository.save(any()) } returnsArgument 0

      val actualUser = userService.getUserForRequest()

      assertThat(actualUser).isEqualTo(expectedUser)
      assertThat(actualUser.hasRole(UserRole.CAS3_REFERRER)).isTrue

      verify(exactly = 1) {
        mockUserRoleAssignmentRepository.save(
          match {
            it.user == actualUser &&
              it.role == UserRole.CAS3_REFERRER
          },
        )
      }
    }

    @Test
    fun `getUserForRequest does not assign a default role for CAS3 if the user already has a Temporary Accommodation role`() {
      val username = "SOMEPERSON"
      val mockPrincipal = mockk<AuthAwareAuthenticationToken>()

      every { mockRequestContextService.getServiceForRequest() } returns ServiceName.temporaryAccommodation

      every { mockHttpAuthService.getDeliusPrincipalOrThrow() } returns mockPrincipal
      every { mockPrincipal.name } returns username

      val expectedUser = UserEntityFactory()
        .withDefaults()
        .produce()

      val roleAssignment = UserRoleAssignmentEntityFactory()
        .withUser(expectedUser)
        .withRole(UserRole.CAS3_ASSESSOR)
        .produce()

      expectedUser.roles += roleAssignment

      every { mockUserRepository.findByDeliusUsername(username) } returns expectedUser

      every { mockUserRoleAssignmentRepository.save(any()) } returnsArgument 0

      val actualUser = userService.getUserForRequest()

      assertThat(actualUser).isEqualTo(expectedUser)
      assertThat(actualUser.hasRole(UserRole.CAS3_ASSESSOR)).isTrue()

      verify(exactly = 0) {
        mockUserRoleAssignmentRepository.save(any())
      }
    }

    @ParameterizedTest
    @EnumSource(value = ServiceName::class, mode = EnumSource.Mode.EXCLUDE, names = ["temporaryAccommodation"])
    fun `getUserForRequest does not assign a default role if not CAS3`(serviceName: ServiceName) {
      val username = "SOMEPERSON"
      val mockPrincipal = mockk<AuthAwareAuthenticationToken>()

      every { mockRequestContextService.getServiceForRequest() } returns serviceName

      every { mockHttpAuthService.getDeliusPrincipalOrThrow() } returns mockPrincipal
      every { mockPrincipal.name } returns username

      val expectedUser = UserEntityFactory()
        .withDefaults()
        .produce()

      every { mockUserRepository.findByDeliusUsername(username) } returns expectedUser

      every { mockUserRoleAssignmentRepository.save(any()) } returnsArgument 0

      val actualUser = userService.getUserForRequest()

      assertThat(actualUser).isEqualTo(expectedUser)
      assertThat(actualUser.roles).isEmpty()

      verify(exactly = 0) {
        mockUserRoleAssignmentRepository.save(any())
      }
    }
  }

  @Nested
  inner class GetUserForRequestOrNull {

    @Test
    fun `getUserForRequestOrNull returns User when exists, does not call Community API or save`() {
      val username = "SOMEPERSON"
      val mockPrincipal = mockk<AuthAwareAuthenticationToken>()

      every { mockHttpAuthService.getDeliusPrincipalOrNull() } returns mockPrincipal
      every { mockPrincipal.name } returns username

      val user = UserEntityFactory()
        .withDefaults()
        .produce()

      every { mockUserRepository.findByDeliusUsername(username) } returns user

      assertThat(userService.getUserForRequestOrNull()).isEqualTo(user)

      verify(exactly = 0) { mockApDeliusContextApiClient.getStaffDetail(username) }
      verify(exactly = 0) { mockUserRepository.save(any()) }
    }

    @Test
    fun `getUserForRequestOrNull returns null when User does not already exist, does not call Community API or save`() {
      val username = "SOMEPERSON"
      val mockPrincipal = mockk<AuthAwareAuthenticationToken>()

      every { mockHttpAuthService.getDeliusPrincipalOrNull() } returns mockPrincipal
      every { mockPrincipal.name } returns username

      every { mockUserRepository.findByDeliusUsername(username) } returns null

      assertThat(userService.getUserForRequestOrNull()).isNull()

      verify(exactly = 0) { mockApDeliusContextApiClient.getStaffDetail(username) }
      verify(exactly = 0) { mockUserRepository.save(any()) }
    }

    @Test
    fun `getUserForRequestOrNull returns null when no principal is available`() {
      every { mockHttpAuthService.getDeliusPrincipalOrNull() } returns null

      assertThat(userService.getUserForRequestOrNull()).isNull()
    }
  }

  private data class RoleAssignmentByNameImpl(
    override val userId: UUID,
    override val roleName: String?,
  ) : RoleAssignmentByUsername

  @Nested
  inner class GetUserForRequestVersionInfo {

    @Test
    fun `getUserForRequestVersion returns same value for version when roles are duplicated`() {
      val username = "SOMEPERSON"
      val userId = UUID.randomUUID()
      val mockPrincipal = mockk<AuthAwareAuthenticationToken>()

      every { mockHttpAuthService.getDeliusPrincipalOrNull() } returns mockPrincipal
      every { mockPrincipal.name } returns username

      every { mockUserRepository.findRoleAssignmentByUsername(username) } returns listOf(
        RoleAssignmentByNameImpl(
          userId = userId,
          roleName = UserRole.CAS1_USER_MANAGER.name,
        ),
        RoleAssignmentByNameImpl(
          userId = userId,
          roleName = UserRole.CAS1_APPEALS_MANAGER.name,
        ),
      )

      val result1 = userService.getUserForRequestVersionInfo()!!
      val version1 = result1.version

      every { mockUserRepository.findRoleAssignmentByUsername(username) } returns listOf(
        RoleAssignmentByNameImpl(
          userId = userId,
          roleName = UserRole.CAS1_USER_MANAGER.name,
        ),
        RoleAssignmentByNameImpl(
          userId = userId,
          roleName = UserRole.CAS1_APPEALS_MANAGER.name,
        ),
      )

      val result2 = userService.getUserForRequestVersionInfo()!!
      val version2 = result2.version

      assertThat(result1.userId).isEqualTo(userId)
      assertThat(result2.userId).isEqualTo(userId)
      assertThat(version1).isEqualTo(version2)
    }

    @Test
    fun `getUserForRequestVersion returns same value for version when roles are in different order`() {
      val username = "SOMEPERSON"
      val userId = UUID.randomUUID()
      val mockPrincipal = mockk<AuthAwareAuthenticationToken>()

      every { mockHttpAuthService.getDeliusPrincipalOrNull() } returns mockPrincipal
      every { mockPrincipal.name } returns username

      every { mockUserRepository.findRoleAssignmentByUsername(username) } returns listOf(
        RoleAssignmentByNameImpl(
          userId = userId,
          roleName = UserRole.CAS1_USER_MANAGER.name,
        ),
        RoleAssignmentByNameImpl(
          userId = userId,
          roleName = UserRole.CAS1_APPEALS_MANAGER.name,
        ),
      )

      val version1 = userService.getUserForRequestVersionInfo()!!.version

      every { mockUserRepository.findRoleAssignmentByUsername(username) } returns listOf(
        RoleAssignmentByNameImpl(
          userId = userId,
          roleName = UserRole.CAS1_APPEALS_MANAGER.name,
        ),
        RoleAssignmentByNameImpl(
          userId = userId,
          roleName = UserRole.CAS1_USER_MANAGER.name,
        ),
      )

      val version2 = userService.getUserForRequestVersionInfo()!!.version

      assertThat(version1).isEqualTo(version2)
    }

    @Test
    fun `getUserForRequestVersion returns userVersion and ID when no roles set`() {
      val username = "SOMEPERSON"
      val userId = UUID.randomUUID()
      val mockPrincipal = mockk<AuthAwareAuthenticationToken>()

      every { mockHttpAuthService.getDeliusPrincipalOrNull() } returns mockPrincipal
      every { mockPrincipal.name } returns username

      every { mockUserRepository.findRoleAssignmentByUsername(username) } returns listOf(
        RoleAssignmentByNameImpl(
          userId = userId,
          roleName = null,
        ),
      )

      val result = userService.getUserForRequestVersionInfo()!!
      assertThat(result.userId).isEqualTo(userId)
      assertThat(result.version).isEqualTo(993)
    }

    @Test
    fun `getUserForRequestVersion returns null when user isn't found`() {
      val username = "SOMEPERSON"
      val mockPrincipal = mockk<AuthAwareAuthenticationToken>()

      every { mockHttpAuthService.getDeliusPrincipalOrNull() } returns mockPrincipal
      every { mockPrincipal.name } returns username

      every { mockUserRepository.findRoleAssignmentByUsername(username) } returns emptyList()

      val result = userService.getUserForRequestVersionInfo()
      assertThat(result).isNull()
    }

    @Test
    fun `getUserForRequestOrNull returns null when no principal is available`() {
      every { mockHttpAuthService.getDeliusPrincipalOrNull() } returns null

      assertThat(userService.getUserForRequestVersionInfo()).isNull()
    }
  }

  @Nested
  inner class UpdateUserPduFromCommunityApiById {

    val user = UserEntityFactory().withDefaults().produce()

    @BeforeEach
    fun setup() {
      every { mockUserRepository.findByIdOrNull(user.id) } returns user
    }

    @Test
    fun `Throw exception if can't determine PDU, no teams`() {
      val deliusUser = StaffDetailFactory.staffDetail(
        deliusUsername = "theusername",
        teams = emptyList(),
      )

      every { mockApDeliusContextApiClient.getStaffDetail(user.deliusUsername) } returns ClientResult.Success(
        HttpStatus.OK,
        deliusUser,
      )

      assertThatThrownBy {
        userService.updateUserPduById(user.id)
      }.hasMessage("PDU could not be determined for user theusername. Considered 0 teams")
    }

    @Test
    fun `Throw exception if can't determine PDU, no teams without end date`() {
      val deliusUser = StaffDetailFactory.staffDetail(
        deliusUsername = "theusername",
        teams = listOf(TeamFactoryDeliusContext.team(endDate = LocalDate.now())),
      )

      every { mockApDeliusContextApiClient.getStaffDetail(user.deliusUsername) } returns ClientResult.Success(
        HttpStatus.OK,
        deliusUser,
      )

      assertThatThrownBy {
        userService.updateUserPduById(user.id)
      }.hasMessage("PDU could not be determined for user theusername. Considered 0 teams")
    }

    @Test
    fun `Throw exception if no mapping for only team's borough`() {
      val deliusUser = StaffDetailFactory.staffDetail(
        deliusUsername = "theusername",
        teams = listOf(
          TeamFactoryDeliusContext.team(
            code = "team1",
            name = "team 1",
            borough = Borough(code = "boroughcode1", description = "borough1"),
            startDate = LocalDate.of(2024, 1, 1),
            endDate = null,
          ),
        ),
      )

      every { mockApDeliusContextApiClient.getStaffDetail(user.deliusUsername) } returns ClientResult.Success(
        HttpStatus.OK,
        deliusUser,
      )

      every { mockProbationDeliveryUnitRepository.findByDeliusCode("boroughcode1") } returns null

      assertThatThrownBy {
        userService.updateUserPduById(user.id)
      }.hasMessage("PDU could not be determined for user theusername. Considered 1 teams team 1 (team1) with borough borough1 (boroughcode1)")
    }

    @Test
    fun `Throw exception if no mapping for any active team's borough`() {
      val deliusUser = StaffDetailFactory.staffDetail(
        deliusUsername = "theusername",
        teams = listOf(
          TeamFactoryDeliusContext.team(
            code = "team3",
            name = "team 3",
            borough = Borough(code = "boroughcode3", description = "borough3"),
            startDate = LocalDate.of(2024, 1, 3),
            endDate = LocalDate.of(2024, 1, 4),
          ),
          TeamFactoryDeliusContext.team(
            code = "team2",
            name = "team 2",
            borough = Borough(code = "boroughcode2", description = "borough2"),
            startDate = LocalDate.of(2024, 1, 2),
            endDate = null,
          ),
          TeamFactoryDeliusContext.team(
            code = "team1",
            name = "team 1",
            borough = Borough(code = "boroughcode1", description = "borough1"),
            startDate = LocalDate.of(2024, 1, 1),
            endDate = null,
          ),
        ),
      )

      every { mockApDeliusContextApiClient.getStaffDetail(user.deliusUsername) } returns ClientResult.Success(
        HttpStatus.OK,
        deliusUser,
      )

      every { mockProbationDeliveryUnitRepository.findByDeliusCode("boroughcode2") } returns null
      every { mockProbationDeliveryUnitRepository.findByDeliusCode("boroughcode1") } returns null

      assertThatThrownBy {
        userService.updateUserPduById(user.id)
      }.hasMessage("PDU could not be determined for user theusername. Considered 2 teams team 2 (team2) with borough borough2 (boroughcode2), team 1 (team1) with borough borough1 (boroughcode1)")
    }

    @Test
    fun `Update PDU using latest team that has a mapping`() {
      val deliusUser = StaffDetailFactory.staffDetail(
        deliusUsername = "theusername",
        teams = listOf(
          TeamFactoryDeliusContext.team(
            borough = Borough(code = "boroughcode2", description = "borough2"),
            startDate = LocalDate.of(2024, 1, 3),
            endDate = LocalDate.of(2024, 1, 4),
          ),
          TeamFactoryDeliusContext.team(
            borough = Borough(code = "nomapping", description = "nomapping"),
            startDate = LocalDate.of(2024, 1, 3),
            endDate = null,
          ),
          TeamFactoryDeliusContext.team(
            borough = Borough(code = "boroughcode2", description = "borough2"),
            startDate = LocalDate.of(2024, 1, 2),
            endDate = null,
          ),
          TeamFactoryDeliusContext.team(
            borough = Borough(code = "boroughcode1", description = "borough1"),
            startDate = LocalDate.of(2024, 1, 1),
            endDate = null,
          ),
        ),
      )

      every { mockApDeliusContextApiClient.getStaffDetail(user.deliusUsername) } returns ClientResult.Success(
        HttpStatus.OK,
        deliusUser,
      )

      every { mockProbationDeliveryUnitRepository.findByDeliusCode("nomapping") } returns null

      val pdu = ProbationDeliveryUnitEntityFactory()
        .withDefaults()
        .produce()
      every { mockProbationDeliveryUnitRepository.findByDeliusCode("boroughcode2") } returns pdu

      val persistedUser = slot<UserEntity>()
      every { mockUserRepository.save(capture(persistedUser)) } answers { it.invocation.args[0] as UserEntity }

      userService.updateUserPduById(user.id)

      assertThat(persistedUser.captured.probationDeliveryUnit).isEqualTo(pdu)
    }
  }

  @Nested
  inner class UpdateUserFromDelius {

    @BeforeEach
    fun setup() {
      every { mockUserRepository.save(any()) } returnsArgument 0
    }

    private val id = UUID.fromString("21b61d19-3a96-4b88-8df9-a5e89bc6fe73")
    private val email = "foo@example.com"
    private val telephoneNumber = "0123456789"
    private val username = "DeliusAPIUser"
    private val forename = "Jim"
    private val surname = "Jimmerson"
    private val staffCode = "STAFF1"

    private val staffDetail = StaffDetailFactory.staffDetail().copy(username = username)
    private val apArea = ApAreaEntityFactory().produce()
    private val regionName = "RegionName"

    private val probationRegion =
      ProbationRegionEntityFactory()
        .withDefaults()
        .withName(regionName)
        .produce()

    private val userFactory =
      UserEntityFactory()
        .withDefaults()
        .withDeliusUsername(username)
        .withName("$forename $surname")
        .withEmail(email)
        .withTelephoneNumber(telephoneNumber)
        .withDeliusStaffCode(staffCode)
        .withProbationRegion(probationRegion)

    val regionMappingEntity = ProbationAreaProbationRegionMapping(probationRegion = probationRegion).toEntity()

    val pdu =
      ProbationDeliveryUnitEntityFactory()
        .withId(UUID.fromString("99bc8c9c-ce26-4f0e-b994-a3b566c57b61"))
        .withDeliusCode("randomStringMultiCaseWithNumbers(8)")
        .withProbationRegion(probationRegion)
        .produce()

    val user =
      userFactory
        .withProbationRegion(probationRegion)
        .withTeamCodes(staffDetail.teamCodes())
        .produce()

    @ParameterizedTest
    @EnumSource(ServiceName::class)
    fun `it always calls save when a result is returned from delius`(serviceName: ServiceName) {
      val clientResultSuccess = ClientResult.Success(HttpStatus.OK, staffDetail)

      every { mockApDeliusContextApiClient.getStaffDetail(username) } returns clientResultSuccess
      every { mockUserRepository.findByIdOrNull(id) } returns user
      every { mockProbationAreaProbationRegionMappingRepository.findByProbationAreaDeliusCode(any()) } returns regionMappingEntity
      every { mockCas1ApAreaMappingService.determineApArea(any(), any(StaffDetail::class), any()) } returns apArea
      every { mockUserRepository.save(any()) } returnsArgument 0
      every { mockProbationDeliveryUnitRepository.findByDeliusCode(any()) } returns pdu

      val newApAreaDefaultCruManagementArea = Cas1CruManagementAreaEntityFactory().produce()
      val newApAreaForCas1 = ApAreaEntityFactory().withDefaultCruManagementArea(newApAreaDefaultCruManagementArea).produce()
      if (serviceName == ServiceName.approvedPremises) {
        every {
          mockCas1ApAreaMappingService.determineApArea(probationRegion, user.teamCodes!!, user.deliusUsername)
        } returns newApAreaForCas1
      }

      val result = userService.updateUserFromDelius(id, serviceName)

      verify(exactly = 1) { mockApDeliusContextApiClient.getStaffDetail(username) }
      verify(exactly = 1) { mockUserRepository.save(any()) }

      assertThat(result).isInstanceOf(CasResult.Success::class.java)
      val getUserResponse = (result as CasResult.Success).value

      assertThat(getUserResponse).isInstanceOf(GetUserResponse::class.java)
      val entity = (getUserResponse as GetUserResponse.Success).user

      assertThat(entity.id).isEqualTo(user.id)
      assertThat(entity.name).isEqualTo(staffDetail.name.deliusName())
      assertThat(entity.deliusUsername).isEqualTo(staffDetail.username)
      assertThat(entity.email).isEqualTo(staffDetail.email)
      assertThat(entity.telephoneNumber).isEqualTo(staffDetail.telephoneNumber)
      assertThat(entity.deliusStaffCode).isEqualTo(staffDetail.code)
      assertThat(entity.probationRegion.name).isEqualTo(probationRegion.name)
      assertThat(entity.probationDeliveryUnit?.id).isEqualTo(pdu.id)
      assertThat(entity.teamCodes ?: emptyList()).isEqualTo(staffDetail.teamCodes())

      if (serviceName == ServiceName.approvedPremises) {
        assertThat(entity.apArea).isEqualTo(newApAreaForCas1)
        assertThat(entity.cruManagementArea).isEqualTo(newApAreaDefaultCruManagementArea)
      } else {
        assertThat(entity.apArea).isNull()
        assertThat(entity.cruManagementArea).isNull()
      }
    }

    @Test
    fun `it doesn't update the cru management area if an override is set`() {
      val cruManagementAreaOverride = Cas1CruManagementAreaEntityFactory().produce()

      val user =
        userFactory
          .withProbationRegion(probationRegion)
          .withTeamCodes(staffDetail.teamCodes())
          .withCruManagementArea(cruManagementAreaOverride)
          .withCruManagementAreaOverride(cruManagementAreaOverride)
          .produce()

      val clientResultSuccess = ClientResult.Success(HttpStatus.OK, staffDetail)

      every { mockApDeliusContextApiClient.getStaffDetail(username) } returns clientResultSuccess
      every { mockUserRepository.findByIdOrNull(id) } returns user
      every { mockProbationAreaProbationRegionMappingRepository.findByProbationAreaDeliusCode(any()) } returns regionMappingEntity
      every { mockUserRepository.save(any()) } returnsArgument 0
      every { mockProbationDeliveryUnitRepository.findByDeliusCode(any()) } returns pdu

      val apAreaDefaultCruManagementArea = Cas1CruManagementAreaEntityFactory().produce()
      val apAreaForCas1 = ApAreaEntityFactory().withDefaultCruManagementArea(apAreaDefaultCruManagementArea).produce()

      every {
        mockCas1ApAreaMappingService.determineApArea(probationRegion, user.teamCodes!!, user.deliusUsername)
      } returns apAreaForCas1

      val result = userService.updateUserFromDelius(id, ServiceName.approvedPremises)

      val entity = ((result as CasResult.Success<GetUserResponse>).value as GetUserResponse.Success).user

      assertThat(entity.id).isEqualTo(user.id)
      assertThat(entity.cruManagementArea).isEqualTo(cruManagementAreaOverride)
      assertThat(entity.cruManagementAreaOverride).isEqualTo(cruManagementAreaOverride)
    }

    @Test
    fun `it stores a null email address if missing from Approved-premises-and-delius API`() {
      every { mockUserRepository.findByIdOrNull(id) } returns user
      every { mockProbationAreaProbationRegionMappingRepository.findByProbationAreaDeliusCode(any()) } returns regionMappingEntity
      every { mockProbationDeliveryUnitRepository.findByDeliusCode(any()) } returns pdu

      val staffDetailNullEmail = staffDetail.copy(email = null)
      val clientResultSuccess = ClientResult.Success(HttpStatus.OK, staffDetailNullEmail)
      every { mockApDeliusContextApiClient.getStaffDetail(username) } returns clientResultSuccess

      val result = userService.updateUserFromDelius(id, ServiceName.temporaryAccommodation)

      assertThat(result).isInstanceOf(CasResult.Success::class.java)
      result as CasResult.Success

      assertThat(result.value).isInstanceOf(GetUserResponse.Success::class.java)
      val entity = (result.value as GetUserResponse.Success).user

      assertThat(entity.email).isNull()

      verify(exactly = 1) { mockApDeliusContextApiClient.getStaffDetail(username) }
      verify(exactly = 1) { mockUserRepository.save(any()) }
    }

    @Test
    fun `it returns not found when there is no user for that ID`() {
      every { mockUserRepository.findByIdOrNull(id) } returns null
      val result = userService.updateUserFromDelius(id, ServiceName.approvedPremises)
      assertThat(result).isInstanceOf(CasResult.NotFound::class.java)
    }

    @Test
    fun `it returns StaffRecordNotFound if staff record not found`() {
      val user =
        userFactory
          .withDefaults()
          .withDeliusUsername("theUsername")
          .produce()

      every { mockUserRepository.findByIdOrNull(id) } returns user

      every { mockApDeliusContextApiClient.getStaffDetail("theUsername") } returns
        ClientResult.Failure.StatusCode(
          HttpMethod.GET,
          "/secure/staff/username",
          HttpStatus.NOT_FOUND,
          body = null,
        )

      val result = userService.updateUserFromDelius(id, ServiceName.approvedPremises)

      assertThat(result).isInstanceOf(CasResult.Success::class.java)
      val getUserResponse = (result as CasResult.Success).value

      assertThat(getUserResponse).isEqualTo(GetUserResponse.StaffRecordNotFound)
    }
  }

  @Nested
  inner class UpdateUser {

    private val userFactory = UserEntityFactory()
      .withDefaults()

    @Test
    fun `updates a user with given role`() {
      val user = userFactory
        .withDefaults()
        .withApArea(ApAreaEntityFactory().produce())
        .produce()

      user.roles.add(
        UserRoleAssignmentEntityFactory().withUser(user).withRole(UserRole.CAS1_USER_MANAGER).produce(),
      )

      user.qualifications.add(
        UserQualificationAssignmentEntityFactory().withUser(user).withQualification(UserQualification.ESAP).produce(),
      )

      every { mockUserRepository.findByIdOrNull(user.id) } returns user
      every { mockUserRepository.save(any()) } answers { it.invocation.args[0] as UserEntity }
      every { mockUserQualificationAssignmentRepository.deleteAllById(any()) } returns Unit
      every { mockUserRoleAssignmentRepository.delete(any()) } answers { it.invocation.args[0] as UserRoleAssignmentEntity }
      every { mockUserRoleAssignmentRepository.save(any()) } answers { it.invocation.args[0] as UserRoleAssignmentEntity }
      every { mockUserQualificationAssignmentRepository.save(any()) } answers { it.invocation.args[0] as UserQualificationAssignmentEntity }

      val result = userService.updateUser(
        id = user.id,
        roles = listOf(ApprovedPremisesUserRole.assessor, ApprovedPremisesUserRole.cruMember),
        qualifications = listOf(APIUserQualification.emergency, APIUserQualification.pipe),
        cruManagementAreaOverrideId = null,
      )

      assertThat(result).isInstanceOf(CasResult.Success::class.java)
      val updatedUser = (result as CasResult.Success).value

      assertThat(updatedUser.id).isEqualTo(user.id)
      assertThat(updatedUser.roles.map { it.role }).containsExactlyInAnyOrder(UserRole.CAS1_ASSESSOR, UserRole.CAS1_CRU_MEMBER)
      assertThat(updatedUser.qualifications.map { it.qualification }).containsExactlyInAnyOrder(UserQualification.EMERGENCY, UserQualification.PIPE)
      assertThat(updatedUser.cruManagementAreaOverride).isNull()
    }

    @Test
    fun `set cru management override`() {
      val user = userFactory
        .withCruManagementArea(Cas1CruManagementAreaEntityFactory().produce())
        .withCruManagementAreaOverride(Cas1CruManagementAreaEntityFactory().produce())
        .produce()

      val cruManagementAreaOverride = Cas1CruManagementAreaEntityFactory().produce()

      every { mockUserRepository.findByIdOrNull(user.id) } returns user
      every { mockCas1CruManagementAreaRepository.findByIdOrNull(cruManagementAreaOverride.id) } returns cruManagementAreaOverride
      every { mockUserRepository.save(any()) } answers { it.invocation.args[0] as UserEntity }
      every { mockUserQualificationAssignmentRepository.deleteAllById(any()) } returns Unit

      val result = userService.updateUser(
        id = user.id,
        roles = emptyList(),
        qualifications = emptyList(),
        cruManagementAreaOverrideId = cruManagementAreaOverride.id,
      )

      assertThat(result).isInstanceOf(CasResult.Success::class.java)
      val updatedUser = (result as CasResult.Success).value

      assertThat(updatedUser.id).isEqualTo(user.id)
      assertThat(updatedUser.cruManagementArea).isEqualTo(cruManagementAreaOverride)
      assertThat(updatedUser.cruManagementAreaOverride).isEqualTo(cruManagementAreaOverride)
    }

    @Test
    fun `remove cru management override, revert to ap area default`() {
      val apAreaDefaultCruManagementArea = Cas1CruManagementAreaEntityFactory().produce()
      val overriddenCruManagementArea = Cas1CruManagementAreaEntityFactory().produce()

      val user = userFactory
        .withApArea(ApAreaEntityFactory().withDefaultCruManagementArea(apAreaDefaultCruManagementArea).produce())
        .withCruManagementArea(overriddenCruManagementArea)
        .withCruManagementAreaOverride(overriddenCruManagementArea)
        .produce()

      val cruManagementAreaOverride = Cas1CruManagementAreaEntityFactory().produce()

      every { mockUserRepository.findByIdOrNull(user.id) } returns user
      every { mockCas1CruManagementAreaRepository.findByIdOrNull(cruManagementAreaOverride.id) } returns cruManagementAreaOverride
      every { mockUserRepository.save(any()) } answers { it.invocation.args[0] as UserEntity }
      every { mockUserQualificationAssignmentRepository.deleteAllById(any()) } returns Unit

      val result = userService.updateUser(
        id = user.id,
        roles = emptyList(),
        qualifications = emptyList(),
        cruManagementAreaOverrideId = null,
      )

      assertThat(result).isInstanceOf(CasResult.Success::class.java)
      val updatedUser = (result as CasResult.Success).value

      assertThat(updatedUser.id).isEqualTo(user.id)
      assertThat(updatedUser.cruManagementArea).isEqualTo(apAreaDefaultCruManagementArea)
      assertThat(updatedUser.cruManagementAreaOverride).isNull()
    }
  }

  @Nested
  inner class SearchUsersOnAPI {
    private val userFactory = UserEntityFactory()
      .withDefaults()

    val user1 = userFactory
      .withName("Johnny Red")
      .produce()

    val user2 = userFactory
      .withName("Johnny Blue")
      .produce()

    @Test
    fun `User search service correctly returns values given by user repo`() {
      every { mockUserRepository.findByNameContainingIgnoreCase("Johnny") } returns listOf(user1, user2)
      every { mockUserRepository.findByNameContainingIgnoreCase("Bob") } returns listOf()
      every { mockUserRepository.findByNameContainingIgnoreCase("Blue") } returns listOf(user2)

      val result = userService.getUsersByPartialName("Johnny")

      assertThat(result.count()).isEqualTo(2)

      val entity1 = result.first()
      val entity2 = result.last()
      assertThat(entity1.name).isEqualTo(user1.name)
      assertThat(entity2.name).isEqualTo(user2.name)

      val resultFail = userService.getUsersByPartialName("Bob")
      assertThat(resultFail.count()).isEqualTo(0)

      val resultBlue = userService.getUsersByPartialName("Blue")
      assertThat(resultBlue.count()).isEqualTo(1)
      val entity = resultBlue.first()
      assertThat(entity.name).isEqualTo(user2.name)
    }
  }

  @Nested
  inner class DeleteUsersOnAPI {
    private val userFactory = UserEntityFactory()
      .withDefaults()

    @Test
    fun `deleted user now has isActive set to false`() {
      val id = UUID.randomUUID()

      val user = userFactory
        .withId(id)
        .produce()

      every { mockUserRepository.findByIdOrNull(id) } answers { user }
      every { mockUserRepository.save(any()) } answers { user }
      userService.deleteUser(id)

      verify(exactly = 1) {
        mockUserRepository.save(
          match {
            it.isActive == false && it.id == user.id
          },
        )
      }
    }
  }

  @Nested
  inner class GetAllocatableUsersForAllocationType {

    @Test
    fun `getAllocatableUsersForAllocationType asserts lao qualification if offender is lao`() {
      every { mockOffenderService.isLao(any()) } returns true

      val userWithLao = UserEntityFactory()
        .withDefaultProbationRegion()
        .produce()

      userWithLao.apply {
        qualifications += UserQualificationAssignmentEntityFactory()
          .withUser(this)
          .withQualification(UserQualification.LAO)
          .produce()
      }

      val userWithoutLao = UserEntityFactory()
        .withDefaultProbationRegion()
        .produce()

      every { mockUserRepository.findActiveUsersWithAtLeastOneRole(any()) } returns listOf(userWithLao, userWithoutLao)

      val allocatableUser = userService.getAllocatableUsersForAllocationType(
        "crn",
        emptyList<UserQualification>(),
        UserPermission.CAS1_ASSESS_PLACEMENT_APPLICATION,
      )
      assertThat(allocatableUser.size).isEqualTo(1)
      assertThat(allocatableUser.first().id).isEqualTo(userWithLao.id)
    }

    @Test
    fun `getAllocatableUsersForAllocationType seeks correct role when process appeal permission is present `() {
      every { mockOffenderService.isLao(any()) } returns false

      val user = UserEntityFactory()
        .withDefaultProbationRegion()
        .produce()

      every { mockUserRepository.findActiveUsersWithAtLeastOneRole(any()) } returns listOf(user)

      val allocatableUser = userService.getAllocatableUsersForAllocationType(
        "crn",
        emptyList<UserQualification>(),
        UserPermission.CAS1_ASSESS_APPEALED_APPLICATION,
      )

      verify(exactly = 1) { mockUserRepository.findActiveUsersWithAtLeastOneRole(listOf(UserRole.CAS1_ASSESSOR, UserRole.CAS1_APPEALS_MANAGER, UserRole.CAS1_JANITOR)) }
    }
  }

  @Nested
  inner class RemoveRoleFromUser {

    @Test
    fun `do nothing if user doesnt have role`() {
      val user = UserEntityFactory()
        .withDefaults()
        .produce()

      userService.removeRoleFromUser(user, UserRole.CAS1_FUTURE_MANAGER)

      verify { mockUserRoleAssignmentRepository wasNot Called }
    }

    @Test
    fun `remove role if user has role`() {
      val user = UserEntityFactory()
        .withDefaults()
        .produce()

      val managerRoleAssignment = UserRoleAssignmentEntityFactory()
        .withUser(user)
        .withRole(UserRole.CAS1_FUTURE_MANAGER)
        .withId(UUID.randomUUID())
        .produce()

      val appealsRoleAssignment = UserRoleAssignmentEntityFactory()
        .withUser(user)
        .withRole(UserRole.CAS1_APPEALS_MANAGER)
        .withId(UUID.randomUUID())
        .produce()

      user.roles.add(managerRoleAssignment)
      user.roles.add(appealsRoleAssignment)

      every { mockUserRoleAssignmentRepository.delete(managerRoleAssignment) } returns Unit

      userService.removeRoleFromUser(user, UserRole.CAS1_FUTURE_MANAGER)

      verify { mockUserRoleAssignmentRepository.delete(managerRoleAssignment) }
    }

    @Test
    fun `remove roles if user has role multiple times`() {
      val user = UserEntityFactory()
        .withDefaults()
        .produce()

      val managerRoleAssignment = UserRoleAssignmentEntityFactory()
        .withUser(user)
        .withRole(UserRole.CAS1_FUTURE_MANAGER)
        .withId(UUID.randomUUID())
        .produce()

      val appealsRoleAssignment1 = UserRoleAssignmentEntityFactory()
        .withUser(user)
        .withRole(UserRole.CAS1_APPEALS_MANAGER)
        .withId(UUID.randomUUID())
        .produce()

      val appealsRoleAssignment2 = UserRoleAssignmentEntityFactory()
        .withUser(user)
        .withRole(UserRole.CAS1_APPEALS_MANAGER)
        .withId(UUID.randomUUID())
        .produce()

      user.roles.add(managerRoleAssignment)
      user.roles.add(appealsRoleAssignment1)
      user.roles.add(appealsRoleAssignment2)

      every { mockUserRoleAssignmentRepository.delete(appealsRoleAssignment1) } returns Unit
      every { mockUserRoleAssignmentRepository.delete(appealsRoleAssignment2) } returns Unit

      userService.removeRoleFromUser(user, UserRole.CAS1_APPEALS_MANAGER)

      verify { mockUserRoleAssignmentRepository.delete(appealsRoleAssignment1) }
      verify { mockUserRoleAssignmentRepository.delete(appealsRoleAssignment2) }
    }
  }
}
