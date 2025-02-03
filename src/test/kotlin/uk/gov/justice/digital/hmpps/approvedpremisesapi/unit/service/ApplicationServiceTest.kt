package uk.gov.justice.digital.hmpps.approvedpremisesapi.unit.service

import com.fasterxml.jackson.databind.ObjectMapper
import io.mockk.Runs
import io.mockk.called
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import org.junit.jupiter.params.provider.EnumSource
import org.junit.jupiter.params.provider.NullSource
import org.springframework.data.repository.findByIdOrNull
import org.springframework.http.HttpStatus
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ApType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ApprovedPremisesApplicationStatus
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas1ApplicationTimelinessCategory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas1ApplicationUserDetails
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ReleaseTypeOption
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.SentenceTypeOption
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ServiceName
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.SituationOption
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.SubmitApprovedPremisesApplication
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.SubmitTemporaryAccommodationApplication
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.ApDeliusContextApiClient
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.ClientResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.ApAreaEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.ApprovedPremisesApplicationEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.ApprovedPremisesApplicationJsonSchemaEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.ApprovedPremisesAssessmentEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.Cas1ApplicationUserDetailsEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.InmateDetailFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.NeedsDetailsFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.OffenderDetailsSummaryFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.OfflineApplicationEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.PersonRisksFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.ProbationRegionEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.TemporaryAccommodationApplicationEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.TemporaryAccommodationApplicationJsonSchemaEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.TemporaryAccommodationAssessmentEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.UserEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.UserRoleAssignmentEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApAreaRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApplicationEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApplicationRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApplicationTeamCodeEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApplicationTeamCodeRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApprovedPremisesApplicationEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApprovedPremisesApplicationJsonSchemaEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApprovedPremisesApplicationSummary
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.Cas1ApplicationUserDetailsEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.Cas1ApplicationUserDetailsRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.Cas1CruManagementAreaRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.LockableApplicationEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.LockableApplicationRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.OfflineApplicationRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.PlacementApplicationAutomaticEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.PlacementApplicationAutomaticRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ProbationDeliveryUnitRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.TemporaryAccommodationApplicationEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.TemporaryAccommodationApplicationJsonSchemaEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserRole
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.listeners.ApplicationListener
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.Mappa
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.PersonInfoResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.RiskWithStatus
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.RoshRisks
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.asApprovedPremisesType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.community.OffenderDetailSummary
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.community.OffenderIds
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.community.OffenderLanguages
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.community.OffenderProfile
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.deliuscontext.ManagingTeamsResponse
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.prisonsapi.AssignedLivingUnit
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.prisonsapi.InmateDetail
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.prisonsapi.InmateStatus
import uk.gov.justice.digital.hmpps.approvedpremisesapi.results.AuthorisableActionResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.results.CasResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.results.ValidatableActionResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.ApplicationService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.ApplicationService.Cas1ApplicationUpdateFields
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.AssessmentService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.JsonSchemaService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.OffenderService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.UserAccessService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.UserService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1.Cas1ApplicationDomainEventService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1.Cas1ApplicationEmailService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1.Cas1DomainEventService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.unit.util.assertThatCasResult
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.util.UUID
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas3.DomainEventService as Cas3DomainEventService

class ApplicationServiceTest {
  private val mockUserRepository = mockk<UserRepository>()
  private val mockApplicationRepository = mockk<ApplicationRepository>()
  private val mockJsonSchemaService = mockk<JsonSchemaService>()
  private val mockOffenderService = mockk<OffenderService>()
  private val mockUserService = mockk<UserService>()
  private val mockAssessmentService = mockk<AssessmentService>()
  private val mockOfflineApplicationRepository = mockk<OfflineApplicationRepository>()
  private val mockDomainEventService = mockk<Cas1DomainEventService>()
  private val mockCas3DomainEventService = mockk<Cas3DomainEventService>()
  private val mockApDeliusContextApiClient = mockk<ApDeliusContextApiClient>()
  private val mockApplicationTeamCodeRepository = mockk<ApplicationTeamCodeRepository>()
  private val mockUserAccessService = mockk<UserAccessService>()
  private val mockObjectMapper = mockk<ObjectMapper>()
  private val mockApAreaRepository = mockk<ApAreaRepository>()
  private val mockCas1ApplicationDomainEventService = mockk<Cas1ApplicationDomainEventService>()
  private val mockCas1ApplicationUserDetailsRepository = mockk<Cas1ApplicationUserDetailsRepository>()
  private val mockCas1ApplicationEmailService = mockk<Cas1ApplicationEmailService>()
  private val mockPlacementApplicationAutomaticRepository = mockk<PlacementApplicationAutomaticRepository>()
  private val mockApplicationListener = mockk<ApplicationListener>()
  private val mockLockableApplicationRepository = mockk<LockableApplicationRepository>()
  private val mockProbationDeliveryUnitRepository = mockk<ProbationDeliveryUnitRepository>()
  private val mockCas1CruManagementAreaRepository = mockk<Cas1CruManagementAreaRepository>()

  private val applicationService = ApplicationService(
    mockUserRepository,
    mockApplicationRepository,
    mockJsonSchemaService,
    mockOffenderService,
    mockUserService,
    mockAssessmentService,
    mockOfflineApplicationRepository,
    mockCas3DomainEventService,
    mockApDeliusContextApiClient,
    mockApplicationTeamCodeRepository,
    mockUserAccessService,
    mockObjectMapper,
    mockApAreaRepository,
    mockCas1ApplicationDomainEventService,
    mockCas1ApplicationUserDetailsRepository,
    mockCas1ApplicationEmailService,
    mockPlacementApplicationAutomaticRepository,
    mockApplicationListener,
    Clock.systemDefaultZone(),
    mockLockableApplicationRepository,
    mockProbationDeliveryUnitRepository,
    mockCas1CruManagementAreaRepository,
  )

  @Test
  fun `Get all applications where Probation Officer exists returns applications returned from repository`() {
    val userId = UUID.fromString("8a0624b8-8e92-47ce-b645-b65ea5a197d0")
    val deliusUsername = "SOMEPERSON"
    val userEntity = UserEntityFactory()
      .withId(userId)
      .withDeliusUsername(deliusUsername)
      .withYieldedProbationRegion {
        ProbationRegionEntityFactory()
          .withYieldedApArea { ApAreaEntityFactory().produce() }
          .produce()
      }
      .produce()
    val applicationSummaries = listOf(
      object : ApprovedPremisesApplicationSummary {
        override fun getIsWomensApplication(): Boolean? = true
        override fun getIsPipeApplication(): Boolean? = true
        override fun getIsEsapApplication() = true
        override fun getIsEmergencyApplication() = true
        override fun getArrivalDate(): Instant? = null
        override fun getRiskRatings(): String? = null
        override fun getId(): UUID = UUID.fromString("8ecbbd9c-3c66-4f0b-8f21-87f537676422")
        override fun getCrn(): String = "CRN123"
        override fun getCreatedByUserId(): UUID = UUID.fromString("60d0a768-1d05-4538-a6fd-78eb723dd310")
        override fun getCreatedAt(): Instant = Instant.parse("2023-04-20T10:11:00+01:00")
        override fun getSubmittedAt(): Instant? = null
        override fun getTier(): String? = null
        override fun getStatus(): String = ApprovedPremisesApplicationStatus.started.toString()
        override fun getIsWithdrawn(): Boolean = false
        override fun getReleaseType(): String = ReleaseTypeOption.licence.toString()
        override fun getHasRequestsForPlacement(): Boolean = false
      },
    )

    every { mockUserRepository.findByDeliusUsername(deliusUsername) } returns userEntity
    every { mockApplicationRepository.findNonWithdrawnApprovedPremisesSummariesForUser(userId) } returns applicationSummaries
    every { mockJsonSchemaService.checkSchemaOutdated(any()) } answers { it.invocation.args[0] as ApplicationEntity }

    val crns = applicationSummaries.map { it.getCrn() }.distinct()
    every { mockOffenderService.canAccessOffenders(deliusUsername, crns) } returns mapOf(crns.first() to true)

    assertThat(
      applicationService.getAllApplicationsForUsername(
        userEntity = userEntity,
        ServiceName.approvedPremises,
      ),
    ).containsAll(applicationSummaries)
  }

  @Test
  fun `getApplicationForUsername where application does not exist returns NotFound result`() {
    val distinguishedName = "SOMEPERSON"
    val applicationId = UUID.fromString("c1750938-19fc-48a1-9ae9-f2e119ffc1f4")

    val probationRegion = ProbationRegionEntityFactory()
      .withYieldedApArea { ApAreaEntityFactory().produce() }
      .produce()
    val deletedApplication = TemporaryAccommodationApplicationEntityFactory()
      .withId(applicationId)
      .withYieldedCreatedByUser {
        UserEntityFactory()
          .withProbationRegion(probationRegion)
          .produce()
      }
      .withProbationRegion(probationRegion)
      .withDeletedAt(OffsetDateTime.now().minusDays(10))
      .produce()

    every { mockApplicationRepository.findByIdOrNull(applicationId) } returns deletedApplication

    assertThat(
      applicationService.getApplicationForUsername(
        applicationId,
        distinguishedName,
      ) is CasResult.NotFound,
    ).isTrue
  }

  @Test
  fun `getApplicationForUsername where temporary accommodation application was deleted returns NotFound result`() {
    val distinguishedName = "SOMEPERSON"
    val applicationId = UUID.fromString("c1750938-19fc-48a1-9ae9-f2e119ffc1f4")

    every { mockApplicationRepository.findByIdOrNull(applicationId) } returns null

    assertThat(
      applicationService.getApplicationForUsername(
        applicationId,
        distinguishedName,
      ) is CasResult.NotFound,
    ).isTrue
  }

  @Test
  fun `getApplicationForUsername where user cannot access the application returns Unauthorised result`() {
    val distinguishedName = "SOMEPERSON"
    val applicationId = UUID.fromString("c1750938-19fc-48a1-9ae9-f2e119ffc1f4")

    every { mockUserRepository.findByDeliusUsername(any()) } returns UserEntityFactory()
      .withDeliusUsername(distinguishedName)
      .withYieldedProbationRegion {
        ProbationRegionEntityFactory()
          .withYieldedApArea { ApAreaEntityFactory().produce() }
          .produce()
      }
      .produce()

    every { mockApplicationRepository.findByIdOrNull(any()) } returns ApprovedPremisesApplicationEntityFactory()
      .withCreatedByUser(
        UserEntityFactory()
          .withYieldedProbationRegion {
            ProbationRegionEntityFactory()
              .withYieldedApArea { ApAreaEntityFactory().produce() }
              .produce()
          }
          .produce(),
      )
      .produce()

    every { mockUserAccessService.userCanViewApplication(any(), any()) } returns false

    assertThat(
      applicationService.getApplicationForUsername(
        applicationId,
        distinguishedName,
      ) is CasResult.Unauthorised,
    ).isTrue
  }

  @Test
  fun `getApplicationForUsername where user can access the application returns Success result with entity from db`() {
    val distinguishedName = "SOMEPERSON"
    val userId = UUID.fromString("239b5e41-f83e-409e-8fc0-8f1e058d417e")
    val applicationId = UUID.fromString("c1750938-19fc-48a1-9ae9-f2e119ffc1f4")

    val newestJsonSchema = ApprovedPremisesApplicationJsonSchemaEntityFactory()
      .withSchema("{}")
      .produce()

    val userEntity = UserEntityFactory()
      .withId(userId)
      .withDeliusUsername(distinguishedName)
      .withYieldedProbationRegion {
        ProbationRegionEntityFactory()
          .withYieldedApArea { ApAreaEntityFactory().produce() }
          .produce()
      }
      .produce()

    val applicationEntity = ApprovedPremisesApplicationEntityFactory()
      .withCreatedByUser(userEntity)
      .withApplicationSchema(newestJsonSchema)
      .produce()

    every { mockJsonSchemaService.checkSchemaOutdated(any()) } answers { it.invocation.args[0] as ApplicationEntity }
    every { mockApplicationRepository.findByIdOrNull(any()) } returns applicationEntity
    every { mockUserRepository.findByDeliusUsername(any()) } returns userEntity
    every { mockUserAccessService.userCanViewApplication(any(), any()) } returns true

    val result = applicationService.getApplicationForUsername(applicationId, distinguishedName)

    assertThat(result is CasResult.Success).isTrue
    result as CasResult.Success

    assertThat(result.value).isEqualTo(applicationEntity)
  }

  @Test
  fun `createApprovedPremisesApplication returns FieldValidationError when convictionId, eventNumber or offenceId are null`() {
    val crn = "CRN345"
    val username = "SOMEPERSON"

    val offenderDetails = OffenderDetailsSummaryFactory()
      .withCrn(crn)
      .produce()

    val user = userWithUsername(username)

    every { mockApDeliusContextApiClient.getTeamsManagingCase(crn) } returns ClientResult.Success(
      HttpStatus.OK,
      ManagingTeamsResponse(
        teamCodes = listOf("TEAMCODE"),
      ),
    )

    val result = applicationService.createApprovedPremisesApplication(offenderDetails, user, null, null, null)

    assertThatCasResult(result).isFieldValidationError()
      .hasMessage("$.convictionId", "empty")
      .hasMessage("$.deliusEventNumber", "empty")
      .hasMessage("$.offenceId", "empty")
  }

  @Test
  fun `createApprovedPremisesApplication returns Success with created Application, persists Risk data and Offender name`() {
    val crn = "CRN345"
    val username = "SOMEPERSON"

    val schema = ApprovedPremisesApplicationJsonSchemaEntityFactory().produce()

    val user = userWithUsername(username)

    every { mockOffenderService.getOASysNeeds(crn) } returns AuthorisableActionResult.Success(
      NeedsDetailsFactory().produce(),
    )

    every { mockApDeliusContextApiClient.getTeamsManagingCase(crn) } returns ClientResult.Success(
      HttpStatus.OK,
      ManagingTeamsResponse(
        teamCodes = listOf("TEAMCODE"),
      ),
    )

    val offenderDetails = OffenderDetailsSummaryFactory()
      .withCrn(crn)
      .produce()

    every { mockUserService.getUserForRequest() } returns user
    every { mockJsonSchemaService.getNewestSchema(ApprovedPremisesApplicationJsonSchemaEntity::class.java) } returns schema
    every { mockApplicationRepository.saveAndFlush(any()) } answers { it.invocation.args[0] as ApplicationEntity }
    every { mockApplicationTeamCodeRepository.save(any()) } answers { it.invocation.args[0] as ApplicationTeamCodeEntity }

    val riskRatings = PersonRisksFactory()
      .withRoshRisks(
        RiskWithStatus(
          value = RoshRisks(
            overallRisk = "High",
            riskToChildren = "Medium",
            riskToPublic = "Low",
            riskToKnownAdult = "High",
            riskToStaff = "High",
            lastUpdated = null,
          ),
        ),
      )
      .withMappa(
        RiskWithStatus(
          value = Mappa(
            level = "",
            lastUpdated = LocalDate.parse("2022-12-12"),
          ),
        ),
      )
      .withFlags(
        RiskWithStatus(
          value = listOf(
            "flag1",
            "flag2",
          ),
        ),
      )
      .produce()

    every { mockOffenderService.getRiskByCrn(crn, username) } returns AuthorisableActionResult.Success(
      riskRatings,
    )

    val result = applicationService.createApprovedPremisesApplication(offenderDetails, user, 123, "1", "A12HI")

    assertThatCasResult(result).isSuccess().with {
      val approvedPremisesApplication = it as ApprovedPremisesApplicationEntity
      assertThat(approvedPremisesApplication.riskRatings).isEqualTo(riskRatings)
      assertThat(approvedPremisesApplication.name).isEqualTo("${offenderDetails.firstName.uppercase()} ${offenderDetails.surname.uppercase()}")
    }
  }

  @Test
  fun `createTemporaryAccommodationApplication returns Unauthorised when user doesn't have CAS3_REFERRER role`() {
    val crn = "CRN345"
    val username = "SOMEPERSON"
    val offenderDetailSummary = createOffenderDetailsSummary(crn)
    val inmateDetail = createInmateDetail(InmateStatus.IN, "Bristol Prison")
    val personInfo = PersonInfoResult.Success.Full(
      crn = crn,
      offenderDetailSummary = offenderDetailSummary,
      inmateDetail = inmateDetail,
    )
    val user = userWithUsername(username).apply {
      this.roles.add(
        UserRoleAssignmentEntityFactory()
          .withUser(this)
          .withRole(UserRole.CAS3_ASSESSOR)
          .produce(),
      )
    }

    val result = applicationService.createTemporaryAccommodationApplication(
      crn,
      user,
      123,
      "1",
      "A12HI",
      personInfo = personInfo,
    )

    assertThatCasResult(result).isUnauthorised()
  }

  @Test
  fun `createTemporaryAccommodationApplication returns FieldValidationError when CRN does not exist`() {
    val crn = "CRN345"
    val username = "SOMEPERSON"
    val offenderDetailSummary = createOffenderDetailsSummary(crn)
    val inmateDetail = createInmateDetail(InmateStatus.IN, "Bristol Prison")
    val personInfo = PersonInfoResult.Success.Full(
      crn = crn,
      offenderDetailSummary = offenderDetailSummary,
      inmateDetail = inmateDetail,
    )
    every { mockOffenderService.getOffenderByCrn(crn, username) } returns AuthorisableActionResult.NotFound()

    val user = userWithUsername(username).apply {
      this.roles.add(
        UserRoleAssignmentEntityFactory()
          .withUser(this)
          .withRole(UserRole.CAS3_REFERRER)
          .produce(),
      )
    }

    val result = applicationService.createTemporaryAccommodationApplication(
      crn,
      user,
      123,
      "1",
      "A12HI",
      personInfo = personInfo,
    )

    assertThatCasResult(result).isFieldValidationError()
      .hasMessage("$.crn", "doesNotExist")
  }

  @Test
  fun `createTemporaryAccommodationApplication returns FieldValidationError when CRN is LAO restricted`() {
    val crn = "CRN345"
    val username = "SOMEPERSON"
    val offenderDetailSummary = createOffenderDetailsSummary(crn)
    val inmateDetail = createInmateDetail(InmateStatus.IN, "Bristol Prison")
    val personInfo = PersonInfoResult.Success.Full(
      crn = crn,
      offenderDetailSummary = offenderDetailSummary,
      inmateDetail = inmateDetail,
    )
    every { mockOffenderService.getOffenderByCrn(crn, username) } returns AuthorisableActionResult.Unauthorised()

    val user = userWithUsername(username).apply {
      this.roles.add(
        UserRoleAssignmentEntityFactory()
          .withUser(this)
          .withRole(UserRole.CAS3_REFERRER)
          .produce(),
      )
    }

    val result = applicationService.createTemporaryAccommodationApplication(
      crn,
      user,
      123,
      "1",
      "A12HI",
      personInfo = personInfo,
    )

    assertThatCasResult(result).isFieldValidationError()
      .hasMessage("$.crn", "userPermission")
  }

  @Test
  fun `createTemporaryAccommodationApplication returns FieldValidationError when convictionId, eventNumber or offenceId are null`() {
    val crn = "CRN345"
    val username = "SOMEPERSON"
    val offenderDetailSummary = createOffenderDetailsSummary(crn)
    val inmateDetail = createInmateDetail(InmateStatus.IN, "HMP Bristol")
    val personInfo = PersonInfoResult.Success.Full(
      crn = crn,
      offenderDetailSummary = offenderDetailSummary,
      inmateDetail = inmateDetail,
    )

    every { mockOffenderService.getOffenderByCrn(crn, username) } returns AuthorisableActionResult.Success(
      OffenderDetailsSummaryFactory().produce(),
    )

    val user = userWithUsername(username).apply {
      this.roles.add(
        UserRoleAssignmentEntityFactory()
          .withUser(this)
          .withRole(UserRole.CAS3_REFERRER)
          .produce(),
      )
    }

    val result = applicationService.createTemporaryAccommodationApplication(
      crn,
      user,
      null,
      null,
      null,
      personInfo = personInfo,
    )

    assertThatCasResult(result).isFieldValidationError()
      .hasMessage("$.convictionId", "empty")
      .hasMessage("$.deliusEventNumber", "empty")
      .hasMessage("$.offenceId", "empty")
  }

  @Test
  fun `createTemporaryAccommodationApplication returns Success with created Application + persisted Risk data`() {
    val crn = "CRN345"
    val username = "SOMEPERSON"
    val offenderDetailSummary = createOffenderDetailsSummary(crn)
    val agencyName = "HMP Bristol"
    val inmateDetail = createInmateDetail(InmateStatus.IN, agencyName)
    val personInfo = PersonInfoResult.Success.Full(
      crn = crn,
      offenderDetailSummary = offenderDetailSummary,
      inmateDetail = inmateDetail,
    )
    val schema = TemporaryAccommodationApplicationJsonSchemaEntityFactory().produce()

    val user = userWithUsername(username).apply {
      this.roles.add(
        UserRoleAssignmentEntityFactory()
          .withUser(this)
          .withRole(UserRole.CAS3_REFERRER)
          .produce(),
      )
    }

    every { mockOffenderService.getOffenderByCrn(crn, username) } returns AuthorisableActionResult.Success(
      OffenderDetailsSummaryFactory().produce(),
    )
    every { mockUserService.getUserForRequest() } returns user
    every { mockJsonSchemaService.getNewestSchema(TemporaryAccommodationApplicationJsonSchemaEntity::class.java) } returns schema
    every { mockApplicationRepository.save(any()) } answers { it.invocation.args[0] as ApplicationEntity }

    val riskRatings = PersonRisksFactory()
      .withRoshRisks(
        RiskWithStatus(
          value = RoshRisks(
            overallRisk = "High",
            riskToChildren = "Medium",
            riskToPublic = "Low",
            riskToKnownAdult = "High",
            riskToStaff = "High",
            lastUpdated = null,
          ),
        ),
      )
      .withMappa(
        RiskWithStatus(
          value = Mappa(
            level = "",
            lastUpdated = LocalDate.parse("2022-12-12"),
          ),
        ),
      )
      .withFlags(
        RiskWithStatus(
          value = listOf(
            "flag1",
            "flag2",
          ),
        ),
      )
      .produce()

    every { mockOffenderService.getRiskByCrn(crn, username) } returns AuthorisableActionResult.Success(
      riskRatings,
    )

    val result = applicationService.createTemporaryAccommodationApplication(
      crn,
      user,
      123,
      "1",
      "A12HI",
      personInfo = personInfo,
    )

    assertThatCasResult(result).isSuccess().with {
      val temporaryAccommodationApplication = it as TemporaryAccommodationApplicationEntity
      assertThat(temporaryAccommodationApplication.riskRatings).isEqualTo(riskRatings)
      assertThat(temporaryAccommodationApplication.prisonNameOnCreation).isEqualTo(agencyName)
    }
  }

  @Test
  fun `createTemporaryAccommodationApplication returns Success with created Application with prison name when person status is TRN`() {
    val crn = "CRN345"
    val username = "SOMEPERSON"
    val offenderDetailSummary = createOffenderDetailsSummary(crn)
    val agencyName = "HMP Bristol"
    val inmateDetail = createInmateDetail(InmateStatus.TRN, agencyName)
    val personInfo = PersonInfoResult.Success.Full(
      crn = crn,
      offenderDetailSummary = offenderDetailSummary,
      inmateDetail = inmateDetail,
    )
    val schema = TemporaryAccommodationApplicationJsonSchemaEntityFactory().produce()

    val user = userWithUsername(username).apply {
      this.roles.add(
        UserRoleAssignmentEntityFactory()
          .withUser(this)
          .withRole(UserRole.CAS3_REFERRER)
          .produce(),
      )
    }

    every { mockOffenderService.getOffenderByCrn(crn, username) } returns AuthorisableActionResult.Success(
      OffenderDetailsSummaryFactory().produce(),
    )
    every { mockUserService.getUserForRequest() } returns user
    every { mockJsonSchemaService.getNewestSchema(TemporaryAccommodationApplicationJsonSchemaEntity::class.java) } returns schema
    every { mockApplicationRepository.save(any()) } answers { it.invocation.args[0] as ApplicationEntity }

    val riskRatings = PersonRisksFactory()
      .withRoshRisks(
        RiskWithStatus(
          value = RoshRisks(
            overallRisk = "High",
            riskToChildren = "Medium",
            riskToPublic = "Low",
            riskToKnownAdult = "High",
            riskToStaff = "High",
            lastUpdated = null,
          ),
        ),
      )
      .withMappa(
        RiskWithStatus(
          value = Mappa(
            level = "",
            lastUpdated = LocalDate.parse("2022-12-12"),
          ),
        ),
      )
      .withFlags(
        RiskWithStatus(
          value = listOf(
            "flag1",
            "flag2",
          ),
        ),
      )
      .produce()

    every { mockOffenderService.getRiskByCrn(crn, username) } returns AuthorisableActionResult.Success(
      riskRatings,
    )

    val result = applicationService.createTemporaryAccommodationApplication(
      crn,
      user,
      123,
      "1",
      "A12HI",
      personInfo = personInfo,
    )

    assertThatCasResult(result).isSuccess().with {
      val temporaryAccommodationApplication = it as TemporaryAccommodationApplicationEntity
      assertThat(temporaryAccommodationApplication.riskRatings).isEqualTo(riskRatings)
      assertThat(temporaryAccommodationApplication.prisonNameOnCreation).isEqualTo(agencyName)
    }
  }

  @Test
  fun `createTemporaryAccommodationApplication returns Success with created Application without prison name when person status is Out`() {
    val crn = "CRN345"
    val username = "SOMEPERSON"
    val offenderDetailSummary = createOffenderDetailsSummary(crn)
    val inmateDetail = createInmateDetail(InmateStatus.OUT, "HMP Bristol")
    val personInfo = PersonInfoResult.Success.Full(
      crn = crn,
      offenderDetailSummary = offenderDetailSummary,
      inmateDetail = inmateDetail,
    )
    val schema = TemporaryAccommodationApplicationJsonSchemaEntityFactory().produce()

    val user = userWithUsername(username).apply {
      this.roles.add(
        UserRoleAssignmentEntityFactory()
          .withUser(this)
          .withRole(UserRole.CAS3_REFERRER)
          .produce(),
      )
    }

    every { mockOffenderService.getOffenderByCrn(crn, username) } returns AuthorisableActionResult.Success(
      OffenderDetailsSummaryFactory().produce(),
    )
    every { mockUserService.getUserForRequest() } returns user
    every { mockJsonSchemaService.getNewestSchema(TemporaryAccommodationApplicationJsonSchemaEntity::class.java) } returns schema
    every { mockApplicationRepository.save(any()) } answers { it.invocation.args[0] as ApplicationEntity }

    val riskRatings = PersonRisksFactory()
      .withRoshRisks(
        RiskWithStatus(
          value = RoshRisks(
            overallRisk = "High",
            riskToChildren = "Medium",
            riskToPublic = "Low",
            riskToKnownAdult = "High",
            riskToStaff = "High",
            lastUpdated = null,
          ),
        ),
      )
      .withMappa(
        RiskWithStatus(
          value = Mappa(
            level = "",
            lastUpdated = LocalDate.parse("2022-12-12"),
          ),
        ),
      )
      .withFlags(
        RiskWithStatus(
          value = listOf(
            "flag1",
            "flag2",
          ),
        ),
      )
      .produce()

    every { mockOffenderService.getRiskByCrn(crn, username) } returns AuthorisableActionResult.Success(
      riskRatings,
    )

    val result = applicationService.createTemporaryAccommodationApplication(
      crn,
      user,
      123,
      "1",
      "A12HI",
      personInfo = personInfo,
    )

    assertThatCasResult(result).isSuccess().with {
      val temporaryAccommodationApplication = it as TemporaryAccommodationApplicationEntity
      assertThat(temporaryAccommodationApplication.riskRatings).isEqualTo(riskRatings)
      assertThat(temporaryAccommodationApplication.prisonNameOnCreation).isNull()
    }
  }

  @Test
  fun `createTemporaryAccommodationApplication returns Success with created Application without prison name when assignedLivingUnit is null`() {
    val crn = "CRN345"
    val username = "SOMEPERSON"
    val offenderDetailSummary = createOffenderDetailsSummary(crn)
    val inmateDetail = createInmateDetail(InmateStatus.IN, null)
    val personInfo = PersonInfoResult.Success.Full(
      crn = crn,
      offenderDetailSummary = offenderDetailSummary,
      inmateDetail = inmateDetail,
    )
    val schema = TemporaryAccommodationApplicationJsonSchemaEntityFactory().produce()

    val user = userWithUsername(username).apply {
      this.roles.add(
        UserRoleAssignmentEntityFactory()
          .withUser(this)
          .withRole(UserRole.CAS3_REFERRER)
          .produce(),
      )
    }

    every { mockOffenderService.getOffenderByCrn(crn, username) } returns AuthorisableActionResult.Success(
      OffenderDetailsSummaryFactory().produce(),
    )
    every { mockUserService.getUserForRequest() } returns user
    every { mockJsonSchemaService.getNewestSchema(TemporaryAccommodationApplicationJsonSchemaEntity::class.java) } returns schema
    every { mockApplicationRepository.save(any()) } answers { it.invocation.args[0] as ApplicationEntity }

    val riskRatings = PersonRisksFactory()
      .withRoshRisks(
        RiskWithStatus(
          value = RoshRisks(
            overallRisk = "High",
            riskToChildren = "Medium",
            riskToPublic = "Low",
            riskToKnownAdult = "High",
            riskToStaff = "High",
            lastUpdated = null,
          ),
        ),
      )
      .withMappa(
        RiskWithStatus(
          value = Mappa(
            level = "",
            lastUpdated = LocalDate.parse("2022-12-12"),
          ),
        ),
      )
      .withFlags(
        RiskWithStatus(
          value = listOf(
            "flag1",
            "flag2",
          ),
        ),
      )
      .produce()

    every { mockOffenderService.getRiskByCrn(crn, username) } returns AuthorisableActionResult.Success(
      riskRatings,
    )

    val result = applicationService.createTemporaryAccommodationApplication(
      crn,
      user,
      123,
      "1",
      "A12HI",
      personInfo = personInfo,
    )

    assertThatCasResult(result).isSuccess().with {
      val temporaryAccommodationApplication = it as TemporaryAccommodationApplicationEntity
      assertThat(temporaryAccommodationApplication.riskRatings).isEqualTo(riskRatings)
      assertThat(temporaryAccommodationApplication.prisonNameOnCreation).isNull()
    }
  }

  @Nested
  inner class UpdateApplicationCas1 {

    val applicationId = UUID.fromString("fa6e97ce-7b9e-473c-883c-83b1c2af773d")
    val username = "SOMEPERSON"

    val user = UserEntityFactory()
      .withDeliusUsername(username)
      .withYieldedProbationRegion {
        ProbationRegionEntityFactory()
          .withYieldedApArea { ApAreaEntityFactory().produce() }
          .produce()
      }
      .produce()

    val newestSchema = ApprovedPremisesApplicationJsonSchemaEntityFactory().produce()
    val updatedData = """
      {
        "aProperty": "value"
      }
    """

    val application = ApprovedPremisesApplicationEntityFactory()
      .withApplicationSchema(newestSchema)
      .withId(applicationId)
      .withCreatedByUser(user)
      .withCreatedAt(OffsetDateTime.now())
      .produce()
      .apply {
        schemaUpToDate = true
      }

    @Test
    fun `updateApprovedPremisesApplication returns NotFound when application doesn't exist`() {
      every { mockApplicationRepository.findByIdOrNull(applicationId) } returns null

      assertThat(
        applicationService.updateApprovedPremisesApplication(
          applicationId = applicationId,
          Cas1ApplicationUpdateFields(
            isWomensApplication = false,
            isPipeApplication = null,
            isEmergencyApplication = false,
            isEsapApplication = false,
            apType = null,
            releaseType = null,
            arrivalDate = null,
            data = "{}",
            isInapplicable = null,
            noticeType = Cas1ApplicationTimelinessCategory.standard,
          ),
          userForRequest = user,
        ) is CasResult.NotFound,
      ).isTrue
    }

    @Test
    fun `updateApprovedPremisesApplication returns Unauthorised when application doesn't belong to request user`() {
      every { mockApplicationRepository.findByIdOrNull(applicationId) } returns application
      every { mockJsonSchemaService.checkSchemaOutdated(application) } returns application

      val otherUser = UserEntityFactory()
        .withDeliusUsername(username)
        .withYieldedProbationRegion {
          ProbationRegionEntityFactory()
            .withYieldedApArea { ApAreaEntityFactory().produce() }
            .produce()
        }.produce()

      assertThat(
        applicationService.updateApprovedPremisesApplication(
          applicationId = applicationId,
          Cas1ApplicationUpdateFields(
            isWomensApplication = false,
            isPipeApplication = null,
            isEmergencyApplication = false,
            isEsapApplication = false,
            apType = null,
            releaseType = null,
            arrivalDate = null,
            data = "{}",
            isInapplicable = null,
            noticeType = Cas1ApplicationTimelinessCategory.standard,
          ),
          userForRequest = otherUser,
        ) is CasResult.Unauthorised,
      ).isTrue
    }

    @Test
    fun `updateApprovedPremisesApplication returns GeneralValidationError when application schema is outdated`() {
      every { mockUserService.getUserForRequest() } returns user
      every { mockApplicationRepository.findByIdOrNull(applicationId) } returns application
      every { mockJsonSchemaService.checkSchemaOutdated(application) } returns application

      application.schemaUpToDate = false

      val result = applicationService.updateApprovedPremisesApplication(
        applicationId = applicationId,
        Cas1ApplicationUpdateFields(
          isWomensApplication = false,
          isPipeApplication = null,
          isEmergencyApplication = false,
          isEsapApplication = false,
          apType = null,
          releaseType = null,
          arrivalDate = null,
          data = "{}",
          isInapplicable = null,
          noticeType = null,
        ),
        userForRequest = user,
      )

      assertThat(result is CasResult.GeneralValidationError).isTrue
      result as CasResult.GeneralValidationError
      assertThat(result.message).isEqualTo("The schema version is outdated")
    }

    @Test
    fun `updateApprovedPremisesApplication returns GeneralValidationError when application has already been submitted`() {
      every { mockUserService.getUserForRequest() } returns user
      every { mockApplicationRepository.findByIdOrNull(applicationId) } returns application
      every { mockJsonSchemaService.checkSchemaOutdated(application) } returns application

      application.submittedAt = OffsetDateTime.now()

      val result = applicationService.updateApprovedPremisesApplication(
        applicationId = applicationId,
        Cas1ApplicationUpdateFields(
          isWomensApplication = false,
          isPipeApplication = null,
          isEmergencyApplication = false,
          isEsapApplication = false,
          apType = null,
          releaseType = null,
          arrivalDate = null,
          data = "{}",
          isInapplicable = null,
          noticeType = Cas1ApplicationTimelinessCategory.emergency,
        ),
        userForRequest = user,
      )

      assertThat(result is CasResult.GeneralValidationError).isTrue
      result as CasResult.GeneralValidationError

      assertThat(result.message).isEqualTo("This application has already been submitted")
    }

    @Test
    fun `updateApprovedPremisesApplication returns GeneralValidationError when application has AP type specified in multiple ways`() {
      every { mockUserService.getUserForRequest() } returns user
      every { mockApplicationRepository.findByIdOrNull(applicationId) } returns application
      every { mockJsonSchemaService.checkSchemaOutdated(application) } returns application

      application.submittedAt = OffsetDateTime.now()

      val result = applicationService.updateApprovedPremisesApplication(
        applicationId = applicationId,
        Cas1ApplicationUpdateFields(
          isWomensApplication = false,
          isPipeApplication = null,
          isEmergencyApplication = false,
          isEsapApplication = false,
          apType = ApType.normal,
          releaseType = null,
          arrivalDate = null,
          data = "{}",
          isInapplicable = null,
          noticeType = null,
        ),
        userForRequest = user,
      )

      assertThat(result is CasResult.GeneralValidationError).isTrue
      result as CasResult.GeneralValidationError
      assertThat(result.message).isEqualTo("`isPipeApplication`/`isEsapApplication` should not be used in conjunction with `apType`")
    }

    @Test
    fun `updateApprovedPremisesApplication returns Success with updated Application when using legacy AP type fields`() {
      setupMocksForSuccess()

      val theApplicantUserDetailsEntity = Cas1ApplicationUserDetailsEntityFactory().produce()
      every {
        mockCas1ApplicationUserDetailsRepository.save(
          match { it.name == "applicantName" && it.email == "applicantEmail" && it.telephoneNumber == "applicantPhone" },
        )
      } returns theApplicantUserDetailsEntity

      val theCaseManagerUserDetailsEntity = Cas1ApplicationUserDetailsEntityFactory().produce()
      every {
        mockCas1ApplicationUserDetailsRepository.save(
          match { it.name == "caseManagerName" && it.email == "caseManagerEmail" && it.telephoneNumber == "caseManagerPhone" },
        )
      } returns theCaseManagerUserDetailsEntity

      val result = applicationService.updateApprovedPremisesApplication(
        applicationId = applicationId,
        Cas1ApplicationUpdateFields(
          isWomensApplication = false,
          isPipeApplication = true,
          isEmergencyApplication = false,
          isEsapApplication = false,
          apType = null,
          releaseType = "rotl",
          arrivalDate = LocalDate.parse("2023-04-17"),
          data = updatedData,
          isInapplicable = false,
          noticeType = Cas1ApplicationTimelinessCategory.emergency,
        ),
        userForRequest = user,
      )

      assertThat(result is CasResult.Success).isTrue
      result as CasResult.Success

      val approvedPremisesApplication = result.value as ApprovedPremisesApplicationEntity

      assertThat(approvedPremisesApplication.data).isEqualTo(updatedData)
      assertThat(approvedPremisesApplication.isWomensApplication).isEqualTo(false)
      assertThat(approvedPremisesApplication.isPipeApplication).isEqualTo(true)
      assertThat(approvedPremisesApplication.releaseType).isEqualTo("rotl")
      assertThat(approvedPremisesApplication.isInapplicable).isEqualTo(false)
      assertThat(approvedPremisesApplication.arrivalDate).isEqualTo(OffsetDateTime.parse("2023-04-17T00:00:00Z"))
      assertThat(approvedPremisesApplication.applicantUserDetails).isNull()
      assertThat(approvedPremisesApplication.caseManagerIsNotApplicant).isNull()
      assertThat(approvedPremisesApplication.caseManagerUserDetails).isNull()
      assertThat(approvedPremisesApplication.noticeType).isEqualTo(Cas1ApplicationTimelinessCategory.emergency)
    }

    @ParameterizedTest
    @EnumSource(ApType::class)
    fun `updateApprovedPremisesApplication returns Success with updated Application when using apType`(apType: ApType) {
      setupMocksForSuccess()

      val theApplicantUserDetailsEntity = Cas1ApplicationUserDetailsEntityFactory().produce()
      every {
        mockCas1ApplicationUserDetailsRepository.save(
          match { it.name == "applicantName" && it.email == "applicantEmail" && it.telephoneNumber == "applicantPhone" },
        )
      } returns theApplicantUserDetailsEntity

      val theCaseManagerUserDetailsEntity = Cas1ApplicationUserDetailsEntityFactory().produce()
      every {
        mockCas1ApplicationUserDetailsRepository.save(
          match { it.name == "caseManagerName" && it.email == "caseManagerEmail" && it.telephoneNumber == "caseManagerPhone" },
        )
      } returns theCaseManagerUserDetailsEntity

      val result = applicationService.updateApprovedPremisesApplication(
        applicationId = applicationId,
        Cas1ApplicationUpdateFields(
          isWomensApplication = false,
          isPipeApplication = null,
          isEmergencyApplication = false,
          isEsapApplication = null,
          apType = apType,
          releaseType = "rotl",
          arrivalDate = LocalDate.parse("2023-04-17"),
          data = updatedData,
          isInapplicable = false,
          noticeType = Cas1ApplicationTimelinessCategory.emergency,
        ),
        userForRequest = user,
      )

      assertThat(result is CasResult.Success).isTrue
      result as CasResult.Success
      val approvedPremisesApplication = result.value as ApprovedPremisesApplicationEntity

      assertThat(approvedPremisesApplication.data).isEqualTo(updatedData)
      assertThat(approvedPremisesApplication.isWomensApplication).isEqualTo(false)
      assertThat(approvedPremisesApplication.isPipeApplication).isEqualTo(apType == ApType.pipe)
      assertThat(approvedPremisesApplication.isEsapApplication).isEqualTo(apType == ApType.esap)
      assertThat(approvedPremisesApplication.apType).isEqualTo(apType.asApprovedPremisesType())
      assertThat(approvedPremisesApplication.releaseType).isEqualTo("rotl")
      assertThat(approvedPremisesApplication.isInapplicable).isEqualTo(false)
      assertThat(approvedPremisesApplication.arrivalDate).isEqualTo(OffsetDateTime.parse("2023-04-17T00:00:00Z"))
      assertThat(approvedPremisesApplication.applicantUserDetails).isNull()
      assertThat(approvedPremisesApplication.caseManagerIsNotApplicant).isNull()
      assertThat(approvedPremisesApplication.caseManagerUserDetails).isNull()
      assertThat(approvedPremisesApplication.noticeType).isEqualTo(Cas1ApplicationTimelinessCategory.emergency)
    }

    @ParameterizedTest
    @EnumSource(value = Cas1ApplicationTimelinessCategory::class)
    fun `updateApprovedPremisesApplication sets noticeType correctly`(noticeType: Cas1ApplicationTimelinessCategory) {
      setupMocksForSuccess()

      val theApplicantUserDetailsEntity = Cas1ApplicationUserDetailsEntityFactory().produce()
      every {
        mockCas1ApplicationUserDetailsRepository.save(
          match { it.name == "applicantName" && it.email == "applicantEmail" && it.telephoneNumber == "applicantPhone" },
        )
      } returns theApplicantUserDetailsEntity

      val theCaseManagerUserDetailsEntity = Cas1ApplicationUserDetailsEntityFactory().produce()
      every {
        mockCas1ApplicationUserDetailsRepository.save(
          match { it.name == "caseManagerName" && it.email == "caseManagerEmail" && it.telephoneNumber == "caseManagerPhone" },
        )
      } returns theCaseManagerUserDetailsEntity

      val result = applicationService.updateApprovedPremisesApplication(
        applicationId = applicationId,
        Cas1ApplicationUpdateFields(
          isWomensApplication = false,
          isPipeApplication = true,
          isEmergencyApplication = noticeType == Cas1ApplicationTimelinessCategory.emergency,
          isEsapApplication = false,
          apType = null,
          releaseType = "rotl",
          arrivalDate = if (noticeType == Cas1ApplicationTimelinessCategory.shortNotice) {
            LocalDate.now().plusDays(10)
          } else {
            LocalDate.now().plusMonths(7)
          },
          data = updatedData,
          isInapplicable = false,
          noticeType = null,
        ),
        userForRequest = user,
      )

      assertThat(result is CasResult.Success).isTrue
      result as CasResult.Success

      val approvedPremisesApplication = result.value as ApprovedPremisesApplicationEntity

      assertThat(approvedPremisesApplication.noticeType).isEqualTo(noticeType)
    }

    private fun setupMocksForSuccess() {
      every { mockUserService.getUserForRequest() } returns user
      every { mockApplicationRepository.findByIdOrNull(applicationId) } returns application
      every { mockJsonSchemaService.checkSchemaOutdated(application) } returns application
      every { mockJsonSchemaService.getNewestSchema(ApprovedPremisesApplicationJsonSchemaEntity::class.java) } returns newestSchema
      every { mockJsonSchemaService.validate(newestSchema, updatedData) } returns true
      every { mockApplicationListener.preUpdate(any()) } returns Unit
      every { mockApplicationRepository.save(any()) } answers { it.invocation.args[0] as ApplicationEntity }
    }
  }

  @Test
  fun `updateTemporaryAccommodationApplication returns NotFound when application doesn't exist`() {
    val applicationId = UUID.fromString("fa6e97ce-7b9e-473c-883c-83b1c2af773d")

    every { mockApplicationRepository.findByIdOrNull(applicationId) } returns null

    assertThat(
      applicationService.updateTemporaryAccommodationApplication(
        applicationId = applicationId,
        data = "{}",
      ) is CasResult.NotFound,
    ).isTrue
  }

  @Test
  fun `updateTemporaryAccommodationApplication returns Unauthorised when application doesn't belong to request user`() {
    val applicationId = UUID.fromString("fa6e97ce-7b9e-473c-883c-83b1c2af773d")
    val username = "SOMEPERSON"

    val probationRegion = ProbationRegionEntityFactory()
      .withYieldedApArea { ApAreaEntityFactory().produce() }
      .produce()
    val application = TemporaryAccommodationApplicationEntityFactory()
      .withId(applicationId)
      .withYieldedCreatedByUser {
        UserEntityFactory()
          .withProbationRegion(probationRegion)
          .produce()
      }
      .withProbationRegion(probationRegion)
      .produce()

    every { mockUserService.getUserForRequest() } returns UserEntityFactory()
      .withDeliusUsername(username)
      .withYieldedProbationRegion {
        ProbationRegionEntityFactory()
          .withYieldedApArea { ApAreaEntityFactory().produce() }
          .produce()
      }
      .produce()
    every { mockApplicationRepository.findByIdOrNull(applicationId) } returns application
    every { mockJsonSchemaService.checkSchemaOutdated(application) } returns application

    assertThat(
      applicationService.updateTemporaryAccommodationApplication(
        applicationId = applicationId,
        data = "{}",
      ) is CasResult.Unauthorised,
    ).isTrue
  }

  @Test
  fun `updateTemporaryAccommodationApplication returns GeneralValidationError when application schema is outdated`() {
    val applicationId = UUID.fromString("fa6e97ce-7b9e-473c-883c-83b1c2af773d")
    val username = "SOMEPERSON"

    val user = UserEntityFactory()
      .withDeliusUsername(username)
      .withYieldedProbationRegion {
        ProbationRegionEntityFactory()
          .withYieldedApArea { ApAreaEntityFactory().produce() }
          .produce()
      }
      .produce()

    val application = TemporaryAccommodationApplicationEntityFactory()
      .withId(applicationId)
      .withCreatedByUser(user)
      .withSubmittedAt(null)
      .withProbationRegion(user.probationRegion)
      .produce()
      .apply {
        schemaUpToDate = false
      }

    every { mockUserService.getUserForRequest() } returns user
    every { mockApplicationRepository.findByIdOrNull(applicationId) } returns application
    every { mockJsonSchemaService.checkSchemaOutdated(application) } returns application

    val result = applicationService.updateTemporaryAccommodationApplication(
      applicationId = applicationId,
      data = "{}",
    )

    assertThat(result is CasResult.GeneralValidationError).isTrue
    result as CasResult.GeneralValidationError

    assertThat(result.message).isEqualTo("The schema version is outdated")
  }

  @Test
  fun `updateTemporaryAccommodationApplication returns GeneralValidationError when application has already been submitted`() {
    val applicationId = UUID.fromString("fa6e97ce-7b9e-473c-883c-83b1c2af773d")
    val username = "SOMEPERSON"

    val newestSchema = TemporaryAccommodationApplicationJsonSchemaEntityFactory().produce()

    val user = UserEntityFactory()
      .withDeliusUsername(username)
      .withYieldedProbationRegion {
        ProbationRegionEntityFactory()
          .withYieldedApArea { ApAreaEntityFactory().produce() }
          .produce()
      }
      .produce()

    val application = TemporaryAccommodationApplicationEntityFactory()
      .withApplicationSchema(newestSchema)
      .withId(applicationId)
      .withCreatedByUser(user)
      .withSubmittedAt(OffsetDateTime.now())
      .withProbationRegion(user.probationRegion)
      .produce()
      .apply {
        schemaUpToDate = true
      }

    every { mockUserService.getUserForRequest() } returns user
    every { mockApplicationRepository.findByIdOrNull(applicationId) } returns application
    every { mockJsonSchemaService.checkSchemaOutdated(application) } returns application

    val result = applicationService.updateTemporaryAccommodationApplication(
      applicationId = applicationId,
      data = "{}",
    )

    assertThat(result is CasResult.GeneralValidationError).isTrue
    result as CasResult.GeneralValidationError

    assertThat(result.message).isEqualTo("This application has already been submitted")
  }

  @Test
  fun `updateTemporaryAccommodationApplication returns GeneralValidationError when application has already been deleted`() {
    val applicationId = UUID.fromString("fa6e97ce-7b9e-473c-883c-83b1c2af773d")
    val username = "SOMEPERSON"

    val newestSchema = TemporaryAccommodationApplicationJsonSchemaEntityFactory().produce()

    val user = UserEntityFactory()
      .withDeliusUsername(username)
      .withYieldedProbationRegion {
        ProbationRegionEntityFactory()
          .withYieldedApArea { ApAreaEntityFactory().produce() }
          .produce()
      }
      .produce()

    val application = TemporaryAccommodationApplicationEntityFactory()
      .withApplicationSchema(newestSchema)
      .withId(applicationId)
      .withCreatedByUser(user)
      .withDeletedAt(OffsetDateTime.now().minusDays(7))
      .withProbationRegion(user.probationRegion)
      .produce()
      .apply {
        schemaUpToDate = true
      }

    every { mockUserService.getUserForRequest() } returns user
    every { mockApplicationRepository.findByIdOrNull(applicationId) } returns application
    every { mockJsonSchemaService.checkSchemaOutdated(application) } returns application

    val result = applicationService.updateTemporaryAccommodationApplication(
      applicationId = applicationId,
      data = "{}",
    )

    assertThat(result is CasResult.GeneralValidationError).isTrue
    result as CasResult.GeneralValidationError

    assertThat(result.message).isEqualTo("This application has already been deleted")
  }

  @Test
  fun `updateTemporaryAccommodationApplication returns Success with updated Application`() {
    val applicationId = UUID.fromString("fa6e97ce-7b9e-473c-883c-83b1c2af773d")
    val username = "SOMEPERSON"

    val user = UserEntityFactory()
      .withDeliusUsername(username)
      .withYieldedProbationRegion {
        ProbationRegionEntityFactory()
          .withYieldedApArea { ApAreaEntityFactory().produce() }
          .produce()
      }
      .produce()

    val newestSchema = TemporaryAccommodationApplicationJsonSchemaEntityFactory().produce()
    val updatedData = """
      {
        "aProperty": "value"
      }
    """

    val application = TemporaryAccommodationApplicationEntityFactory()
      .withApplicationSchema(newestSchema)
      .withId(applicationId)
      .withCreatedByUser(user)
      .withProbationRegion(user.probationRegion)
      .produce()
      .apply {
        schemaUpToDate = true
      }

    every { mockUserService.getUserForRequest() } returns user
    every { mockApplicationRepository.findByIdOrNull(applicationId) } returns application
    every { mockJsonSchemaService.checkSchemaOutdated(application) } returns application
    every { mockJsonSchemaService.validate(newestSchema, updatedData) } returns true
    every { mockApplicationRepository.save(any()) } answers { it.invocation.args[0] as ApplicationEntity }

    val result = applicationService.updateTemporaryAccommodationApplication(
      applicationId = applicationId,
      data = updatedData,
    )

    assertThat(result is CasResult.Success).isTrue
    result as CasResult.Success

    val approvedPremisesApplication = result.value as TemporaryAccommodationApplicationEntity

    assertThat(approvedPremisesApplication.data).isEqualTo(updatedData)
  }

  @Nested
  inner class SubmitApplicationCas1 {
    val applicationId: UUID = UUID.fromString("fa6e97ce-7b9e-473c-883c-83b1c2af773d")
    val username = "SOMEPERSON"
    val apArea = ApAreaEntityFactory().produce()
    val user = UserEntityFactory()
      .withDefaults()
      .withDeliusUsername(this.username)
      .withApArea(apArea)
      .produce()

    private var defaultSubmitApprovedPremisesApplication = SubmitApprovedPremisesApplication(
      translatedDocument = {},
      isPipeApplication = true,
      isWomensApplication = false,
      isEmergencyApplication = false,
      isEsapApplication = false,
      targetLocation = "SW1A 1AA",
      releaseType = ReleaseTypeOption.licence,
      type = "CAS1",
      sentenceType = SentenceTypeOption.nonStatutory,
      applicantUserDetails = Cas1ApplicationUserDetails("applicantName", "applicantEmail", "applicantPhone"),
      caseManagerIsNotApplicant = false,
    )

    private val newestSchema = ApprovedPremisesApplicationJsonSchemaEntityFactory().produce()

    @BeforeEach
    fun setup() {
      every { mockLockableApplicationRepository.acquirePessimisticLock(any()) } returns LockableApplicationEntity(UUID.randomUUID())
      every { mockObjectMapper.writeValueAsString(defaultSubmitApprovedPremisesApplication.translatedDocument) } returns "{}"
    }

    @Test
    fun `submitApprovedPremisesApplication returns NotFound when application doesn't exist`() {
      every { mockApplicationRepository.findByIdOrNull(applicationId) } returns null

      assertThat(
        applicationService.submitApprovedPremisesApplication(
          applicationId,
          defaultSubmitApprovedPremisesApplication,
          user,
          apAreaId = UUID.randomUUID(),
        ) is AuthorisableActionResult.NotFound,
      ).isTrue
    }

    @Test
    fun `submitApprovedPremisesApplication returns Unauthorised when application doesn't belong to request user`() {
      val application = ApprovedPremisesApplicationEntityFactory()
        .withId(applicationId)
        .withYieldedCreatedByUser { UserEntityFactory().withDefaultProbationRegion().produce() }
        .produce()

      every { mockApplicationRepository.findByIdOrNull(applicationId) } returns application
      every { mockJsonSchemaService.checkSchemaOutdated(application) } returns application

      assertThat(
        applicationService.submitApprovedPremisesApplication(
          applicationId,
          defaultSubmitApprovedPremisesApplication,
          user,
          apAreaId = UUID.randomUUID(),
        ) is AuthorisableActionResult.Unauthorised,
      ).isTrue
    }

    @Test
    fun `submitApprovedPremisesApplication returns GeneralValidationError when application schema is outdated`() {
      val application = ApprovedPremisesApplicationEntityFactory()
        .withId(applicationId)
        .withCreatedByUser(user)
        .withSubmittedAt(null)
        .produce()
        .apply {
          schemaUpToDate = false
        }

      every { mockApplicationRepository.findByIdOrNull(applicationId) } returns application
      every { mockJsonSchemaService.checkSchemaOutdated(application) } returns application

      val result = applicationService.submitApprovedPremisesApplication(
        applicationId,
        defaultSubmitApprovedPremisesApplication,
        user,
        apAreaId = UUID.randomUUID(),
      )

      assertThat(result is AuthorisableActionResult.Success).isTrue
      result as AuthorisableActionResult.Success

      assertThat(result.entity is ValidatableActionResult.GeneralValidationError).isTrue
      val validatableActionResult = result.entity as ValidatableActionResult.GeneralValidationError

      assertThat(validatableActionResult.message).isEqualTo("The schema version is outdated")
    }

    @Test
    fun `submitApprovedPremisesApplication returns GeneralValidationError when application has already been submitted`() {
      val newestSchema = ApprovedPremisesApplicationJsonSchemaEntityFactory().produce()

      val application = ApprovedPremisesApplicationEntityFactory()
        .withApplicationSchema(newestSchema)
        .withId(applicationId)
        .withCreatedByUser(user)
        .withSubmittedAt(OffsetDateTime.now())
        .produce()
        .apply {
          schemaUpToDate = true
        }

      every { mockApplicationRepository.findByIdOrNull(applicationId) } returns application
      every { mockJsonSchemaService.checkSchemaOutdated(application) } returns application

      val result = applicationService.submitApprovedPremisesApplication(
        applicationId,
        defaultSubmitApprovedPremisesApplication,
        user,
        apAreaId = UUID.randomUUID(),
      )

      assertThat(result is AuthorisableActionResult.Success).isTrue
      result as AuthorisableActionResult.Success

      assertThat(result.entity is ValidatableActionResult.GeneralValidationError).isTrue
      val validatableActionResult = result.entity as ValidatableActionResult.GeneralValidationError

      assertThat(validatableActionResult.message).isEqualTo("This application has already been submitted")
    }

    @Test
    fun `submitApprovedPremisesApplication returns GeneralValidationError when applicantIsNotCaseManager is true and no case manager details are provided`() {
      val newestSchema = ApprovedPremisesApplicationJsonSchemaEntityFactory().produce()

      val application = ApprovedPremisesApplicationEntityFactory()
        .withApplicationSchema(newestSchema)
        .withId(applicationId)
        .withCreatedByUser(user)
        .withSubmittedAt(null)
        .produce()
        .apply {
          schemaUpToDate = true
        }

      every { mockApplicationRepository.findByIdOrNull(applicationId) } returns application
      every { mockJsonSchemaService.checkSchemaOutdated(application) } returns application

      defaultSubmitApprovedPremisesApplication = SubmitApprovedPremisesApplication(
        translatedDocument = {},
        isPipeApplication = true,
        isWomensApplication = false,
        isEmergencyApplication = false,
        isEsapApplication = false,
        targetLocation = "SW1A 1AA",
        releaseType = ReleaseTypeOption.licence,
        type = "CAS1",
        sentenceType = SentenceTypeOption.nonStatutory,
        applicantUserDetails = Cas1ApplicationUserDetails("applicantName", "applicantEmail", "applicantPhone"),
        caseManagerIsNotApplicant = true,
      )

      every { mockObjectMapper.writeValueAsString(defaultSubmitApprovedPremisesApplication.translatedDocument) } returns "{}"

      val result = applicationService.submitApprovedPremisesApplication(
        applicationId,
        defaultSubmitApprovedPremisesApplication,
        user,
        apAreaId = UUID.randomUUID(),
      )

      assertThat(result is AuthorisableActionResult.Success).isTrue
      result as AuthorisableActionResult.Success
      assertThat(result.entity is ValidatableActionResult.GeneralValidationError).isTrue
      val validatableActionResult = result.entity as ValidatableActionResult.GeneralValidationError

      assertThat(validatableActionResult.message).isEqualTo("caseManagerUserDetails must be provided if caseManagerIsNotApplicant is true")
    }

    @Test
    fun `submitApprovedPremisesApplication returns GeneralValidationError when application has AP type specified in multiple ways`() {
      val newestSchema = ApprovedPremisesApplicationJsonSchemaEntityFactory().produce()

      val application = ApprovedPremisesApplicationEntityFactory()
        .withApplicationSchema(newestSchema)
        .withId(applicationId)
        .withCreatedByUser(user)
        .withSubmittedAt(null)
        .produce()
        .apply {
          schemaUpToDate = true
        }

      every { mockApplicationRepository.findByIdOrNull(applicationId) } returns application
      every { mockJsonSchemaService.checkSchemaOutdated(application) } returns application

      defaultSubmitApprovedPremisesApplication = SubmitApprovedPremisesApplication(
        translatedDocument = {},
        isPipeApplication = true,
        isWomensApplication = false,
        isEmergencyApplication = false,
        isEsapApplication = false,
        apType = ApType.normal,
        targetLocation = "SW1A 1AA",
        releaseType = ReleaseTypeOption.licence,
        type = "CAS1",
        sentenceType = SentenceTypeOption.nonStatutory,
        applicantUserDetails = null,
        caseManagerIsNotApplicant = false,
      )

      every { mockObjectMapper.writeValueAsString(defaultSubmitApprovedPremisesApplication.translatedDocument) } returns "{}"

      val result = applicationService.submitApprovedPremisesApplication(
        applicationId,
        defaultSubmitApprovedPremisesApplication,
        user,
        apAreaId = UUID.randomUUID(),
      )

      assertThat(result is AuthorisableActionResult.Success).isTrue
      result as AuthorisableActionResult.Success
      assertThat(result.entity is ValidatableActionResult.GeneralValidationError).isTrue
      val validatableActionResult = result.entity as ValidatableActionResult.GeneralValidationError

      assertThat(validatableActionResult.message).isEqualTo("`isPipeApplication`/`isEsapApplication` should not be used in conjunction with `apType`")
    }

    @ParameterizedTest
    @EnumSource(value = SituationOption::class)
    @NullSource
    @Suppress("CyclomaticComplexMethod")
    fun `submitApprovedPremisesApplication returns Success, creates assessment and stores event, triggers email`(
      situation: SituationOption?,
    ) {
      defaultSubmitApprovedPremisesApplication = SubmitApprovedPremisesApplication(
        translatedDocument = {},
        isPipeApplication = true,
        isWomensApplication = false,
        isEmergencyApplication = false,
        isEsapApplication = false,
        targetLocation = "SW1A 1AA",
        releaseType = ReleaseTypeOption.licence,
        type = "CAS1",
        sentenceType = SentenceTypeOption.nonStatutory,
        situation = situation,
        applicantUserDetails = Cas1ApplicationUserDetails("applicantName", "applicantEmail", "applicantPhone"),
        caseManagerIsNotApplicant = true,
        caseManagerUserDetails = Cas1ApplicationUserDetails("caseManagerName", "caseManagerEmail", "caseManagerPhone"),
        noticeType = Cas1ApplicationTimelinessCategory.standard,
      )

      val application = ApprovedPremisesApplicationEntityFactory()
        .withApplicationSchema(newestSchema)
        .withId(applicationId)
        .withCreatedByUser(user)
        .withSubmittedAt(null)
        .produce()
        .apply {
          schemaUpToDate = true
        }

      setupMocksForSuccess(application)

      val theApplicantUserDetailsEntity = Cas1ApplicationUserDetailsEntityFactory().produce()
      every {
        mockCas1ApplicationUserDetailsRepository.save(
          match { it.name == "applicantName" && it.email == "applicantEmail" && it.telephoneNumber == "applicantPhone" },
        )
      } returns theApplicantUserDetailsEntity

      val theCaseManagerUserDetailsEntity = Cas1ApplicationUserDetailsEntityFactory().produce()
      every {
        mockCas1ApplicationUserDetailsRepository.save(
          match { it.name == "caseManagerName" && it.email == "caseManagerEmail" && it.telephoneNumber == "caseManagerPhone" },
        )
      } returns theCaseManagerUserDetailsEntity

      val result =
        applicationService.submitApprovedPremisesApplication(
          applicationId,
          defaultSubmitApprovedPremisesApplication,
          user,
          apAreaId = apArea.id,
        )

      assertThat(result is AuthorisableActionResult.Success).isTrue
      result as AuthorisableActionResult.Success

      assertThat(result.entity is ValidatableActionResult.Success).isTrue
      val validatableActionResult = result.entity as ValidatableActionResult.Success
      val persistedApplication = validatableActionResult.entity as ApprovedPremisesApplicationEntity
      assertThat(persistedApplication.isPipeApplication).isTrue
      assertThat(persistedApplication.isWomensApplication).isFalse
      assertThat(persistedApplication.releaseType).isEqualTo(defaultSubmitApprovedPremisesApplication.releaseType.toString())
      if (situation == null) {
        assertThat(persistedApplication.situation).isNull()
      } else {
        assertThat(persistedApplication.situation).isEqualTo(situation.toString())
      }
      assertThat(persistedApplication.targetLocation).isEqualTo(defaultSubmitApprovedPremisesApplication.targetLocation)
      assertThat(persistedApplication.inmateInOutStatusOnSubmission).isEqualTo("OUT")
      assertThat(persistedApplication.applicantUserDetails).isEqualTo(theApplicantUserDetailsEntity)
      assertThat(persistedApplication.caseManagerIsNotApplicant).isEqualTo(true)
      assertThat(persistedApplication.caseManagerUserDetails).isEqualTo(theCaseManagerUserDetailsEntity)
      assertThat(persistedApplication.noticeType).isEqualTo(Cas1ApplicationTimelinessCategory.standard)
      assertThat(persistedApplication.apArea).isEqualTo(apArea)
      assertThat(persistedApplication.cruManagementArea).isEqualTo(apArea.defaultCruManagementArea)
      assertThat(persistedApplication.licenceExpiryDate).isNull()

      verify { mockApplicationRepository.save(any()) }
      verify(exactly = 1) { mockAssessmentService.createApprovedPremisesAssessment(application) }

      verify(exactly = 1) {
        mockCas1ApplicationDomainEventService.applicationSubmitted(
          application,
          defaultSubmitApprovedPremisesApplication,
          username,
        )
      }

      verify(exactly = 1) { mockCas1ApplicationEmailService.applicationSubmitted(application) }
      verify(exactly = 0) { mockPlacementApplicationAutomaticRepository.save(any()) }
    }

    @ParameterizedTest
    @EnumSource(value = Cas1ApplicationTimelinessCategory::class)
    fun `submitApprovedPremisesApplication sets noticeType correctly`(noticeType: Cas1ApplicationTimelinessCategory) {
      defaultSubmitApprovedPremisesApplication = SubmitApprovedPremisesApplication(
        translatedDocument = {},
        isPipeApplication = true,
        isWomensApplication = false,
        isEmergencyApplication = noticeType == Cas1ApplicationTimelinessCategory.emergency,
        isEsapApplication = false,
        targetLocation = "SW1A 1AA",
        releaseType = ReleaseTypeOption.licence,
        type = "CAS1",
        sentenceType = SentenceTypeOption.nonStatutory,
        applicantUserDetails = Cas1ApplicationUserDetails("applicantName", "applicantEmail", "applicantPhone"),
        caseManagerIsNotApplicant = true,
        caseManagerUserDetails = Cas1ApplicationUserDetails("caseManagerName", "caseManagerEmail", "caseManagerPhone"),
        arrivalDate = if (noticeType == Cas1ApplicationTimelinessCategory.shortNotice) {
          LocalDate.now().plusDays(10)
        } else {
          LocalDate.now().plusMonths(7)
        },
        noticeType = null,
      )

      val application = ApprovedPremisesApplicationEntityFactory()
        .withApplicationSchema(newestSchema)
        .withId(applicationId)
        .withCreatedByUser(user)
        .withCreatedAt(OffsetDateTime.now())
        .withSubmittedAt(null)
        .produce()
        .apply {
          schemaUpToDate = true
        }

      setupMocksForSuccess(application)

      val theApplicantUserDetailsEntity = Cas1ApplicationUserDetailsEntityFactory().produce()
      every {
        mockCas1ApplicationUserDetailsRepository.save(
          match { it.name == "applicantName" && it.email == "applicantEmail" && it.telephoneNumber == "applicantPhone" },
        )
      } returns theApplicantUserDetailsEntity

      val theCaseManagerUserDetailsEntity = Cas1ApplicationUserDetailsEntityFactory().produce()
      every {
        mockCas1ApplicationUserDetailsRepository.save(
          match { it.name == "caseManagerName" && it.email == "caseManagerEmail" && it.telephoneNumber == "caseManagerPhone" },
        )
      } returns theCaseManagerUserDetailsEntity

      val result =
        applicationService.submitApprovedPremisesApplication(
          applicationId,
          defaultSubmitApprovedPremisesApplication,
          user,
          apAreaId = apArea.id,
        )

      assertThat(result is AuthorisableActionResult.Success).isTrue
      result as AuthorisableActionResult.Success

      assertThat(result.entity is ValidatableActionResult.Success).isTrue
      val validatableActionResult = result.entity as ValidatableActionResult.Success
      val persistedApplication = validatableActionResult.entity as ApprovedPremisesApplicationEntity
      assertThat(persistedApplication.isPipeApplication).isTrue
      assertThat(persistedApplication.isWomensApplication).isFalse
      assertThat(persistedApplication.releaseType).isEqualTo(defaultSubmitApprovedPremisesApplication.releaseType.toString())
      assertThat(persistedApplication.noticeType).isEqualTo(noticeType)
      assertThat(persistedApplication.targetLocation).isEqualTo(defaultSubmitApprovedPremisesApplication.targetLocation)
      assertThat(persistedApplication.inmateInOutStatusOnSubmission).isEqualTo("OUT")
      assertThat(persistedApplication.applicantUserDetails).isEqualTo(theApplicantUserDetailsEntity)
      assertThat(persistedApplication.caseManagerIsNotApplicant).isEqualTo(true)
      assertThat(persistedApplication.caseManagerUserDetails).isEqualTo(theCaseManagerUserDetailsEntity)

      verify { mockApplicationRepository.save(any()) }
      verify(exactly = 1) { mockAssessmentService.createApprovedPremisesAssessment(application) }

      verify(exactly = 1) {
        mockCas1ApplicationDomainEventService.applicationSubmitted(
          application,
          defaultSubmitApprovedPremisesApplication,
          username,
        )
      }

      verify(exactly = 1) { mockCas1ApplicationEmailService.applicationSubmitted(application) }
      verify(exactly = 1) { mockPlacementApplicationAutomaticRepository.save(any()) }
    }

    @ParameterizedTest
    @EnumSource(ApType::class)
    fun `submitApprovedPremisesApplication sets apType correctly`(apType: ApType) {
      defaultSubmitApprovedPremisesApplication = SubmitApprovedPremisesApplication(
        translatedDocument = {},
        isPipeApplication = null,
        isWomensApplication = false,
        isEmergencyApplication = false,
        isEsapApplication = null,
        apType = apType,
        targetLocation = "SW1A 1AA",
        releaseType = ReleaseTypeOption.licence,
        type = "CAS1",
        sentenceType = SentenceTypeOption.nonStatutory,
        applicantUserDetails = Cas1ApplicationUserDetails("applicantName", "applicantEmail", "applicantPhone"),
        caseManagerIsNotApplicant = true,
        caseManagerUserDetails = Cas1ApplicationUserDetails("caseManagerName", "caseManagerEmail", "caseManagerPhone"),
        arrivalDate = LocalDate.now().plusMonths(7),
        noticeType = null,
      )

      val application = ApprovedPremisesApplicationEntityFactory()
        .withApplicationSchema(newestSchema)
        .withId(applicationId)
        .withCreatedByUser(user)
        .withCreatedAt(OffsetDateTime.now())
        .withSubmittedAt(null)
        .produce()
        .apply {
          schemaUpToDate = true
        }

      setupMocksForSuccess(application)

      val theApplicantUserDetailsEntity = Cas1ApplicationUserDetailsEntityFactory().produce()
      every {
        mockCas1ApplicationUserDetailsRepository.save(
          match { it.name == "applicantName" && it.email == "applicantEmail" && it.telephoneNumber == "applicantPhone" },
        )
      } returns theApplicantUserDetailsEntity

      val theCaseManagerUserDetailsEntity = Cas1ApplicationUserDetailsEntityFactory().produce()
      every {
        mockCas1ApplicationUserDetailsRepository.save(
          match { it.name == "caseManagerName" && it.email == "caseManagerEmail" && it.telephoneNumber == "caseManagerPhone" },
        )
      } returns theCaseManagerUserDetailsEntity

      val result =
        applicationService.submitApprovedPremisesApplication(
          applicationId,
          defaultSubmitApprovedPremisesApplication,
          user,
          apAreaId = apArea.id,
        )

      assertThat(result is AuthorisableActionResult.Success).isTrue
      result as AuthorisableActionResult.Success

      assertThat(result.entity is ValidatableActionResult.Success).isTrue
      val validatableActionResult = result.entity as ValidatableActionResult.Success
      val persistedApplication = validatableActionResult.entity as ApprovedPremisesApplicationEntity
      assertThat(persistedApplication.isPipeApplication).isEqualTo(apType == ApType.pipe)
      assertThat(persistedApplication.isEsapApplication).isEqualTo(apType == ApType.esap)
      assertThat(persistedApplication.apType).isEqualTo(apType.asApprovedPremisesType())
      assertThat(persistedApplication.isWomensApplication).isFalse
      assertThat(persistedApplication.releaseType).isEqualTo(defaultSubmitApprovedPremisesApplication.releaseType.toString())
      assertThat(persistedApplication.noticeType).isEqualTo(Cas1ApplicationTimelinessCategory.standard)
      assertThat(persistedApplication.targetLocation).isEqualTo(defaultSubmitApprovedPremisesApplication.targetLocation)
      assertThat(persistedApplication.inmateInOutStatusOnSubmission).isEqualTo("OUT")
      assertThat(persistedApplication.applicantUserDetails).isEqualTo(theApplicantUserDetailsEntity)
      assertThat(persistedApplication.caseManagerIsNotApplicant).isEqualTo(true)
      assertThat(persistedApplication.caseManagerUserDetails).isEqualTo(theCaseManagerUserDetailsEntity)

      verify { mockApplicationRepository.save(any()) }
      verify(exactly = 1) { mockAssessmentService.createApprovedPremisesAssessment(application) }

      verify(exactly = 1) {
        mockCas1ApplicationDomainEventService.applicationSubmitted(
          application,
          defaultSubmitApprovedPremisesApplication,
          username,
        )
      }

      verify(exactly = 1) { mockCas1ApplicationEmailService.applicationSubmitted(application) }
    }

    @Test
    fun `submitApprovedPremisesApplication updates existing application user details`() {
      defaultSubmitApprovedPremisesApplication = SubmitApprovedPremisesApplication(
        translatedDocument = {},
        isPipeApplication = true,
        isWomensApplication = false,
        isEmergencyApplication = false,
        isEsapApplication = false,
        targetLocation = "SW1A 1AA",
        releaseType = ReleaseTypeOption.licence,
        type = "CAS1",
        sentenceType = SentenceTypeOption.nonStatutory,
        situation = SituationOption.bailSentence,
        applicantUserDetails = Cas1ApplicationUserDetails("applicantName", "applicantEmail", "applicantPhone"),
        caseManagerIsNotApplicant = true,
        caseManagerUserDetails = Cas1ApplicationUserDetails("caseManagerName", "caseManagerEmail", "caseManagerPhone"),
      )

      val application = ApprovedPremisesApplicationEntityFactory()
        .withApplicationSchema(newestSchema)
        .withId(applicationId)
        .withCreatedByUser(user)
        .withSubmittedAt(null)
        .produce()
        .apply {
          schemaUpToDate = true
        }

      setupMocksForSuccess(application)

      val existingApplicantUserDetails = Cas1ApplicationUserDetailsEntity(
        UUID.randomUUID(),
        "oldApplicantEmail",
        "oldApplicantName",
        "oldApplicantPhone",
      )
      val existingCaseManagerUserDetails = Cas1ApplicationUserDetailsEntity(
        UUID.randomUUID(),
        "oldApplicantEmail",
        "oldApplicantName",
        "oldApplicantPhone",
      )

      val theUpdatedApplicantUserDetailsEntity = Cas1ApplicationUserDetailsEntityFactory().produce()
      every {
        mockCas1ApplicationUserDetailsRepository.save(
          match {
            it.id == existingApplicantUserDetails.id &&
              it.name == "applicantName" &&
              it.email == "applicantEmail" &&
              it.telephoneNumber == "applicantPhone"
          },
        )
      } returns theUpdatedApplicantUserDetailsEntity

      val theUpdatedCaseManagerUserDetailsEntity = Cas1ApplicationUserDetailsEntityFactory().produce()
      every {
        mockCas1ApplicationUserDetailsRepository.save(
          match {
            it.id == existingCaseManagerUserDetails.id &&
              it.name == "caseManagerName" &&
              it.email == "caseManagerEmail" &&
              it.telephoneNumber == "caseManagerPhone"
          },
        )
      } returns theUpdatedCaseManagerUserDetailsEntity

      application.applicantUserDetails = existingApplicantUserDetails
      application.caseManagerUserDetails = existingCaseManagerUserDetails

      val result =
        applicationService.submitApprovedPremisesApplication(
          applicationId,
          defaultSubmitApprovedPremisesApplication,
          user,
          apAreaId = apArea.id,
        )

      result as AuthorisableActionResult.Success
      val validatableActionResult = result.entity as ValidatableActionResult.Success
      val persistedApplication = validatableActionResult.entity as ApprovedPremisesApplicationEntity

      assertThat(persistedApplication.applicantUserDetails).isEqualTo(theUpdatedApplicantUserDetailsEntity)
      assertThat(persistedApplication.caseManagerIsNotApplicant).isEqualTo(true)
      assertThat(persistedApplication.caseManagerUserDetails).isEqualTo(theUpdatedCaseManagerUserDetailsEntity)
    }

    @Test
    fun `updateApprovedPremisesApplication if applicant is now case manager, removes existing case manager user details`() {
      defaultSubmitApprovedPremisesApplication = SubmitApprovedPremisesApplication(
        translatedDocument = {},
        isPipeApplication = true,
        isWomensApplication = false,
        isEmergencyApplication = false,
        isEsapApplication = false,
        targetLocation = "SW1A 1AA",
        releaseType = ReleaseTypeOption.licence,
        type = "CAS1",
        sentenceType = SentenceTypeOption.nonStatutory,
        situation = SituationOption.bailSentence,
        applicantUserDetails = Cas1ApplicationUserDetails("applicantName", "applicantEmail", "applicantPhone"),
        caseManagerIsNotApplicant = false,
        caseManagerUserDetails = null,
      )

      val application = ApprovedPremisesApplicationEntityFactory()
        .withApplicationSchema(newestSchema)
        .withId(applicationId)
        .withCreatedByUser(user)
        .withSubmittedAt(null)
        .withApplicantUserDetails(Cas1ApplicationUserDetailsEntity(UUID.randomUUID(), "applicantName", "applicantEmail", "applicantPhone"))
        .withCaseManagerUserDetails(Cas1ApplicationUserDetailsEntity(UUID.randomUUID(), "oldCaseManEmail", "oldCaseManName", "oldCaseManPhone"))
        .produce()
        .apply {
          schemaUpToDate = true
        }

      setupMocksForSuccess(application)

      val existingApplicantUserDetails = application.applicantUserDetails!!
      val existingCaseManagerUserDetails = application.caseManagerUserDetails!!

      every {
        mockCas1ApplicationUserDetailsRepository.save(match { it.id == existingApplicantUserDetails.id })
      } answers { it.invocation.args[0] as Cas1ApplicationUserDetailsEntity }

      every { mockCas1ApplicationUserDetailsRepository.delete(existingCaseManagerUserDetails) } returns Unit

      val result = applicationService.submitApprovedPremisesApplication(
        applicationId,
        defaultSubmitApprovedPremisesApplication,
        user,
        apAreaId = apArea.id,
      )

      assertThat(result is AuthorisableActionResult.Success).isTrue

      verify { mockCas1ApplicationUserDetailsRepository.delete(existingCaseManagerUserDetails) }
    }

    private fun setupMocksForSuccess(application: ApprovedPremisesApplicationEntity) {
      every { mockObjectMapper.writeValueAsString(defaultSubmitApprovedPremisesApplication.translatedDocument) } returns "{}"
      every { mockApplicationRepository.findByIdOrNull(applicationId) } returns application
      every { mockJsonSchemaService.checkSchemaOutdated(application) } returns application
      every { mockJsonSchemaService.validate(newestSchema, application.data!!) } returns true
      every { mockApplicationListener.preUpdate(any()) } returns Unit
      every { mockApplicationRepository.save(any()) } answers { it.invocation.args[0] as ApplicationEntity }
      every { mockOffenderService.getInmateDetailByNomsNumber(any(), any()) } returns AuthorisableActionResult.Success(
        InmateDetailFactory().withCustodyStatus(InmateStatus.OUT).produce(),
      )

      every { mockApAreaRepository.findByIdOrNull(apArea.id) } returns apArea

      every { mockAssessmentService.createApprovedPremisesAssessment(application) } returns ApprovedPremisesAssessmentEntityFactory()
        .withApplication(application)
        .withAllocatedToUser(user)
        .produce()

      every {
        mockCas1ApplicationDomainEventService.applicationSubmitted(
          application,
          defaultSubmitApprovedPremisesApplication,
          username,
        )
      } returns Unit

      every { mockCas1ApplicationEmailService.applicationSubmitted(any()) } just Runs
      every {
        mockPlacementApplicationAutomaticRepository.save(any())
      } answers { it.invocation.args[0] as PlacementApplicationAutomaticEntity }
    }
  }

  @SuppressWarnings("UnusedPrivateProperty")
  @Nested
  inner class SubmitApplicationCas3 {
    val applicationId: UUID = UUID.fromString("fa6e97ce-7b9e-473c-883c-83b1c2af773d")
    val username = "SOMEPERSON"
    val user = UserEntityFactory()
      .withDeliusUsername(this.username)
      .withYieldedProbationRegion {
        ProbationRegionEntityFactory()
          .withYieldedApArea { ApAreaEntityFactory().produce() }
          .produce()
      }
      .produce()

    private val submitTemporaryAccommodationApplication = SubmitTemporaryAccommodationApplication(
      translatedDocument = {},
      type = "CAS3",
      arrivalDate = LocalDate.now(),
      summaryData = {
        val num = 50
        val text = "Hello world!"
      },
    )

    private val submitTemporaryAccommodationApplicationWithMiReportingData = SubmitTemporaryAccommodationApplication(
      translatedDocument = {},
      type = "CAS3",
      arrivalDate = LocalDate.now(),
      summaryData = {
        val num = 50
        val text = "Hello world!"
      },
      isRegisteredSexOffender = true,
      needsAccessibleProperty = true,
      hasHistoryOfArson = true,
      isDutyToReferSubmitted = true,
      dutyToReferSubmissionDate = LocalDate.now().minusDays(7),
      dutyToReferOutcome = "Accepted – Prevention/ Relief Duty",
      isApplicationEligible = true,
      eligibilityReason = "homelessFromApprovedPremises",
      dutyToReferLocalAuthorityAreaName = "Aberdeen City",
      personReleaseDate = LocalDate.now().plusDays(1),
      pdu = "Probation Delivery Unit Test",
      isHistoryOfSexualOffence = true,
      isConcerningSexualBehaviour = true,
      isConcerningArsonBehaviour = true,
      prisonReleaseTypes = listOf(
        "Standard recall",
        "ECSL",
        "PSS",
      ),
    )

    @BeforeEach
    fun setup() {
      every { mockLockableApplicationRepository.acquirePessimisticLock(any()) } returns LockableApplicationEntity(UUID.randomUUID())
      every { mockObjectMapper.writeValueAsString(submitTemporaryAccommodationApplication.translatedDocument) } returns "{}"
      every { mockObjectMapper.writeValueAsString(submitTemporaryAccommodationApplicationWithMiReportingData.translatedDocument) } returns "{}"
    }

    @Test
    fun `submitTemporaryAccommodationApplication returns NotFound when application doesn't exist`() {
      val applicationId = UUID.fromString("fa6e97ce-7b9e-473c-883c-83b1c2af773d")
      val username = "SOMEPERSON"

      every { mockApplicationRepository.findByIdOrNull(applicationId) } returns null

      assertThat(
        applicationService.submitTemporaryAccommodationApplication(
          applicationId,
          submitTemporaryAccommodationApplication,
        ) is AuthorisableActionResult.NotFound,
      ).isTrue
    }

    @Test
    fun `submitTemporaryAccommodationApplication returns Unauthorised when application doesn't belong to request user`() {
      val user = UserEntityFactory()
        .withYieldedProbationRegion {
          ProbationRegionEntityFactory()
            .withYieldedApArea { ApAreaEntityFactory().produce() }
            .produce()
        }
        .produce()

      val application = TemporaryAccommodationApplicationEntityFactory()
        .withId(applicationId)
        .withCreatedByUser(user)
        .withProbationRegion(user.probationRegion)
        .produce()

      every { mockUserService.getUserForRequest() } returns UserEntityFactory()
        .withDeliusUsername(username)
        .withYieldedProbationRegion {
          ProbationRegionEntityFactory()
            .withYieldedApArea { ApAreaEntityFactory().produce() }
            .produce()
        }
        .produce()
      every { mockApplicationRepository.findByIdOrNull(applicationId) } returns application
      every { mockJsonSchemaService.checkSchemaOutdated(application) } returns application

      assertThat(
        applicationService.submitTemporaryAccommodationApplication(
          applicationId,
          submitTemporaryAccommodationApplication,
        ) is AuthorisableActionResult.Unauthorised,
      ).isTrue
    }

    @Test
    fun `submitTemporaryAccommodationApplication returns GeneralValidationError when application schema is outdated`() {
      val application = TemporaryAccommodationApplicationEntityFactory()
        .withId(applicationId)
        .withCreatedByUser(user)
        .withSubmittedAt(null)
        .withProbationRegion(user.probationRegion)
        .produce()
        .apply {
          schemaUpToDate = false
        }

      every { mockUserService.getUserForRequest() } returns user
      every { mockApplicationRepository.findByIdOrNull(applicationId) } returns application
      every { mockJsonSchemaService.checkSchemaOutdated(application) } returns application

      val result = applicationService.submitTemporaryAccommodationApplication(
        applicationId,
        submitTemporaryAccommodationApplication,
      )

      assertThat(result is AuthorisableActionResult.Success).isTrue
      result as AuthorisableActionResult.Success

      assertThat(result.entity is ValidatableActionResult.GeneralValidationError).isTrue
      val validatableActionResult = result.entity as ValidatableActionResult.GeneralValidationError

      assertThat(validatableActionResult.message).isEqualTo("The schema version is outdated")
    }

    @Test
    fun `submitTemporaryAccommodationApplication returns GeneralValidationError when application has already been submitted`() {
      val newestSchema = TemporaryAccommodationApplicationJsonSchemaEntityFactory().produce()

      val application = TemporaryAccommodationApplicationEntityFactory()
        .withApplicationSchema(newestSchema)
        .withId(applicationId)
        .withCreatedByUser(user)
        .withSubmittedAt(OffsetDateTime.now())
        .withProbationRegion(user.probationRegion)
        .produce()
        .apply {
          schemaUpToDate = true
        }

      every { mockUserService.getUserForRequest() } returns user
      every { mockApplicationRepository.findByIdOrNull(applicationId) } returns application
      every { mockJsonSchemaService.checkSchemaOutdated(application) } returns application

      val result = applicationService.submitTemporaryAccommodationApplication(
        applicationId,
        submitTemporaryAccommodationApplication,
      )

      assertThat(result is AuthorisableActionResult.Success).isTrue
      result as AuthorisableActionResult.Success

      assertThat(result.entity is ValidatableActionResult.GeneralValidationError).isTrue
      val validatableActionResult = result.entity as ValidatableActionResult.GeneralValidationError

      assertThat(validatableActionResult.message).isEqualTo("This application has already been submitted")
    }

    @Test
    fun `submitTemporaryAccommodationApplication returns GeneralValidationError when application has already been deleted`() {
      val newestSchema = TemporaryAccommodationApplicationJsonSchemaEntityFactory().produce()

      val application = TemporaryAccommodationApplicationEntityFactory()
        .withApplicationSchema(newestSchema)
        .withId(applicationId)
        .withCreatedByUser(user)
        .withDeletedAt(OffsetDateTime.now().minusDays(22))
        .withProbationRegion(user.probationRegion)
        .produce()
        .apply {
          schemaUpToDate = true
        }

      every { mockUserService.getUserForRequest() } returns user
      every { mockApplicationRepository.findByIdOrNull(applicationId) } returns application
      every { mockJsonSchemaService.checkSchemaOutdated(application) } returns application

      val result = applicationService.submitTemporaryAccommodationApplication(
        applicationId,
        submitTemporaryAccommodationApplication,
      )

      assertThat(result is AuthorisableActionResult.Success).isTrue
      result as AuthorisableActionResult.Success

      assertThat(result.entity is ValidatableActionResult.GeneralValidationError).isTrue
      val validatableActionResult = result.entity as ValidatableActionResult.GeneralValidationError

      assertThat(validatableActionResult.message).isEqualTo("This application has already been deleted")
    }

    @Test
    fun `submitTemporaryAccommodationApplication returns Success and creates assessment`() {
      val newestSchema = TemporaryAccommodationApplicationJsonSchemaEntityFactory().produce()

      val application = TemporaryAccommodationApplicationEntityFactory()
        .withApplicationSchema(newestSchema)
        .withId(applicationId)
        .withCreatedByUser(user)
        .withSubmittedAt(null)
        .withProbationRegion(user.probationRegion)
        .produce()
        .apply {
          schemaUpToDate = true
        }

      every { mockUserService.getUserForRequest() } returns user
      every { mockApplicationRepository.findByIdOrNull(applicationId) } returns application
      every { mockJsonSchemaService.checkSchemaOutdated(application) } returns application
      every { mockJsonSchemaService.validate(newestSchema, application.data!!) } returns true
      every { mockApplicationRepository.save(any()) } answers { it.invocation.args[0] as ApplicationEntity }
      every { mockProbationDeliveryUnitRepository.findByIdOrNull(any()) } returns null
      every {
        mockAssessmentService.createTemporaryAccommodationAssessment(
          application,
          submitTemporaryAccommodationApplication.summaryData!!,
        )
      } returns TemporaryAccommodationAssessmentEntityFactory()
        .withApplication(application)
        .withSummaryData("{\"num\":50,\"text\":\"Hello world!\"}")
        .produce()

      every { mockCas3DomainEventService.saveReferralSubmittedEvent(any()) } just Runs

      val result = applicationService.submitTemporaryAccommodationApplication(
        applicationId,
        submitTemporaryAccommodationApplication,
      )

      assertThat(result is AuthorisableActionResult.Success).isTrue
      result as AuthorisableActionResult.Success

      assertThat(result.entity is ValidatableActionResult.Success).isTrue
      val validatableActionResult = result.entity as ValidatableActionResult.Success
      val persistedApplication = validatableActionResult.entity as TemporaryAccommodationApplicationEntity
      assertThat(persistedApplication.arrivalDate).isEqualTo(
        OffsetDateTime.of(
          submitTemporaryAccommodationApplication.arrivalDate,
          LocalTime.MIDNIGHT,
          ZoneOffset.UTC,
        ),
      )

      verify { mockApplicationRepository.save(any()) }
      verify(exactly = 1) {
        mockAssessmentService.createTemporaryAccommodationAssessment(
          application,
          submitTemporaryAccommodationApplication.summaryData!!,
        )
      }
      verify { mockDomainEventService wasNot called }

      verify(exactly = 1) {
        mockCas3DomainEventService.saveReferralSubmittedEvent(application)
      }
    }

    @Test
    fun `submitTemporaryAccommodationApplication records MI reporting data when supplied`() {
      val newestSchema = TemporaryAccommodationApplicationJsonSchemaEntityFactory().produce()

      val application = TemporaryAccommodationApplicationEntityFactory()
        .withApplicationSchema(newestSchema)
        .withId(applicationId)
        .withCreatedByUser(user)
        .withSubmittedAt(null)
        .withProbationRegion(user.probationRegion)
        .withName(user.name)
        .produce()
        .apply {
          schemaUpToDate = true
        }

      every { mockUserService.getUserForRequest() } returns user
      every { mockApplicationRepository.findByIdOrNull(applicationId) } returns application
      every { mockJsonSchemaService.checkSchemaOutdated(application) } returns application
      every { mockJsonSchemaService.validate(newestSchema, application.data!!) } returns true
      every { mockApplicationRepository.save(any()) } answers { it.invocation.args[0] as ApplicationEntity }
      every { mockProbationDeliveryUnitRepository.findByIdOrNull(any()) } returns null
      every {
        mockAssessmentService.createTemporaryAccommodationAssessment(
          application,
          submitTemporaryAccommodationApplicationWithMiReportingData.summaryData!!,
        )
      } returns TemporaryAccommodationAssessmentEntityFactory()
        .withApplication(application)
        .withSummaryData("{\"num\":50,\"text\":\"Hello world!\"}")
        .produce()

      every { mockCas3DomainEventService.saveReferralSubmittedEvent(any()) } just Runs

      val result = applicationService.submitTemporaryAccommodationApplication(
        applicationId,
        submitTemporaryAccommodationApplicationWithMiReportingData,
      )

      assertThat(result is AuthorisableActionResult.Success).isTrue
      result as AuthorisableActionResult.Success

      assertThat(result.entity is ValidatableActionResult.Success).isTrue
      val validatableActionResult = result.entity as ValidatableActionResult.Success
      val persistedApplication = validatableActionResult.entity as TemporaryAccommodationApplicationEntity
      assertThat(persistedApplication.arrivalDate).isEqualTo(
        OffsetDateTime.of(
          submitTemporaryAccommodationApplication.arrivalDate,
          LocalTime.MIDNIGHT,
          ZoneOffset.UTC,
        ),
      )
      assertThat(persistedApplication.isRegisteredSexOffender).isEqualTo(true)
      assertThat(persistedApplication.needsAccessibleProperty).isEqualTo(true)
      assertThat(persistedApplication.hasHistoryOfArson).isEqualTo(true)
      assertThat(persistedApplication.isDutyToReferSubmitted).isEqualTo(true)
      assertThat(persistedApplication.dutyToReferSubmissionDate).isEqualTo(LocalDate.now().minusDays(7))
      assertThat(persistedApplication.isEligible).isEqualTo(true)
      assertThat(persistedApplication.eligibilityReason).isEqualTo("homelessFromApprovedPremises")
      assertThat(persistedApplication.dutyToReferLocalAuthorityAreaName).isEqualTo("Aberdeen City")
      assertThat(persistedApplication.personReleaseDate).isEqualTo(submitTemporaryAccommodationApplicationWithMiReportingData.personReleaseDate)
      assertThat(persistedApplication.pdu).isEqualTo("Probation Delivery Unit Test")
      assertThat(persistedApplication.name).isEqualTo(user.name)
      assertThat(persistedApplication.isHistoryOfSexualOffence).isEqualTo(true)
      assertThat(persistedApplication.isConcerningSexualBehaviour).isEqualTo(true)
      assertThat(persistedApplication.isConcerningArsonBehaviour).isEqualTo(true)
      assertThat(persistedApplication.dutyToReferOutcome).isEqualTo("Accepted – Prevention/ Relief Duty")
      assertThat(persistedApplication.prisonReleaseTypes).isEqualTo("Standard recall,ECSL,PSS")

      verify { mockApplicationRepository.save(any()) }
      verify(exactly = 1) {
        mockAssessmentService.createTemporaryAccommodationAssessment(
          application,
          submitTemporaryAccommodationApplicationWithMiReportingData.summaryData!!,
        )
      }
      verify { mockDomainEventService wasNot called }
    }
  }

  @Test
  fun `getOfflineApplicationForUsername where application does not exist returns NotFound result`() {
    val distinguishedName = "SOMEPERSON"
    val applicationId = UUID.fromString("c1750938-19fc-48a1-9ae9-f2e119ffc1f4")

    every { mockOfflineApplicationRepository.findByIdOrNull(applicationId) } returns null

    assertThat(
      applicationService.getOfflineApplicationForUsername(
        applicationId,
        distinguishedName,
      ) is CasResult.NotFound,
    ).isTrue
  }

  @Test
  fun `getOfflineApplicationForUsername where where caller is not one of one of roles WORKFLOW_MANAGER, ASSESSOR, MATCHER, MANAGER returns Unauthorised result`() {
    val distinguishedName = "SOMEPERSON"
    val applicationId = UUID.fromString("c1750938-19fc-48a1-9ae9-f2e119ffc1f4")

    every { mockUserRepository.findByDeliusUsername(distinguishedName) } returns UserEntityFactory()
      .withYieldedProbationRegion {
        ProbationRegionEntityFactory()
          .withYieldedApArea { ApAreaEntityFactory().produce() }
          .produce()
      }
      .produce()
    every { mockOfflineApplicationRepository.findByIdOrNull(applicationId) } returns OfflineApplicationEntityFactory()
      .produce()

    assertThat(
      applicationService.getOfflineApplicationForUsername(
        applicationId,
        distinguishedName,
      ) is CasResult.Unauthorised,
    ).isTrue
  }

  @ParameterizedTest
  @EnumSource(
    value = UserRole::class,
    names = ["CAS1_WORKFLOW_MANAGER", "CAS1_ASSESSOR", "CAS1_MATCHER", "CAS1_FUTURE_MANAGER"],
  )
  fun `getOfflineApplicationForUsername where user has one of roles WORKFLOW_MANAGER, ASSESSOR, MATCHER, FUTURE_MANAGER but does not pass LAO check returns Unauthorised result`(
    role: UserRole,
  ) {
    val distinguishedName = "SOMEPERSON"
    val userId = UUID.fromString("239b5e41-f83e-409e-8fc0-8f1e058d417e")
    val applicationId = UUID.fromString("c1750938-19fc-48a1-9ae9-f2e119ffc1f4")

    val userEntity = UserEntityFactory()
      .withId(userId)
      .withDeliusUsername(distinguishedName)
      .withYieldedProbationRegion {
        ProbationRegionEntityFactory()
          .withYieldedApArea { ApAreaEntityFactory().produce() }
          .produce()
      }
      .produce()
      .apply {
        roles += UserRoleAssignmentEntityFactory()
          .withUser(this)
          .withRole(role)
          .produce()
      }

    val applicationEntity = OfflineApplicationEntityFactory()
      .produce()

    every { mockOfflineApplicationRepository.findByIdOrNull(applicationId) } returns applicationEntity
    every { mockUserRepository.findByDeliusUsername(distinguishedName) } returns userEntity
    every { mockOffenderService.canAccessOffender(distinguishedName, applicationEntity.crn) } returns false

    val result = applicationService.getOfflineApplicationForUsername(applicationId, distinguishedName)

    assertThat(result is CasResult.Unauthorised).isTrue
  }

  @Test
  fun `getOfflineApplicationForUsername where user has any of roles WORKFLOW_MANAGER, ASSESSOR, MATCHER, FUTURE_MANAGER and passes LAO check returns Success result with entity from db`() {
    listOf(
      UserRole.CAS1_WORKFLOW_MANAGER,
      UserRole.CAS1_ASSESSOR,
      UserRole.CAS1_MATCHER,
      UserRole.CAS1_FUTURE_MANAGER,
    ).forEach { role ->
      val distinguishedName = "SOMEPERSON"
      val userId = UUID.fromString("239b5e41-f83e-409e-8fc0-8f1e058d417e")
      val applicationId = UUID.fromString("c1750938-19fc-48a1-9ae9-f2e119ffc1f4")

      val userEntity = UserEntityFactory()
        .withId(userId)
        .withDeliusUsername(distinguishedName)
        .withYieldedProbationRegion {
          ProbationRegionEntityFactory()
            .withYieldedApArea { ApAreaEntityFactory().produce() }
            .produce()
        }
        .produce()
        .apply {
          roles += UserRoleAssignmentEntityFactory()
            .withUser(this)
            .withRole(role)
            .produce()
        }

      val applicationEntity = OfflineApplicationEntityFactory()
        .produce()

      every { mockOfflineApplicationRepository.findByIdOrNull(applicationId) } returns applicationEntity
      every { mockUserRepository.findByDeliusUsername(distinguishedName) } returns userEntity
      every { mockOffenderService.canAccessOffender(distinguishedName, applicationEntity.crn) } returns true

      val result = applicationService.getOfflineApplicationForUsername(applicationId, distinguishedName)

      assertThat(result is CasResult.Success).isTrue
      result as CasResult.Success

      assertThat(result.value).isEqualTo(applicationEntity)
    }
  }

  @Nested
  inner class WithdrawApprovedPremisesApplication {

    @Test
    fun `withdrawApprovedPremisesApplication returns NotFound if Application does not exist`() {
      val user = UserEntityFactory()
        .withUnitTestControlProbationRegion()
        .produce()

      val applicationId = UUID.fromString("bb13d346-f278-43d7-9c23-5c4077c031ca")

      every { mockApplicationRepository.findByIdOrNull(applicationId) } returns null

      val result = applicationService.withdrawApprovedPremisesApplication(
        applicationId,
        user,
        "alternative_identified_placement_no_longer_required",
        null,
      )

      assertThat(result is CasResult.NotFound).isTrue
    }

    @Test
    fun `withdrawApprovedPremisesApplication returns GeneralValidationError if Application is not AP Application`() {
      val user = UserEntityFactory()
        .withUnitTestControlProbationRegion()
        .produce()

      val application = TemporaryAccommodationApplicationEntityFactory()
        .withCreatedByUser(user)
        .withProbationRegion(user.probationRegion)
        .produce()

      every { mockApplicationRepository.findByIdOrNull(application.id) } returns application
      every { mockUserAccessService.userMayWithdrawApplication(user, application) } returns true

      val result = applicationService.withdrawApprovedPremisesApplication(
        application.id,
        user,
        "alternative_identified_placement_no_longer_required",
        null,
      )

      assertThat(result is CasResult.GeneralValidationError).isTrue
      val generalValidationError = (result as CasResult.GeneralValidationError).message

      assertThat(generalValidationError).isEqualTo("onlyCas1Supported")
    }

    @Test
    fun `withdrawApprovedPremisesApplication is idempotent and returns success if already withdrawn`() {
      val user = UserEntityFactory()
        .withUnitTestControlProbationRegion()
        .produce()

      val application = ApprovedPremisesApplicationEntityFactory()
        .withCreatedByUser(user)
        .withIsWithdrawn(true)
        .produce()

      every { mockApplicationRepository.findByIdOrNull(application.id) } returns application
      every { mockUserAccessService.userMayWithdrawApplication(user, application) } returns true

      val result =
        applicationService.withdrawApprovedPremisesApplication(application.id, user, "other", null)

      assertThat(result is CasResult.Success).isTrue
    }

    @Test
    fun `withdrawApprovedPremisesApplication returns Success and saves Application with isWithdrawn set to true, triggers domain event and email`() {
      val user = UserEntityFactory()
        .withUnitTestControlProbationRegion()
        .produce()

      val application = ApprovedPremisesApplicationEntityFactory()
        .withCreatedByUser(user)
        .produce()

      every { mockApplicationRepository.findByIdOrNull(application.id) } returns application
      every { mockUserAccessService.userMayWithdrawApplication(user, application) } returns true
      every { mockApplicationListener.preUpdate(any()) } returns Unit
      every { mockApplicationRepository.save(any()) } answers { it.invocation.args[0] as ApplicationEntity }
      every { mockCas1ApplicationDomainEventService.applicationWithdrawn(any(), any()) } just Runs
      every { mockCas1ApplicationEmailService.applicationWithdrawn(any(), any()) } just Runs

      val result = applicationService.withdrawApprovedPremisesApplication(
        application.id,
        user,
        "alternative_identified_placement_no_longer_required",
        null,
      )

      assertThat(result is CasResult.Success).isTrue

      verify {
        mockApplicationRepository.save(
          match {
            it.id == application.id &&
              it is ApprovedPremisesApplicationEntity &&
              it.isWithdrawn &&
              it.withdrawalReason == "alternative_identified_placement_no_longer_required"
          },
        )
      }

      verify { mockCas1ApplicationDomainEventService.applicationWithdrawn(application, user) }
      verify { mockCas1ApplicationEmailService.applicationWithdrawn(application, user) }
    }

    @Test
    fun `withdrawApprovedPremisesApplication returns Success and saves Application with isWithdrawn set to true, triggers domain event when other reason is set`() {
      val user = UserEntityFactory()
        .withUnitTestControlProbationRegion()
        .produce()

      val application = ApprovedPremisesApplicationEntityFactory()
        .withCreatedByUser(user)
        .produce()

      every { mockApplicationRepository.findByIdOrNull(application.id) } returns application
      every { mockUserAccessService.userMayWithdrawApplication(user, application) } returns true
      every { mockApplicationListener.preUpdate(any()) } returns Unit
      every { mockApplicationRepository.save(any()) } answers { it.invocation.args[0] as ApplicationEntity }
      every { mockCas1ApplicationDomainEventService.applicationWithdrawn(any(), any()) } just Runs
      every { mockCas1ApplicationEmailService.applicationWithdrawn(any(), any()) } just Runs

      val result =
        applicationService.withdrawApprovedPremisesApplication(application.id, user, "other", "Some other reason")

      assertThat(result is CasResult.Success).isTrue

      verify {
        mockApplicationRepository.save(
          match {
            it.id == application.id &&
              it is ApprovedPremisesApplicationEntity &&
              it.isWithdrawn &&
              it.withdrawalReason == "other" &&
              it.otherWithdrawalReason == "Some other reason"
          },
        )
      }

      verify { mockCas1ApplicationDomainEventService.applicationWithdrawn(application, user) }
      verify { mockCas1ApplicationEmailService.applicationWithdrawn(application, user) }
    }

    @Test
    fun `withdrawApprovedPremisesApplication does not persist otherWithdrawalReason if withdrawlReason is not other`() {
      val user = UserEntityFactory()
        .withUnitTestControlProbationRegion()
        .produce()

      val application = ApprovedPremisesApplicationEntityFactory()
        .withCreatedByUser(user)
        .produce()

      every { mockApplicationRepository.findByIdOrNull(application.id) } returns application
      every { mockUserAccessService.userMayWithdrawApplication(user, application) } returns true
      every { mockApplicationListener.preUpdate(any()) } returns Unit
      every { mockApplicationRepository.save(any()) } answers { it.invocation.args[0] as ApplicationEntity }
      every { mockCas1ApplicationEmailService.applicationWithdrawn(any(), any()) } returns Unit
      every { mockCas1ApplicationDomainEventService.applicationWithdrawn(any(), any()) } just Runs

      applicationService.withdrawApprovedPremisesApplication(
        application.id,
        user,
        "alternative_identified_placement_no_longer_required",
        "Some other reason",
      )

      verify {
        mockApplicationRepository.save(
          match {
            it.id == application.id &&
              it is ApprovedPremisesApplicationEntity &&
              it.isWithdrawn &&
              it.withdrawalReason == "alternative_identified_placement_no_longer_required" &&
              it.otherWithdrawalReason == null
          },
        )
      }

      verify { mockCas1ApplicationDomainEventService.applicationWithdrawn(application, user) }
      verify { mockCas1ApplicationEmailService.applicationWithdrawn(application, user) }
    }
  }

  @Nested
  inner class GetWithdrawableState {
    val user = UserEntityFactory()
      .withUnitTestControlProbationRegion()
      .produce()

    @Test
    fun `getWithdrawableState withdrawable if application not withdrawn`() {
      val application = ApprovedPremisesApplicationEntityFactory()
        .withCreatedByUser(user)
        .withIsWithdrawn(false)
        .produce()

      every { mockUserAccessService.userMayWithdrawApplication(user, application) } returns true

      val result = applicationService.getWithdrawableState(application, user)

      assertThat(result.withdrawn).isFalse()
      assertThat(result.withdrawable).isTrue()
    }

    @Test
    fun `getWithdrawableState not withdrawable if application already withdrawn `() {
      val application = ApprovedPremisesApplicationEntityFactory()
        .withCreatedByUser(user)
        .withIsWithdrawn(true)
        .produce()

      every { mockUserAccessService.userMayWithdrawApplication(user, application) } returns true

      val result = applicationService.getWithdrawableState(application, user)

      assertThat(result.withdrawn).isTrue()
      assertThat(result.withdrawable).isFalse()
    }

    @ParameterizedTest
    @CsvSource("true", "false")
    fun `getWithdrawableState userMayDirectlyWithdraw delegates to user access service`(canWithdraw: Boolean) {
      val application = ApprovedPremisesApplicationEntityFactory()
        .withCreatedByUser(user)
        .withIsWithdrawn(false)
        .produce()

      every { mockUserAccessService.userMayWithdrawApplication(user, application) } returns canWithdraw

      val result = applicationService.getWithdrawableState(application, user)

      assertThat(result.userMayDirectlyWithdraw).isEqualTo(canWithdraw)
    }
  }

  private fun userWithUsername(username: String) = UserEntityFactory()
    .withDeliusUsername(username)
    .withProbationRegion(
      ProbationRegionEntityFactory()
        .withApArea(ApAreaEntityFactory().produce())
        .produce(),
    )
    .produce()

  private fun createInmateDetail(
    status: InmateStatus,
    agencyName: String?,
  ) = InmateDetail(
    offenderNo = "NOMS321",
    assignedLivingUnit = agencyName?.let {
      AssignedLivingUnit(
        agencyId = "BRI",
        locationId = 5,
        description = "B-2F-004",
        agencyName = it,
      )
    },
    custodyStatus = status,
  )

  private fun createOffenderDetailsSummary(crn: String) = OffenderDetailSummary(
    offenderId = 547839,
    title = "Mr",
    firstName = "Greggory",
    middleNames = listOf(),
    surname = "Someone",
    previousSurname = null,
    preferredName = null,
    dateOfBirth = LocalDate.parse("1980-09-12"),
    gender = "Male",
    otherIds = OffenderIds(
      crn = crn,
      croNumber = null,
      immigrationNumber = null,
      mostRecentPrisonNumber = null,
      niNumber = null,
      nomsNumber = "NOMS321",
      pncNumber = "PNC456",
    ),
    offenderProfile = OffenderProfile(
      ethnicity = "White and Asian",
      nationality = "Spanish",
      secondaryNationality = null,
      notes = null,
      immigrationStatus = null,
      offenderLanguages = OffenderLanguages(
        primaryLanguage = null,
        otherLanguages = listOf(),
        languageConcerns = null,
        requiresInterpreter = null,
      ),
      religion = "Sikh",
      sexualOrientation = null,
      offenderDetails = null,
      remandStatus = null,
      riskColour = null,
      disabilities = listOf(),
      genderIdentity = null,
      selfDescribedGender = null,
    ),
    softDeleted = null,
    currentDisposal = "",
    partitionArea = null,
    currentRestriction = false,
    currentExclusion = false,
    isActiveProbationManagedSentence = false,
  )
}
