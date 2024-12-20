package uk.gov.justice.digital.hmpps.approvedpremisesapi.unit.service.cas1

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import org.springframework.http.HttpStatus
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas1.model.DatePeriod
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas1.model.PersonReference
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas1.model.RequestForPlacementType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.PlacementApplicationDecisionEnvelope
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.ApDeliusContextApiClient
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.ClientResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.ApprovedPremisesApplicationEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.PlacementApplicationEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.PlacementDateEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.StaffDetailFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.UserEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.events.StaffMemberFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.events.WithdrawnByFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.MetaDataName
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.PlacementApplicationDecision
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.PlacementApplicationEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.PlacementApplicationWithdrawalReason
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.PlacementType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1.Cas1DomainEventService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1.Cas1PlacementApplicationDomainEventService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1.WithdrawableEntityType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1.WithdrawalContext
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1.WithdrawalTriggeredBySeedJob
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1.WithdrawalTriggeredByUser
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.DomainEventTransformer
import uk.gov.justice.digital.hmpps.approvedpremisesapi.unit.service.cas1.Cas1PlacementApplicationCas1DomainEventServiceTest.TestConstants.CRN
import uk.gov.justice.digital.hmpps.approvedpremisesapi.unit.service.cas1.Cas1PlacementApplicationCas1DomainEventServiceTest.TestConstants.USERNAME
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.UrlTemplate
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.isWithinTheLastMinute
import java.time.LocalDate
import java.time.OffsetDateTime
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.PlacementApplicationDecision as ApiDecision

class Cas1PlacementApplicationCas1DomainEventServiceTest {

  private object TestConstants {
    const val CRN = "CRN123"
    const val USERNAME = "theUserName"
  }

  val domainEventService = mockk<Cas1DomainEventService>()
  val domainEventTransformer = mockk<DomainEventTransformer>()
  val apDeliusContextApiClient = mockk<ApDeliusContextApiClient>()

  val service = Cas1PlacementApplicationDomainEventService(
    domainEventService,
    domainEventTransformer,
    apDeliusContextApiClient,
    applicationUrlTemplate = UrlTemplate("http://frontend/applications/#id"),
  )

  val user = UserEntityFactory()
    .withUnitTestControlProbationRegion()
    .produce()

  val application = ApprovedPremisesApplicationEntityFactory()
    .withCrn(CRN)
    .withCreatedByUser(user)
    .withSubmittedAt(OffsetDateTime.now())
    .produce()

  @Nested
  inner class PlacementApplicationSubmitted {

    @ParameterizedTest
    @CsvSource(
      "ROTL,rotl",
      "RELEASE_FOLLOWING_DECISION,releaseFollowingDecisions",
      "ADDITIONAL_PLACEMENT,additionalPlacement",
    )
    fun `it creates a domain event`(placementType: PlacementType, expectedRequestForPlacementType: RequestForPlacementType) {
      val placementApplication = PlacementApplicationEntityFactory()
        .withApplication(application)
        .withAllocatedToUser(UserEntityFactory().withDefaultProbationRegion().produce())
        .withDecision(null)
        .withCreatedByUser(user)
        .withPlacementType(placementType)
        .produce()

      placementApplication.placementDates = mutableListOf(
        PlacementDateEntityFactory()
          .withPlacementApplication(placementApplication)
          .withExpectedArrival(LocalDate.of(2024, 5, 3))
          .withDuration(7)
          .produce(),
      )

      val staffUserDetails = StaffDetailFactory.staffDetail(deliusUsername = USERNAME)
      every { apDeliusContextApiClient.getStaffDetail(USERNAME) } returns ClientResult.Success(
        status = HttpStatus.OK,
        body = staffUserDetails,
      )

      val staffMember = staffUserDetails.toStaffMember()
      every { domainEventService.saveRequestForPlacementCreatedEvent(any(), any()) } returns Unit

      service.placementApplicationSubmitted(placementApplication, USERNAME)

      verify {
        domainEventService.saveRequestForPlacementCreatedEvent(
          withArg {
            assertThat(it.id).isNotNull()
            assertThat(it.applicationId).isEqualTo(application.id)
            assertThat(it.crn).isEqualTo(CRN)
            assertThat(it.nomsNumber).isEqualTo(application.nomsNumber)
            assertThat(it.occurredAt).isWithinTheLastMinute()

            val eventDetails = it.data.eventDetails
            assertThat(eventDetails.applicationId).isEqualTo(application.id)
            assertThat(eventDetails.applicationUrl).isEqualTo("http://frontend/applications/${application.id}")
            assertThat(eventDetails.requestForPlacementId).isEqualTo(placementApplication.id)
            assertThat(eventDetails.personReference.crn).isEqualTo(CRN)
            assertThat(eventDetails.personReference.noms).isEqualTo(application.nomsNumber)
            assertThat(eventDetails.deliusEventNumber).isEqualTo(application.eventNumber)
            assertThat(eventDetails.createdAt).isWithinTheLastMinute()
            assertThat(eventDetails.createdBy).isEqualTo(staffMember)
            assertThat(eventDetails.expectedArrival).isEqualTo(LocalDate.of(2024, 5, 3))
            assertThat(eventDetails.duration).isEqualTo(7)
            assertThat(eventDetails.requestForPlacementType).isEqualTo(expectedRequestForPlacementType)
          },
          emit = false,
        )
      }
    }
  }

  @Nested
  inner class PlacementApplicationWithdrawn {

    val placementApplication = PlacementApplicationEntityFactory()
      .withApplication(application)
      .withAllocatedToUser(UserEntityFactory().withDefaultProbationRegion().produce())
      .withDecision(null)
      .withCreatedByUser(user)
      .withWithdrawalReason(PlacementApplicationWithdrawalReason.ALTERNATIVE_PROVISION_IDENTIFIED)
      .produce()

    @Test
    fun `it errors if triggered by seed job`() {
      val withdrawnBy = WithdrawnByFactory().produce()
      every { domainEventTransformer.toWithdrawnBy(user) } returns withdrawnBy
      every { domainEventService.savePlacementApplicationWithdrawnEvent(any()) } returns Unit

      assertThatThrownBy {
        service.placementApplicationWithdrawn(
          placementApplication,
          withdrawalContext = WithdrawalContext(
            WithdrawalTriggeredBySeedJob,
            WithdrawableEntityType.PlacementApplication,
            placementApplication.id,
          ),
        )
      }.hasMessage("Only withdrawals triggered by users are supported")
    }

    @Test
    fun `it creates a domain event`() {
      placementApplication.placementDates = mutableListOf(
        PlacementDateEntityFactory()
          .withPlacementApplication(placementApplication)
          .withExpectedArrival(LocalDate.of(2024, 5, 3))
          .withDuration(7)
          .produce(),
      )

      val withdrawnBy = WithdrawnByFactory().produce()
      every { domainEventTransformer.toWithdrawnBy(user) } returns withdrawnBy
      every { domainEventService.savePlacementApplicationWithdrawnEvent(any()) } returns Unit

      service.placementApplicationWithdrawn(
        placementApplication,
        withdrawalContext = WithdrawalContext(
          WithdrawalTriggeredByUser(user),
          WithdrawableEntityType.PlacementApplication,
          placementApplication.id,
        ),
      )

      verify(exactly = 1) {
        domainEventService.savePlacementApplicationWithdrawnEvent(
          withArg {
            assertThat(it.id).isNotNull()
            assertThat(it.applicationId).isEqualTo(application.id)
            assertThat(it.crn).isEqualTo(CRN)
            assertThat(it.nomsNumber).isEqualTo(application.nomsNumber)
            assertThat(it.occurredAt).isWithinTheLastMinute()
            assertThat(it.metadata).containsEntry(MetaDataName.CAS1_PLACEMENT_APPLICATION_ID, placementApplication.id.toString())

            val eventDetails = it.data.eventDetails
            assertThat(eventDetails.applicationId).isEqualTo(application.id)
            assertThat(eventDetails.applicationUrl).isEqualTo("http://frontend/applications/${application.id}")
            assertThat(eventDetails.placementApplicationId).isEqualTo(placementApplication.id)
            assertThat(eventDetails.personReference.crn).isEqualTo(CRN)
            assertThat(eventDetails.personReference.noms).isEqualTo(application.nomsNumber)
            assertThat(eventDetails.deliusEventNumber).isEqualTo(application.eventNumber)
            assertThat(eventDetails.withdrawnBy).isEqualTo(withdrawnBy)
            assertThat(eventDetails.withdrawalReason).isEqualTo("ALTERNATIVE_PROVISION_IDENTIFIED")
            assertThat(eventDetails.placementDates).hasSize(1)
            assertThat(eventDetails.placementDates!![0].startDate).isEqualTo(LocalDate.of(2024, 5, 3))
            assertThat(eventDetails.placementDates!![0].endDate).isEqualTo(LocalDate.of(2024, 5, 10))
          },
        )
      }
    }
  }

  @Nested
  inner class PlacementApplicationAllocated {
    @Test
    fun `allocatedAt cannot be null`() {
      val user = UserEntityFactory()
        .withUnitTestControlProbationRegion()
        .produce()

      val application = ApprovedPremisesApplicationEntityFactory()
        .withCrn(TestConstants.CRN)
        .withCreatedByUser(user)
        .withSubmittedAt(OffsetDateTime.now())
        .produce()

      val placementApplication = PlacementApplicationEntityFactory()
        .withApplication(application)
        .withAllocatedToUser(UserEntityFactory().withDefaultProbationRegion().produce())
        .withDecision(null)
        .withCreatedByUser(user)
        .withWithdrawalReason(PlacementApplicationWithdrawalReason.ALTERNATIVE_PROVISION_IDENTIFIED)
        .produce()

      placementApplication.placementDates = mutableListOf(
        PlacementDateEntityFactory()
          .withPlacementApplication(placementApplication)
          .withExpectedArrival(LocalDate.of(2024, 5, 3))
          .withDuration(7)
          .produce(),
        PlacementDateEntityFactory()
          .withPlacementApplication(placementApplication)
          .withExpectedArrival(LocalDate.of(2025, 2, 2))
          .withDuration(14)
          .produce(),
      )

      assertThrows<IllegalArgumentException> { service.placementApplicationAllocated(placementApplication, user) }
    }

    @Test
    fun `allocatedToUser cannot be null`() {
      val user = UserEntityFactory()
        .withUnitTestControlProbationRegion()
        .produce()

      val application = ApprovedPremisesApplicationEntityFactory()
        .withCrn(TestConstants.CRN)
        .withCreatedByUser(user)
        .withSubmittedAt(OffsetDateTime.now())
        .produce()

      val placementApplication = PlacementApplicationEntityFactory()
        .withApplication(application)
        .withAllocatedToUser(null)
        .withDecision(null)
        .withCreatedByUser(user)
        .withWithdrawalReason(PlacementApplicationWithdrawalReason.ALTERNATIVE_PROVISION_IDENTIFIED)
        .produce()
        .apply {
          allocatedAt = OffsetDateTime.now()
        }

      placementApplication.placementDates = mutableListOf(
        PlacementDateEntityFactory()
          .withPlacementApplication(placementApplication)
          .withExpectedArrival(LocalDate.of(2024, 5, 3))
          .withDuration(7)
          .produce(),
        PlacementDateEntityFactory()
          .withPlacementApplication(placementApplication)
          .withExpectedArrival(LocalDate.of(2025, 2, 2))
          .withDuration(14)
          .produce(),
      )

      assertThrows<IllegalArgumentException> { service.placementApplicationAllocated(placementApplication, user) }
    }

    @Test
    fun `it creates a domain event`() {
      val allocatedByUser = UserEntityFactory()
        .withUnitTestControlProbationRegion()
        .produce()

      val allocatedToUser = UserEntityFactory()
        .withDefaultProbationRegion()
        .produce()

      val application = ApprovedPremisesApplicationEntityFactory()
        .withCrn(TestConstants.CRN)
        .withCreatedByUser(allocatedByUser)
        .withSubmittedAt(OffsetDateTime.now())
        .produce()

      val placementApplication = PlacementApplicationEntityFactory()
        .withApplication(application)
        .withAllocatedToUser(allocatedToUser)
        .withDecision(null)
        .withCreatedByUser(allocatedByUser)
        .withWithdrawalReason(PlacementApplicationWithdrawalReason.ALTERNATIVE_PROVISION_IDENTIFIED)
        .produce()
        .apply {
          allocatedAt = OffsetDateTime.now()
        }

      placementApplication.placementDates = mutableListOf(
        PlacementDateEntityFactory()
          .withPlacementApplication(placementApplication)
          .withExpectedArrival(LocalDate.of(2024, 5, 3))
          .withDuration(7)
          .produce(),
        PlacementDateEntityFactory()
          .withPlacementApplication(placementApplication)
          .withExpectedArrival(LocalDate.of(2025, 2, 2))
          .withDuration(14)
          .produce(),
      )

      val allocatedBy = StaffMemberFactory().produce()
      val allocatedTo = StaffMemberFactory().produce()
      every { domainEventTransformer.toStaffMember(allocatedByUser) } returns allocatedBy
      every { domainEventTransformer.toStaffMember(allocatedToUser) } returns allocatedTo
      every { domainEventService.savePlacementApplicationAllocatedEvent(any()) } returns Unit

      service.placementApplicationAllocated(placementApplication, allocatedByUser)

      verify(exactly = 1) {
        domainEventService.savePlacementApplicationAllocatedEvent(
          match {
            val data = it.data.eventDetails
            println(data)

            it.applicationId == application.id &&
              it.crn == application.crn &&
              it.nomsNumber == application.nomsNumber &&
              data.applicationId == application.id &&
              data.applicationUrl == "http://frontend/applications/${application.id}" &&
              data.placementApplicationId == placementApplication.id &&
              data.personReference == PersonReference(
                crn = application.crn,
                noms = application.nomsNumber!!,
              ) &&
              data.allocatedBy == allocatedBy &&
              data.allocatedTo == allocatedTo &&
              data.placementDates == listOf(
                DatePeriod(
                  LocalDate.of(2024, 5, 3),
                  LocalDate.of(2024, 5, 10),
                ),
                DatePeriod(
                  LocalDate.of(2025, 2, 2),
                  LocalDate.of(2025, 2, 16),
                ),
              )
          },
        )
      }
    }
  }

  @Nested
  inner class PlacementApplicationAssessed {

    @Test
    fun `it creates a domain event`() {
      val placementApplication = getPlacementApplicationWithDecision(PlacementApplicationDecision.ACCEPTED)
      val assessedByStaffMember = StaffMemberFactory().produce()
      val decisionSummary = "Decision Summary: Accepted"
      val decisionMade = ApiDecision.accepted

      every { domainEventTransformer.toStaffMember(user) } returns assessedByStaffMember
      every { domainEventService.saveRequestForPlacementAssessedEvent(any()) } returns Unit

      service.placementApplicationAssessed(
        placementApplication,
        user,
        PlacementApplicationDecisionEnvelope(
          decisionMade,
          "Summary of Changes",
          decisionSummary,
        ),
      )

      verify(exactly = 1) {
        domainEventService.saveRequestForPlacementAssessedEvent(
          withArg {
            assertThat(it.id).isNotNull()
            assertThat(it.applicationId).isEqualTo(application.id)
            assertThat(it.crn).isEqualTo(CRN)
            assertThat(it.nomsNumber).isEqualTo(application.nomsNumber)
            assertThat(it.occurredAt).isWithinTheLastMinute()
            assertThat(it.metadata).containsEntry(MetaDataName.CAS1_PLACEMENT_APPLICATION_ID, placementApplication.id.toString())

            val eventDetails = it.data.eventDetails
            assertThat(eventDetails.applicationId).isEqualTo(application.id)
            assertThat(eventDetails.applicationUrl).isEqualTo("http://frontend/applications/${application.id}")
            assertThat(eventDetails.assessedBy).isEqualTo(assessedByStaffMember)
            assertThat(eventDetails.decision.value).isEqualTo(decisionMade.value)
            assertThat(eventDetails.decisionSummary).isEqualTo(decisionSummary)
            assertThat(eventDetails.expectedArrival).isEqualTo(LocalDate.of(2024, 5, 3))
            assertThat(eventDetails.duration).isEqualTo(7)
          },
        )
      }
    }

    @Test
    fun `PlacementApplicationDecision cannot be deprecated value`() {
      val placementApplication = getPlacementApplicationWithDecision(PlacementApplicationDecision.WITHDRAW)
      val assessedByStaffMember = StaffMemberFactory().produce()
      every { domainEventTransformer.toStaffMember(user) } returns assessedByStaffMember

      val exception = assertThrows<IllegalArgumentException> {
        service.placementApplicationAssessed(
          placementApplication,
          user,
          PlacementApplicationDecisionEnvelope(
            ApiDecision.withdraw,
            "Summary of Changes",
            "decisionSummary",
          ),
        )
      }
      assertThat(exception.message).isEqualTo("PlacementApplicationDecision 'WITHDRAW' has been deprecated")
    }

    @Test
    fun `PlacementApplicationDecision cannot be null`() {
      val placementApplication = getPlacementApplicationWithDecision(null)
      val assessedByStaffMember = StaffMemberFactory().produce()
      every { domainEventTransformer.toStaffMember(user) } returns assessedByStaffMember

      val exception = assertThrows<IllegalArgumentException> {
        service.placementApplicationAssessed(
          placementApplication,
          user,
          PlacementApplicationDecisionEnvelope(
            ApiDecision.accepted,
            "Summary of Changes",
            "decisionSummary",
          ),
        )
      }
      assertThat(exception.message).isEqualTo("PlacementApplicationDecision was null")
    }

    private fun getPlacementApplicationWithDecision(decision: PlacementApplicationDecision?): PlacementApplicationEntity {
      val placementApplication = PlacementApplicationEntityFactory()
        .withApplication(application)
        .withAllocatedToUser(UserEntityFactory().withDefaultProbationRegion().produce())
        .withDecision(decision)
        .withCreatedByUser(user)
        .produce()
      placementApplication.placementDates = mutableListOf(
        PlacementDateEntityFactory()
          .withPlacementApplication(placementApplication)
          .withExpectedArrival(LocalDate.of(2024, 5, 3))
          .withDuration(7)
          .produce(),
      )
      return placementApplication
    }
  }
}
