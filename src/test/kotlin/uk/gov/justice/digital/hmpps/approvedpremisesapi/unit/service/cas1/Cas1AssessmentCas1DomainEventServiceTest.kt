package uk.gov.justice.digital.hmpps.approvedpremisesapi.unit.service.cas1

import io.mockk.Runs
import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.NullSource
import org.junit.jupiter.params.provider.ValueSource
import org.springframework.http.HttpStatus
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas1.model.ApplicationAssessedAssessedBy
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas1.model.Cru
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas1.model.EventType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas1.model.PersonReference
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas1.model.ProbationArea
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas1.model.StaffMember
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ApType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.PlacementDates
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.ApDeliusContextApiClient
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.ClientResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.ApAreaEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.ApprovedPremisesApplicationEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.ApprovedPremisesAssessmentEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.AssessmentClarificationNoteEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.OffenderDetailsSummaryFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.ProbationRegionEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.StaffDetailFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.UserEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.UserQualificationAssignmentEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.UserRoleAssignmentEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApprovedPremisesApplicationEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApprovedPremisesAssessmentEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApprovedPremisesAssessmentJsonSchemaEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.MetaDataName
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.TriggerSourceType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserQualification
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserRole
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.deliuscontext.StaffDetail
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1.Cas1AssessmentDomainEventService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1.Cas1DomainEventService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.UrlTemplate
import java.time.LocalDate
import java.time.OffsetDateTime
import java.util.UUID

class Cas1AssessmentCas1DomainEventServiceTest {

  private val domainEventService = mockk<Cas1DomainEventService>()
  private val apDeliusContextApiClient = mockk<ApDeliusContextApiClient>()

  val service = Cas1AssessmentDomainEventService(
    domainEventService,
    apDeliusContextApiClient,
    UrlTemplate("http://frontend/applications/#id"),
    UrlTemplate("http://frontend/assessments/#id"),
  )

  @Nested
  inner class AssessmentAllocated {

    private val assessment = createAssessment()

    private val assigneeUser = UserEntityFactory()
      .withDeliusUsername("Assignee User")
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

    private val allocatingUser = UserEntityFactory()
      .withDeliusUsername("Acting User")
      .withYieldedProbationRegion {
        ProbationRegionEntityFactory()
          .withYieldedApArea { ApAreaEntityFactory().produce() }
          .produce()
      }.produce()

    @Test
    fun `assessmentAllocated raises domain event`() {
      val assigneeUserStaffDetails = StaffDetailFactory.staffDetail()
      every { apDeliusContextApiClient.getStaffDetail(assigneeUser.deliusUsername) } returns ClientResult.Success(
        HttpStatus.OK,
        assigneeUserStaffDetails,
      )

      val allocatingUserStaffDetails = StaffDetailFactory.staffDetail()
      every { apDeliusContextApiClient.getStaffDetail(allocatingUser.deliusUsername) } returns ClientResult.Success(
        HttpStatus.OK,
        allocatingUserStaffDetails,
      )

      every { domainEventService.saveAssessmentAllocatedEvent(any(), any()) } just Runs

      service.assessmentAllocated(assessment, assigneeUser, allocatingUser)

      verify(exactly = 1) {
        domainEventService.saveAssessmentAllocatedEvent(
          match {
            val envelope = it.data
            val eventDetails = envelope.eventDetails

            val rootDomainEventDataMatches = (
              it.assessmentId == assessment.id &&
                it.applicationId == assessment.application.id &&
                it.crn == assessment.application.crn &&
                it.nomsNumber == assessment.application.nomsNumber
              )

            val envelopeMatches = envelope.eventType == EventType.assessmentAllocated

            val allocatedToUserDetailsMatch =
              assertStaffMemberDetailsMatch(eventDetails.allocatedTo, assigneeUserStaffDetails)
            val allocatedByUserDetailsMatch =
              assertStaffMemberDetailsMatch(eventDetails.allocatedBy, allocatingUserStaffDetails)

            val eventDetailsMatch = (
              eventDetails.assessmentId == assessment.id &&
                eventDetails.assessmentUrl == "http://frontend/assessments/${assessment.id}" &&
                eventDetails.personReference.crn == assessment.application.crn &&
                eventDetails.personReference.noms == assessment.application.nomsNumber!! &&
                allocatedToUserDetailsMatch &&
                allocatedByUserDetailsMatch
              )

            rootDomainEventDataMatches && envelopeMatches && eventDetailsMatch
          },
          match {
            val triggerSource = it
            triggerSource == TriggerSourceType.USER
          },
        )
      }
    }

    @Test
    fun `assessmentAllocated allocating user is system`() {
      val assigneeUserStaffDetails = StaffDetailFactory.staffDetail()
      every { apDeliusContextApiClient.getStaffDetail(assigneeUser.deliusUsername) } returns ClientResult.Success(
        HttpStatus.OK,
        assigneeUserStaffDetails,
      )

      every { domainEventService.saveAssessmentAllocatedEvent(any(), any()) } just Runs

      service.assessmentAllocated(assessment, assigneeUser, allocatingUser = null)

      verify(exactly = 1) {
        domainEventService.saveAssessmentAllocatedEvent(
          withArg {
            assertThat(it.data.eventDetails.allocatedBy).isNull()
          },
          TriggerSourceType.SYSTEM,
        )
      }
    }
  }

  @Nested
  inner class AssessmentAccepted {

    @Test
    fun `assessmentAccepted raises domain event`() {
      val user = UserEntityFactory().withYieldedProbationRegion {
        ProbationRegionEntityFactory().withYieldedApArea { ApAreaEntityFactory().produce() }.produce()
      }.withYieldedApArea {
        ApAreaEntityFactory()
          .withName("South West & South Central").produce()
      }.produce()

      val assessmentId = UUID.randomUUID()

      val assessmentSchema = ApprovedPremisesAssessmentJsonSchemaEntity(
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
        .withAssessmentSchema(assessmentSchema)
        .withData("{\"test\": \"data\"}")
        .withSubmittedAt(OffsetDateTime.now())
        .produce()

      val application = assessment.application as ApprovedPremisesApplicationEntity
      val offenderDetails = OffenderDetailsSummaryFactory().produce()
      val staffUserDetails = StaffDetailFactory.staffDetail(
        probationArea = uk.gov.justice.digital.hmpps.approvedpremisesapi.model.deliuscontext.ProbationArea(code = "N26", description = "description"),
      )

      val placementDates = PlacementDates(
        expectedArrival = LocalDate.now(),
        duration = 12,
      )
      val apType = ApType.normal

      every { apDeliusContextApiClient.getStaffDetail(user.deliusUsername) } returns ClientResult.Success(
        HttpStatus.OK,
        staffUserDetails,
      )
      every { domainEventService.saveApplicationAssessedDomainEvent(any()) } just Runs

      service.assessmentAccepted(application, assessment, offenderDetails, placementDates, apType, user)

      verify(exactly = 1) {
        apDeliusContextApiClient.getStaffDetail(user.deliusUsername)
      }

      verify(exactly = 1) {
        domainEventService.saveApplicationAssessedDomainEvent(
          match {
            val data = it.data.eventDetails
            val expectedPersonReference = PersonReference(
              crn = offenderDetails.otherIds.crn,
              noms = offenderDetails.otherIds.nomsNumber!!,
            )
            val expectedAssessor = ApplicationAssessedAssessedBy(
              staffUserDetails.toStaffMember(),
              probationArea = ProbationArea(
                code = staffUserDetails.probationArea.code,
                name = staffUserDetails.probationArea.description,
              ),
              cru = Cru(
                name = "South West & South Central",
              ),
            )

            it.applicationId == assessment.application.id &&
              it.crn == assessment.application.crn &&
              data.applicationId == assessment.application.id &&
              data.applicationUrl == "http://frontend/applications/${assessment.application.id}" &&
              data.personReference == expectedPersonReference &&
              data.deliusEventNumber == (assessment.application as ApprovedPremisesApplicationEntity).eventNumber &&
              data.assessedBy == expectedAssessor &&
              data.decision == "ACCEPTED" &&
              data.decisionRationale == null &&
              it.metadata[MetaDataName.CAS1_REQUESTED_AP_TYPE].equals("NORMAL")
          },
        )
      }
    }
  }

  @Nested
  inner class FurtherInformationRequested {
    @BeforeEach
    fun setup() {
      clearAllMocks()
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(booleans = [true, false])
    fun `it raises a Further Information Requested Domain Event`(emit: Boolean?) {
      val assessment = createAssessment()
      val clarificationNoteEntity = AssessmentClarificationNoteEntityFactory()
        .withAssessment(assessment)
        .withCreatedBy(
          UserEntityFactory()
            .withUnitTestControlProbationRegion()
            .produce(),
        )
        .produce()

      val requesterStaffDetails = StaffDetailFactory.staffDetail()
      val recipientStaffDetails = StaffDetailFactory.staffDetail()

      every { apDeliusContextApiClient.getStaffDetail(clarificationNoteEntity.createdByUser.deliusUsername) } returns ClientResult.Success(
        HttpStatus.OK,
        requesterStaffDetails,
      )
      every { apDeliusContextApiClient.getStaffDetail(assessment.application.createdByUser.deliusUsername) } returns ClientResult.Success(
        HttpStatus.OK,
        recipientStaffDetails,
      )

      val emitValue: Boolean

      if (emit !== null) {
        emitValue = emit
        every { domainEventService.saveFurtherInformationRequestedEvent(any(), emit) } just Runs
        service.furtherInformationRequested(assessment, clarificationNoteEntity, emit)
      } else {
        emitValue = true
        every { domainEventService.saveFurtherInformationRequestedEvent(any(), true) } just Runs
        service.furtherInformationRequested(assessment, clarificationNoteEntity)
      }

      verify(exactly = 1) {
        domainEventService.saveFurtherInformationRequestedEvent(
          match {
            val envelope = it.data
            val eventDetails = envelope.eventDetails

            val rootDomainEventDataMatches = (
              it.assessmentId == assessment.id &&
                it.applicationId == assessment.application.id &&
                it.crn == assessment.application.crn &&
                it.nomsNumber == assessment.application.nomsNumber
              )

            val envelopeMatches = envelope.eventType == EventType.informationRequestMade

            val requesterUserDetailsMatch =
              assertStaffMemberDetailsMatch(eventDetails.requester, requesterStaffDetails)
            val recipientUserDetailsMatch =
              assertStaffMemberDetailsMatch(eventDetails.recipient, recipientStaffDetails)

            val eventDetailsMatch = (
              eventDetails.assessmentId == assessment.id &&
                eventDetails.assessmentUrl == "http://frontend/assessments/${assessment.id}" &&
                eventDetails.applicationUrl == "http://frontend/applications/${assessment.application.id}" &&
                eventDetails.personReference.crn == assessment.application.crn &&
                eventDetails.personReference.noms == assessment.application.nomsNumber!! &&
                eventDetails.requestId == clarificationNoteEntity.id
              )

            rootDomainEventDataMatches && envelopeMatches && eventDetailsMatch && requesterUserDetailsMatch && recipientUserDetailsMatch
          },
          emitValue,
        )
      }
    }
  }

  private fun createAssessment(): ApprovedPremisesAssessmentEntity {
    val allocatedUser: UserEntity = UserEntityFactory()
      .withDefaultProbationRegion()
      .produce()

    val schema = ApprovedPremisesAssessmentJsonSchemaEntity(
      id = UUID.randomUUID(),
      addedAt = OffsetDateTime.now(),
      schema = "{}",
    )

    val assessment = ApprovedPremisesAssessmentEntityFactory()
      .withApplication(
        ApprovedPremisesApplicationEntityFactory()
          .withCrn(Cas1AssessmentEmailServiceTest.Constants.CRN)
          .withCreatedByUser(UserEntityFactory().withDefaultProbationRegion().produce())
          .produce(),
      )
      .withAssessmentSchema(schema)
      .withData("{\"test\": \"data\"}")
      .withAllocatedToUser(allocatedUser)
      .withReallocatedAt(null)
      .withIsWithdrawn(false)
      .produce()

    return assessment
  }

  private fun assertStaffMemberDetailsMatch(staffMember: StaffMember?, staffDetails: StaffDetail?) = when {
    staffMember == null -> staffDetails == null
    else ->
      staffDetails != null &&
        staffMember.staffCode == staffDetails.code &&
        staffMember.forenames == staffDetails.name.forenames() &&
        staffMember.surname == staffDetails.name.surname &&
        staffMember.username == staffDetails.username
  }
}
