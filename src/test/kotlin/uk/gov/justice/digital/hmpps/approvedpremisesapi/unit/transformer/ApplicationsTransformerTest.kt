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
import org.junit.jupiter.api.assertThrows
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ApplicationStatus
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ApprovedPremisesApplication
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ApprovedPremisesApplicationSummary
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Person
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.TemporaryAccommodationApplication
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.TemporaryAccommodationApplicationSummary
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.ApAreaEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.ApprovedPremisesApplicationEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.ApprovedPremisesAssessmentEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.ApprovedPremisesEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.AssessmentClarificationNoteEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.BookingEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.PersonRisksFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.PlacementRequestEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.PlacementRequirementsEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.ProbationRegionEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.TemporaryAccommodationApplicationEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.UserEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.AssessmentDecision
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.DomainEventType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.PersonRisks
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.ApplicationsTransformer
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.PersonTransformer
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.RisksTransformer
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.randomStringMultiCaseWithNumbers
import java.sql.Timestamp
import java.time.Instant
import java.time.LocalDate
import java.time.OffsetDateTime
import java.util.UUID
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.AssessmentDecision as ApiAssessmentDecision
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApprovedPremisesApplicationSummary as DomainApprovedPremisesApplicationSummary
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.TemporaryAccommodationApplicationSummary as DomainTemporaryAccommodationApplicationSummary

class ApplicationsTransformerTest {
  private val mockPersonTransformer = mockk<PersonTransformer>()
  private val mockRisksTransformer = mockk<RisksTransformer>()

  private val objectMapper = ObjectMapper().apply {
    registerModule(Jdk8Module())
    registerModule(JavaTimeModule())
    registerKotlinModule()
  }

  private val applicationsTransformer = ApplicationsTransformer(
    objectMapper,
    mockPersonTransformer,
    mockRisksTransformer,
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

  private val unSubmittedApprovedPremisesApplicationFactory = approvedPremisesApplicationFactory
    .withSubmittedAt(null)

  private val submittedApprovedPremisesApplicationFactory = approvedPremisesApplicationFactory
    .withSubmittedAt(OffsetDateTime.now())

  private val submittedTemporaryAccommodationApplicationFactory = temporaryAccommodationApplicationEntityFactory
    .withArrivalDate(OffsetDateTime.now().toLocalDate().plusDays(7))
    .withSubmittedAt(OffsetDateTime.now())

  @BeforeEach
  fun setup() {
    every { mockPersonTransformer.transformModelToPersonApi(any()) } returns mockk<Person>()
    every { mockRisksTransformer.transformDomainToApi(any<PersonRisks>(), any<String>()) } returns mockk()
  }

  @Test
  fun `transformJpaToApi transforms an in progress Approved Premises application correctly`() {
    val application = approvedPremisesApplicationFactory.withSubmittedAt(null).produce()

    val result = applicationsTransformer.transformJpaToApi(application, mockk()) as ApprovedPremisesApplication

    assertThat(result.id).isEqualTo(application.id)
    assertThat(result.createdByUserId).isEqualTo(user.id)
    assertThat(result.status).isEqualTo(ApplicationStatus.inProgress)
    assertThat(result.assessmentDecision).isNull()
  }

  @Test
  fun `transformJpaToApi transforms an inapplicable Approved Premises application correctly`() {
    val application = approvedPremisesApplicationFactory.withIsInapplicable(true).produce()

    val result = applicationsTransformer.transformJpaToApi(application, mockk()) as ApprovedPremisesApplication

    assertThat(result.id).isEqualTo(application.id)
    assertThat(result.createdByUserId).isEqualTo(user.id)
    assertThat(result.status).isEqualTo(ApplicationStatus.inapplicable)
    assertThat(result.assessmentDecision).isNull()
  }

  @Test
  fun `transformJpaToApi transforms a withdrawn Approved Premises application correctly`() {
    val application = approvedPremisesApplicationFactory.withIsWithdrawn(true).produce()

    val result = applicationsTransformer.transformJpaToApi(application, mockk()) as ApprovedPremisesApplication

    assertThat(result.id).isEqualTo(application.id)
    assertThat(result.createdByUserId).isEqualTo(user.id)
    assertThat(result.status).isEqualTo(ApplicationStatus.withdrawn)
    assertThat(result.assessmentDecision).isNull()
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
  }

  @Test
  fun `transformJpaToApi transforms a submitted Approved Premises application correctly`() {
    val application = submittedApprovedPremisesApplicationFactory.produce()

    val result = applicationsTransformer.transformJpaToApi(application, mockk()) as ApprovedPremisesApplication

    assertThat(result.status).isEqualTo(ApplicationStatus.submitted)
    assertThat(result.assessmentDecision).isNull()
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
  fun `transformJpaToApi sets status as 'requested further information' when transforming an Approved Premises application with requested clarification notes`() {
    val application = submittedApprovedPremisesApplicationFactory.produce()
    val assessment = assessmentFactory
      .withDecision(null)
      .withApplication(application)
      .produce()

    application.assessments = mutableListOf(assessment)
    assessment.clarificationNotes = mutableListOf(
      completedClarificationNoteFactory
        .withAssessment(assessment)
        .produce(),
      awaitingClarificationNoteFactory
        .withAssessment(assessment)
        .produce(),
    )

    val result = applicationsTransformer.transformJpaToApi(application, mockk()) as ApprovedPremisesApplication

    assertThat(result.status).isEqualTo(ApplicationStatus.requestedFurtherInformation)
    assertThat(result.assessmentDecision).isNull()
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
  fun `transformJpaToApi sets status as 'submitted' when transforming an Approved Premises application with a completed clarification note`() {
    val application = submittedApprovedPremisesApplicationFactory.produce()
    val assessment = assessmentFactory
      .withDecision(null)
      .withApplication(application).produce()

    assessment.clarificationNotes = mutableListOf(
      completedClarificationNoteFactory
        .withAssessment(assessment)
        .produce(),
    )

    application.assessments = mutableListOf(assessment)

    val result = applicationsTransformer.transformJpaToApi(application, mockk()) as ApprovedPremisesApplication

    assertThat(result.status).isEqualTo(ApplicationStatus.submitted)
    assertThat(result.assessmentDecision).isNull()
    assertThat(result.assessmentId).isEqualTo(assessment.id)
    assertThat(result.assessmentDecisionDate).isNull()
  }

  @Test
  fun `transformJpaToApi sets status as 'rejected' when transforming an Approved Premises application with a rejected Assessment`() {
    val application = submittedApprovedPremisesApplicationFactory.produce()
    val assessment = assessmentFactory
      .withSubmittedAt(OffsetDateTime.now())
      .withDecision(AssessmentDecision.REJECTED)
      .withApplication(application)
      .withSubmittedAt(OffsetDateTime.now())
      .produce()

    application.assessments = mutableListOf(assessment)

    val result = applicationsTransformer.transformJpaToApi(application, mockk()) as ApprovedPremisesApplication

    assertThat(result.status).isEqualTo(ApplicationStatus.rejected)
    assertThat(result.assessmentDecision).isEqualTo(ApiAssessmentDecision.rejected)
    assertThat(result.assessmentId).isEqualTo(assessment.id)
    assertThat(result.assessmentDecisionDate).isEqualTo(LocalDate.now())
  }

  @Test
  fun `transformJpaToApi sets status as 'pending' when transforming an Approved Premises application with an approved Assessment but no Placement Request`() {
    val application = submittedApprovedPremisesApplicationFactory.produce()
    val assessment = assessmentFactory
      .withSubmittedAt(OffsetDateTime.now())
      .withDecision(AssessmentDecision.ACCEPTED)
      .withApplication(application)
      .withSubmittedAt(OffsetDateTime.now())
      .produce()

    application.assessments = mutableListOf(assessment)

    val result = applicationsTransformer.transformJpaToApi(application, mockk()) as ApprovedPremisesApplication

    assertThat(result.status).isEqualTo(ApplicationStatus.pending)
    assertThat(result.assessmentDecision).isEqualTo(ApiAssessmentDecision.accepted)
    assertThat(result.assessmentId).isEqualTo(assessment.id)
    assertThat(result.assessmentDecisionDate).isEqualTo(LocalDate.now())
  }

  @Test
  fun `transformJpaToApi sets status as 'awaiting placement' when transforming an Approved Premises application with an approved Assessment with a Placement Request that has no Booking`() {
    val application = submittedApprovedPremisesApplicationFactory.produce()
    val assessment = assessmentFactory
      .withSubmittedAt(OffsetDateTime.now())
      .withDecision(AssessmentDecision.ACCEPTED)
      .withApplication(application)
      .withSubmittedAt(OffsetDateTime.now())
      .produce()

    application.assessments = mutableListOf(assessment)

    val placementRequest = PlacementRequestEntityFactory()
      .withPlacementRequirements(
        PlacementRequirementsEntityFactory()
          .withApplication(application)
          .withAssessment(assessment)
          .produce(),
      )
      .withApplication(application)
      .withAssessment(assessment)
      .withAllocatedToUser(
        UserEntityFactory()
          .withUnitTestControlProbationRegion()
          .produce(),
      )
      .produce()

    application.placementRequests += placementRequest

    val result = applicationsTransformer.transformJpaToApi(application, mockk()) as ApprovedPremisesApplication

    assertThat(result.status).isEqualTo(ApplicationStatus.awaitingPlacement)
    assertThat(result.assessmentDecision).isEqualTo(ApiAssessmentDecision.accepted)
    assertThat(result.assessmentId).isEqualTo(assessment.id)
    assertThat(result.assessmentDecisionDate).isEqualTo(LocalDate.now())
  }

  @Test
  fun `transformJpaToApi sets status as 'accepted' when transforming an Approved Premises application with an approved Assessment with a Placement Request that has a Booking`() {
    val application = submittedApprovedPremisesApplicationFactory.produce()
    val assessment = assessmentFactory
      .withSubmittedAt(OffsetDateTime.now())
      .withDecision(AssessmentDecision.ACCEPTED)
      .withApplication(application)
      .withSubmittedAt(OffsetDateTime.now())
      .produce()

    application.assessments = mutableListOf(assessment)

    val booking = BookingEntityFactory()
      .withPremises(
        ApprovedPremisesEntityFactory()
          .withUnitTestControlTestProbationAreaAndLocalAuthority()
          .produce(),
      )
      .produce()

    val placementRequest = PlacementRequestEntityFactory()
      .withPlacementRequirements(
        PlacementRequirementsEntityFactory()
          .withApplication(application)
          .withAssessment(assessment)
          .produce(),
      )
      .withApplication(application)
      .withAssessment(assessment)
      .withBooking(booking)
      .withAllocatedToUser(
        UserEntityFactory()
          .withUnitTestControlProbationRegion()
          .produce(),
      )
      .produce()

    application.placementRequests += placementRequest

    val result = applicationsTransformer.transformJpaToApi(application, mockk()) as ApprovedPremisesApplication

    assertThat(result.status).isEqualTo(ApplicationStatus.placed)
    assertThat(result.assessmentDecision).isEqualTo(ApiAssessmentDecision.accepted)
    assertThat(result.assessmentId).isEqualTo(assessment.id)
    assertThat(result.assessmentDecisionDate).isEqualTo(LocalDate.now())
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

  @Test
  fun `transformJpaToApiSummary transforms an in progress Approved Premises application correctly`() {
    val application = object : DomainApprovedPremisesApplicationSummary {
      override fun getHasPlacementRequest() = true
      override fun getIsWomensApplication() = false
      override fun getIsEmergencyApplication() = true
      override fun getIsEsapApplication() = true
      override fun getIsPipeApplication() = true
      override fun getArrivalDate() = Timestamp(Instant.parse("2023-04-19T14:25:00+01:00").toEpochMilli())
      override fun getRiskRatings() = objectMapper.writeValueAsString(PersonRisksFactory().produce())
      override fun getId() = UUID.fromString("2f838a8c-dffc-48a3-9536-f0e95985e809")
      override fun getCrn() = randomStringMultiCaseWithNumbers(6)
      override fun getCreatedByUserId() = UUID.fromString("836a9460-b177-433a-a0d9-262509092c9f")
      override fun getCreatedAt() = Timestamp(Instant.parse("2023-04-19T13:25:00+01:00").toEpochMilli())
      override fun getSubmittedAt() = null
      override fun getLatestAssessmentSubmittedAt() = null
      override fun getLatestAssessmentDecision() = null
      override fun getLatestAssessmentHasClarificationNotesWithoutResponse() = false
      override fun getHasBooking() = false
      override fun getTier(): String? = null
    }

    val result = applicationsTransformer.transformDomainToApiSummary(application, mockk()) as ApprovedPremisesApplicationSummary

    assertThat(result.id).isEqualTo(application.getId())
    assertThat(result.createdByUserId).isEqualTo(application.getCreatedByUserId())
    assertThat(result.status).isEqualTo(ApplicationStatus.inProgress)
  }

  @Test
  fun `transformJpaToApiSummary transforms a submitted Approved Premises application correctly`() {
    val application = object : DomainApprovedPremisesApplicationSummary {
      override fun getHasPlacementRequest() = true
      override fun getIsWomensApplication() = false
      override fun getIsPipeApplication() = true
      override fun getIsEsapApplication() = true
      override fun getIsEmergencyApplication() = true
      override fun getArrivalDate() = Timestamp(Instant.parse("2023-04-19T14:25:00+01:00").toEpochMilli())
      override fun getRiskRatings() = objectMapper.writeValueAsString(PersonRisksFactory().produce())
      override fun getId() = UUID.fromString("2f838a8c-dffc-48a3-9536-f0e95985e809")
      override fun getCrn() = randomStringMultiCaseWithNumbers(6)
      override fun getCreatedByUserId() = UUID.fromString("836a9460-b177-433a-a0d9-262509092c9f")
      override fun getCreatedAt() = Timestamp(Instant.parse("2023-04-19T13:25:00+01:00").toEpochMilli())
      override fun getSubmittedAt() = Timestamp(Instant.parse("2023-04-19T13:25:30+01:00").toEpochMilli())
      override fun getLatestAssessmentSubmittedAt() = null
      override fun getLatestAssessmentDecision() = null
      override fun getLatestAssessmentHasClarificationNotesWithoutResponse() = false
      override fun getHasBooking() = false
      override fun getTier(): String? = null
    }

    val result = applicationsTransformer.transformDomainToApiSummary(application, mockk()) as ApprovedPremisesApplicationSummary

    assertThat(result.id).isEqualTo(application.getId())
    assertThat(result.createdByUserId).isEqualTo(application.getCreatedByUserId())
    assertThat(result.status).isEqualTo(ApplicationStatus.submitted)
  }

  @Test
  fun `transformJpaToApiSummary transforms a requested information Approved Premises application correctly`() {
    val application = object : DomainApprovedPremisesApplicationSummary {
      override fun getHasPlacementRequest() = true
      override fun getIsWomensApplication() = false
      override fun getIsPipeApplication() = true
      override fun getIsEsapApplication() = true
      override fun getIsEmergencyApplication() = true
      override fun getArrivalDate() = Timestamp(Instant.parse("2023-04-19T14:25:00+01:00").toEpochMilli())
      override fun getRiskRatings() = objectMapper.writeValueAsString(PersonRisksFactory().produce())
      override fun getId() = UUID.fromString("2f838a8c-dffc-48a3-9536-f0e95985e809")
      override fun getCrn() = randomStringMultiCaseWithNumbers(6)
      override fun getCreatedByUserId() = UUID.fromString("836a9460-b177-433a-a0d9-262509092c9f")
      override fun getCreatedAt() = Timestamp(Instant.parse("2023-04-19T13:25:00+01:00").toEpochMilli())
      override fun getSubmittedAt() = Timestamp(Instant.parse("2023-04-19T13:25:30+01:00").toEpochMilli())
      override fun getLatestAssessmentSubmittedAt() = null
      override fun getLatestAssessmentDecision() = null
      override fun getLatestAssessmentHasClarificationNotesWithoutResponse() = true
      override fun getHasBooking() = false
      override fun getTier(): String? = null
    }

    val result = applicationsTransformer.transformDomainToApiSummary(application, mockk()) as ApprovedPremisesApplicationSummary

    assertThat(result.id).isEqualTo(application.getId())
    assertThat(result.createdByUserId).isEqualTo(application.getCreatedByUserId())
    assertThat(result.status).isEqualTo(ApplicationStatus.requestedFurtherInformation)
  }

  @Test
  fun `transformJpaToApiSummary transforms a placed Approved Premises application correctly`() {
    val application = object : DomainApprovedPremisesApplicationSummary {
      override fun getHasPlacementRequest() = true
      override fun getIsWomensApplication() = false
      override fun getIsPipeApplication() = true
      override fun getIsEsapApplication() = true
      override fun getIsEmergencyApplication() = true
      override fun getArrivalDate() = Timestamp(Instant.parse("2023-04-19T14:25:00+01:00").toEpochMilli())
      override fun getRiskRatings() = objectMapper.writeValueAsString(PersonRisksFactory().produce())
      override fun getId() = UUID.fromString("2f838a8c-dffc-48a3-9536-f0e95985e809")
      override fun getCrn() = randomStringMultiCaseWithNumbers(6)
      override fun getCreatedByUserId() = UUID.fromString("836a9460-b177-433a-a0d9-262509092c9f")
      override fun getCreatedAt() = Timestamp(Instant.parse("2023-04-19T13:25:00+01:00").toEpochMilli())
      override fun getSubmittedAt() = Timestamp(Instant.parse("2023-04-19T13:25:30+01:00").toEpochMilli())
      override fun getLatestAssessmentSubmittedAt() = null
      override fun getLatestAssessmentDecision() = AssessmentDecision.ACCEPTED
      override fun getLatestAssessmentHasClarificationNotesWithoutResponse() = false
      override fun getHasBooking() = true
      override fun getTier(): String? = null
    }

    val result = applicationsTransformer.transformDomainToApiSummary(application, mockk()) as ApprovedPremisesApplicationSummary

    assertThat(result.id).isEqualTo(application.getId())
    assertThat(result.createdByUserId).isEqualTo(application.getCreatedByUserId())
    assertThat(result.status).isEqualTo(ApplicationStatus.placed)
  }

  @Test
  fun `transformJpaToApiSummary transforms an awaitingPlacement Approved Premises application correctly`() {
    val application = object : DomainApprovedPremisesApplicationSummary {
      override fun getHasPlacementRequest() = true
      override fun getIsWomensApplication() = false
      override fun getIsPipeApplication() = true
      override fun getIsEsapApplication() = true
      override fun getIsEmergencyApplication() = true
      override fun getArrivalDate() = Timestamp(Instant.parse("2023-04-19T14:25:00+01:00").toEpochMilli())
      override fun getRiskRatings() = objectMapper.writeValueAsString(PersonRisksFactory().produce())
      override fun getId() = UUID.fromString("2f838a8c-dffc-48a3-9536-f0e95985e809")
      override fun getCrn() = randomStringMultiCaseWithNumbers(6)
      override fun getCreatedByUserId() = UUID.fromString("836a9460-b177-433a-a0d9-262509092c9f")
      override fun getCreatedAt() = Timestamp(Instant.parse("2023-04-19T13:25:00+01:00").toEpochMilli())
      override fun getSubmittedAt() = Timestamp(Instant.parse("2023-04-19T13:25:30+01:00").toEpochMilli())
      override fun getLatestAssessmentSubmittedAt() = null
      override fun getLatestAssessmentDecision() = AssessmentDecision.ACCEPTED
      override fun getLatestAssessmentHasClarificationNotesWithoutResponse() = false
      override fun getHasBooking() = false
      override fun getTier(): String? = null
    }

    val result = applicationsTransformer.transformDomainToApiSummary(application, mockk()) as ApprovedPremisesApplicationSummary

    assertThat(result.id).isEqualTo(application.getId())
    assertThat(result.createdByUserId).isEqualTo(application.getCreatedByUserId())
    assertThat(result.status).isEqualTo(ApplicationStatus.awaitingPlacement)
  }

  @Test
  fun `transformJpaToApiSummary transforms a pending Approved Premises application correctly`() {
    val application = object : DomainApprovedPremisesApplicationSummary {
      override fun getHasPlacementRequest() = false
      override fun getIsWomensApplication() = false
      override fun getIsPipeApplication() = true
      override fun getIsEsapApplication() = true
      override fun getIsEmergencyApplication() = true
      override fun getArrivalDate() = Timestamp(Instant.parse("2023-04-19T14:25:00+01:00").toEpochMilli())
      override fun getRiskRatings() = objectMapper.writeValueAsString(PersonRisksFactory().produce())
      override fun getId() = UUID.fromString("2f838a8c-dffc-48a3-9536-f0e95985e809")
      override fun getCrn() = randomStringMultiCaseWithNumbers(6)
      override fun getCreatedByUserId() = UUID.fromString("836a9460-b177-433a-a0d9-262509092c9f")
      override fun getCreatedAt() = Timestamp(Instant.parse("2023-04-19T13:25:00+01:00").toEpochMilli())
      override fun getSubmittedAt() = Timestamp(Instant.parse("2023-04-19T13:25:30+01:00").toEpochMilli())
      override fun getLatestAssessmentSubmittedAt() = null
      override fun getLatestAssessmentDecision() = AssessmentDecision.ACCEPTED
      override fun getLatestAssessmentHasClarificationNotesWithoutResponse() = false
      override fun getHasBooking() = false
      override fun getTier(): String? = null
    }

    val result = applicationsTransformer.transformDomainToApiSummary(application, mockk()) as ApprovedPremisesApplicationSummary

    assertThat(result.id).isEqualTo(application.getId())
    assertThat(result.createdByUserId).isEqualTo(application.getCreatedByUserId())
    assertThat(result.status).isEqualTo(ApplicationStatus.pending)
  }

  @Test
  fun `transformJpaToApiSummary transforms a rejected Approved Premises application correctly`() {
    val application = object : DomainApprovedPremisesApplicationSummary {
      override fun getHasPlacementRequest() = false
      override fun getIsWomensApplication() = false
      override fun getIsPipeApplication() = true
      override fun getIsEsapApplication() = true
      override fun getIsEmergencyApplication() = true
      override fun getArrivalDate() = Timestamp(Instant.parse("2023-04-19T14:25:00+01:00").toEpochMilli())
      override fun getRiskRatings() = objectMapper.writeValueAsString(PersonRisksFactory().produce())
      override fun getId() = UUID.fromString("2f838a8c-dffc-48a3-9536-f0e95985e809")
      override fun getCrn() = randomStringMultiCaseWithNumbers(6)
      override fun getCreatedByUserId() = UUID.fromString("836a9460-b177-433a-a0d9-262509092c9f")
      override fun getCreatedAt() = Timestamp(Instant.parse("2023-04-19T13:25:00+01:00").toEpochMilli())
      override fun getSubmittedAt() = Timestamp(Instant.parse("2023-04-19T13:25:30+01:00").toEpochMilli())
      override fun getLatestAssessmentSubmittedAt() = null
      override fun getLatestAssessmentDecision() = AssessmentDecision.REJECTED
      override fun getLatestAssessmentHasClarificationNotesWithoutResponse() = false
      override fun getHasBooking() = false
      override fun getTier(): String? = null
    }

    val result = applicationsTransformer.transformDomainToApiSummary(application, mockk()) as ApprovedPremisesApplicationSummary

    assertThat(result.id).isEqualTo(application.getId())
    assertThat(result.createdByUserId).isEqualTo(application.getCreatedByUserId())
    assertThat(result.status).isEqualTo(ApplicationStatus.rejected)
  }

  @Test
  fun `transformJpaToApiSummary transforms an in progress Temporary Accommodation application correctly`() {
    val application = object : DomainTemporaryAccommodationApplicationSummary {
      override fun getRiskRatings() = objectMapper.writeValueAsString(PersonRisksFactory().produce())
      override fun getId() = UUID.fromString("2f838a8c-dffc-48a3-9536-f0e95985e809")
      override fun getCrn() = randomStringMultiCaseWithNumbers(6)
      override fun getCreatedByUserId() = UUID.fromString("836a9460-b177-433a-a0d9-262509092c9f")
      override fun getCreatedAt() = Timestamp(Instant.parse("2023-04-19T13:25:00+01:00").toEpochMilli())
      override fun getSubmittedAt() = null
      override fun getLatestAssessmentSubmittedAt() = null
      override fun getLatestAssessmentDecision() = null
      override fun getLatestAssessmentHasClarificationNotesWithoutResponse() = false
      override fun getHasBooking() = false
    }

    val result = applicationsTransformer.transformDomainToApiSummary(application, mockk()) as TemporaryAccommodationApplicationSummary

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
      override fun getCreatedAt() = Timestamp(Instant.parse("2023-04-19T13:25:00+01:00").toEpochMilli())
      override fun getSubmittedAt() = Timestamp(Instant.parse("2023-04-19T13:25:30+01:00").toEpochMilli())
      override fun getLatestAssessmentSubmittedAt() = null
      override fun getLatestAssessmentDecision() = null
      override fun getLatestAssessmentHasClarificationNotesWithoutResponse() = false
      override fun getHasBooking() = false
    }

    val result = applicationsTransformer.transformDomainToApiSummary(application, mockk()) as TemporaryAccommodationApplicationSummary

    assertThat(result.id).isEqualTo(application.getId())
    assertThat(result.createdByUserId).isEqualTo(application.getCreatedByUserId())
    assertThat(result.status).isEqualTo(ApplicationStatus.submitted)
    assertThat(result.risks).isNotNull
  }

  @Test
  fun `transformDomainEventTypeToTimelineEventType throws error if given CAS2 domain event type`() {
    val cas2DomainEventType = DomainEventType.CAS2_APPLICATION_SUBMITTED

    val exception = assertThrows<RuntimeException> { applicationsTransformer.transformDomainEventTypeToTimelineEventType(cas2DomainEventType) }
    assertThat(exception.message).isEqualTo("Only CAS1 is currently supported")
  }
}
