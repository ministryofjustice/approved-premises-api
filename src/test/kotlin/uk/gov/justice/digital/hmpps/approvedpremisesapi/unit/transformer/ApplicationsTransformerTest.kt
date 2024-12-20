package uk.gov.justice.digital.hmpps.approvedpremisesapi.unit.transformer

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import org.junit.jupiter.params.provider.EnumSource
import org.junit.jupiter.params.provider.MethodSource
import org.junit.jupiter.params.provider.NullSource
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ApArea
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ApType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ApplicationStatus
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ApprovedPremisesApplication
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ApprovedPremisesApplicationSummary
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas1ApplicationUserDetails
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas1CruManagementArea
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Person
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.PersonStatus
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ReleaseTypeOption
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.TemporaryAccommodationApplication
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.TemporaryAccommodationApplicationSummary
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.ApAreaEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.ApprovedPremisesApplicationEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.ApprovedPremisesAssessmentEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.AssessmentClarificationNoteEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.Cas1ApplicationUserDetailsEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.PersonRisksFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.ProbationRegionEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.TemporaryAccommodationApplicationEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.TemporaryAccommodationAssessmentEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.UserEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.cas1.Cas1CruManagementAreaEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.ApprovedPremisesApplicationStatus
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.ApprovedPremisesType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.PersonInfoResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.PersonRisks
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.ApAreaTransformer
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.ApplicationsTransformer
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.PersonTransformer
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.RisksTransformer
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.cas1.Cas1ApplicationUserDetailsTransformer
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.cas1.Cas1CruManagementAreaTransformer
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.randomStringMultiCaseWithNumbers
import java.time.Instant
import java.time.LocalDate
import java.time.OffsetDateTime
import java.util.UUID
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ApprovedPremisesApplicationStatus as ApiApprovedPremisesApplicationStatus
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApprovedPremisesApplicationSummary as DomainApprovedPremisesApplicationSummary
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.TemporaryAccommodationApplicationSummary as DomainTemporaryAccommodationApplicationSummary

class ApplicationsTransformerTest {
  private val mockPersonTransformer = mockk<PersonTransformer>()
  private val mockRisksTransformer = mockk<RisksTransformer>()
  private val mockApAreaTransformer = mockk<ApAreaTransformer>()
  private val mockCas1ApplicationUserDetailsTransformer = mockk<Cas1ApplicationUserDetailsTransformer>()
  private val mockCas1CruManagementAreaTransformer = mockk<Cas1CruManagementAreaTransformer>()

  private val objectMapper = ObjectMapper().apply {
    registerModule(Jdk8Module())
    registerModule(JavaTimeModule())
    registerKotlinModule()
  }

  private val applicationsTransformer = ApplicationsTransformer(
    objectMapper,
    mockPersonTransformer,
    mockRisksTransformer,
    mockApAreaTransformer,
    mockCas1ApplicationUserDetailsTransformer,
    mockCas1CruManagementAreaTransformer,
  )

  private val user = UserEntityFactory()
    .withYieldedProbationRegion {
      ProbationRegionEntityFactory()
        .withYieldedApArea { ApAreaEntityFactory().produce() }
        .produce()
    }
    .produce()

  private val allocatedToUser = UserEntityFactory()
    .withYieldedProbationRegion {
      ProbationRegionEntityFactory()
        .withYieldedApArea { ApAreaEntityFactory().produce() }
        .produce()
    }
    .produce()

  private val approvedPremisesApplicationFactory = ApprovedPremisesApplicationEntityFactory()
    .withCreatedByUser(user)

  private val temporaryAccommodationApplicationEntityFactory = TemporaryAccommodationApplicationEntityFactory()
    .withCreatedByUser(user)

  private val assessmentFactory = ApprovedPremisesAssessmentEntityFactory()
    .withAllocatedToUser(allocatedToUser)

  private val completedClarificationNoteFactory = AssessmentClarificationNoteEntityFactory()
    .withResponse("Response")
    .withCreatedBy(allocatedToUser)

  private val awaitingClarificationNoteFactory = AssessmentClarificationNoteEntityFactory()
    .withCreatedBy(allocatedToUser)

  private val submittedTemporaryAccommodationApplicationFactory = temporaryAccommodationApplicationEntityFactory
    .withArrivalDate(OffsetDateTime.now().toLocalDate().plusDays(7))
    .withSubmittedAt(OffsetDateTime.now())

  @BeforeEach
  fun setup() {
    every { mockPersonTransformer.transformModelToPersonApi(any()) } returns mockk<Person>()
    every { mockPersonTransformer.inmateStatusToPersonInfoApiStatus(any()) } returns PersonStatus.inCommunity
    every { mockRisksTransformer.transformDomainToApi(any<PersonRisks>(), any<String>()) } returns mockk()
  }

  @ParameterizedTest
  @MethodSource("applicationStatusArgs")
  fun `transformJpaToApi transforms an Approved Premises application correctly`(args: Pair<ApiApprovedPremisesApplicationStatus, ApprovedPremisesApplicationStatus>) {
    val (apiStatus, jpaStatus) = args

    val applicantUserDetails = Cas1ApplicationUserDetailsEntityFactory().produce()
    val caseManagerUserDetails = Cas1ApplicationUserDetailsEntityFactory().produce()

    val application = approvedPremisesApplicationFactory
      .withStatus(jpaStatus)
      .withApArea(null)
      .withApplicantUserDetails(applicantUserDetails)
      .withCaseManagerIsNotApplicant(true)
      .withCaseManagerUserDetails(caseManagerUserDetails)
      .withLicenseExpiredDate(LocalDate.of(2026, 5, 5))
      .produce()

    every { mockCas1ApplicationUserDetailsTransformer.transformJpaToApi(applicantUserDetails) } returns Cas1ApplicationUserDetails("applicant", "", "")
    every { mockCas1ApplicationUserDetailsTransformer.transformJpaToApi(caseManagerUserDetails) } returns Cas1ApplicationUserDetails("caseManager", "", "")

    val result = applicationsTransformer.transformJpaToApi(application, mockk()) as ApprovedPremisesApplication

    assertThat(result.id).isEqualTo(application.id)
    assertThat(result.createdByUserId).isEqualTo(user.id)
    assertThat(result.status).isEqualTo(apiStatus)
    assertThat(result.apArea).isNull()
    assertThat(result.applicantUserDetails!!.name).isEqualTo("applicant")
    assertThat(result.caseManagerIsNotApplicant).isTrue()
    assertThat(result.caseManagerUserDetails!!.name).isEqualTo("caseManager")
    assertThat(result.licenceExpiryDate).isEqualTo(LocalDate.of(2026, 5, 5))
  }

  @Test
  fun `transformJpaToApi returns the Ap and Cru Management Area`() {
    val apAreaEntity = ApAreaEntityFactory().produce()
    val cruManagementAreaEntity = Cas1CruManagementAreaEntityFactory().produce()
    val application = approvedPremisesApplicationFactory
      .withApArea(apAreaEntity)
      .withCruManagementArea(cruManagementAreaEntity)
      .produce()

    val apArea = mockk<ApArea>()
    val cCruManagementArea = mockk<Cas1CruManagementArea>()

    every { mockApAreaTransformer.transformJpaToApi(apAreaEntity) } returns apArea
    every {
      mockCas1CruManagementAreaTransformer.transformJpaToApi(
        cruManagementAreaEntity,
      )
    } returns cCruManagementArea
    every { mockCas1ApplicationUserDetailsTransformer.transformJpaToApi(any()) } returns Cas1ApplicationUserDetails("", "", "")

    val result = applicationsTransformer.transformJpaToApi(application, mockk()) as ApprovedPremisesApplication

    assertThat(result.apArea).isEqualTo(apArea)
    assertThat(result.cruManagementArea).isEqualTo(cCruManagementArea)
  }

  @ParameterizedTest
  @CsvSource(
    "NORMAL,normal",
    "PIPE,pipe",
    "ESAP,esap",
    "RFAP,rfap",
    "MHAP_ST_JOSEPHS,mhapStJosephs",
    "MHAP_ELLIOTT_HOUSE,mhapElliottHouse",
  )
  fun `transformJpaToApi transforms ap type correctly`(jpaTypeString: String, apiTypeString: String) {
    val jpaType = ApprovedPremisesType.valueOf(jpaTypeString)
    val expectedApiType = ApType.valueOf(apiTypeString)

    val application = approvedPremisesApplicationFactory.withApType(jpaType).produce()

    every { mockCas1ApplicationUserDetailsTransformer.transformJpaToApi(any()) } returns Cas1ApplicationUserDetails("", "", "")

    val result = applicationsTransformer.transformJpaToApi(application, mockk()) as ApprovedPremisesApplication

    assertThat(result.apType).isEqualTo(expectedApiType)
  }

  @Test
  fun `transformJpaToApi transforms an in progress Temporary Accommodation application correctly`() {
    val application = temporaryAccommodationApplicationEntityFactory
      .withSubmittedAt(null)
      .withArrivalDate(null)
      .withYieldedProbationRegion {
        ProbationRegionEntityFactory()
          .withApArea(
            ApAreaEntityFactory()
              .produce(),
          )
          .produce()
      }
      .produce()

    val result = applicationsTransformer.transformJpaToApi(application, mockk()) as TemporaryAccommodationApplication

    assertThat(result.id).isEqualTo(application.id)
    assertThat(result.createdByUserId).isEqualTo(user.id)
    assertThat(result.status).isEqualTo(ApplicationStatus.inProgress)
    assertThat(result.risks).isNotNull
    assertThat(result.arrivalDate).isNull()
    assertThat(result.offenceId).isEqualTo(application.offenceId)
    assertThat(result.assessmentId).isNull()
  }

  @Test
  fun `transformJpaToApi populates assessmentId`() {
    val application = temporaryAccommodationApplicationEntityFactory.withDefaults().produce()
    val assessment = TemporaryAccommodationAssessmentEntityFactory().withApplication(application).produce()
    application.assessments = mutableListOf(assessment)
    val result = applicationsTransformer.transformJpaToApi(application, mockk()) as TemporaryAccommodationApplication

    assertThat(result.assessmentId).isEqualTo(assessment.id)
  }

  @Test
  fun `transformJpaToApi transforms a submitted Temporary Accommodation application correctly`() {
    val application = submittedTemporaryAccommodationApplicationFactory
      .withYieldedProbationRegion {
        ProbationRegionEntityFactory()
          .withApArea(
            ApAreaEntityFactory()
              .produce(),
          )
          .produce()
      }
      .produce()

    val result = applicationsTransformer.transformJpaToApi(application, mockk()) as TemporaryAccommodationApplication

    assertThat(result.status).isEqualTo(ApplicationStatus.submitted)
    assertThat(result.arrivalDate).isEqualTo(application.arrivalDate!!.toInstant())
    assertThat(result.offenceId).isEqualTo(application.offenceId)
  }

  @Test
  fun `transformJpaToApi sets status as 'requested further information' when transforming a Temporary Accommodation application with requested clarification notes`() {
    val application = submittedTemporaryAccommodationApplicationFactory
      .withYieldedProbationRegion {
        ProbationRegionEntityFactory()
          .withApArea(
            ApAreaEntityFactory()
              .produce(),
          )
          .produce()
      }
      .produce()
    val assessment = assessmentFactory.withApplication(application).produce()

    application.assessments = mutableListOf(assessment)
    assessment.clarificationNotes = mutableListOf(
      completedClarificationNoteFactory
        .withAssessment(assessment)
        .produce(),
      awaitingClarificationNoteFactory
        .withAssessment(assessment)
        .produce(),
    )

    val result = applicationsTransformer.transformJpaToApi(application, mockk()) as TemporaryAccommodationApplication

    assertThat(result.status).isEqualTo(ApplicationStatus.requestedFurtherInformation)
  }

  @Test
  fun `transformJpaToApi sets status as 'submitted' when transforming a Temporary Accommodation application with a completed clarification note`() {
    val application = submittedTemporaryAccommodationApplicationFactory
      .withYieldedProbationRegion {
        ProbationRegionEntityFactory()
          .withApArea(
            ApAreaEntityFactory()
              .produce(),
          )
          .produce()
      }
      .produce()
    val assessment = assessmentFactory.withApplication(application).produce()

    assessment.clarificationNotes = mutableListOf(
      completedClarificationNoteFactory
        .withAssessment(assessment)
        .produce(),
    )

    application.assessments = mutableListOf(assessment)

    val result = applicationsTransformer.transformJpaToApi(application, mockk()) as TemporaryAccommodationApplication

    assertThat(result.status).isEqualTo(ApplicationStatus.submitted)
  }

  @Test
  fun `transformJpaToApi uses the latest assessment`() {
    val application = submittedTemporaryAccommodationApplicationFactory
      .withYieldedProbationRegion {
        ProbationRegionEntityFactory()
          .withApArea(
            ApAreaEntityFactory()
              .produce(),
          )
          .produce()
      }
      .produce()
    val oldAssessment = assessmentFactory.withApplication(application)
      .withCreatedAt(OffsetDateTime.parse("2022-09-01T12:34:56.789Z"))
      .produce()
    val latestAssessment = assessmentFactory.withApplication(application)
      .withCreatedAt(OffsetDateTime.now())
      .produce()

    oldAssessment.clarificationNotes = mutableListOf(
      awaitingClarificationNoteFactory
        .withAssessment(oldAssessment)
        .produce(),
    )

    latestAssessment.clarificationNotes = mutableListOf(
      completedClarificationNoteFactory
        .withAssessment(latestAssessment)
        .produce(),
    )

    application.assessments = mutableListOf(oldAssessment, latestAssessment)

    val result = applicationsTransformer.transformJpaToApi(application, mockk()) as TemporaryAccommodationApplication

    assertThat(result.status).isEqualTo(ApplicationStatus.submitted)
  }

  @ParameterizedTest
  @MethodSource("applicationStatusArgs")
  fun `transformJpaToApiSummary transforms an Approved Premises application correctly`(args: Pair<ApiApprovedPremisesApplicationStatus, ApprovedPremisesApplicationStatus>) {
    val (apiStatus, jpaStatus) = args
    val mockPersonInfoResult = mockk<PersonInfoResult>()
    val mockPerson = mockk<Person>()

    val application = object : DomainApprovedPremisesApplicationSummary {
      override fun getIsWomensApplication() = false
      override fun getIsEmergencyApplication() = true
      override fun getIsEsapApplication() = true
      override fun getIsPipeApplication() = true
      override fun getArrivalDate() = Instant.parse("2023-04-19T14:25:00+01:00")
      override fun getRiskRatings() = objectMapper.writeValueAsString(PersonRisksFactory().produce())
      override fun getId() = UUID.fromString("2f838a8c-dffc-48a3-9536-f0e95985e809")
      override fun getCrn() = randomStringMultiCaseWithNumbers(6)
      override fun getCreatedByUserId() = UUID.fromString("836a9460-b177-433a-a0d9-262509092c9f")
      override fun getCreatedAt() = Instant.parse("2023-04-19T13:25:00+01:00")
      override fun getSubmittedAt() = Instant.parse("2023-04-19T13:25:00+01:00")
      override fun getTier(): String? = null
      override fun getStatus(): String = jpaStatus.toString()
      override fun getIsWithdrawn(): Boolean = true
      override fun getReleaseType(): String = ReleaseTypeOption.licence.toString()
      override fun getHasRequestsForPlacement(): Boolean = true
    }
    every { mockPersonTransformer.transformModelToPersonApi(mockPersonInfoResult) } returns mockPerson

    val result = applicationsTransformer.transformDomainToApiSummary(
      application,
      mockPersonInfoResult,
    ) as ApprovedPremisesApplicationSummary

    assertThat(result.id).isEqualTo(application.getId())
    assertThat(result.person).isEqualTo(mockPerson)
    assertThat(result.createdByUserId).isEqualTo(application.getCreatedByUserId())
    assertThat(result.createdAt).isEqualTo(application.getCreatedAt())
    assertThat(result.submittedAt).isEqualTo(application.getSubmittedAt())
    assertThat(result.isWomensApplication).isEqualTo(application.getIsWomensApplication())
    assertThat(result.isPipeApplication).isEqualTo(application.getIsPipeApplication())
    assertThat(result.arrivalDate).isEqualTo(application.getArrivalDate())
    assertThat(result.status).isEqualTo(apiStatus)
    assertThat(result.type).isEqualTo("CAS1")
    assertThat(result.tier).isEqualTo(application.getTier())
    assertThat(result.isWithdrawn).isEqualTo(true)
    assertThat(result.hasRequestsForPlacement).isEqualTo(true)
  }

  @ParameterizedTest
  @EnumSource(ReleaseTypeOption::class)
  @NullSource
  fun `transformJpaToApiSummary transforms an Approved Premises application's release type correctly`(releaseTypeOption: ReleaseTypeOption?) {
    val mockPersonInfoResult = mockk<PersonInfoResult>()
    val mockPerson = mockk<Person>()

    val application = object : DomainApprovedPremisesApplicationSummary {
      override fun getIsWomensApplication() = false
      override fun getIsEmergencyApplication() = true
      override fun getIsEsapApplication() = true
      override fun getIsPipeApplication() = true
      override fun getArrivalDate() = Instant.parse("2023-04-19T14:25:00+01:00")
      override fun getRiskRatings() = objectMapper.writeValueAsString(PersonRisksFactory().produce())
      override fun getId() = UUID.fromString("2f838a8c-dffc-48a3-9536-f0e95985e809")
      override fun getCrn() = randomStringMultiCaseWithNumbers(6)
      override fun getCreatedByUserId() = UUID.fromString("836a9460-b177-433a-a0d9-262509092c9f")
      override fun getCreatedAt() = Instant.parse("2023-04-19T13:25:00+01:00")
      override fun getSubmittedAt() = Instant.parse("2023-04-19T13:25:00+01:00")
      override fun getTier(): String? = null
      override fun getStatus(): String = ApprovedPremisesApplicationStatus.SUBMITTED.toString()
      override fun getIsWithdrawn(): Boolean = true
      override fun getReleaseType(): String? = releaseTypeOption?.let { releaseTypeOption.toString() }
      override fun getHasRequestsForPlacement(): Boolean = true
    }

    every { mockPersonTransformer.transformModelToPersonApi(mockPersonInfoResult) } returns mockPerson

    val result = applicationsTransformer.transformDomainToApiSummary(
      application,
      mockPersonInfoResult,
    ) as ApprovedPremisesApplicationSummary

    assertThat(result.releaseType).isEqualTo(releaseTypeOption)
  }

  @Test
  fun `transformJpaToApiSummary transforms an in progress Temporary Accommodation application correctly`() {
    val application = object : DomainTemporaryAccommodationApplicationSummary {
      override fun getRiskRatings() = objectMapper.writeValueAsString(PersonRisksFactory().produce())
      override fun getId() = UUID.fromString("2f838a8c-dffc-48a3-9536-f0e95985e809")
      override fun getCrn() = randomStringMultiCaseWithNumbers(6)
      override fun getCreatedByUserId() = UUID.fromString("836a9460-b177-433a-a0d9-262509092c9f")
      override fun getCreatedAt() = Instant.parse("2023-04-19T13:25:00+01:00")
      override fun getSubmittedAt() = null
      override fun getLatestAssessmentSubmittedAt() = null
      override fun getLatestAssessmentDecision() = null
      override fun getLatestAssessmentHasClarificationNotesWithoutResponse() = false
      override fun getHasBooking() = false
    }

    val result = applicationsTransformer.transformDomainToApiSummary(
      application,
      mockk(),
    ) as TemporaryAccommodationApplicationSummary

    assertThat(result.id).isEqualTo(application.getId())
    assertThat(result.createdByUserId).isEqualTo(application.getCreatedByUserId())
    assertThat(result.status).isEqualTo(ApplicationStatus.inProgress)
    assertThat(result.risks).isNotNull
  }

  @Test
  fun `transformJpaToApiSummary transforms a submitted Temporary Accommodation application correctly`() {
    val application = object : DomainTemporaryAccommodationApplicationSummary {
      override fun getRiskRatings() = objectMapper.writeValueAsString(PersonRisksFactory().produce())
      override fun getId() = UUID.fromString("2f838a8c-dffc-48a3-9536-f0e95985e809")
      override fun getCrn() = randomStringMultiCaseWithNumbers(6)
      override fun getCreatedByUserId() = UUID.fromString("836a9460-b177-433a-a0d9-262509092c9f")
      override fun getCreatedAt() = Instant.parse("2023-04-19T13:25:00+01:00")
      override fun getSubmittedAt() = Instant.parse("2023-04-19T13:25:30+01:00")
      override fun getLatestAssessmentSubmittedAt() = null
      override fun getLatestAssessmentDecision() = null
      override fun getLatestAssessmentHasClarificationNotesWithoutResponse() = false
      override fun getHasBooking() = false
    }

    val result = applicationsTransformer.transformDomainToApiSummary(
      application,
      mockk(),
    ) as TemporaryAccommodationApplicationSummary

    assertThat(result.id).isEqualTo(application.getId())
    assertThat(result.createdByUserId).isEqualTo(application.getCreatedByUserId())
    assertThat(result.status).isEqualTo(ApplicationStatus.submitted)
    assertThat(result.risks).isNotNull
  }

  private companion object {
    @JvmStatic
    fun applicationStatusArgs() = listOf(
      ApiApprovedPremisesApplicationStatus.assesmentInProgress to ApprovedPremisesApplicationStatus.ASSESSMENT_IN_PROGRESS,
      ApiApprovedPremisesApplicationStatus.started to ApprovedPremisesApplicationStatus.STARTED,
      ApiApprovedPremisesApplicationStatus.submitted to ApprovedPremisesApplicationStatus.SUBMITTED,
      ApiApprovedPremisesApplicationStatus.rejected to ApprovedPremisesApplicationStatus.REJECTED,
      ApiApprovedPremisesApplicationStatus.awaitingAssesment to ApprovedPremisesApplicationStatus.AWAITING_ASSESSMENT,
      ApiApprovedPremisesApplicationStatus.unallocatedAssesment to ApprovedPremisesApplicationStatus.UNALLOCATED_ASSESSMENT,
      ApiApprovedPremisesApplicationStatus.awaitingPlacement to ApprovedPremisesApplicationStatus.AWAITING_PLACEMENT,
      ApiApprovedPremisesApplicationStatus.placementAllocated to ApprovedPremisesApplicationStatus.PLACEMENT_ALLOCATED,
      ApiApprovedPremisesApplicationStatus.inapplicable to ApprovedPremisesApplicationStatus.INAPPLICABLE,
      ApiApprovedPremisesApplicationStatus.withdrawn to ApprovedPremisesApplicationStatus.WITHDRAWN,
      ApiApprovedPremisesApplicationStatus.requestedFurtherInformation to ApprovedPremisesApplicationStatus.REQUESTED_FURTHER_INFORMATION,
      ApiApprovedPremisesApplicationStatus.pendingPlacementRequest to ApprovedPremisesApplicationStatus.PENDING_PLACEMENT_REQUEST,
    )
  }
}
