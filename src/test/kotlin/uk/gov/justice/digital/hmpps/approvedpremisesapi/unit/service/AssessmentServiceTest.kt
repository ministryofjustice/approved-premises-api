package uk.gov.justice.digital.hmpps.approvedpremisesapi.unit.service

import com.fasterxml.jackson.databind.ObjectMapper
import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.spyk
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatExceptionOfType
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.assertj.core.api.Assertions.entry
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.data.repository.findByIdOrNull
import org.springframework.http.HttpStatus
import uk.gov.justice.digital.hmpps.approvedpremisesapi.allocations.UserAllocator
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.model.ApplicationAssessedAssessedBy
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.model.Cru
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.model.PersonReference
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.model.ProbationArea
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.model.StaffMember
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.AssessmentSortField
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ServiceName
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.SortDirection
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.ClientResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.CommunityApiClient
import uk.gov.justice.digital.hmpps.approvedpremisesapi.config.NotifyConfig
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.ApAreaEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.ApprovedPremisesApplicationEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.ApprovedPremisesApplicationJsonSchemaEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.ApprovedPremisesAssessmentEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.AssessmentClarificationNoteEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.OffenderDetailsSummaryFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.ProbationRegionEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.StaffUserDetailsFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.TemporaryAccommodationApplicationEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.TemporaryAccommodationApplicationJsonSchemaEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.TemporaryAccommodationAssessmentEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.UserEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.UserQualificationAssignmentEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.UserRoleAssignmentEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApprovedPremisesApplicationEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApprovedPremisesAssessmentEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApprovedPremisesAssessmentJsonSchemaEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.AssessmentClarificationNoteEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.AssessmentClarificationNoteRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.AssessmentDecision
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.AssessmentEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.AssessmentReferralHistoryNoteRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.AssessmentRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.DomainAssessmentSummaryStatus
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ReferralHistorySystemNoteType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.TemporaryAccommodationAssessmentEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.TemporaryAccommodationAssessmentJsonSchemaEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserQualification
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserRole
import uk.gov.justice.digital.hmpps.approvedpremisesapi.results.AuthorisableActionResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.results.ValidatableActionResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.AssessmentService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.CruService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.DomainEventService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.EmailNotificationService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.JsonSchemaService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.OffenderService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.PlacementRequestService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.PlacementRequirementsService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.UserAccessService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.UserService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.unit.util.assertAssessmentHasSystemNote
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.PageCriteria
import java.time.LocalDate
import java.time.OffsetDateTime
import java.util.UUID

class AssessmentServiceTest {
  private val userServiceMock = mockk<UserService>()
  private val userAccessServiceMock = mockk<UserAccessService>()
  private val assessmentRepositoryMock = mockk<AssessmentRepository>()
  private val assessmentClarificationNoteRepositoryMock = mockk<AssessmentClarificationNoteRepository>()
  private val assessmentReferralHistoryNoteRepositoryMock = mockk<AssessmentReferralHistoryNoteRepository>()
  private val jsonSchemaServiceMock = mockk<JsonSchemaService>()
  private val domainEventServiceMock = mockk<DomainEventService>()
  private val offenderServiceMock = mockk<OffenderService>()
  private val cruServiceMock = mockk<CruService>()
  private val communityApiClientMock = mockk<CommunityApiClient>()
  private val placementRequestServiceMock = mockk<PlacementRequestService>()
  private val emailNotificationServiceMock = mockk<EmailNotificationService>()
  private val placementRequirementsServiceMock = mockk<PlacementRequirementsService>()
  private val userAllocatorMock = mockk<UserAllocator>()
  private val objectMapperMock = spyk<ObjectMapper>()

  private val assessmentService = AssessmentService(
    userServiceMock,
    userAccessServiceMock,
    assessmentRepositoryMock,
    assessmentClarificationNoteRepositoryMock,
    assessmentReferralHistoryNoteRepositoryMock,
    jsonSchemaServiceMock,
    domainEventServiceMock,
    offenderServiceMock,
    communityApiClientMock,
    cruServiceMock,
    placementRequestServiceMock,
    emailNotificationServiceMock,
    NotifyConfig(),
    placementRequirementsServiceMock,
    userAllocatorMock,
    objectMapperMock,
    "http://frontend/applications/#id",
    "http://frontend/assessments/#id",
  )

  @Test
  fun `getVisibleAssessmentSummariesForUserCAS1 only fetches Approved Premises assessments allocated to the user that have not been reallocated`() {
    val user = UserEntityFactory()
      .withYieldedProbationRegion {
        ProbationRegionEntityFactory()
          .withYieldedApArea { ApAreaEntityFactory().produce() }
          .produce()
      }
      .produce()

    user.roles.add(
      UserRoleAssignmentEntityFactory()
        .withRole(UserRole.CAS1_ASSESSOR)
        .withUser(user)
        .produce(),
    )

    every {
      assessmentRepositoryMock.findAllApprovedPremisesAssessmentSummariesNotReallocated(
        any(),
        listOf("NOT_STARTED", "IN_PROGRESS"),
        PageRequest.of(4, 7, Sort.by("status").ascending()),
      )
    } returns Page.empty()

    assessmentService.getVisibleAssessmentSummariesForUserCAS1(
      user,
      statuses = listOf(DomainAssessmentSummaryStatus.NOT_STARTED, DomainAssessmentSummaryStatus.IN_PROGRESS),
      PageCriteria(sortBy = AssessmentSortField.assessmentStatus, sortDirection = SortDirection.asc, page = 5, perPage = 7),
    )

    verify(exactly = 1) {
      assessmentRepositoryMock.findAllApprovedPremisesAssessmentSummariesNotReallocated(
        user.id.toString(),
        listOf("NOT_STARTED", "IN_PROGRESS"),
        PageRequest.of(4, 7, Sort.by("status").ascending()),
      )
    }
  }

  @Test
  fun `getVisibleAssessmentSummariesForUserCAS3 only fetches Temporary Accommodation assessments within the user's probation region`() {
    val user = UserEntityFactory()
      .withYieldedProbationRegion {
        ProbationRegionEntityFactory()
          .withYieldedApArea { ApAreaEntityFactory().produce() }
          .produce()
      }
      .produce()

    user.roles.add(
      UserRoleAssignmentEntityFactory()
        .withRole(UserRole.CAS3_ASSESSOR)
        .withUser(user)
        .produce(),
    )

    every { assessmentRepositoryMock.findAllTemporaryAccommodationAssessmentSummariesForRegion(any()) } returns emptyList()

    assessmentService.getVisibleAssessmentSummariesForUserCAS3(user)

    verify(exactly = 1) { assessmentRepositoryMock.findAllTemporaryAccommodationAssessmentSummariesForRegion(user.probationRegion.id) }
  }

  @Test
  fun `getAssessmentSummariesByCrnForUser is not supported for Approved Premises`() {
    val user = UserEntityFactory()
      .withYieldedProbationRegion {
        ProbationRegionEntityFactory()
          .withYieldedApArea { ApAreaEntityFactory().produce() }
          .produce()
      }
      .produce()

    user.roles.add(
      UserRoleAssignmentEntityFactory()
        .withRole(UserRole.CAS1_ASSESSOR)
        .withUser(user)
        .produce(),
    )

    assertThatExceptionOfType(RuntimeException::class.java)
      .isThrownBy { assessmentService.getAssessmentSummariesByCrnForUserCAS3(user, "SOMECRN", ServiceName.approvedPremises) }
      .withMessage("Only CAS3 assessments are currently supported")
  }

  @Test
  fun `getAssessmentSummariesByCrnForUser only fetches Temporary Accommodation assessments for the given CRN and within the user's probation region`() {
    val user = UserEntityFactory()
      .withYieldedProbationRegion {
        ProbationRegionEntityFactory()
          .withYieldedApArea { ApAreaEntityFactory().produce() }
          .produce()
      }
      .produce()

    user.roles.add(
      UserRoleAssignmentEntityFactory()
        .withRole(UserRole.CAS3_ASSESSOR)
        .withUser(user)
        .produce(),
    )

    every { assessmentRepositoryMock.findTemporaryAccommodationAssessmentSummariesForRegionAndCrn(any(), any()) } returns emptyList()

    assessmentService.getAssessmentSummariesByCrnForUserCAS3(user, "SOMECRN", ServiceName.temporaryAccommodation)

    verify(exactly = 1) { assessmentRepositoryMock.findTemporaryAccommodationAssessmentSummariesForRegionAndCrn(user.probationRegion.id, "SOMECRN") }
  }

  @Test
  fun `getAssessmentForUser gets assessment when user is authorised to view assessment`() {
    val user = UserEntityFactory()
      .withYieldedProbationRegion {
        ProbationRegionEntityFactory()
          .withYieldedApArea { ApAreaEntityFactory().produce() }
          .produce()
      }
      .produce()

    val assessment = ApprovedPremisesAssessmentEntityFactory()
      .withAllocatedToUser(
        UserEntityFactory()
          .withYieldedProbationRegion {
            ProbationRegionEntityFactory()
              .withYieldedApArea { ApAreaEntityFactory().produce() }
              .produce()
          }
          .produce(),
      )
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
      .produce()

    every { userAccessServiceMock.userCanViewAssessment(user, assessment) } returns true

    every { assessmentRepositoryMock.findByIdOrNull(assessment.id) } returns assessment
    every { jsonSchemaServiceMock.getNewestSchema(ApprovedPremisesAssessmentJsonSchemaEntity::class.java) } returns ApprovedPremisesApplicationJsonSchemaEntityFactory().produce()

    every { offenderServiceMock.getOffenderByCrn(assessment.application.crn, user.deliusUsername) } returns AuthorisableActionResult.Success(
      OffenderDetailsSummaryFactory().produce(),
    )

    val result = assessmentService.getAssessmentForUser(user, assessment.id)

    assertThat(result is AuthorisableActionResult.Success).isTrue
    result as AuthorisableActionResult.Success
    assertThat(result.entity).isEqualTo(assessment)
  }

  @Test
  fun `getAssessmentForUser does not get assessment when user is not authorised to view assessment`() {
    val assessmentId = UUID.randomUUID()

    val user = UserEntityFactory()
      .withYieldedProbationRegion {
        ProbationRegionEntityFactory()
          .withYieldedApArea { ApAreaEntityFactory().produce() }
          .produce()
      }
      .produce()

    val assessment =
      ApprovedPremisesAssessmentEntityFactory()
        .withId(assessmentId)
        .withAllocatedToUser(
          UserEntityFactory()
            .withYieldedProbationRegion {
              ProbationRegionEntityFactory()
                .withYieldedApArea { ApAreaEntityFactory().produce() }
                .produce()
            }
            .produce(),
        )
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
        .produce()

    every { userAccessServiceMock.userCanViewAssessment(user, assessment) } returns false

    every { assessmentRepositoryMock.findByIdOrNull(assessmentId) } returns assessment
    every { jsonSchemaServiceMock.getNewestSchema(ApprovedPremisesAssessmentJsonSchemaEntity::class.java) } returns ApprovedPremisesApplicationJsonSchemaEntityFactory().produce()

    val result = assessmentService.getAssessmentForUser(user, assessmentId)

    assertThat(result is AuthorisableActionResult.Unauthorised).isTrue
  }

  @Test
  fun `getAssessmentForUser returns not found for non-existent Assessment`() {
    val assessmentId = UUID.randomUUID()

    val user = UserEntityFactory()
      .withYieldedProbationRegion {
        ProbationRegionEntityFactory()
          .withYieldedApArea { ApAreaEntityFactory().produce() }
          .produce()
      }
      .produce()

    every { assessmentRepositoryMock.findByIdOrNull(assessmentId) } returns null
    every { jsonSchemaServiceMock.getNewestSchema(ApprovedPremisesAssessmentJsonSchemaEntity::class.java) } returns ApprovedPremisesApplicationJsonSchemaEntityFactory().produce()

    val result = assessmentService.getAssessmentForUser(user, assessmentId)

    assertThat(result is AuthorisableActionResult.NotFound).isTrue
  }

  @Test
  fun `addAssessmentClarificationNote returns not found for non-existent Assessment`() {
    val assessmentId = UUID.randomUUID()

    val user = UserEntityFactory()
      .withYieldedProbationRegion {
        ProbationRegionEntityFactory()
          .withYieldedApArea { ApAreaEntityFactory().produce() }
          .produce()
      }
      .produce()

    every { assessmentRepositoryMock.findByIdOrNull(assessmentId) } returns null
    every { jsonSchemaServiceMock.getNewestSchema(ApprovedPremisesAssessmentJsonSchemaEntity::class.java) } returns ApprovedPremisesApplicationJsonSchemaEntityFactory().produce()

    val result = assessmentService.addAssessmentClarificationNote(user, assessmentId, "clarification note")

    assertThat(result is AuthorisableActionResult.NotFound).isTrue
  }

  @Test
  fun `addAssessmentClarificationNote returns unauthorised when the user does not have permission to access the assessment`() {
    val assessmentId = UUID.randomUUID()

    val user = UserEntityFactory()
      .withYieldedProbationRegion {
        ProbationRegionEntityFactory()
          .withYieldedApArea { ApAreaEntityFactory().produce() }
          .produce()
      }
      .produce()

    every { assessmentRepositoryMock.findByIdOrNull(assessmentId) } returns ApprovedPremisesAssessmentEntityFactory()
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

    every { jsonSchemaServiceMock.getNewestSchema(ApprovedPremisesAssessmentJsonSchemaEntity::class.java) } returns ApprovedPremisesApplicationJsonSchemaEntityFactory().produce()

    val result = assessmentService.addAssessmentClarificationNote(user, assessmentId, "clarification note")

    assertThat(result is AuthorisableActionResult.Unauthorised).isTrue
  }

  @Test
  fun `addAssessmentClarificationNote adds note to assessment allocated to different user for workflow managers`() {
    val user = UserEntityFactory()
      .withYieldedProbationRegion {
        ProbationRegionEntityFactory()
          .withYieldedApArea { ApAreaEntityFactory().produce() }
          .produce()
      }
      .produce()

    user.roles.add(
      UserRoleAssignmentEntityFactory()
        .withRole(UserRole.CAS1_WORKFLOW_MANAGER)
        .withUser(user)
        .produce(),
    )

    val assessment = ApprovedPremisesAssessmentEntityFactory()
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

    every { userAccessServiceMock.userCanViewAssessment(any(), any()) } returns true

    every { assessmentRepositoryMock.findByIdOrNull(assessment.id) } returns assessment

    every { assessmentClarificationNoteRepositoryMock.save(any()) } answers {
      it.invocation.args[0] as AssessmentClarificationNoteEntity
    }

    every { jsonSchemaServiceMock.getNewestSchema(ApprovedPremisesAssessmentJsonSchemaEntity::class.java) } returns ApprovedPremisesApplicationJsonSchemaEntityFactory().produce()

    every { offenderServiceMock.getOffenderByCrn(assessment.application.crn, user.deliusUsername) } returns AuthorisableActionResult.Success(
      OffenderDetailsSummaryFactory().produce(),
    )

    val result = assessmentService.addAssessmentClarificationNote(user, assessment.id, "clarification note")

    assertThat(result is AuthorisableActionResult.Success).isTrue

    verify(exactly = 1) { assessmentClarificationNoteRepositoryMock.save(any()) }
  }

  @Test
  fun `addAssessmentClarificationNote adds note to assessment allocated to calling user`() {
    val user = UserEntityFactory()
      .withYieldedProbationRegion {
        ProbationRegionEntityFactory()
          .withYieldedApArea { ApAreaEntityFactory().produce() }
          .produce()
      }
      .produce()

    val assessment = ApprovedPremisesAssessmentEntityFactory()
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
      .produce()

    every { userAccessServiceMock.userCanViewAssessment(any(), any()) } returns true

    every { assessmentRepositoryMock.findByIdOrNull(assessment.id) } returns assessment

    every { assessmentClarificationNoteRepositoryMock.save(any()) } answers {
      it.invocation.args[0] as AssessmentClarificationNoteEntity
    }

    every { jsonSchemaServiceMock.getNewestSchema(ApprovedPremisesAssessmentJsonSchemaEntity::class.java) } returns ApprovedPremisesApplicationJsonSchemaEntityFactory().produce()

    every { offenderServiceMock.getOffenderByCrn(assessment.application.crn, user.deliusUsername) } returns AuthorisableActionResult.Success(
      OffenderDetailsSummaryFactory().produce(),
    )

    val result = assessmentService.addAssessmentClarificationNote(user, assessment.id, "clarification note")

    assertThat(result is AuthorisableActionResult.Success).isTrue

    verify(exactly = 1) { assessmentClarificationNoteRepositoryMock.save(any()) }
  }

  @Test
  fun `addReferralHistoryUserNote returns not found for non-existent Assessment`() {
    val assessmentId = UUID.randomUUID()

    val user = UserEntityFactory()
      .withYieldedProbationRegion {
        ProbationRegionEntityFactory()
          .withYieldedApArea { ApAreaEntityFactory().produce() }
          .produce()
      }
      .produce()

    every { assessmentRepositoryMock.findByIdOrNull(assessmentId) } returns null
    every { jsonSchemaServiceMock.getNewestSchema(ApprovedPremisesAssessmentJsonSchemaEntity::class.java) } returns ApprovedPremisesApplicationJsonSchemaEntityFactory().produce()

    val result = assessmentService.addAssessmentReferralHistoryUserNote(user, assessmentId, "referral history note")

    assertThat(result is AuthorisableActionResult.NotFound).isTrue()
  }

  @Test
  fun `updateAssessment returns unauthorised when the user does not have permission to access the assessment`() {
    val assessmentId = UUID.randomUUID()

    val user = UserEntityFactory()
      .withYieldedProbationRegion {
        ProbationRegionEntityFactory()
          .withYieldedApArea { ApAreaEntityFactory().produce() }
          .produce()
      }
      .produce()

    every { assessmentRepositoryMock.findByIdOrNull(assessmentId) } returns ApprovedPremisesAssessmentEntityFactory()
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

    every { jsonSchemaServiceMock.getNewestSchema(ApprovedPremisesAssessmentJsonSchemaEntity::class.java) } returns ApprovedPremisesApplicationJsonSchemaEntityFactory().produce()

    val result = assessmentService.updateAssessment(user, assessmentId, "{}")

    assertThat(result is AuthorisableActionResult.Unauthorised).isTrue
  }

  @Test
  fun `updateAssessment returns general validation error for Assessment where schema is outdated`() {
    val user = UserEntityFactory()
      .withYieldedProbationRegion {
        ProbationRegionEntityFactory()
          .withYieldedApArea { ApAreaEntityFactory().produce() }
          .produce()
      }
      .produce()

    val assessment = ApprovedPremisesAssessmentEntityFactory()
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
      .withSubmittedAt(OffsetDateTime.now())
      .withDecision(AssessmentDecision.ACCEPTED)
      .withAllocatedToUser(user)
      .produce()

    every { userAccessServiceMock.userCanViewAssessment(any(), any()) } returns true

    every { assessmentRepositoryMock.findByIdOrNull(assessment.id) } returns assessment

    every { jsonSchemaServiceMock.getNewestSchema(ApprovedPremisesAssessmentJsonSchemaEntity::class.java) } returns ApprovedPremisesApplicationJsonSchemaEntityFactory().produce()

    every { offenderServiceMock.getOffenderByCrn(assessment.application.crn, user.deliusUsername) } returns AuthorisableActionResult.Success(
      OffenderDetailsSummaryFactory().produce(),
    )

    val result = assessmentService.updateAssessment(user, assessment.id, "{}")

    assertThat(result is AuthorisableActionResult.Success).isTrue
    val validationResult = (result as AuthorisableActionResult.Success).entity
    assertThat(validationResult is ValidatableActionResult.GeneralValidationError)
    val generalValidationError = validationResult as ValidatableActionResult.GeneralValidationError
    assertThat(generalValidationError.message).isEqualTo("The schema version is outdated")
  }

  @Test
  fun `updateAssessment returns general validation error for Assessment where assessment is withdrawn`() {
    val user = UserEntityFactory()
      .withYieldedProbationRegion {
        ProbationRegionEntityFactory()
          .withYieldedApArea { ApAreaEntityFactory().produce() }
          .produce()
      }
      .produce()

    val assessment = ApprovedPremisesAssessmentEntityFactory()
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
          .withIsWithdrawn(true)
          .produce(),
      )
      .withSubmittedAt(OffsetDateTime.now())
      .withDecision(AssessmentDecision.ACCEPTED)
      .withAllocatedToUser(user)
      .withIsWithdrawn(true)
      .produce()

    every { userAccessServiceMock.userCanViewAssessment(any(), any()) } returns true

    every { assessmentRepositoryMock.findByIdOrNull(assessment.id) } returns assessment

    every { jsonSchemaServiceMock.getNewestSchema(ApprovedPremisesAssessmentJsonSchemaEntity::class.java) } returns ApprovedPremisesApplicationJsonSchemaEntityFactory().produce()

    every { offenderServiceMock.getOffenderByCrn(assessment.application.crn, user.deliusUsername) } returns AuthorisableActionResult.Success(
      OffenderDetailsSummaryFactory().produce(),
    )

    val result = assessmentService.updateAssessment(user, assessment.id, "{}")

    assertThat(result is AuthorisableActionResult.Success).isTrue
    val validationResult = (result as AuthorisableActionResult.Success).entity
    assertThat(validationResult is ValidatableActionResult.GeneralValidationError)
    val generalValidationError = validationResult as ValidatableActionResult.GeneralValidationError
    assertThat(generalValidationError.message).isEqualTo("The application has been withdrawn.")
  }

  @Test
  fun `updateAssessment returns general validation error for Assessment where decision has already been taken`() {
    val user = UserEntityFactory()
      .withYieldedProbationRegion {
        ProbationRegionEntityFactory()
          .withYieldedApArea { ApAreaEntityFactory().produce() }
          .produce()
      }
      .produce()

    val schema = ApprovedPremisesAssessmentJsonSchemaEntity(
      id = UUID.randomUUID(),
      addedAt = OffsetDateTime.now(),
      schema = "{}",
    )

    val assessment = ApprovedPremisesAssessmentEntityFactory()
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
      .withSubmittedAt(OffsetDateTime.now())
      .withDecision(AssessmentDecision.ACCEPTED)
      .withAllocatedToUser(user)
      .withAssessmentSchema(schema)
      .produce()

    every { userAccessServiceMock.userCanViewAssessment(any(), any()) } returns true

    every { assessmentRepositoryMock.findByIdOrNull(assessment.id) } returns assessment

    every { jsonSchemaServiceMock.getNewestSchema(ApprovedPremisesAssessmentJsonSchemaEntity::class.java) } returns schema

    every { offenderServiceMock.getOffenderByCrn(assessment.application.crn, user.deliusUsername) } returns AuthorisableActionResult.Success(
      OffenderDetailsSummaryFactory().produce(),
    )

    val result = assessmentService.updateAssessment(user, assessment.id, "{}")

    assertThat(result is AuthorisableActionResult.Success).isTrue
    val validationResult = (result as AuthorisableActionResult.Success).entity
    assertThat(validationResult is ValidatableActionResult.GeneralValidationError)
    val generalValidationError = validationResult as ValidatableActionResult.GeneralValidationError
    assertThat(generalValidationError.message).isEqualTo("A decision has already been taken on this assessment")
  }

  @Test
  fun `updateAssessment returns general validation error for Assessment where assessment has been deallocated`() {
    val user = UserEntityFactory()
      .withYieldedProbationRegion {
        ProbationRegionEntityFactory()
          .withYieldedApArea { ApAreaEntityFactory().produce() }
          .produce()
      }
      .produce()

    val schema = ApprovedPremisesAssessmentJsonSchemaEntity(
      id = UUID.randomUUID(),
      addedAt = OffsetDateTime.now(),
      schema = "{}",
    )

    val assessment = ApprovedPremisesAssessmentEntityFactory()
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
      .withSubmittedAt(null)
      .withDecision(null)
      .withAllocatedToUser(user)
      .withAssessmentSchema(schema)
      .withReallocatedAt(OffsetDateTime.now())
      .produce()

    every { userAccessServiceMock.userCanViewAssessment(any(), any()) } returns true

    every { assessmentRepositoryMock.findByIdOrNull(assessment.id) } returns assessment

    every { jsonSchemaServiceMock.getNewestSchema(ApprovedPremisesAssessmentJsonSchemaEntity::class.java) } returns schema

    every { offenderServiceMock.getOffenderByCrn(assessment.application.crn, user.deliusUsername) } returns AuthorisableActionResult.Success(
      OffenderDetailsSummaryFactory().produce(),
    )

    val result = assessmentService.updateAssessment(user, assessment.id, "{}")

    assertThat(result is AuthorisableActionResult.Success).isTrue
    val validationResult = (result as AuthorisableActionResult.Success).entity
    assertThat(validationResult is ValidatableActionResult.GeneralValidationError)
    val generalValidationError = validationResult as ValidatableActionResult.GeneralValidationError
    assertThat(generalValidationError.message).isEqualTo("The application has been reallocated, this assessment is read only")
  }

  @Test
  fun `updateAssessment returns unauthorised when user cannot view Offender (LAO)`() {
    val user = UserEntityFactory()
      .withYieldedProbationRegion {
        ProbationRegionEntityFactory()
          .withYieldedApArea { ApAreaEntityFactory().produce() }
          .produce()
      }
      .produce()

    val schema = ApprovedPremisesAssessmentJsonSchemaEntity(
      id = UUID.randomUUID(),
      addedAt = OffsetDateTime.now(),
      schema = "{}",
    )

    val assessment = ApprovedPremisesAssessmentEntityFactory()
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
      .withAssessmentSchema(schema)
      .produce()

    every { userAccessServiceMock.userCanViewAssessment(any(), any()) } returns true

    every { assessmentRepositoryMock.findByIdOrNull(assessment.id) } returns assessment

    every { jsonSchemaServiceMock.getNewestSchema(ApprovedPremisesAssessmentJsonSchemaEntity::class.java) } returns schema

    every { assessmentRepositoryMock.save(any()) } answers { it.invocation.args[0] as ApprovedPremisesAssessmentEntity }

    every { offenderServiceMock.getOffenderByCrn(assessment.application.crn, user.deliusUsername) } returns AuthorisableActionResult.Unauthorised()

    val result = assessmentService.updateAssessment(user, assessment.id, "{\"test\": \"data\"}")

    assertThat(result is AuthorisableActionResult.Unauthorised).isTrue
  }

  @Test
  fun `updateAssessment returns updated assessment`() {
    val user = UserEntityFactory()
      .withYieldedProbationRegion {
        ProbationRegionEntityFactory()
          .withYieldedApArea { ApAreaEntityFactory().produce() }
          .produce()
      }
      .produce()

    val schema = ApprovedPremisesAssessmentJsonSchemaEntity(
      id = UUID.randomUUID(),
      addedAt = OffsetDateTime.now(),
      schema = "{}",
    )

    val assessment = ApprovedPremisesAssessmentEntityFactory()
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
      .withAssessmentSchema(schema)
      .produce()

    every { userAccessServiceMock.userCanViewAssessment(any(), any()) } returns true

    every { assessmentRepositoryMock.findByIdOrNull(assessment.id) } returns assessment

    every { jsonSchemaServiceMock.getNewestSchema(ApprovedPremisesAssessmentJsonSchemaEntity::class.java) } returns schema

    every { assessmentRepositoryMock.save(any()) } answers { it.invocation.args[0] as ApprovedPremisesAssessmentEntity }

    every { offenderServiceMock.getOffenderByCrn(assessment.application.crn, user.deliusUsername) } returns AuthorisableActionResult.Success(
      OffenderDetailsSummaryFactory().produce(),
    )

    val result = assessmentService.updateAssessment(user, assessment.id, "{\"test\": \"data\"}")

    assertThat(result is AuthorisableActionResult.Success).isTrue
    val validationResult = (result as AuthorisableActionResult.Success).entity
    assertThat(validationResult is ValidatableActionResult.Success)
    val updatedAssessment = (validationResult as ValidatableActionResult.Success).entity
    assertThat(updatedAssessment.data).isEqualTo("{\"test\": \"data\"}")
  }

  @Test
  fun `rejectAssessment returns unauthorised when the user does not have permission to access the assessment`() {
    val assessmentId = UUID.randomUUID()

    val user = UserEntityFactory()
      .withYieldedProbationRegion {
        ProbationRegionEntityFactory()
          .withYieldedApArea { ApAreaEntityFactory().produce() }
          .produce()
      }
      .produce()

    every { assessmentRepositoryMock.findByIdOrNull(assessmentId) } returns ApprovedPremisesAssessmentEntityFactory()
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

    every { jsonSchemaServiceMock.getNewestSchema(ApprovedPremisesAssessmentJsonSchemaEntity::class.java) } returns ApprovedPremisesApplicationJsonSchemaEntityFactory().produce()

    val result = assessmentService.rejectAssessment(user, assessmentId, "{}", "reasoning")

    assertThat(result is AuthorisableActionResult.Unauthorised).isTrue
  }

  @Test
  fun `rejectAssessment returns general validation error for Assessment where schema is outdated`() {
    val user = UserEntityFactory()
      .withYieldedProbationRegion {
        ProbationRegionEntityFactory()
          .withYieldedApArea { ApAreaEntityFactory().produce() }
          .produce()
      }
      .produce()

    val assessment = ApprovedPremisesAssessmentEntityFactory()
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
      .withSubmittedAt(OffsetDateTime.now())
      .withDecision(AssessmentDecision.ACCEPTED)
      .withAllocatedToUser(user)
      .produce()

    every { userAccessServiceMock.userCanViewAssessment(any(), any()) } returns true

    every { assessmentRepositoryMock.findByIdOrNull(assessment.id) } returns assessment

    every { jsonSchemaServiceMock.getNewestSchema(ApprovedPremisesAssessmentJsonSchemaEntity::class.java) } returns ApprovedPremisesApplicationJsonSchemaEntityFactory().produce()

    every { offenderServiceMock.getOffenderByCrn(assessment.application.crn, user.deliusUsername) } returns AuthorisableActionResult.Success(
      OffenderDetailsSummaryFactory().produce(),
    )

    val result = assessmentService.rejectAssessment(user, assessment.id, "{}", "reasoning")

    assertThat(result is AuthorisableActionResult.Success).isTrue
    val validationResult = (result as AuthorisableActionResult.Success).entity
    assertThat(validationResult is ValidatableActionResult.GeneralValidationError)
    val generalValidationError = validationResult as ValidatableActionResult.GeneralValidationError
    assertThat(generalValidationError.message).isEqualTo("The schema version is outdated")
  }

  @Test
  fun `rejectAssessment returns general validation error for Assessment where decision has already been taken`() {
    val user = UserEntityFactory()
      .withYieldedProbationRegion {
        ProbationRegionEntityFactory()
          .withYieldedApArea { ApAreaEntityFactory().produce() }
          .produce()
      }
      .produce()

    val schema = ApprovedPremisesAssessmentJsonSchemaEntity(
      id = UUID.randomUUID(),
      addedAt = OffsetDateTime.now(),
      schema = "{}",
    )

    val assessment = ApprovedPremisesAssessmentEntityFactory()
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
      .withSubmittedAt(OffsetDateTime.now())
      .withDecision(AssessmentDecision.ACCEPTED)
      .withAllocatedToUser(user)
      .withAssessmentSchema(schema)
      .produce()

    every { userAccessServiceMock.userCanViewAssessment(any(), any()) } returns true

    every { assessmentRepositoryMock.findByIdOrNull(assessment.id) } returns assessment

    every { jsonSchemaServiceMock.getNewestSchema(ApprovedPremisesAssessmentJsonSchemaEntity::class.java) } returns schema

    every { offenderServiceMock.getOffenderByCrn(assessment.application.crn, user.deliusUsername) } returns AuthorisableActionResult.Success(
      OffenderDetailsSummaryFactory().produce(),
    )

    val result = assessmentService.rejectAssessment(user, assessment.id, "{}", "reasoning")

    assertThat(result is AuthorisableActionResult.Success).isTrue
    val validationResult = (result as AuthorisableActionResult.Success).entity
    assertThat(validationResult is ValidatableActionResult.GeneralValidationError)
    val generalValidationError = validationResult as ValidatableActionResult.GeneralValidationError
    assertThat(generalValidationError.message).isEqualTo("A decision has already been taken on this assessment")
  }

  @Test
  fun `rejectAssessment returns general validation error for Assessment where assessment has been deallocated`() {
    val user = UserEntityFactory()
      .withYieldedProbationRegion {
        ProbationRegionEntityFactory()
          .withYieldedApArea { ApAreaEntityFactory().produce() }
          .produce()
      }
      .produce()

    val schema = ApprovedPremisesAssessmentJsonSchemaEntity(
      id = UUID.randomUUID(),
      addedAt = OffsetDateTime.now(),
      schema = "{}",
    )

    val assessment = ApprovedPremisesAssessmentEntityFactory()
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
      .withSubmittedAt(null)
      .withDecision(null)
      .withAllocatedToUser(user)
      .withAssessmentSchema(schema)
      .withReallocatedAt(OffsetDateTime.now())
      .produce()

    every { userAccessServiceMock.userCanViewAssessment(any(), any()) } returns true

    every { assessmentRepositoryMock.findByIdOrNull(assessment.id) } returns assessment

    every { jsonSchemaServiceMock.getNewestSchema(ApprovedPremisesAssessmentJsonSchemaEntity::class.java) } returns schema

    every { offenderServiceMock.getOffenderByCrn(assessment.application.crn, user.deliusUsername) } returns AuthorisableActionResult.Success(
      OffenderDetailsSummaryFactory().produce(),
    )

    val result = assessmentService.rejectAssessment(user, assessment.id, "{}", "reasoning")

    assertThat(result is AuthorisableActionResult.Success).isTrue
    val validationResult = (result as AuthorisableActionResult.Success).entity
    assertThat(validationResult is ValidatableActionResult.GeneralValidationError)
    val generalValidationError = validationResult as ValidatableActionResult.GeneralValidationError
    assertThat(generalValidationError.message).isEqualTo("The application has been reallocated, this assessment is read only")
  }

  @Test
  fun `rejectAssessment returns field validation error when JSON schema not satisfied by data`() {
    val user = UserEntityFactory()
      .withYieldedProbationRegion {
        ProbationRegionEntityFactory()
          .withYieldedApArea { ApAreaEntityFactory().produce() }
          .produce()
      }
      .produce()

    val schema = ApprovedPremisesAssessmentJsonSchemaEntity(
      id = UUID.randomUUID(),
      addedAt = OffsetDateTime.now(),
      schema = "{}",
    )

    val assessment = ApprovedPremisesAssessmentEntityFactory()
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
      .withAssessmentSchema(schema)
      .withData("{\"test\": \"data\"}")
      .produce()

    every { userAccessServiceMock.userCanViewAssessment(any(), any()) } returns true

    every { assessmentRepositoryMock.findByIdOrNull(assessment.id) } returns assessment

    every { jsonSchemaServiceMock.getNewestSchema(ApprovedPremisesAssessmentJsonSchemaEntity::class.java) } returns schema

    every { jsonSchemaServiceMock.validate(schema, "{\"test\": \"data\"}") } returns false

    every { assessmentRepositoryMock.save(any()) } answers { it.invocation.args[0] as ApprovedPremisesAssessmentEntity }

    every { offenderServiceMock.getOffenderByCrn(assessment.application.crn, user.deliusUsername) } returns AuthorisableActionResult.Success(
      OffenderDetailsSummaryFactory().produce(),
    )

    val result = assessmentService.rejectAssessment(user, assessment.id, "{\"test\": \"data\"}", "reasoning")

    assertThat(result is AuthorisableActionResult.Success).isTrue
    val validationResult = (result as AuthorisableActionResult.Success).entity
    assertThat(validationResult is ValidatableActionResult.FieldValidationError)
    val fieldValidationError = (validationResult as ValidatableActionResult.FieldValidationError)
    assertThat(fieldValidationError.validationMessages).contains(
      entry("$.data", "invalid"),
    )
  }

  @Test
  fun `rejectAssessment returns unauthorised when user not allowed to view Offender (LAO)`() {
    val assessmentId = UUID.randomUUID()

    val user = UserEntityFactory()
      .withYieldedProbationRegion {
        ProbationRegionEntityFactory()
          .withYieldedApArea { ApAreaEntityFactory().produce() }
          .produce()
      }
      .produce()

    val schema = ApprovedPremisesAssessmentJsonSchemaEntity(
      id = UUID.randomUUID(),
      addedAt = OffsetDateTime.now(),
      schema = "{}",
    )

    val assessment = ApprovedPremisesAssessmentEntityFactory()
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
      .withAssessmentSchema(schema)
      .withData("{\"test\": \"data\"}")
      .produce()

    every { userAccessServiceMock.userCanViewAssessment(any(), any()) } returns true

    every { assessmentRepositoryMock.findByIdOrNull(assessmentId) } returns assessment

    every { jsonSchemaServiceMock.getNewestSchema(ApprovedPremisesAssessmentJsonSchemaEntity::class.java) } returns schema

    every { jsonSchemaServiceMock.validate(schema, "{\"test\": \"data\"}") } returns true

    every { assessmentRepositoryMock.save(any()) } answers { it.invocation.args[0] as ApprovedPremisesAssessmentEntity }

    every { emailNotificationServiceMock.sendEmail(any(), any(), any()) } just Runs

    every { offenderServiceMock.getOffenderByCrn(assessment.application.crn, user.deliusUsername) } returns AuthorisableActionResult.Unauthorised()

    val result = assessmentService.rejectAssessment(user, assessmentId, "{\"test\": \"data\"}", "reasoning")

    assertThat(result is AuthorisableActionResult.Unauthorised).isTrue
  }

  @Test
  fun `rejectAssessment returns updated assessment, emits domain event, sends email`() {
    val assessmentId = UUID.randomUUID()

    val user = UserEntityFactory()
      .withYieldedProbationRegion {
        ProbationRegionEntityFactory()
          .withYieldedApArea { ApAreaEntityFactory().produce() }
          .produce()
      }
      .produce()

    val schema = ApprovedPremisesAssessmentJsonSchemaEntity(
      id = UUID.randomUUID(),
      addedAt = OffsetDateTime.now(),
      schema = "{}",
    )

    val assessment = ApprovedPremisesAssessmentEntityFactory()
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
      .withAssessmentSchema(schema)
      .withData("{\"test\": \"data\"}")
      .produce()

    every { userAccessServiceMock.userCanViewAssessment(any(), any()) } returns true

    every { assessmentRepositoryMock.findByIdOrNull(assessmentId) } returns assessment

    every { jsonSchemaServiceMock.getNewestSchema(ApprovedPremisesAssessmentJsonSchemaEntity::class.java) } returns schema

    every { jsonSchemaServiceMock.validate(schema, "{\"test\": \"data\"}") } returns true

    every { assessmentRepositoryMock.save(any()) } answers { it.invocation.args[0] as ApprovedPremisesAssessmentEntity }

    every { emailNotificationServiceMock.sendEmail(any(), any(), any()) } just Runs
    val offenderDetails = OffenderDetailsSummaryFactory()
      .withCrn(assessment.application.crn)
      .produce()

    every { offenderServiceMock.getOffenderByCrn(assessment.application.crn, user.deliusUsername, any()) } returns AuthorisableActionResult.Success(offenderDetails)

    val staffUserDetails = StaffUserDetailsFactory()
      .withProbationAreaCode("N26")
      .produce()

    every { cruServiceMock.cruNameFromProbationAreaCode("N26") } returns "South West & South Central"

    every { communityApiClientMock.getStaffUserDetails(user.deliusUsername) } returns ClientResult.Success(HttpStatus.OK, staffUserDetails)

    every { domainEventServiceMock.saveApplicationAssessedDomainEvent(any()) } just Runs

    val result = assessmentService.rejectAssessment(user, assessmentId, "{\"test\": \"data\"}", "reasoning")

    assertThat(result is AuthorisableActionResult.Success).isTrue
    val validationResult = (result as AuthorisableActionResult.Success).entity
    assertThat(validationResult is ValidatableActionResult.Success)
    val updatedAssessment = (validationResult as ValidatableActionResult.Success).entity
    assertThat(updatedAssessment.decision).isEqualTo(AssessmentDecision.REJECTED)
    assertThat(updatedAssessment.rejectionRationale).isEqualTo("reasoning")
    assertThat(updatedAssessment.submittedAt).isNotNull()
    assertThat(updatedAssessment.document).isEqualTo("{\"test\": \"data\"}")

    verify(exactly = 1) {
      domainEventServiceMock.saveApplicationAssessedDomainEvent(
        match {
          val data = it.data.eventDetails

          it.applicationId == assessment.application.id &&
            it.assessmentId == assessment.id &&
            it.crn == assessment.application.crn &&
            data.applicationId == assessment.application.id &&
            data.applicationUrl == "http://frontend/applications/${assessment.application.id}" &&
            data.personReference == PersonReference(
            crn = offenderDetails.otherIds.crn,
            noms = offenderDetails.otherIds.nomsNumber!!,
          ) &&
            data.deliusEventNumber == (assessment.application as ApprovedPremisesApplicationEntity).eventNumber &&
            data.assessedBy == ApplicationAssessedAssessedBy(
            staffMember = StaffMember(
              staffCode = staffUserDetails.staffCode,
              staffIdentifier = staffUserDetails.staffIdentifier,
              forenames = staffUserDetails.staff.forenames,
              surname = staffUserDetails.staff.surname,
              username = staffUserDetails.username,
            ),
            probationArea = ProbationArea(
              code = staffUserDetails.probationArea.code,
              name = staffUserDetails.probationArea.description,
            ),
            cru = Cru(
              name = "South West & South Central",
            ),
          ) &&
            data.decision == "REJECTED" &&
            data.decisionRationale == "reasoning"
        },
      )
    }

    verify(exactly = 1) {
      emailNotificationServiceMock.sendEmail(
        any(),
        "b3a98c60-8fe0-4450-8fd0-6430198ee43b",
        match {
          it["name"] == assessment.application.createdByUser.name &&
            (it["applicationUrl"] as String).matches(Regex("http://frontend/applications/[0-9a-fA-F]{8}\\b-[0-9a-fA-F]{4}\\b-[0-9a-fA-F]{4}\\b-[0-9a-fA-F]{4}\\b-[0-9a-fA-F]{12}"))
        },
      )
    }
  }

  @Test
  fun `rejectAssessment sets completed at timestamp to null for Temporary Accommodation`() {
    val assessmentId = UUID.randomUUID()

    val probationRegion = ProbationRegionEntityFactory()
      .withYieldedApArea { ApAreaEntityFactory().produce() }
      .produce()

    val user = UserEntityFactory()
      .withProbationRegion(probationRegion)
      .produce()

    val schema = TemporaryAccommodationAssessmentJsonSchemaEntity(
      id = UUID.randomUUID(),
      addedAt = OffsetDateTime.now(),
      schema = "{}",
    )

    val assessment = TemporaryAccommodationAssessmentEntityFactory()
      .withId(assessmentId)
      .withApplication(
        TemporaryAccommodationApplicationEntityFactory()
          .withCreatedByUser(
            UserEntityFactory()
              .withProbationRegion(probationRegion)
              .produce(),
          )
          .withProbationRegion(probationRegion)
          .produce(),
      )
      .withAllocatedToUser(user)
      .withAssessmentSchema(schema)
      .withData("{\"test\": \"data\"}")
      .produce()

    every { userAccessServiceMock.userCanViewAssessment(any(), any()) } returns true

    every { assessmentRepositoryMock.findByIdOrNull(assessmentId) } returns assessment

    every { jsonSchemaServiceMock.getNewestSchema(TemporaryAccommodationAssessmentJsonSchemaEntity::class.java) } returns schema

    every { jsonSchemaServiceMock.validate(schema, "{\"test\": \"data\"}") } returns true

    every { assessmentRepositoryMock.save(any()) } answers { it.invocation.args[0] as TemporaryAccommodationAssessmentEntity }

    val offenderDetails = OffenderDetailsSummaryFactory()
      .withCrn(assessment.application.crn)
      .produce()

    every { offenderServiceMock.getOffenderByCrn(assessment.application.crn, user.deliusUsername, any()) } returns AuthorisableActionResult.Success(offenderDetails)

    val staffUserDetails = StaffUserDetailsFactory()
      .withProbationAreaCode("N26")
      .produce()

    every { cruServiceMock.cruNameFromProbationAreaCode("N26") } returns "South West & South Central"

    every { communityApiClientMock.getStaffUserDetails(user.deliusUsername) } returns ClientResult.Success(HttpStatus.OK, staffUserDetails)

    every { userServiceMock.getUserForRequest() } returns user
    every { assessmentReferralHistoryNoteRepositoryMock.save(any()) } returnsArgument 0

    val result = assessmentService.rejectAssessment(user, assessmentId, "{\"test\": \"data\"}", "reasoning")

    assertThat(result is AuthorisableActionResult.Success).isTrue
    val validationResult = (result as AuthorisableActionResult.Success).entity
    assertThat(validationResult is ValidatableActionResult.Success)
    val updatedAssessment = (validationResult as ValidatableActionResult.Success).entity as TemporaryAccommodationAssessmentEntity
    assertThat(updatedAssessment.decision).isEqualTo(AssessmentDecision.REJECTED)
    assertThat(updatedAssessment.rejectionRationale).isEqualTo("reasoning")
    assertThat(updatedAssessment.submittedAt).isNotNull()
    assertThat(updatedAssessment.document).isEqualTo("{\"test\": \"data\"}")
    assertThat(updatedAssessment.completedAt).isNull()
    assertAssessmentHasSystemNote(assessment, user, ReferralHistorySystemNoteType.REJECTED)
  }

  @Test
  fun `closeAssessment returns unauthorised when the user does not have permission to access the assessment`() {
    val assessmentId = UUID.randomUUID()

    val probationRegion = ProbationRegionEntityFactory()
      .withYieldedApArea { ApAreaEntityFactory().produce() }
      .produce()

    val user = UserEntityFactory()
      .withProbationRegion(probationRegion)
      .produce()

    every { assessmentRepositoryMock.findByIdOrNull(assessmentId) } returns TemporaryAccommodationAssessmentEntityFactory()
      .withId(assessmentId)
      .withApplication(
        TemporaryAccommodationApplicationEntityFactory()
          .withCreatedByUser(
            UserEntityFactory()
              .withProbationRegion(probationRegion)
              .produce(),
          )
          .withProbationRegion(probationRegion)
          .produce(),
      )
      .withAllocatedToUser(
        UserEntityFactory()
          .withProbationRegion(probationRegion)
          .produce(),
      )
      .produce()

    every { userAccessServiceMock.userCanViewAssessment(any(), any()) } returns false

    every { jsonSchemaServiceMock.getNewestSchema(TemporaryAccommodationAssessmentJsonSchemaEntity::class.java) } returns TemporaryAccommodationApplicationJsonSchemaEntityFactory().produce()

    val result = assessmentService.closeAssessment(user, assessmentId)

    assertThat(result is AuthorisableActionResult.Unauthorised).isTrue
  }

  @Test
  fun `closeAssessment returns general validation error for Assessment where schema is outdated`() {
    val probationRegion = ProbationRegionEntityFactory()
      .withYieldedApArea { ApAreaEntityFactory().produce() }
      .produce()

    val user = UserEntityFactory()
      .withProbationRegion(probationRegion)
      .produce()

    val assessment = TemporaryAccommodationAssessmentEntityFactory()
      .withApplication(
        TemporaryAccommodationApplicationEntityFactory()
          .withCreatedByUser(
            UserEntityFactory()
              .withProbationRegion(probationRegion)
              .produce(),
          )
          .withProbationRegion(probationRegion)
          .produce(),
      )
      .withSubmittedAt(OffsetDateTime.now())
      .withDecision(AssessmentDecision.ACCEPTED)
      .withAllocatedToUser(user)
      .produce()

    every { userAccessServiceMock.userCanViewAssessment(any(), any()) } returns true

    every { assessmentRepositoryMock.findByIdOrNull(assessment.id) } returns assessment

    every { jsonSchemaServiceMock.getNewestSchema(TemporaryAccommodationAssessmentJsonSchemaEntity::class.java) } returns TemporaryAccommodationApplicationJsonSchemaEntityFactory().produce()

    every { offenderServiceMock.getOffenderByCrn(assessment.application.crn, user.deliusUsername) } returns AuthorisableActionResult.Success(
      OffenderDetailsSummaryFactory().produce(),
    )

    val result = assessmentService.closeAssessment(user, assessment.id)

    assertThat(result is AuthorisableActionResult.Success).isTrue
    val validationResult = (result as AuthorisableActionResult.Success).entity
    assertThat(validationResult is ValidatableActionResult.GeneralValidationError)
    val generalValidationError = validationResult as ValidatableActionResult.GeneralValidationError
    assertThat(generalValidationError.message).isEqualTo("The schema version is outdated")
  }

  @Test
  fun `closeAssessment returns general validation error for Assessment where it has already been closed`() {
    val probationRegion = ProbationRegionEntityFactory()
      .withYieldedApArea { ApAreaEntityFactory().produce() }
      .produce()

    val user = UserEntityFactory()
      .withProbationRegion(probationRegion)
      .produce()

    val schema = TemporaryAccommodationAssessmentJsonSchemaEntity(
      id = UUID.randomUUID(),
      addedAt = OffsetDateTime.now(),
      schema = "{}",
    )

    val assessment = TemporaryAccommodationAssessmentEntityFactory()
      .withApplication(
        TemporaryAccommodationApplicationEntityFactory()
          .withCreatedByUser(
            UserEntityFactory()
              .withProbationRegion(probationRegion)
              .produce(),
          )
          .withProbationRegion(probationRegion)
          .produce(),
      )
      .withSubmittedAt(OffsetDateTime.now())
      .withDecision(AssessmentDecision.ACCEPTED)
      .withAllocatedToUser(user)
      .withAssessmentSchema(schema)
      .withCompletedAt(OffsetDateTime.now())
      .produce()

    every { userAccessServiceMock.userCanViewAssessment(any(), any()) } returns true

    every { assessmentRepositoryMock.findByIdOrNull(assessment.id) } returns assessment

    every { jsonSchemaServiceMock.getNewestSchema(TemporaryAccommodationAssessmentJsonSchemaEntity::class.java) } returns schema

    every { offenderServiceMock.getOffenderByCrn(assessment.application.crn, user.deliusUsername) } returns AuthorisableActionResult.Success(
      OffenderDetailsSummaryFactory().produce(),
    )

    val result = assessmentService.closeAssessment(user, assessment.id)

    assertThat(result is AuthorisableActionResult.Success).isTrue
    val validationResult = (result as AuthorisableActionResult.Success).entity
    assertThat(validationResult is ValidatableActionResult.GeneralValidationError)
    val generalValidationError = validationResult as ValidatableActionResult.GeneralValidationError
    assertThat(generalValidationError.message).isEqualTo("This assessment has already been closed")
  }

  @Test
  fun `closeAssessment returns updated assessment`() {
    val assessmentId = UUID.randomUUID()

    val probationRegion = ProbationRegionEntityFactory()
      .withYieldedApArea { ApAreaEntityFactory().produce() }
      .produce()

    val user = UserEntityFactory()
      .withProbationRegion(probationRegion)
      .produce()

    val schema = TemporaryAccommodationAssessmentJsonSchemaEntity(
      id = UUID.randomUUID(),
      addedAt = OffsetDateTime.now(),
      schema = "{}",
    )

    val assessment = TemporaryAccommodationAssessmentEntityFactory()
      .withId(assessmentId)
      .withApplication(
        TemporaryAccommodationApplicationEntityFactory()
          .withCreatedByUser(
            UserEntityFactory()
              .withProbationRegion(probationRegion)
              .produce(),
          )
          .withProbationRegion(probationRegion)
          .produce(),
      )
      .withAllocatedToUser(user)
      .withAssessmentSchema(schema)
      .withData("{\"test\": \"data\"}")
      .produce()

    every { userAccessServiceMock.userCanViewAssessment(any(), any()) } returns true

    every { assessmentRepositoryMock.findByIdOrNull(assessmentId) } returns assessment

    every { jsonSchemaServiceMock.getNewestSchema(TemporaryAccommodationAssessmentJsonSchemaEntity::class.java) } returns schema

    every { jsonSchemaServiceMock.validate(schema, "{\"test\": \"data\"}") } returns true

    every { assessmentRepositoryMock.save(any()) } answers { it.invocation.args[0] as TemporaryAccommodationAssessmentEntity }

    val offenderDetails = OffenderDetailsSummaryFactory()
      .withCrn(assessment.application.crn)
      .produce()

    every { offenderServiceMock.getOffenderByCrn(assessment.application.crn, user.deliusUsername, any()) } returns AuthorisableActionResult.Success(offenderDetails)

    every { userServiceMock.getUserForRequest() } returns user
    every { assessmentReferralHistoryNoteRepositoryMock.save(any()) } returnsArgument 0

    val result = assessmentService.closeAssessment(user, assessmentId)

    assertThat(result is AuthorisableActionResult.Success).isTrue
    val validationResult = (result as AuthorisableActionResult.Success).entity
    assertThat(validationResult is ValidatableActionResult.Success)
    val updatedAssessment = (validationResult as ValidatableActionResult.Success).entity
    assertThat(updatedAssessment.decision).isEqualTo(AssessmentDecision.ACCEPTED)
    assertThat(updatedAssessment is TemporaryAccommodationAssessmentEntity)
    updatedAssessment as TemporaryAccommodationAssessmentEntity
    assertThat(updatedAssessment.completedAt).isNotNull()
    assertAssessmentHasSystemNote(assessment, user, ReferralHistorySystemNoteType.COMPLETED)
  }

  @Test
  fun `reallocateAssessment for Approved Premises returns General Validation Error when application already has a submitted assessment`() {
    val assigneeUser = UserEntityFactory()
      .withYieldedProbationRegion {
        ProbationRegionEntityFactory()
          .withYieldedApArea { ApAreaEntityFactory().produce() }
          .produce()
      }
      .produce()

    val application = ApprovedPremisesApplicationEntityFactory()
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

    val previousAssessment = ApprovedPremisesAssessmentEntityFactory()
      .withApplication(application)
      .withAllocatedToUser(
        UserEntityFactory()
          .withYieldedProbationRegion {
            ProbationRegionEntityFactory()
              .withYieldedApArea { ApAreaEntityFactory().produce() }
              .produce()
          }
          .produce(),
      )
      .withSubmittedAt(OffsetDateTime.now())
      .produce()

    every { assessmentRepositoryMock.findByIdOrNull(previousAssessment.id) } returns previousAssessment

    val result = assessmentService.reallocateAssessment(assigneeUser, previousAssessment.id)

    assertThat(result is AuthorisableActionResult.Success).isTrue
    val validationResult = (result as AuthorisableActionResult.Success).entity

    assertThat(validationResult is ValidatableActionResult.GeneralValidationError).isTrue
    validationResult as ValidatableActionResult.GeneralValidationError
    assertThat(validationResult.message).isEqualTo("A decision has already been taken on this assessment")
  }

  @Test
  fun `reallocateAssessment for Approved Premises returns Field Validation Error when user to assign to is not an ASSESSOR`() {
    val assigneeUser = UserEntityFactory()
      .withYieldedProbationRegion {
        ProbationRegionEntityFactory()
          .withYieldedApArea { ApAreaEntityFactory().produce() }
          .produce()
      }
      .produce()

    val application = ApprovedPremisesApplicationEntityFactory()
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

    val previousAssessment = ApprovedPremisesAssessmentEntityFactory()
      .withApplication(application)
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

    every { assessmentRepositoryMock.findByIdOrNull(previousAssessment.id) } returns previousAssessment

    val result = assessmentService.reallocateAssessment(assigneeUser, previousAssessment.id)

    assertThat(result is AuthorisableActionResult.Success).isTrue
    val validationResult = (result as AuthorisableActionResult.Success).entity

    assertThat(validationResult is ValidatableActionResult.FieldValidationError).isTrue
    validationResult as ValidatableActionResult.FieldValidationError
    assertThat(validationResult.validationMessages).containsEntry("$.userId", "lackingAssessorRole")
  }

  @Test
  fun `reallocateAssessment for Approved Premises returns Field Validation Error when user to assign to does not have relevant qualifications`() {
    val assigneeUser = UserEntityFactory()
      .withYieldedProbationRegion {
        ProbationRegionEntityFactory()
          .withYieldedApArea { ApAreaEntityFactory().produce() }
          .produce()
      }
      .produce()
      .apply {
        roles += UserRoleAssignmentEntityFactory()
          .withUser(this)
          .withRole(UserRole.CAS1_ASSESSOR)
          .produce()
      }

    val application = ApprovedPremisesApplicationEntityFactory()
      .withCreatedByUser(
        UserEntityFactory()
          .withYieldedProbationRegion {
            ProbationRegionEntityFactory()
              .withYieldedApArea { ApAreaEntityFactory().produce() }
              .produce()
          }
          .produce(),
      )
      .withIsPipeApplication(true)
      .produce()

    val previousAssessment = ApprovedPremisesAssessmentEntityFactory()
      .withApplication(application)
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

    every { assessmentRepositoryMock.findByIdOrNull(previousAssessment.id) } returns previousAssessment

    val result = assessmentService.reallocateAssessment(assigneeUser, previousAssessment.id)

    assertThat(result is AuthorisableActionResult.Success).isTrue
    val validationResult = (result as AuthorisableActionResult.Success).entity

    assertThat(validationResult is ValidatableActionResult.FieldValidationError).isTrue
    validationResult as ValidatableActionResult.FieldValidationError
    assertThat(validationResult.validationMessages).containsEntry("$.userId", "lackingQualifications")
  }

  @Test
  fun `reallocateAssessment for Approved Premises returns Success, deallocates old assessment and creates a new one, sends allocation email & deallocation email`() {
    val assigneeUser = UserEntityFactory()
      .withYieldedProbationRegion {
        ProbationRegionEntityFactory()
          .withYieldedApArea { ApAreaEntityFactory().produce() }
          .produce()
      }
      .produce()
      .apply {
        roles += UserRoleAssignmentEntityFactory()
          .withUser(this)
          .withRole(UserRole.CAS1_ASSESSOR)
          .produce()

        qualifications += UserQualificationAssignmentEntityFactory()
          .withUser(this)
          .withQualification(UserQualification.PIPE)
          .produce()
      }

    val application = ApprovedPremisesApplicationEntityFactory()
      .withCreatedByUser(
        UserEntityFactory()
          .withYieldedProbationRegion {
            ProbationRegionEntityFactory()
              .withYieldedApArea { ApAreaEntityFactory().produce() }
              .produce()
          }
          .produce(),
      )
      .withIsPipeApplication(true)
      .produce()

    val previousAssessment = ApprovedPremisesAssessmentEntityFactory()
      .withApplication(application)
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

    every { assessmentRepositoryMock.findByIdOrNull(previousAssessment.id) } returns previousAssessment

    every { jsonSchemaServiceMock.getNewestSchema(ApprovedPremisesAssessmentJsonSchemaEntity::class.java) } returns ApprovedPremisesAssessmentJsonSchemaEntity(
      id = UUID.randomUUID(),
      addedAt = OffsetDateTime.now(),
      schema = "{}",
    )

    every { assessmentRepositoryMock.save(any()) } answers { it.invocation.args[0] as ApprovedPremisesAssessmentEntity }

    every { emailNotificationServiceMock.sendEmail(any(), any(), any()) } just Runs

    val result = assessmentService.reallocateAssessment(assigneeUser, previousAssessment.id)

    assertThat(result is AuthorisableActionResult.Success).isTrue
    val validationResult = (result as AuthorisableActionResult.Success).entity

    assertThat(validationResult is ValidatableActionResult.Success).isTrue
    validationResult as ValidatableActionResult.Success

    assertThat(previousAssessment.reallocatedAt).isNotNull

    verify { assessmentRepositoryMock.save(match { it.allocatedToUser == assigneeUser }) }
    verify(exactly = 1) {
      emailNotificationServiceMock.sendEmail(
        match { it == assigneeUser.email },
        "f3d78814-383f-4b5f-a681-9bd3ab912888",
        match {
          it["name"] == assigneeUser.name &&
            (it["assessmentUrl"] as String).matches(Regex("http://frontend/assessments/[0-9a-fA-F]{8}\\b-[0-9a-fA-F]{4}\\b-[0-9a-fA-F]{4}\\b-[0-9a-fA-F]{4}\\b-[0-9a-fA-F]{12}"))
        },
      )
    }
    verify(exactly = 1) {
      emailNotificationServiceMock.sendEmail(
        match { it == previousAssessment.allocatedToUser!!.email },
        "331ce452-ea83-4f0c-aec0-5eafe85094f2",
        match {
          it["name"] == previousAssessment.allocatedToUser!!.name &&
            (it["assessmentUrl"] as String).matches(Regex("http://frontend/assessments/[0-9a-fA-F]{8}\\b-[0-9a-fA-F]{4}\\b-[0-9a-fA-F]{4}\\b-[0-9a-fA-F]{4}\\b-[0-9a-fA-F]{12}"))
        },
      )
    }
  }

  @Test
  fun `reallocateAssessment for Temporary Accommodation returns Field Validation Error when user to assign to is not an ASSESSOR`() {
    val probationRegion = ProbationRegionEntityFactory()
      .withYieldedApArea { ApAreaEntityFactory().produce() }
      .produce()

    val assigneeUser = UserEntityFactory()
      .withProbationRegion(probationRegion)
      .produce()

    val application = TemporaryAccommodationApplicationEntityFactory()
      .withCreatedByUser(
        UserEntityFactory()
          .withProbationRegion(probationRegion)
          .produce(),
      )
      .withProbationRegion(probationRegion)
      .produce()

    val previousAssessment = TemporaryAccommodationAssessmentEntityFactory()
      .withApplication(application)
      .withAllocatedToUser(
        UserEntityFactory()
          .withProbationRegion(probationRegion)
          .produce(),
      )
      .produce()

    every { assessmentRepositoryMock.findByIdOrNull(previousAssessment.id) } returns previousAssessment

    val result = assessmentService.reallocateAssessment(assigneeUser, previousAssessment.id)

    assertThat(result is AuthorisableActionResult.Success).isTrue
    val validationResult = (result as AuthorisableActionResult.Success).entity

    assertThat(validationResult is ValidatableActionResult.FieldValidationError).isTrue
    validationResult as ValidatableActionResult.FieldValidationError
    assertThat(validationResult.validationMessages).containsEntry("$.userId", "lackingAssessorRole")
  }

  @Test
  fun `reallocateAssessment for Temporary Accommodation returns Success and updates the assigned user in place`() {
    val probationRegion = ProbationRegionEntityFactory()
      .withYieldedApArea { ApAreaEntityFactory().produce() }
      .produce()

    val assigneeUser = UserEntityFactory()
      .withProbationRegion(probationRegion)
      .produce()
      .apply {
        roles += UserRoleAssignmentEntityFactory()
          .withUser(this)
          .withRole(UserRole.CAS3_ASSESSOR)
          .produce()
      }

    val application = TemporaryAccommodationApplicationEntityFactory()
      .withCreatedByUser(
        UserEntityFactory()
          .withProbationRegion(probationRegion)
          .produce(),
      )
      .withProbationRegion(probationRegion)
      .produce()

    val previousAssessment = TemporaryAccommodationAssessmentEntityFactory()
      .withApplication(application)
      .withAllocatedToUser(
        UserEntityFactory()
          .withProbationRegion(probationRegion)
          .produce(),
      )
      .produce()

    every { assessmentRepositoryMock.findByIdOrNull(previousAssessment.id) } returns previousAssessment

    every { jsonSchemaServiceMock.getNewestSchema(TemporaryAccommodationAssessmentJsonSchemaEntity::class.java) } returns TemporaryAccommodationAssessmentJsonSchemaEntity(
      id = UUID.randomUUID(),
      addedAt = OffsetDateTime.now(),
      schema = "{}",
    )

    every { assessmentRepositoryMock.save(any()) } answers { it.invocation.args[0] as TemporaryAccommodationAssessmentEntity }

    every { userServiceMock.getUserForRequest() } returns assigneeUser
    every { assessmentReferralHistoryNoteRepositoryMock.save(any()) } returnsArgument 0

    val result = assessmentService.reallocateAssessment(assigneeUser, previousAssessment.id)

    assertThat(result is AuthorisableActionResult.Success).isTrue
    val validationResult = (result as AuthorisableActionResult.Success).entity

    assertThat(validationResult is ValidatableActionResult.Success).isTrue
    validationResult as ValidatableActionResult.Success

    assertThat(validationResult.entity).isEqualTo(previousAssessment)
    assertAssessmentHasSystemNote(validationResult.entity, assigneeUser, ReferralHistorySystemNoteType.IN_REVIEW)

    verify { assessmentRepositoryMock.save(match { it.allocatedToUser == assigneeUser }) }
  }

  @Test
  fun `deallocateAssessment throws an exception when the assessment is not a Temporary Accommodation assessment`() {
    val application = ApprovedPremisesApplicationEntityFactory()
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

    val previousAssessment = ApprovedPremisesAssessmentEntityFactory()
      .withApplication(application)
      .withAllocatedToUser(
        UserEntityFactory()
          .withYieldedProbationRegion {
            ProbationRegionEntityFactory()
              .withYieldedApArea { ApAreaEntityFactory().produce() }
              .produce()
          }
          .produce(),
      )
      .withSubmittedAt(OffsetDateTime.now())
      .produce()

    every { assessmentRepositoryMock.findByIdOrNull(previousAssessment.id) } returns previousAssessment

    assertThatThrownBy { assessmentService.deallocateAssessment(previousAssessment.id) }
      .isInstanceOf(RuntimeException::class.java)
      .hasMessage("Only CAS3 Assessments are currently supported")
  }

  @Test
  fun `deallocateAssessment returns a NotFound error if the assessment could not be found`() {
    every { assessmentRepositoryMock.findByIdOrNull(any()) } returns null

    val result = assessmentService.deallocateAssessment(UUID.randomUUID())

    assertThat(result is AuthorisableActionResult.NotFound).isTrue
  }

  @Test
  fun `deallocateAssessment returns Success, removes the assigned user in place, and unsets any decision made`() {
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

    val previousAssessment = TemporaryAccommodationAssessmentEntityFactory()
      .withApplication(application)
      .withAllocatedToUser(
        UserEntityFactory()
          .withProbationRegion(probationRegion)
          .produce(),
      )
      .withDecision(AssessmentDecision.REJECTED)
      .produce()

    every { assessmentRepositoryMock.findByIdOrNull(previousAssessment.id) } returns previousAssessment

    every { jsonSchemaServiceMock.getNewestSchema(TemporaryAccommodationAssessmentJsonSchemaEntity::class.java) } returns TemporaryAccommodationAssessmentJsonSchemaEntity(
      id = UUID.randomUUID(),
      addedAt = OffsetDateTime.now(),
      schema = "{}",
    )

    every { assessmentRepositoryMock.save(any()) } answers { it.invocation.args[0] as TemporaryAccommodationAssessmentEntity }

    every { userServiceMock.getUserForRequest() } returns user
    every { assessmentReferralHistoryNoteRepositoryMock.save(any()) } returnsArgument 0

    val result = assessmentService.deallocateAssessment(previousAssessment.id)

    assertThat(result is AuthorisableActionResult.Success).isTrue
    val validationResult = (result as AuthorisableActionResult.Success).entity

    assertThat(validationResult is ValidatableActionResult.Success).isTrue
    validationResult as ValidatableActionResult.Success

    assertThat(validationResult.entity).isEqualTo(previousAssessment)
    assertAssessmentHasSystemNote(validationResult.entity, user, ReferralHistorySystemNoteType.UNALLOCATED)

    verify {
      assessmentRepositoryMock.save(
        match {
          it.allocatedToUser == null &&
            it.decision == null &&
            it.submittedAt == null
        },
      )
    }
  }

  @Nested
  inner class UpdateAssessmentClarificationNote {
    private val userServiceMock = mockk<UserService>()
    private val userAccessServiceMock = mockk<UserAccessService>()
    private val assessmentRepositoryMock = mockk<AssessmentRepository>()
    private val assessmentClarificationNoteRepositoryMock = mockk<AssessmentClarificationNoteRepository>()
    private val assessmentReferralHistoryNoteRepositoryMock = mockk<AssessmentReferralHistoryNoteRepository>()
    private val jsonSchemaServiceMock = mockk<JsonSchemaService>()
    private val domainEventServiceMock = mockk<DomainEventService>()
    private val offenderServiceMock = mockk<OffenderService>()
    private val communityApiClientMock = mockk<CommunityApiClient>()
    private val cruServiceMock = mockk<CruService>()
    private val placementRequestServiceMock = mockk<PlacementRequestService>()
    private val emailNotificationServiceMock = mockk<EmailNotificationService>()
    private val placementRequirementsServiceMock = mockk<PlacementRequirementsService>()
    private val userAllocatorMock = mockk<UserAllocator>()
    private val objectMapperMock = spyk<ObjectMapper>()

    private val assessmentService = AssessmentService(
      userServiceMock,
      userAccessServiceMock,
      assessmentRepositoryMock,
      assessmentClarificationNoteRepositoryMock,
      assessmentReferralHistoryNoteRepositoryMock,
      jsonSchemaServiceMock,
      domainEventServiceMock,
      offenderServiceMock,
      communityApiClientMock,
      cruServiceMock,
      placementRequestServiceMock,
      emailNotificationServiceMock,
      NotifyConfig(),
      placementRequirementsServiceMock,
      userAllocatorMock,
      objectMapperMock,
      "http://frontend/applications/#id",
      "http://frontend/assessments/#id",
    )

    private val user = UserEntityFactory()
      .withYieldedProbationRegion {
        ProbationRegionEntityFactory()
          .withYieldedApArea { ApAreaEntityFactory().produce() }
          .produce()
      }
      .produce()

    private val apSchema = ApprovedPremisesAssessmentJsonSchemaEntity(
      id = UUID.randomUUID(),
      addedAt = OffsetDateTime.now(),
      schema = "{}",
    )

    private val taSchema = TemporaryAccommodationAssessmentJsonSchemaEntity(
      id = UUID.randomUUID(),
      addedAt = OffsetDateTime.now(),
      schema = "{}",
    )

    private val assessment = ApprovedPremisesAssessmentEntityFactory()
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
      .withAssessmentSchema(apSchema)
      .withData("{\"test\": \"data\"}")
      .produce()

    private val assessmentClarificationNoteEntity = AssessmentClarificationNoteEntityFactory()
      .withAssessment(assessment)
      .withCreatedBy(user)
      .produce()

    @BeforeEach
    fun setup() {
      every { jsonSchemaServiceMock.getNewestSchema(ApprovedPremisesAssessmentJsonSchemaEntity::class.java) } returns apSchema
      every { jsonSchemaServiceMock.getNewestSchema(TemporaryAccommodationAssessmentJsonSchemaEntity::class.java) } returns taSchema

      every { jsonSchemaServiceMock.validate(apSchema, "{\"test\": \"data\"}") } returns true
    }

    @Test
    fun `updateAssessmentClarificationNote returns updated clarification note`() {
      every { userAccessServiceMock.userCanViewAssessment(any(), any()) } returns true
      every { assessmentRepositoryMock.findByIdOrNull(assessment.id) } returns assessment

      every {
        assessmentClarificationNoteRepositoryMock.findByAssessmentIdAndId(
          assessment.id,
          assessmentClarificationNoteEntity.id,
        )
      } returns assessmentClarificationNoteEntity

      every { assessmentClarificationNoteRepositoryMock.save(any()) } answers { it.invocation.args[0] as AssessmentClarificationNoteEntity }
      every { assessmentRepositoryMock.save(any()) } answers { it.invocation.args[0] as AssessmentEntity }

      every { offenderServiceMock.getOffenderByCrn(assessment.application.crn, user.deliusUsername) } returns AuthorisableActionResult.Success(
        OffenderDetailsSummaryFactory().produce(),
      )

      val result = assessmentService.updateAssessmentClarificationNote(
        user,
        assessment.id,
        assessmentClarificationNoteEntity.id,
        "Some response",
        LocalDate.parse("2022-03-03"),
      )

      assertThat(result is AuthorisableActionResult.Success).isTrue

      val validationResult = (result as AuthorisableActionResult.Success).entity
      assertThat(validationResult is ValidatableActionResult.Success)

      val updatedAssessmentClarificationNote = (validationResult as ValidatableActionResult.Success).entity
      assertThat(updatedAssessmentClarificationNote.response contentEquals "Some response")
    }

    @Test
    fun `updateAssessmentClarificationNote returns not found if the note is not found`() {
      every { userAccessServiceMock.userCanViewAssessment(any(), any()) } returns true
      every { assessmentRepositoryMock.findByIdOrNull(assessment.id) } returns assessment
      every {
        assessmentClarificationNoteRepositoryMock.findByAssessmentIdAndId(
          assessment.id,
          assessmentClarificationNoteEntity.id,
        )
      } returns null

      every { offenderServiceMock.getOffenderByCrn(assessment.application.crn, user.deliusUsername) } returns AuthorisableActionResult.Success(
        OffenderDetailsSummaryFactory().produce(),
      )

      val result = assessmentService.updateAssessmentClarificationNote(
        user,
        assessment.id,
        assessmentClarificationNoteEntity.id,
        "Some response",
        LocalDate.parse("2022-03-03"),
      )

      assertThat(result is AuthorisableActionResult.NotFound).isTrue
    }

    @Test
    fun `updateAssessmentClarificationNote returns an error if the note already has a response`() {
      every { userAccessServiceMock.userCanViewAssessment(any(), any()) } returns true
      every { assessmentRepositoryMock.findByIdOrNull(assessment.id) } returns assessment
      every {
        assessmentClarificationNoteRepositoryMock.findByAssessmentIdAndId(
          assessment.id,
          assessmentClarificationNoteEntity.id,
        )
      } returns AssessmentClarificationNoteEntityFactory()
        .withAssessment(assessment)
        .withCreatedBy(user)
        .withResponse("I already have a response!")
        .produce()

      every { offenderServiceMock.getOffenderByCrn(assessment.application.crn, user.deliusUsername) } returns AuthorisableActionResult.Success(
        OffenderDetailsSummaryFactory().produce(),
      )

      val result = assessmentService.updateAssessmentClarificationNote(
        user,
        assessment.id,
        assessmentClarificationNoteEntity.id,
        "Some response",
        LocalDate.parse("2022-03-03"),
      )

      assertThat(result is AuthorisableActionResult.Success).isTrue

      val validationResult = (result as AuthorisableActionResult.Success).entity
      assertThat(validationResult is ValidatableActionResult.GeneralValidationError)

      val generalValidationError = validationResult as ValidatableActionResult.GeneralValidationError
      assertThat(generalValidationError.message).isEqualTo("A response has already been added to this note")
    }

    @Test
    fun `updateAssessmentClarificationNote returns unauthorised if the note is not owned by the requesting user`() {
      every { userAccessServiceMock.userCanViewAssessment(any(), any()) } returns true
      every { assessmentRepositoryMock.findByIdOrNull(assessment.id) } returns assessment
      every {
        assessmentClarificationNoteRepositoryMock.findByAssessmentIdAndId(
          assessment.id,
          assessmentClarificationNoteEntity.id,
        )
      } returns AssessmentClarificationNoteEntityFactory()
        .withAssessment(assessment)
        .withCreatedBy(
          UserEntityFactory()
            .withYieldedProbationRegion {
              ProbationRegionEntityFactory()
                .withYieldedApArea { ApAreaEntityFactory().produce() }
                .produce()
            }
            .produce(),
        )
        .produce()

      every { offenderServiceMock.getOffenderByCrn(assessment.application.crn, user.deliusUsername) } returns AuthorisableActionResult.Success(
        OffenderDetailsSummaryFactory().produce(),
      )

      val result = assessmentService.updateAssessmentClarificationNote(
        user,
        assessment.id,
        assessmentClarificationNoteEntity.id,
        "Some response",
        LocalDate.parse("2022-03-03"),
      )

      assertThat(result is AuthorisableActionResult.Unauthorised).isTrue
    }

    @Test
    fun `createApprovedPremisesAssessment creates an Assessment and sends allocation email`() {
      val userWithLeastAllocatedAssessments = UserEntityFactory()
        .withYieldedProbationRegion {
          ProbationRegionEntityFactory()
            .withYieldedApArea { ApAreaEntityFactory().produce() }
            .produce()
        }
        .produce()
        .apply {
          roles += UserRoleAssignmentEntityFactory()
            .withUser(this)
            .withRole(UserRole.CAS1_ASSESSOR)
            .produce()

          qualifications += UserQualificationAssignmentEntityFactory()
            .withUser(this)
            .withQualification(UserQualification.PIPE)
            .produce()
        }

      val application = ApprovedPremisesApplicationEntityFactory()
        .withCreatedByUser(
          UserEntityFactory()
            .withYieldedProbationRegion {
              ProbationRegionEntityFactory()
                .withYieldedApArea { ApAreaEntityFactory().produce() }
                .produce()
            }
            .produce(),
        )
        .withIsPipeApplication(true)
        .produce()

      every { assessmentRepositoryMock.save(any()) } answers { it.invocation.args[0] as ApprovedPremisesAssessmentEntity }

      every { userAllocatorMock.getUserForAssessmentAllocation(any()) } returns userWithLeastAllocatedAssessments

      every { emailNotificationServiceMock.sendEmail(any(), any(), any()) } just Runs

      assessmentService.createApprovedPremisesAssessment(application)

      verify { assessmentRepositoryMock.save(match { it.allocatedToUser == userWithLeastAllocatedAssessments }) }
      verify(exactly = 1) {
        emailNotificationServiceMock.sendEmail(
          any(),
          "f3d78814-383f-4b5f-a681-9bd3ab912888",
          match {
            it["name"] == userWithLeastAllocatedAssessments.name &&
              (it["assessmentUrl"] as String).matches(Regex("http://frontend/assessments/[0-9a-fA-F]{8}\\b-[0-9a-fA-F]{4}\\b-[0-9a-fA-F]{4}\\b-[0-9a-fA-F]{4}\\b-[0-9a-fA-F]{12}"))
          },
        )
      }
    }

    @Test
    fun `createTemporaryAccommodationAssessment creates an Assessment`() {
      val probationRegion = ProbationRegionEntityFactory()
        .withYieldedApArea { ApAreaEntityFactory().produce() }
        .produce()

      val application = TemporaryAccommodationApplicationEntityFactory()
        .withCreatedByUser(
          UserEntityFactory()
            .withProbationRegion(probationRegion)
            .produce(),
        )
        .withProbationRegion(probationRegion)
        .produce()

      every { assessmentRepositoryMock.save(any()) } answers { it.invocation.args[0] as TemporaryAccommodationAssessmentEntity }

      every { userServiceMock.getUserForRequest() } returns user
      every { assessmentReferralHistoryNoteRepositoryMock.save(any()) } returnsArgument 0

      val summaryData = object {
        val num = 50
        val text = "Hello world!"
      }

      val result = assessmentService.createTemporaryAccommodationAssessment(application, summaryData)

      assertAssessmentHasSystemNote(result, user, ReferralHistorySystemNoteType.SUBMITTED)
      assertThat(result.summaryData).isEqualTo("{\"num\":50,\"text\":\"Hello world!\"}")

      verify { assessmentRepositoryMock.save(match { it.application == application }) }
    }
  }
}
