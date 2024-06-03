package uk.gov.justice.digital.hmpps.approvedpremisesapi.unit.service

import com.fasterxml.jackson.databind.ObjectMapper
import io.mockk.Called
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
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import org.junit.jupiter.params.provider.ValueSource
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
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas1ApplicationTimelinessCategory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ServiceName
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ServiceName.temporaryAccommodation
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
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.ReferralRejectionReasonEntityFactory
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
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ReferralRejectionReasonRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.TemporaryAccommodationAssessmentEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.TemporaryAccommodationAssessmentJsonSchemaEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserQualification
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserRole
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.ApprovedPremisesType
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
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.TaskDeadlineService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.UserAccessService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.UserService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1.Cas1AssessmentDomainEventService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1.Cas1AssessmentEmailService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.unit.util.assertAssessmentHasSystemNote
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.PageCriteria
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.UrlTemplate
import java.time.LocalDate
import java.time.OffsetDateTime
import java.util.UUID

class AssessmentServiceTest {
  private val userServiceMock = mockk<UserService>()
  private val userAccessServiceMock = mockk<UserAccessService>()
  private val assessmentRepositoryMock = mockk<AssessmentRepository>()
  private val assessmentClarificationNoteRepositoryMock = mockk<AssessmentClarificationNoteRepository>()
  private val assessmentReferralHistoryNoteRepositoryMock = mockk<AssessmentReferralHistoryNoteRepository>()
  private val referralRejectionReasonRepositoryMock = mockk<ReferralRejectionReasonRepository>()
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
  private val taskDeadlineServiceMock = mockk<TaskDeadlineService>()
  private val assessmentEmailServiceMock = mockk<Cas1AssessmentEmailService>()
  private val cas1AssessmentDomainEventService = mockk<Cas1AssessmentDomainEventService>()

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
    communityApiClientMock,
    cruServiceMock,
    placementRequestServiceMock,
    emailNotificationServiceMock,
    NotifyConfig(),
    placementRequirementsServiceMock,
    userAllocatorMock,
    objectMapperMock,
    UrlTemplate("http://frontend/applications/#id"),
    taskDeadlineServiceMock,
    assessmentEmailServiceMock,
    cas1AssessmentDomainEventService,
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
    val pageCriteria = PageCriteria(
      sortBy = AssessmentSortField.assessmentStatus,
      sortDirection = SortDirection.asc,
      page = 5,
      perPage = 7,
    )
    val pageRequest = PageRequest.of(4, 7, Sort.by("status").ascending())

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

    every {
      assessmentRepositoryMock.findTemporaryAccommodationAssessmentSummariesForRegionAndCrnAndStatus(
        any(),
        null,
        emptyList(),
        pageRequest,
      )
    } returns Page.empty()

    assessmentService.getAssessmentSummariesForUserCAS3(user, null, temporaryAccommodation, emptyList(), pageCriteria)

    verify(exactly = 1) {
      assessmentRepositoryMock.findTemporaryAccommodationAssessmentSummariesForRegionAndCrnAndStatus(
        user.probationRegion.id,
        null,
        emptyList(),
        pageRequest,
      )
    }
  }

  @Test
  fun `getAssessmentSummariesByCrnForUser is not supported for Approved Premises`() {
    val pageCriteria = PageCriteria(
      sortBy = AssessmentSortField.assessmentStatus,
      sortDirection = SortDirection.asc,
      page = 5,
      perPage = 7,
    )

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
      .isThrownBy { assessmentService.getAssessmentSummariesForUserCAS3(user, "SOMECRN", ServiceName.approvedPremises, emptyList(), pageCriteria) }
      .withMessage("Only CAS3 assessments are currently supported")
  }

  @Test
  fun `getAssessmentSummariesByCrnForUser only fetches Temporary Accommodation assessments for the given CRN and within the user's probation region`() {
    val pageCriteria = PageCriteria(
      sortBy = AssessmentSortField.assessmentStatus,
      sortDirection = SortDirection.asc,
      page = 5,
      perPage = 7,
    )
    val pageRequest = PageRequest.of(4, 7, Sort.by("status").ascending())

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

    every {
      assessmentRepositoryMock.findTemporaryAccommodationAssessmentSummariesForRegionAndCrnAndStatus(
        user.probationRegion.id,
        "SOMECRN",
        emptyList(),
        pageRequest,
      )
    } returns Page.empty()

    assessmentService.getAssessmentSummariesForUserCAS3(user, "SOMECRN", temporaryAccommodation, emptyList(), pageCriteria)

    verify(exactly = 1) {
      assessmentRepositoryMock.findTemporaryAccommodationAssessmentSummariesForRegionAndCrnAndStatus(
        user.probationRegion.id,
        "SOMECRN",
        emptyList(),
        pageRequest,
      )
    }
  }

  @Test
  fun `getAssessmentSummariesForUserCAS3 only fetches Temporary Accommodation assessments sorted by default arrivalDate when requested sort field is personName`() {
    val pageCriteria = PageCriteria(
      sortBy = AssessmentSortField.personName,
      sortDirection = SortDirection.asc,
      page = 5,
      perPage = 7,
    )
    val pageRequest = PageRequest.of(4, 7, Sort.by("arrivalDate").ascending())

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

    every {
      assessmentRepositoryMock.findTemporaryAccommodationAssessmentSummariesForRegionAndCrnAndStatus(
        user.probationRegion.id,
        "SOMECRN",
        listOf(),
        pageRequest,
      )
    } returns Page.empty()

    assessmentService.getAssessmentSummariesForUserCAS3(user, "SOMECRN", temporaryAccommodation, emptyList(), pageCriteria)

    verify(exactly = 1) {
      assessmentRepositoryMock.findTemporaryAccommodationAssessmentSummariesForRegionAndCrnAndStatus(
        user.probationRegion.id,
        "SOMECRN",
        emptyList(),
        pageRequest,
      )
    }
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

  @Nested
  inner class AddAssessmentClarificationNote {
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

      every { cas1AssessmentDomainEventService.furtherInformationRequested(any(), any()) } just Runs

      val text = "clarification note"
      val result = assessmentService.addAssessmentClarificationNote(user, assessment.id, text)

      assertThat(result is AuthorisableActionResult.Success).isTrue
      result as AuthorisableActionResult.Success

      verify(exactly = 1) {
        assessmentClarificationNoteRepositoryMock.save(
          match {
            it.assessment == assessment &&
              it.createdByUser == user &&
              it.query == text
          },
        )
      }

      verify(exactly = 1) {
        cas1AssessmentDomainEventService.furtherInformationRequested(assessment, result.entity)
      }
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

      every { cas1AssessmentDomainEventService.furtherInformationRequested(any(), any()) } just Runs

      val text = "clarification note"
      val result = assessmentService.addAssessmentClarificationNote(user, assessment.id, text)

      assertThat(result is AuthorisableActionResult.Success).isTrue
      result as AuthorisableActionResult.Success

      verify(exactly = 1) {
        assessmentClarificationNoteRepositoryMock.save(
          match {
            it.assessment == assessment &&
              it.createdByUser == user &&
              it.query == text
          },
        )
      }

      verify(exactly = 1) {
        cas1AssessmentDomainEventService.furtherInformationRequested(assessment, result.entity)
      }
    }
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
            it.nomsNumber == offenderDetails.otherIds.nomsNumber &&
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
    val referralRejectionReasonId = UUID.randomUUID()

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

    val referralRejectionReason = ReferralRejectionReasonEntityFactory()
      .withId(referralRejectionReasonId)
      .produce()

    every { userAccessServiceMock.userCanViewAssessment(any(), any()) } returns true

    every { assessmentRepositoryMock.findByIdOrNull(assessmentId) } returns assessment

    every { jsonSchemaServiceMock.getNewestSchema(TemporaryAccommodationAssessmentJsonSchemaEntity::class.java) } returns schema

    every { jsonSchemaServiceMock.validate(schema, "{\"test\": \"data\"}") } returns true

    every { assessmentRepositoryMock.save(any()) } answers { it.invocation.args[0] as TemporaryAccommodationAssessmentEntity }

    every { referralRejectionReasonRepositoryMock.findByIdOrNull(referralRejectionReasonId) } returns referralRejectionReason

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

    val result = assessmentService.rejectAssessment(user, assessmentId, "{\"test\": \"data\"}", "reasoning", referralRejectionReasonId, "referral rejection reason detail", false)

    assertThat(result is AuthorisableActionResult.Success).isTrue
    val validationResult = (result as AuthorisableActionResult.Success).entity
    assertThat(validationResult is ValidatableActionResult.Success)
    val updatedAssessment = (validationResult as ValidatableActionResult.Success).entity as TemporaryAccommodationAssessmentEntity
    assertThat(updatedAssessment.decision).isEqualTo(AssessmentDecision.REJECTED)
    assertThat(updatedAssessment.rejectionRationale).isEqualTo("reasoning")
    assertThat(updatedAssessment.submittedAt).isNotNull()
    assertThat(updatedAssessment.document).isEqualTo("{\"test\": \"data\"}")
    assertThat(updatedAssessment.referralRejectionReason?.id).isEqualTo(referralRejectionReasonId)
    assertThat(updatedAssessment.referralRejectionReasonDetail).isEqualTo("referral rejection reason detail")
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

  @Nested
  inner class ReallocateAssessment {
    private val application = ApprovedPremisesApplicationEntityFactory()
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

    private val previousAssessment = ApprovedPremisesAssessmentEntityFactory()
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

    private val assigneeUser = UserEntityFactory()
      .withYieldedProbationRegion {
        ProbationRegionEntityFactory()
          .withYieldedApArea { ApAreaEntityFactory().produce() }
          .produce()
      }
      .produce()

    @Test
    fun `reallocateAssessment for Approved Premises returns General Validation Error when application already has a submitted assessment`() {
      previousAssessment.apply {
        submittedAt = OffsetDateTime.now()
      }

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
      assigneeUser.apply {
        roles = mutableListOf()
      }

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

      assigneeUser.apply {
        roles += UserRoleAssignmentEntityFactory()
          .withUser(this)
          .withRole(UserRole.CAS1_ASSESSOR)
          .produce()
      }

      application.apply {
        apType = ApprovedPremisesType.PIPE
      }

      every { assessmentRepositoryMock.findByIdOrNull(previousAssessment.id) } returns previousAssessment

      val result = assessmentService.reallocateAssessment(assigneeUser, previousAssessment.id)

      assertThat(result is AuthorisableActionResult.Success).isTrue
      val validationResult = (result as AuthorisableActionResult.Success).entity

      assertThat(validationResult is ValidatableActionResult.FieldValidationError).isTrue
      validationResult as ValidatableActionResult.FieldValidationError
      assertThat(validationResult.validationMessages).containsEntry("$.userId", "lackingQualifications")
    }

    @Test
    fun `reallocateAssessment for Approved Premises returns Conflict Error when assessment has already been reallocated`() {
      previousAssessment.apply {
        reallocatedAt = OffsetDateTime.now()
      }

      every { assessmentRepositoryMock.findByIdOrNull(previousAssessment.id) } returns previousAssessment

      val result = assessmentService.reallocateAssessment(assigneeUser, previousAssessment.id)

      assertThat(result is AuthorisableActionResult.Success).isTrue
      val validationResult = (result as AuthorisableActionResult.Success).entity

      assertThat(validationResult is ValidatableActionResult.ConflictError).isTrue
      validationResult as ValidatableActionResult.ConflictError
      assertThat(validationResult.conflictingEntityId).isEqualTo(previousAssessment.id)
      assertThat(validationResult.message).isEqualTo("This assessment has already been reallocated")
    }

    @ParameterizedTest
    @ValueSource(booleans = [true, false])
    fun `reallocateAssessment for Approved Premises returns Success, deallocates old assessment and creates a new one, sends allocation & deallocation emails and domain events`(createdFromAppeal: Boolean) {
      assigneeUser.apply {
        roles += UserRoleAssignmentEntityFactory()
          .withUser(this)
          .withRole(UserRole.CAS1_ASSESSOR)
          .produce()

        qualifications += UserQualificationAssignmentEntityFactory()
          .withUser(this)
          .withQualification(UserQualification.PIPE)
          .produce()
      }

      previousAssessment.apply {
        this.createdFromAppeal = createdFromAppeal
      }

      val dueAt = OffsetDateTime.now()

      val actingUser = UserEntityFactory()
        .withDeliusUsername("Acting User")
        .withYieldedProbationRegion {
          ProbationRegionEntityFactory()
            .withYieldedApArea { ApAreaEntityFactory().produce() }
            .produce()
        }.produce()

      every { assessmentRepositoryMock.findByIdOrNull(previousAssessment.id) } returns previousAssessment

      every { jsonSchemaServiceMock.getNewestSchema(ApprovedPremisesAssessmentJsonSchemaEntity::class.java) } returns ApprovedPremisesAssessmentJsonSchemaEntity(
        id = UUID.randomUUID(),
        addedAt = OffsetDateTime.now(),
        schema = "{}",
      )

      every { assessmentRepositoryMock.save(any()) } answers { it.invocation.args[0] as ApprovedPremisesAssessmentEntity }

      every { assessmentEmailServiceMock.assessmentAllocated(any(), any(), any(), any(), any()) } just Runs

      every { assessmentEmailServiceMock.assessmentDeallocated(any(), any(), any()) } just Runs

      every { taskDeadlineServiceMock.getDeadline(any<ApprovedPremisesAssessmentEntity>()) } returns dueAt

      every { userServiceMock.getUserForRequest() } returns actingUser

      every { cas1AssessmentDomainEventService.assessmentAllocated(any(), any(), any()) } just Runs

      val result = assessmentService.reallocateAssessment(assigneeUser, previousAssessment.id)

      assertThat(result is AuthorisableActionResult.Success).isTrue
      val validationResult = (result as AuthorisableActionResult.Success).entity

      assertThat(validationResult is ValidatableActionResult.Success).isTrue
      val newAssessment = (validationResult as ValidatableActionResult.Success).entity as ApprovedPremisesAssessmentEntity

      assertThat(previousAssessment.reallocatedAt).isNotNull
      assertThat(newAssessment.createdFromAppeal).isEqualTo(createdFromAppeal)
      assertThat(newAssessment.dueAt).isEqualTo(dueAt)

      verify { assessmentRepositoryMock.save(match { it.allocatedToUser == assigneeUser }) }

      verify(exactly = 1) {
        assessmentEmailServiceMock.assessmentAllocated(
          match { it.id == assigneeUser.id },
          any<UUID>(),
          application.crn,
          dueAt,
          false,
        )
      }

      verify(exactly = 1) {
        assessmentEmailServiceMock.assessmentDeallocated(
          match { it.id == previousAssessment.allocatedToUser!!.id },
          any<UUID>(),
          application.crn,
        )
      }

      verify {
        cas1AssessmentDomainEventService.assessmentAllocated(any(), assigneeUser, actingUser)
      }
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
    private val cas1AssessmentDomainEventService = mockk<Cas1AssessmentDomainEventService>()

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
      communityApiClientMock,
      cruServiceMock,
      placementRequestServiceMock,
      emailNotificationServiceMock,
      NotifyConfig(),
      placementRequirementsServiceMock,
      userAllocatorMock,
      objectMapperMock,
      UrlTemplate("http://frontend/applications/#id"),
      taskDeadlineServiceMock,
      assessmentEmailServiceMock,
      cas1AssessmentDomainEventService,
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
  }

  @Nested
  inner class CreateAssessments {
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

    private val actingUser = UserEntityFactory()
      .withDeliusUsername("Acting User")
      .withYieldedProbationRegion {
        ProbationRegionEntityFactory()
          .withYieldedApArea { ApAreaEntityFactory().produce() }
          .produce()
      }.produce()

    @BeforeEach
    fun setup() {
      every { jsonSchemaServiceMock.getNewestSchema(ApprovedPremisesAssessmentJsonSchemaEntity::class.java) } returns apSchema
      every { jsonSchemaServiceMock.getNewestSchema(TemporaryAccommodationAssessmentJsonSchemaEntity::class.java) } returns taSchema

      every { jsonSchemaServiceMock.validate(apSchema, "{\"test\": \"data\"}") } returns true
    }

    @ParameterizedTest
    @CsvSource(
      value = [
        "emergency,true", "standard,true", "shortNotice,true",
        "emergency,false", "standard,false", "shortNotice,false",
      ],
    )
    fun `create CAS1 Assessment creates an Assessment, sends allocation email and allocated domain event`(timelinessCategory: Cas1ApplicationTimelinessCategory, createdFromAppeal: Boolean) {
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

          if (timelinessCategory != Cas1ApplicationTimelinessCategory.standard) {
            qualifications += UserQualificationAssignmentEntityFactory()
              .withUser(this)
              .withQualification(UserQualification.EMERGENCY)
              .produce()
          }
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
        .withApType(ApprovedPremisesType.PIPE)
        .withNoticeType(timelinessCategory)
        .produce()

      val dueAt = OffsetDateTime.now()

      every { assessmentRepositoryMock.save(any()) } answers { it.invocation.args[0] as ApprovedPremisesAssessmentEntity }

      every { userAllocatorMock.getUserForAssessmentAllocation(any()) } returns userWithLeastAllocatedAssessments

      every { taskDeadlineServiceMock.getDeadline(any<ApprovedPremisesAssessmentEntity>()) } returns dueAt

      if (createdFromAppeal) {
        every { assessmentEmailServiceMock.appealedAssessmentAllocated(any(), any(), any()) } just Runs
      } else {
        every { assessmentEmailServiceMock.assessmentAllocated(any(), any(), any(), any(), any()) } just Runs
      }

      every { userServiceMock.getUserForRequest() } returns actingUser

      every { cas1AssessmentDomainEventService.assessmentAllocated(any(), any(), any()) } just Runs

      val assessment = assessmentService.createApprovedPremisesAssessment(application, createdFromAppeal)

      verify { assessmentRepositoryMock.save(match { it.allocatedToUser == userWithLeastAllocatedAssessments && it.dueAt == dueAt }) }

      if (createdFromAppeal) {
        verify(exactly = 1) {
          assessmentEmailServiceMock.appealedAssessmentAllocated(
            match { it.id == userWithLeastAllocatedAssessments.id },
            any<UUID>(),
            application.crn,
          )
        }
      } else {
        verify(exactly = 1) {
          assessmentEmailServiceMock.assessmentAllocated(
            match { it.id == userWithLeastAllocatedAssessments.id },
            any<UUID>(),
            application.crn,
            dueAt,
            timelinessCategory == Cas1ApplicationTimelinessCategory.emergency,
          )
        }
      }

      verify {
        cas1AssessmentDomainEventService.assessmentAllocated(
          assessment,
          allocatedToUser = userWithLeastAllocatedAssessments,
          allocatingUser = null,
        )
      }
    }

    @Test
    fun `create CAS1 Assessment doesn't create allocated domain event if no suitable allocation found`() {
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
        .withApType(ApprovedPremisesType.PIPE)
        .withNoticeType(Cas1ApplicationTimelinessCategory.shortNotice)
        .produce()

      val dueAt = OffsetDateTime.now()

      every { assessmentRepositoryMock.save(any()) } answers { it.invocation.args[0] as ApprovedPremisesAssessmentEntity }

      every { userAllocatorMock.getUserForAssessmentAllocation(any()) } returns null

      every { taskDeadlineServiceMock.getDeadline(any<ApprovedPremisesAssessmentEntity>()) } returns dueAt

      every { assessmentEmailServiceMock.assessmentAllocated(any(), any(), any(), any(), any()) } just Runs

      every { userServiceMock.getUserForRequest() } returns actingUser

      every { cas1AssessmentDomainEventService.assessmentAllocated(any(), any(), any()) } just Runs

      assessmentService.createApprovedPremisesAssessment(application, createdFromAppeal = false)

      verify { cas1AssessmentDomainEventService wasNot Called }
    }

    @Test
    fun `create CAS3 Assessment`() {
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

      every { userServiceMock.getUserForRequest() } returns actingUser
      every { assessmentReferralHistoryNoteRepositoryMock.save(any()) } returnsArgument 0

      val summaryData = object {
        val num = 50
        val text = "Hello world!"
      }

      val result = assessmentService.createTemporaryAccommodationAssessment(application, summaryData)

      assertAssessmentHasSystemNote(result, actingUser, ReferralHistorySystemNoteType.SUBMITTED)
      assertThat(result.summaryData).isEqualTo("{\"num\":50,\"text\":\"Hello world!\"}")

      verify { assessmentRepositoryMock.save(match { it.application == application }) }
    }
  }

  @Nested
  inner class UpdateCas1AssessmentWithdrawn {
    private val assessmentId: UUID = UUID.randomUUID()

    private val allocatedUser: UserEntity = UserEntityFactory()
      .withDefaultProbationRegion()
      .withEmail("user@test.com")
      .produce()

    private val withdrawingUser: UserEntity = UserEntityFactory()
      .withDefaultProbationRegion()
      .withEmail("withdrawing@test.com")
      .produce()

    private val schema = ApprovedPremisesAssessmentJsonSchemaEntity(
      id = UUID.randomUUID(),
      addedAt = OffsetDateTime.now(),
      schema = "{}",
    )

    @Test
    fun `updateCas1AssessmentWithdrawn triggers email with assessment pending if assessment active and allocated`() {
      val assessment = ApprovedPremisesAssessmentEntityFactory()
        .withId(assessmentId)
        .withApplication(
          ApprovedPremisesApplicationEntityFactory()
            .withCreatedByUser(UserEntityFactory().withDefaultProbationRegion().produce())
            .produce(),
        )
        .withAssessmentSchema(schema)
        .withData("{\"test\": \"data\"}")
        .withAllocatedToUser(allocatedUser)
        .withSubmittedAt(null)
        .withReallocatedAt(null)
        .withIsWithdrawn(false)
        .produce()

      every { assessmentRepositoryMock.findByIdOrNull(assessment.id) } returns assessment
      every { assessmentRepositoryMock.save(any()) } answers { it.invocation.args[0] as ApprovedPremisesAssessmentEntity }
      every { assessmentEmailServiceMock.assessmentWithdrawn(any(), any(), any()) } just Runs

      assessmentService.updateCas1AssessmentWithdrawn(assessmentId, withdrawingUser)

      verify {
        assessmentEmailServiceMock.assessmentWithdrawn(
          assessment,
          isAssessmentPending = true,
          withdrawingUser = withdrawingUser,
        )
      }
    }

    @Test
    fun `updateCas1AssessmentWithdrawn triggers email with assessment not pending if assessment withdrawn`() {
      val assessment = ApprovedPremisesAssessmentEntityFactory()
        .withId(assessmentId)
        .withApplication(
          ApprovedPremisesApplicationEntityFactory()
            .withCreatedByUser(UserEntityFactory().withDefaultProbationRegion().produce())
            .produce(),
        )
        .withAssessmentSchema(schema)
        .withData("{\"test\": \"data\"}")
        .withAllocatedToUser(allocatedUser)
        .withSubmittedAt(null)
        .withReallocatedAt(null)
        .withIsWithdrawn(true)
        .produce()

      every { assessmentRepositoryMock.findByIdOrNull(assessment.id) } returns assessment
      every { assessmentRepositoryMock.save(any()) } answers { it.invocation.args[0] as ApprovedPremisesAssessmentEntity }
      every { assessmentEmailServiceMock.assessmentWithdrawn(any(), any(), any()) } just Runs

      assessmentService.updateCas1AssessmentWithdrawn(assessmentId, withdrawingUser)

      verify {
        assessmentEmailServiceMock.assessmentWithdrawn(
          assessment,
          isAssessmentPending = false,
          withdrawingUser = withdrawingUser,
        )
      }
    }

    @Test
    fun `updateCas1AssessmentWithdrawn triggers email with assessment not pending if assessment submitted`() {
      val assessment = ApprovedPremisesAssessmentEntityFactory()
        .withId(assessmentId)
        .withApplication(
          ApprovedPremisesApplicationEntityFactory()
            .withCreatedByUser(UserEntityFactory().withDefaultProbationRegion().produce())
            .produce(),
        )
        .withAssessmentSchema(schema)
        .withData("{\"test\": \"data\"}")
        .withAllocatedToUser(allocatedUser)
        .withSubmittedAt(OffsetDateTime.now())
        .withReallocatedAt(null)
        .withIsWithdrawn(false)
        .produce()

      every { assessmentRepositoryMock.findByIdOrNull(assessment.id) } returns assessment
      every { assessmentRepositoryMock.save(any()) } answers { it.invocation.args[0] as ApprovedPremisesAssessmentEntity }
      every { assessmentEmailServiceMock.assessmentWithdrawn(any(), any(), any()) } just Runs

      assessmentService.updateCas1AssessmentWithdrawn(assessmentId, withdrawingUser)

      verify {
        assessmentEmailServiceMock.assessmentWithdrawn(
          assessment,
          isAssessmentPending = false,
          withdrawingUser = withdrawingUser,
        )
      }
    }

    @Test
    fun `updateCas1AssessmentWithdrawn triggers email with assessment not pending if assessment reallocated`() {
      val assessment = ApprovedPremisesAssessmentEntityFactory()
        .withId(assessmentId)
        .withApplication(
          ApprovedPremisesApplicationEntityFactory()
            .withCreatedByUser(UserEntityFactory().withDefaultProbationRegion().produce())
            .produce(),
        )
        .withAssessmentSchema(schema)
        .withData("{\"test\": \"data\"}")
        .withAllocatedToUser(allocatedUser)
        .withSubmittedAt(null)
        .withReallocatedAt(OffsetDateTime.now())
        .withIsWithdrawn(false)
        .produce()

      every { assessmentRepositoryMock.findByIdOrNull(assessment.id) } returns assessment
      every { assessmentRepositoryMock.save(any()) } answers { it.invocation.args[0] as ApprovedPremisesAssessmentEntity }
      every { assessmentEmailServiceMock.assessmentWithdrawn(any(), any(), any()) } just Runs

      assessmentService.updateCas1AssessmentWithdrawn(assessmentId, withdrawingUser)

      verify {
        assessmentEmailServiceMock.assessmentWithdrawn(
          assessment,
          isAssessmentPending = false,
          withdrawingUser = withdrawingUser,
        )
      }
    }
  }
}
