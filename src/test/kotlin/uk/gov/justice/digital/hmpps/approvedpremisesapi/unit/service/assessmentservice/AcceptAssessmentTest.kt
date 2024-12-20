package uk.gov.justice.digital.hmpps.approvedpremisesapi.unit.service.assessmentservice

import com.fasterxml.jackson.databind.ObjectMapper
import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.verify
import org.assertj.core.api.Assertions
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.data.repository.findByIdOrNull
import org.springframework.http.HttpStatus
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ApType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Gender
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.PlacementDates
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.PlacementRequirements
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.ApDeliusContextApiClient
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.ClientResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.ApAreaEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.ApprovedPremisesApplicationEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.ApprovedPremisesAssessmentEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.ApprovedPremisesAssessmentJsonSchemaEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.OffenderDetailsSummaryFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.PlacementRequestEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.PlacementRequirementsEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.ProbationRegionEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.StaffDetailFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.TemporaryAccommodationApplicationEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.TemporaryAccommodationAssessmentEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.TemporaryAccommodationAssessmentJsonSchemaEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.UserEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApprovedPremisesApplicationEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApprovedPremisesAssessmentJsonSchemaEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.AssessmentClarificationNoteRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.AssessmentDecision
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.AssessmentEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.AssessmentReferralHistoryNoteRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.AssessmentRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.LockableAssessmentRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ReferralHistorySystemNoteType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ReferralRejectionReasonRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.TemporaryAccommodationAssessmentEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.TemporaryAccommodationAssessmentJsonSchemaEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.listeners.AssessmentClarificationNoteListener
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.listeners.AssessmentListener
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.deliuscontext.ProbationArea
import uk.gov.justice.digital.hmpps.approvedpremisesapi.results.AuthorisableActionResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.results.CasResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.AssessmentService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.EmailNotificationService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.JsonSchemaService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.OffenderService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.PlacementRequestService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.PlacementRequestSource
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.PlacementRequirementsService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.TaskDeadlineService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.UserAccessService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.UserService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1.Cas1AssessmentDomainEventService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1.Cas1AssessmentEmailService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1.Cas1DomainEventService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1.Cas1PlacementRequestEmailService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1.allocations.UserAllocator
import uk.gov.justice.digital.hmpps.approvedpremisesapi.unit.util.assertAssessmentHasSystemNote
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.UrlTemplate
import java.time.Clock
import java.time.LocalDate
import java.time.OffsetDateTime
import java.util.UUID

class AcceptAssessmentTest {
  private val userServiceMock = mockk<UserService>()
  private val userAccessServiceMock = mockk<UserAccessService>()
  private val assessmentRepositoryMock = mockk<AssessmentRepository>()
  private val assessmentClarificationNoteRepositoryMock = mockk<AssessmentClarificationNoteRepository>()
  private val assessmentReferralHistoryNoteRepositoryMock = mockk<AssessmentReferralHistoryNoteRepository>()
  private val referralRejectionReasonRepositoryMock = mockk<ReferralRejectionReasonRepository>()
  private val jsonSchemaServiceMock = mockk<JsonSchemaService>()
  private val domainEventServiceMock = mockk<Cas1DomainEventService>()
  private val offenderServiceMock = mockk<OffenderService>()
  private val apDeliusContextApiClient = mockk<ApDeliusContextApiClient>()
  private val placementRequestServiceMock = mockk<PlacementRequestService>()
  private val emailNotificationServiceMock = mockk<EmailNotificationService>()
  private val placementRequirementsServiceMock = mockk<PlacementRequirementsService>()
  private val userAllocator = mockk<UserAllocator>()
  private val objectMapperMock = mockk<ObjectMapper>()
  private val taskDeadlineServiceMock = mockk<TaskDeadlineService>()
  private val cas1AssessmentEmailServiceMock = mockk<Cas1AssessmentEmailService>()
  private val cas1PlacementRequestEmailServiceMock = mockk<Cas1PlacementRequestEmailService>()
  private val cas1AssessmentDomainEventServiceMock = mockk<Cas1AssessmentDomainEventService>()
  private val assessmentListener = mockk<AssessmentListener>()
  private val assessmentClarificationNoteListener = mockk<AssessmentClarificationNoteListener>()
  private val lockableAssessmentRepository = mockk<LockableAssessmentRepository>()

  private val assessmentService = AssessmentService(
    userServiceMock,
    userAccessServiceMock,
    assessmentRepositoryMock,
    assessmentClarificationNoteRepositoryMock,
    assessmentReferralHistoryNoteRepositoryMock,
    referralRejectionReasonRepositoryMock,
    jsonSchemaServiceMock,
    domainEventServiceMock,
    offenderServiceMock,
    apDeliusContextApiClient,
    placementRequestServiceMock,
    placementRequirementsServiceMock,
    userAllocator,
    objectMapperMock,
    UrlTemplate("http://frontend/applications/#id"),
    taskDeadlineServiceMock,
    cas1AssessmentEmailServiceMock,
    cas1AssessmentDomainEventServiceMock,
    cas1PlacementRequestEmailServiceMock,
    assessmentListener,
    assessmentClarificationNoteListener,
    Clock.systemDefaultZone(),
    lockableAssessmentRepository,
  )

  lateinit var user: UserEntity
  lateinit var assessmentId: UUID

  private lateinit var assessmentFactory: ApprovedPremisesAssessmentEntityFactory
  private lateinit var assessmentSchema: ApprovedPremisesAssessmentJsonSchemaEntity
  private lateinit var placementRequirements: PlacementRequirements

  @BeforeEach
  fun setup() {
    user = UserEntityFactory().withYieldedProbationRegion {
      ProbationRegionEntityFactory().withYieldedApArea { ApAreaEntityFactory().produce() }.produce()
    }.produce()

    assessmentId = UUID.randomUUID()

    assessmentSchema = ApprovedPremisesAssessmentJsonSchemaEntity(
      id = UUID.randomUUID(),
      addedAt = OffsetDateTime.now(),
      schema = "{}",
    )

    assessmentFactory = ApprovedPremisesAssessmentEntityFactory()
      .withId(assessmentId)
      .withApplication(
        ApprovedPremisesApplicationEntityFactory()
          .withCreatedByUser(
            UserEntityFactory()
              .withYieldedProbationRegion {
                ProbationRegionEntityFactory()
                  .withYieldedApArea { ApAreaEntityFactory().produce() }
                  .produce()
              }
              .produce(),
          )
          .produce(),
      )
      .withAllocatedToUser(user)
      .withAssessmentSchema(assessmentSchema)
      .withData("{\"test\": \"data\"}")

    placementRequirements = PlacementRequirements(
      gender = Gender.male,
      type = ApType.normal,
      location = "AB123",
      radius = 50,
      desirableCriteria = listOf(),
      essentialCriteria = listOf(),
    )
  }

  @Test
  fun `acceptAssessment returns unauthorised when the user does not have permissions to access the assessment`() {
    every { assessmentRepositoryMock.findByIdOrNull(assessmentId) } returns assessmentFactory
      .withAllocatedToUser(
        UserEntityFactory()
          .withYieldedProbationRegion {
            ProbationRegionEntityFactory()
              .withYieldedApArea { ApAreaEntityFactory().produce() }
              .produce()
          }
          .produce(),
      )
      .produce()

    every { userAccessServiceMock.userCanViewAssessment(any(), any()) } returns false

    every { jsonSchemaServiceMock.getNewestSchema(ApprovedPremisesAssessmentJsonSchemaEntity::class.java) } returns ApprovedPremisesAssessmentJsonSchemaEntityFactory().produce()

    val result = assessmentService.acceptAssessment(user, assessmentId, "{}", placementRequirements, null, null, null)

    assertThat(result is CasResult.Unauthorised).isTrue
  }

  @Test
  fun `acceptAssessment returns general validation error for Assessment where schema is outdated`() {
    val assessment = assessmentFactory.produce()

    every { userAccessServiceMock.userCanViewAssessment(any(), any()) } returns true

    every { assessmentRepositoryMock.findByIdOrNull(assessmentId) } returns assessment

    every { jsonSchemaServiceMock.getNewestSchema(ApprovedPremisesAssessmentJsonSchemaEntity::class.java) } returns ApprovedPremisesAssessmentJsonSchemaEntityFactory().produce()

    every { offenderServiceMock.getOffenderByCrn(assessment.application.crn, user.deliusUsername) } returns AuthorisableActionResult.Success(
      OffenderDetailsSummaryFactory().produce(),
    )

    val result = assessmentService.acceptAssessment(user, assessmentId, "{}", placementRequirements, null, null, null)

    assertThat(result is CasResult.GeneralValidationError).isTrue
    result as CasResult.GeneralValidationError
    assertThat(result.message).isEqualTo("The schema version is outdated")
  }

  @Test
  fun `acceptAssessment returns general validation error for Assessment where decision has already been taken`() {
    val assessment = assessmentFactory
      .withDecision(AssessmentDecision.ACCEPTED)
      .withSubmittedAt(OffsetDateTime.now())
      .produce()

    every { userAccessServiceMock.userCanViewAssessment(any(), any()) } returns true

    every { assessmentRepositoryMock.findByIdOrNull(assessmentId) } returns assessment

    every { jsonSchemaServiceMock.getNewestSchema(ApprovedPremisesAssessmentJsonSchemaEntity::class.java) } returns assessmentSchema

    every { offenderServiceMock.getOffenderByCrn(assessment.application.crn, user.deliusUsername) } returns AuthorisableActionResult.Success(
      OffenderDetailsSummaryFactory().produce(),
    )

    val result = assessmentService.acceptAssessment(user, assessmentId, "{}", placementRequirements, null, null, null)

    assertThat(result is CasResult.GeneralValidationError).isTrue
    result as CasResult.GeneralValidationError
    assertThat(result.message).isEqualTo("A decision has already been taken on this assessment")
  }

  @Test
  fun `acceptAssessment returns general validation error for Assessment where assessment has been deallocated`() {
    val assessment = assessmentFactory
      .withReallocatedAt(OffsetDateTime.now())
      .produce()

    every { userAccessServiceMock.userCanViewAssessment(any(), any()) } returns true

    every { assessmentRepositoryMock.findByIdOrNull(assessmentId) } returns assessment

    every { jsonSchemaServiceMock.getNewestSchema(ApprovedPremisesAssessmentJsonSchemaEntity::class.java) } returns assessmentSchema

    every { offenderServiceMock.getOffenderByCrn(assessment.application.crn, user.deliusUsername) } returns AuthorisableActionResult.Success(
      OffenderDetailsSummaryFactory().produce(),
    )

    val result = assessmentService.acceptAssessment(user, assessmentId, "{}", placementRequirements, null, null, null)

    assertThat(result is CasResult.GeneralValidationError).isTrue
    result as CasResult.GeneralValidationError
    assertThat(result.message).isEqualTo("The application has been reallocated, this assessment is read only")
  }

  @Test
  fun `acceptAssessment CAS1 returns field validation error when JSON schema not satisfied by data`() {
    val assessment = assessmentFactory
      .withData("{\"test\": \"data\"}")
      .produce()

    every { userAccessServiceMock.userCanViewAssessment(any(), any()) } returns true

    every { assessmentRepositoryMock.findByIdOrNull(assessmentId) } returns assessment

    every { jsonSchemaServiceMock.getNewestSchema(ApprovedPremisesAssessmentJsonSchemaEntity::class.java) } returns assessmentSchema

    every { jsonSchemaServiceMock.validate(assessmentSchema, "{\"test\": \"data\"}") } returns false

    every { assessmentRepositoryMock.save(any()) } answers { it.invocation.args[0] as AssessmentEntity }

    every { offenderServiceMock.getOffenderByCrn(assessment.application.crn, user.deliusUsername) } returns AuthorisableActionResult.Success(
      OffenderDetailsSummaryFactory().produce(),
    )

    val result = assessmentService.acceptAssessment(user, assessmentId, "{\"test\": \"data\"}", placementRequirements, null, null, null)

    assertThat(result is CasResult.FieldValidationError).isTrue
    result as CasResult.FieldValidationError
    assertThat(result.validationMessages).contains(
      Assertions.entry("$.data", "invalid"),
    )
  }

  @Test
  fun `acceptAssessment returns unauthorised when user not allowed to view Offender (LAO)`() {
    val assessment = assessmentFactory.produce()

    every { userAccessServiceMock.userCanViewAssessment(any(), any()) } returns true

    every { assessmentRepositoryMock.findByIdOrNull(assessmentId) } returns assessment

    every { jsonSchemaServiceMock.getNewestSchema(ApprovedPremisesAssessmentJsonSchemaEntity::class.java) } returns assessmentSchema

    every { jsonSchemaServiceMock.validate(assessmentSchema, "{\"test\": \"data\"}") } returns true

    every { assessmentRepositoryMock.save(any()) } answers { it.invocation.args[0] as AssessmentEntity }

    every { offenderServiceMock.getOffenderByCrn(assessment.application.crn, user.deliusUsername) } returns AuthorisableActionResult.Unauthorised()

    val result = assessmentService.acceptAssessment(user, assessmentId, "{\"test\": \"data\"}", placementRequirements, null, null, null)

    assertThat(result is CasResult.Unauthorised).isTrue
  }

  @Test
  fun `acceptAssessment CAS1 returns updated assessment, emits domain event, sends email, does not create placement request when no date information provided`() {
    val assessment = assessmentFactory.produce()
    val application = assessment.application as ApprovedPremisesApplicationEntity

    val placementRequirementEntity = PlacementRequirementsEntityFactory()
      .withApplication(application)
      .withAssessment(assessment)
      .produce()

    every { userAccessServiceMock.userCanViewAssessment(any(), any()) } returns true

    every { assessmentRepositoryMock.findByIdOrNull(assessmentId) } returns assessment

    every { jsonSchemaServiceMock.getNewestSchema(ApprovedPremisesAssessmentJsonSchemaEntity::class.java) } returns assessmentSchema

    every { jsonSchemaServiceMock.validate(assessmentSchema, "{\"test\": \"data\"}") } returns true

    every { assessmentListener.preUpdate(any()) } returns Unit
    every { assessmentRepositoryMock.save(any()) } answers { it.invocation.args[0] as AssessmentEntity }

    val offenderDetails = OffenderDetailsSummaryFactory().produce()

    every { offenderServiceMock.getOffenderByCrn(assessment.application.crn, user.deliusUsername, any()) } returns AuthorisableActionResult.Success(offenderDetails)

    every { userServiceMock.getUserForRequest() } returns user

    every { placementRequirementsServiceMock.createPlacementRequirements(assessment, placementRequirements) } returns CasResult.Success(placementRequirementEntity)

    every { cas1AssessmentDomainEventServiceMock.assessmentAccepted(any(), any(), any(), any(), any(), any()) } just Runs

    every { cas1AssessmentEmailServiceMock.assessmentAccepted(any()) } just Runs

    val result = assessmentService.acceptAssessment(user, assessmentId, "{\"test\": \"data\"}", placementRequirements, null, null, null)

    assertThat(result is CasResult.Success).isTrue

    val updatedAssessment = (result as CasResult.Success).value
    assertThat(updatedAssessment.decision).isEqualTo(AssessmentDecision.ACCEPTED)
    assertThat(updatedAssessment.submittedAt).isNotNull()
    assertThat(updatedAssessment.document).isEqualTo("{\"test\": \"data\"}")

    verify(exactly = 0) {
      placementRequestServiceMock.createPlacementRequest(any(), any(), any(), any(), false, null)
    }

    verify(exactly = 1) {
      cas1AssessmentDomainEventServiceMock.assessmentAccepted(application, any(), any(), any(), any(), any())
    }
    verify(exactly = 1) {
      cas1AssessmentEmailServiceMock.assessmentAccepted(application)
    }
  }

  @Test
  fun `acceptAssessment CAS1 returns updated assessment, emits domain event, sends emails, creates placement request when requirements provided`() {
    val assessment = assessmentFactory.produce()
    val application = assessment.application as ApprovedPremisesApplicationEntity

    val placementRequirementEntity = PlacementRequirementsEntityFactory()
      .withApplication(application)
      .withAssessment(assessment)
      .produce()

    val placementDates = PlacementDates(
      expectedArrival = LocalDate.now(),
      duration = 12,
    )

    val notes = "Some Notes"

    every { userAccessServiceMock.userCanViewAssessment(any(), any()) } returns true

    every { assessmentRepositoryMock.findByIdOrNull(assessmentId) } returns assessment

    every { jsonSchemaServiceMock.getNewestSchema(ApprovedPremisesAssessmentJsonSchemaEntity::class.java) } returns assessmentSchema

    every { jsonSchemaServiceMock.validate(assessmentSchema, "{\"test\": \"data\"}") } returns true

    every { assessmentListener.preUpdate(any()) } returns Unit
    every { assessmentRepositoryMock.save(any()) } answers { it.invocation.args[0] as AssessmentEntity }

    every { placementRequirementsServiceMock.createPlacementRequirements(assessment, placementRequirements) } returns CasResult.Success(placementRequirementEntity)

    every { placementRequestServiceMock.createPlacementRequest(any(), any(), any(), any(), any(), any()) } returns PlacementRequestEntityFactory()
      .withPlacementRequirements(
        PlacementRequirementsEntityFactory()
          .withApplication(assessment.application as ApprovedPremisesApplicationEntity)
          .withAssessment(assessment)
          .produce(),
      )
      .withApplication(assessment.application as ApprovedPremisesApplicationEntity)
      .withAssessment(assessment)
      .withAllocatedToUser(user)
      .produce()

    val offenderDetails = OffenderDetailsSummaryFactory().produce()

    every { offenderServiceMock.getOffenderByCrn(assessment.application.crn, user.deliusUsername, any()) } returns AuthorisableActionResult.Success(offenderDetails)

    every { userServiceMock.getUserForRequest() } returns user

    every { domainEventServiceMock.saveApplicationAssessedDomainEvent(any()) } just Runs

    every { cas1AssessmentEmailServiceMock.assessmentAccepted(any()) } just Runs

    every { cas1AssessmentDomainEventServiceMock.assessmentAccepted(any(), any(), any(), any(), any(), any()) } just Runs

    every { cas1PlacementRequestEmailServiceMock.placementRequestSubmitted(any()) } just Runs

    val result = assessmentService.acceptAssessment(user, assessmentId, "{\"test\": \"data\"}", placementRequirements, placementDates, null, notes)

    assertThat(result is CasResult.Success).isTrue
    val updatedAssessment = (result as CasResult.Success).value
    assertThat(updatedAssessment.decision).isEqualTo(AssessmentDecision.ACCEPTED)
    assertThat(updatedAssessment.submittedAt).isNotNull()
    assertThat(updatedAssessment.document).isEqualTo("{\"test\": \"data\"}")

    verify(exactly = 1) {
      placementRequestServiceMock.createPlacementRequest(
        source = PlacementRequestSource.ASSESSMENT_OF_APPLICATION,
        placementRequirements = placementRequirementEntity,
        placementDates = placementDates,
        notes = notes,
        isParole = false,
        placementApplicationEntity = null,
      )
    }

    verify(exactly = 1) {
      cas1AssessmentEmailServiceMock.assessmentAccepted(application)
    }

    verify(exactly = 1) {
      cas1AssessmentDomainEventServiceMock.assessmentAccepted(application, any(), any(), any(), any(), any())
    }

    verify(exactly = 1) {
      cas1PlacementRequestEmailServiceMock.placementRequestSubmitted(assessment.application as ApprovedPremisesApplicationEntity)
    }
  }

  @Test
  fun `acceptAssessment CAS1 does not emit Domain Event when failing to create Placement Requirements`() {
    val assessment = assessmentFactory.produce()

    every { userAccessServiceMock.userCanViewAssessment(any(), any()) } returns true

    every { assessmentRepositoryMock.findByIdOrNull(assessmentId) } returns assessment

    every { jsonSchemaServiceMock.getNewestSchema(ApprovedPremisesAssessmentJsonSchemaEntity::class.java) } returns assessmentSchema

    every { jsonSchemaServiceMock.validate(assessmentSchema, "{\"test\": \"data\"}") } returns true

    every { assessmentListener.preUpdate(any()) } returns Unit
    every { assessmentRepositoryMock.save(any()) } answers { it.invocation.args[0] as AssessmentEntity }

    every { placementRequirementsServiceMock.createPlacementRequirements(assessment, placementRequirements) } returns CasResult.GeneralValidationError("Couldn't create Placement Requirements")

    every { offenderServiceMock.getOffenderByCrn(assessment.application.crn, user.deliusUsername) } returns AuthorisableActionResult.Success(
      OffenderDetailsSummaryFactory().produce(),
    )

    val result = assessmentService.acceptAssessment(user, assessmentId, "{\"test\": \"data\"}", placementRequirements, null, null, null)

    assertThat(result is CasResult.GeneralValidationError).isTrue

    verify(exactly = 0) {
      domainEventServiceMock.saveApplicationAssessedDomainEvent(any())
    }

    verify(exactly = 1) {
      placementRequirementsServiceMock.createPlacementRequirements(assessment, placementRequirements)
    }
  }

  @Test
  fun `acceptAssessment CAS3 sets completed at timestamp to null`() {
    val probationRegion = ProbationRegionEntityFactory()
      .withYieldedApArea { ApAreaEntityFactory().produce() }
      .produce()

    val user = UserEntityFactory()
      .withProbationRegion(probationRegion)
      .produce()

    val application = TemporaryAccommodationApplicationEntityFactory()
      .withCreatedByUser(user)
      .withProbationRegion(probationRegion)
      .produce()

    val schema = TemporaryAccommodationAssessmentJsonSchemaEntityFactory()
      .withPermissiveSchema()
      .produce()

    val assessment = TemporaryAccommodationAssessmentEntityFactory()
      .withCompletedAt(OffsetDateTime.now())
      .withDecision(AssessmentDecision.REJECTED)
      .withApplication(application)
      .withAllocatedToUser(user)
      .withAssessmentSchema(schema)
      .produce()

    every { userAccessServiceMock.userCanViewAssessment(any(), any()) } returns true

    every { assessmentRepositoryMock.findByIdOrNull(assessmentId) } returns assessment

    every { jsonSchemaServiceMock.getNewestSchema(TemporaryAccommodationAssessmentJsonSchemaEntity::class.java) } returns schema

    every { jsonSchemaServiceMock.validate(assessmentSchema, "{\"test\": \"data\"}") } returns true

    every { assessmentRepositoryMock.save(any()) } answers { it.invocation.args[0] as TemporaryAccommodationAssessmentEntity }

    val offenderDetails = OffenderDetailsSummaryFactory().produce()

    every { offenderServiceMock.getOffenderByCrn(assessment.application.crn, user.deliusUsername, any()) } returns AuthorisableActionResult.Success(offenderDetails)

    val staffUserDetails = StaffDetailFactory.staffDetail(probationArea = ProbationArea(code = "N26", description = "description"))

    every { apDeliusContextApiClient.getStaffDetail(user.deliusUsername) } returns ClientResult.Success(
      HttpStatus.OK,
      staffUserDetails,
    )

    every { cas1AssessmentDomainEventServiceMock.assessmentAccepted(any(), any(), any(), any(), any(), any()) } just Runs

    every { emailNotificationServiceMock.sendEmail(any(), any(), any()) } just Runs

    every { userServiceMock.getUserForRequest() } returns user
    every { assessmentReferralHistoryNoteRepositoryMock.save(any()) } returnsArgument 0

    val result = assessmentService.acceptAssessment(user, assessmentId, "{\"test\": \"data\"}", null, null, null, null)

    assertThat(result is CasResult.Success).isTrue

    val updatedAssessment = (result as CasResult.Success).value as TemporaryAccommodationAssessmentEntity
    assertThat(updatedAssessment.decision).isEqualTo(AssessmentDecision.ACCEPTED)
    assertThat(updatedAssessment.submittedAt).isNotNull()
    assertThat(updatedAssessment.document).isEqualTo("{\"test\": \"data\"}")
    assertThat(updatedAssessment.completedAt).isNull()
    assertAssessmentHasSystemNote(assessment, user, ReferralHistorySystemNoteType.READY_TO_PLACE)
  }
}
