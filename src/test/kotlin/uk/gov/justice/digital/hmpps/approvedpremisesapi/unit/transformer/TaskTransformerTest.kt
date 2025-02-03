package uk.gov.justice.digital.hmpps.approvedpremisesapi.unit.transformer

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import org.junit.jupiter.params.provider.ValueSource
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ApArea
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ApprovedPremisesUser
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.FullPersonSummary
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.PersonSummaryDiscriminator
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.PlacementDates
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ProbationDeliveryUnit
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ReleaseTypeOption
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.RestrictedPersonSummary
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.RiskTierEnvelope
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ServiceName
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.TaskStatus
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.TaskType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.UnknownPersonSummary
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.ApAreaEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.ApprovedPremisesApplicationEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.ApprovedPremisesAssessmentEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.CaseSummaryFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.NameFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.PersonRisksFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.PlacementApplicationEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.ProbationDeliveryUnitEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.ProbationRegionEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.TemporaryAccommodationAssessmentEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.UserEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.AssessmentDecision
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.PlacementApplicationDecision
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.PlacementDateEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.PlacementType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.ApprovedPremisesApplicationStatus
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.PersonSummaryInfoResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.RiskTier
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.RiskWithStatus
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.ApAreaTransformer
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.AssessmentTransformer
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.PersonTransformer
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.PlacementRequestTransformer
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.ProbationDeliveryUnitTransformer
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.RisksTransformer
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.TaskTransformer
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.UserTransformer
import java.time.LocalDate
import java.time.OffsetDateTime
import java.util.UUID
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.PlacementType as ApiPlacementType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.PlacementType as JpaPlacementType

class TaskTransformerTest {
  private val mockUserTransformer = mockk<UserTransformer>()
  private val mockRisksTransformer = mockk<RisksTransformer>()
  private val mockPlacementRequestTransformer = mockk<PlacementRequestTransformer>()
  private val mockApAreaTransformer = mockk<ApAreaTransformer>()
  private val mockAssessmentTransformer = mockk<AssessmentTransformer>()
  private val mockProbationDeliveryUnitTransformer = mockk<ProbationDeliveryUnitTransformer>()

  private val mockUser = mockk<ApprovedPremisesUser>()

  private val probationRegion = ProbationRegionEntityFactory()
    .withYieldedApArea { ApAreaEntityFactory().produce() }
    .produce()

  private val probationDeliveryUnit = ProbationDeliveryUnitEntityFactory()
    .withProbationRegion(probationRegion)
    .withName("probation delivery unit")
    .produce()

  private val user = UserEntityFactory()
    .withYieldedProbationRegion { probationRegion }
    .withProbationDeliveryUnit { probationDeliveryUnit }
    .produce()

  private val applicationFactory = ApprovedPremisesApplicationEntityFactory()
    .withCreatedByUser(user)
    .withRiskRatings(
      PersonRisksFactory()
        .withTier(
          RiskWithStatus(
            RiskTier(
              level = "M1",
              lastUpdated = LocalDate.parse("2023-06-26"),
            ),
          ),
        )
        .produce(),
    )

  private val application = applicationFactory.produce()

  private val assessmentFactory = ApprovedPremisesAssessmentEntityFactory()
    .withApplication(application)
    .withAllocatedToUser(user)
    .withDueAt(OffsetDateTime.now())
    .withCreatedAt(OffsetDateTime.parse("2022-12-07T10:40:00Z"))

  val placementApplicationFactory = PlacementApplicationEntityFactory()
    .withApplication(application)
    .withAllocatedToUser(user)
    .withCreatedByUser(user)
    .withDueAt(OffsetDateTime.now())

  private val mockApArea = ApArea(UUID.randomUUID(), "someIdentifier", "someName")
  private val mockPdu = ProbationDeliveryUnit(UUID.randomUUID(), "thePduName")

  private val taskTransformer = TaskTransformer(
    mockUserTransformer,
    mockRisksTransformer,
    mockPlacementRequestTransformer,
    mockApAreaTransformer,
    mockAssessmentTransformer,
    mockProbationDeliveryUnitTransformer,
    PersonTransformer(),
  )

  @BeforeEach
  fun setup() {
    every { mockUserTransformer.transformJpaToApi(user, ServiceName.approvedPremises) } returns mockUser
    every { mockApAreaTransformer.transformJpaToApi(any()) } returns mockApArea
    every { mockProbationDeliveryUnitTransformer.transformJpaToApi(probationDeliveryUnit) } returns mockPdu
  }

  @Nested
  inner class TransformAssessmentsTest {
    @Test
    fun `Not started assessment is correctly transformed`() {
      val assessment = assessmentFactory
        .withDecision(null)
        .produce()

      assessment.data = null

      val result = taskTransformer.transformAssessmentToTask(
        assessment,
        getOffenderSummariesWithDiscriminator(assessment.application.crn, PersonSummaryDiscriminator.fullPersonSummary),
      )

      assertThat(result.id).isEqualTo(assessment.id)
      assertThat(result.status).isEqualTo(TaskStatus.notStarted)
      assertThat(result.taskType).isEqualTo(TaskType.assessment)
      assertThat(result.applicationId).isEqualTo(application.id)
      assertThat(result.dueDate).isEqualTo(assessment.dueAt!!.toLocalDate())
      assertThat(result.dueAt).isEqualTo(assessment.dueAt!!.toInstant())
      assertThat(result.personName).isEqualTo("First Last")
      assertThat(result.crn).isEqualTo(assessment.application.crn)
      assertThat(result.allocatedToStaffMember).isEqualTo(mockUser)
      assertThat(result.personSummary is FullPersonSummary).isTrue
      assertThat(result.personSummary.crn).isEqualTo(assessment.application.crn)
      assertThat((result.personSummary as FullPersonSummary).name).isEqualTo("First Last")
      assertThat(result.probationDeliveryUnit!!.name).isEqualTo("thePduName")
    }

    @Test
    fun `In Progress assessment is correctly transformed`() {
      val assessment = assessmentFactory
        .withDecision(null)
        .withData("{\"test\": \"data\"}")
        .produce()

      val result = taskTransformer.transformAssessmentToTask(
        assessment,
        getOffenderSummariesWithDiscriminator(assessment.application.crn, PersonSummaryDiscriminator.fullPersonSummary),
      )

      assertThat(result.status).isEqualTo(TaskStatus.inProgress)
    }

    @Test
    fun `Complete assessment is correctly transformed`() {
      val submittedAt = OffsetDateTime.now()
      val assessment = assessmentFactory
        .withDecision(AssessmentDecision.ACCEPTED)
        .withSubmittedAt(submittedAt)
        .withData("{\"test\": \"data\"}")
        .produce()

      val apiDecision = uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.AssessmentDecision.accepted

      every { mockAssessmentTransformer.transformJpaDecisionToApi(assessment.decision) } returns apiDecision

      val result = taskTransformer.transformAssessmentToTask(
        assessment,
        getOffenderSummariesWithDiscriminator(assessment.application.crn, PersonSummaryDiscriminator.fullPersonSummary),
      )

      assertThat(result.status).isEqualTo(TaskStatus.complete)
      assertThat(result.outcome).isEqualTo(apiDecision)
      assertThat(result.outcomeRecordedAt).isEqualTo(submittedAt.toInstant())
    }

    @Test
    fun `Assessment awaiting information request is correctly transformed`() {
      val application = applicationFactory
        .withStatus(ApprovedPremisesApplicationStatus.REQUESTED_FURTHER_INFORMATION)
        .produce()
      val assessment = assessmentFactory
        .withDecision(null)
        .withData("{\"test\": \"data\"}")
        .withApplication(application)
        .produce()

      val result = taskTransformer.transformAssessmentToTask(
        assessment,
        getOffenderSummariesWithDiscriminator(assessment.application.crn, PersonSummaryDiscriminator.fullPersonSummary),
      )

      assertThat(result.status).isEqualTo(TaskStatus.infoRequested)
    }

    @Test
    fun `assessment with ApArea is correctly transformed`() {
      val apArea = ApAreaEntityFactory().produce()
      val application = applicationFactory.withApArea(apArea).produce()
      val assessment = assessmentFactory
        .withApplication(application)
        .withDecision(null)
        .produce()

      val result = taskTransformer.transformAssessmentToTask(
        assessment,
        getOffenderSummariesWithDiscriminator(assessment.application.crn, PersonSummaryDiscriminator.fullPersonSummary),
      )

      assertThat(result.apArea).isEqualTo(mockApArea)

      verify {
        mockApAreaTransformer.transformJpaToApi(apArea)
      }
    }

    @ParameterizedTest
    @ValueSource(booleans = [true, false])
    fun `assessment with createdFromAppeal is transformed correctly`(createdFromAppeal: Boolean) {
      val assessment = assessmentFactory.withDecision(null).withCreatedFromAppeal(createdFromAppeal).produce()

      val result = taskTransformer.transformAssessmentToTask(
        assessment,
        getOffenderSummariesWithDiscriminator(assessment.application.crn, PersonSummaryDiscriminator.fullPersonSummary),
      )

      assertThat(result.createdFromAppeal).isEqualTo(createdFromAppeal)
    }

    @Test
    fun `Temporary Accommodation assessment returns false for createdFromAppeal`() {
      val assessment = TemporaryAccommodationAssessmentEntityFactory()
        .withApplication(application)
        .withAllocatedToUser(user)
        .withDecision(null)
        .withCreatedAt(OffsetDateTime.parse("2022-12-07T10:40:00Z"))
        .withDueAt(OffsetDateTime.now())
        .produce()

      val result = taskTransformer.transformAssessmentToTask(
        assessment,
        getOffenderSummariesWithDiscriminator(assessment.application.crn, PersonSummaryDiscriminator.fullPersonSummary),
      )

      assertThat(result.createdFromAppeal).isEqualTo(false)
    }
  }

  @Nested
  inner class TransformPlacementApplicationToTaskTest {
    private val placementApplication = placementApplicationFactory
      .withPlacementType(PlacementType.ADDITIONAL_PLACEMENT)
      .withData(null)
      .produce()
    val application = placementApplication.application
    private val mockTier = mockk<RiskTierEnvelope>()
    private val releaseType = ReleaseTypeOption.licence

    @BeforeEach
    fun setup() {
      every { mockRisksTransformer.transformTierDomainToApi(application.riskRatings!!.tier) } returns mockTier
      every { mockPlacementRequestTransformer.getReleaseType(application.releaseType) } returns releaseType
    }

    @Test
    fun `Placement application is correctly transformed`() {
      placementApplication.placementDates = mutableListOf(
        PlacementDateEntity(
          id = UUID.randomUUID(),
          createdAt = OffsetDateTime.now(),
          expectedArrival = LocalDate.now(),
          duration = 12,
          placementApplication = placementApplication,
        ),
      )
      val result = taskTransformer.transformPlacementApplicationToTask(
        placementApplication,
        getOffenderSummariesWithDiscriminator(placementApplication.application.crn, PersonSummaryDiscriminator.fullPersonSummary),
      )

      assertThat(result.status).isEqualTo(TaskStatus.notStarted)
      assertThat(result.id).isEqualTo(placementApplication.id)
      assertThat(result.tier).isEqualTo(mockTier)
      assertThat(result.releaseType).isEqualTo(releaseType)
      assertThat(result.personName).isEqualTo("First Last")
      assertThat(result.crn).isEqualTo(placementApplication.application.crn)
      assertThat(result.placementDates).isEqualTo(
        mutableListOf(
          PlacementDates(
            placementApplication.placementDates[0].expectedArrival,
            placementApplication.placementDates[0].duration,
          ),
        ),
      )
      assertThat(result.dueDate).isEqualTo(placementApplication.dueAt!!.toLocalDate())
      assertThat(result.dueAt).isEqualTo(placementApplication.dueAt!!.toInstant())
      assertThat(result.probationDeliveryUnit!!.name).isEqualTo("thePduName")
    }

    @ParameterizedTest
    @EnumSource(value = JpaPlacementType::class)
    fun `Placement types are transformed correctly`(placementType: JpaPlacementType) {
      val placementApplication = placementApplicationFactory
        .withPlacementType(placementType)
        .produce()

      val result = taskTransformer.transformPlacementApplicationToTask(
        placementApplication,
        getOffenderSummariesWithDiscriminator(placementApplication.application.crn, PersonSummaryDiscriminator.fullPersonSummary),
      )

      if (placementType === JpaPlacementType.ROTL) {
        assertThat(result.placementType).isEqualTo(ApiPlacementType.rotl)
      } else if (placementType === JpaPlacementType.ADDITIONAL_PLACEMENT) {
        assertThat(result.placementType).isEqualTo(ApiPlacementType.additionalPlacement)
      } else if (placementType === JpaPlacementType.RELEASE_FOLLOWING_DECISION) {
        assertThat(result.placementType).isEqualTo(ApiPlacementType.releaseFollowingDecision)
      }
    }

    @Test
    fun `In-progress placement application is correctly transformed`() {
      val placementApplication = placementApplicationFactory
        .withData("{}")
        .withPlacementType(PlacementType.ADDITIONAL_PLACEMENT)
        .produce()

      val result = taskTransformer.transformPlacementApplicationToTask(
        placementApplication,
        getOffenderSummariesWithDiscriminator(placementApplication.application.crn, PersonSummaryDiscriminator.fullPersonSummary),
      )

      assertThat(result.status).isEqualTo(TaskStatus.inProgress)
    }

    @Test
    fun `Completed placement application is correctly transformed`() {
      val decisionMadeAt = OffsetDateTime.now()
      val decision = PlacementApplicationDecision.ACCEPTED

      val placementApplication = placementApplicationFactory
        .withData("{}")
        .withPlacementType(PlacementType.ADDITIONAL_PLACEMENT)
        .withDecision(decision)
        .withDecisionMadeAt(decisionMadeAt)
        .produce()

      val result = taskTransformer.transformPlacementApplicationToTask(
        placementApplication,
        getOffenderSummariesWithDiscriminator(placementApplication.application.crn, PersonSummaryDiscriminator.fullPersonSummary),
      )

      assertThat(result.status).isEqualTo(TaskStatus.complete)
      assertThat(result.outcome).isEqualTo(decision.apiValue)
      assertThat(result.outcomeRecordedAt).isEqualTo(decisionMadeAt.toInstant())
    }

    @Test
    fun `placement application with ApArea is correctly transformed`() {
      val apArea = ApAreaEntityFactory().produce()
      val application = applicationFactory.withApArea(apArea).produce()
      val placementApplication = placementApplicationFactory
        .withApplication(application)
        .produce()

      val result = taskTransformer.transformPlacementApplicationToTask(
        placementApplication,
        getOffenderSummariesWithDiscriminator(placementApplication.application.crn, PersonSummaryDiscriminator.fullPersonSummary),
      )

      assertThat(result.apArea).isEqualTo(mockApArea)

      verify {
        mockApAreaTransformer.transformJpaToApi(apArea)
      }
    }
  }

  @Test
  fun `assessment is transformed correctly with restricted offender summary discriminator`() {
    val assessment = assessmentFactory.withDecision(null).produce()

    val result = taskTransformer.transformAssessmentToTask(
      assessment,
      getOffenderSummariesWithDiscriminator(assessment.application.crn, PersonSummaryDiscriminator.restrictedPersonSummary),
    )

    assertThat(result.personSummary is RestrictedPersonSummary).isTrue()
    assertThat(result.personSummary.crn).isEqualTo(assessment.application.crn)
  }

  @Test
  fun `assessment is transformed correctly with unknown offender summary discriminator`() {
    val assessment = assessmentFactory.withDecision(null).produce()

    val result = taskTransformer.transformAssessmentToTask(
      assessment,
      getOffenderSummariesWithDiscriminator(assessment.application.crn, PersonSummaryDiscriminator.unknownPersonSummary),
    )

    assertThat(result.personSummary is UnknownPersonSummary).isTrue()
    assertThat(result.personSummary.crn).isEqualTo(assessment.application.crn)
  }

  fun getOffenderSummariesWithDiscriminator(crn: String, discriminator: PersonSummaryDiscriminator): List<PersonSummaryInfoResult> {
    PersonSummaryDiscriminator.fullPersonSummary
    when (discriminator) {
      PersonSummaryDiscriminator.fullPersonSummary ->
        return listOf(
          PersonSummaryInfoResult.Success.Full(
            crn,
            CaseSummaryFactory().withName(NameFactory().withForename("First").withSurname("Last").produce())
              .produce(),
          ),
        )
      PersonSummaryDiscriminator.restrictedPersonSummary -> {
        return listOf(
          PersonSummaryInfoResult.Success.Restricted(crn, "nomsNumber"),
        )
      }
      PersonSummaryDiscriminator.unknownPersonSummary -> {
        return listOf(
          PersonSummaryInfoResult.Unknown(crn),
        )
      }
    }
  }
}
