package uk.gov.justice.digital.hmpps.approvedpremisesapi.unit.service.cas1

import io.mockk.Called
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas1.model.DatePeriod
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas1.model.PersonReference
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas1.model.RequestForPlacementType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.ApprovedPremisesApplicationEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.ApprovedPremisesAssessmentEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.PlacementApplicationEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.PlacementRequestEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.PlacementRequirementsEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.UserEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.events.WithdrawnByFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.PlacementRequestWithdrawalReason
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.PlacementRequestSource
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1.Cas1DomainEventService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1.Cas1PlacementRequestDomainEventService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1.WithdrawableEntityType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1.WithdrawalContext
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1.WithdrawalTriggeredBySeedJob
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1.WithdrawalTriggeredByUser
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.DomainEventTransformer
import uk.gov.justice.digital.hmpps.approvedpremisesapi.unit.service.cas1.Cas1PlacementRequestCas1DomainEventServiceTest.TestConstants.CRN
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.UrlTemplate
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.isWithinTheLastMinute
import java.time.Clock
import java.time.LocalDate
import java.time.OffsetDateTime

class Cas1PlacementRequestCas1DomainEventServiceTest {

  private object TestConstants {
    const val CRN = "CRN123"
  }

  val domainEventService = mockk<Cas1DomainEventService>()
  val domainEventTransformer = mockk<DomainEventTransformer>()
  val service = Cas1PlacementRequestDomainEventService(
    domainEventService,
    domainEventTransformer,
    applicationUrlTemplate = UrlTemplate("http://frontend/applications/#id"),
    Clock.systemDefaultZone(),
  )
  val user = UserEntityFactory()
    .withUnitTestControlProbationRegion()
    .produce()

  val application = ApprovedPremisesApplicationEntityFactory()
    .withCrn(CRN)
    .withCreatedByUser(user)
    .withSubmittedAt(OffsetDateTime.now())
    .produce()

  val assessment = ApprovedPremisesAssessmentEntityFactory()
    .withApplication(application)
    .produce()

  val placementRequirements = PlacementRequirementsEntityFactory()
    .withApplication(application)
    .withAssessment(assessment)
    .produce()

  val placementRequest = PlacementRequestEntityFactory()
    .withApplication(application)
    .withPlacementRequirements(placementRequirements)
    .withPlacementApplication(null)
    .withAssessment(assessment)
    .withAllocatedToUser(UserEntityFactory().withDefaultProbationRegion().produce())
    .withWithdrawalReason(PlacementRequestWithdrawalReason.DUPLICATE_PLACEMENT_REQUEST)
    .withExpectedArrival(LocalDate.of(2024, 5, 3))
    .withDuration(7)
    .produce()

  @Nested
  inner class PlacementRequestCreated {

    @Test
    fun `if source is appeal, don't create a domain event`() {
      service.placementRequestCreated(
        placementRequest = placementRequest,
        source = PlacementRequestSource.APPEAL,
      )

      verify { domainEventService wasNot Called }
    }

    @Test
    fun `if source is assessment of placement application, don't create a domain event`() {
      service.placementRequestCreated(
        placementRequest = placementRequest,
        source = PlacementRequestSource.ASSESSMENT_OF_PLACEMENT_APPLICATION,
      )

      verify { domainEventService wasNot Called }
    }

    @Test
    fun `if source is application assessment, create a domain event`() {
      every { domainEventService.saveRequestForPlacementCreatedEvent(any(), any()) } returns Unit

      service.placementRequestCreated(
        placementRequest = placementRequest,
        source = PlacementRequestSource.ASSESSMENT_OF_APPLICATION,
      )

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
            assertThat(eventDetails.requestForPlacementId).isEqualTo(placementRequest.id)
            assertThat(eventDetails.personReference.crn).isEqualTo(CRN)
            assertThat(eventDetails.personReference.noms).isEqualTo(application.nomsNumber)
            assertThat(eventDetails.deliusEventNumber).isEqualTo(application.eventNumber)
            assertThat(eventDetails.createdAt).isWithinTheLastMinute()
            assertThat(eventDetails.createdBy).isNull()
            assertThat(eventDetails.expectedArrival).isEqualTo(LocalDate.of(2024, 5, 3))
            assertThat(eventDetails.duration).isEqualTo(7)
            assertThat(eventDetails.requestForPlacementType).isEqualTo(RequestForPlacementType.initial)
          },
          emit = false,
        )
      }
    }
  }

  @Nested
  inner class PlacementRequestWithdrawn {

    @Test
    fun `it errors if triggered by seed job`() {
      val withdrawnBy = WithdrawnByFactory().produce()
      every { domainEventTransformer.toWithdrawnBy(user) } returns withdrawnBy
      every { domainEventService.saveMatchRequestWithdrawnEvent(any()) } returns Unit

      assertThatThrownBy {
        service.placementRequestWithdrawn(
          placementRequest,
          withdrawalContext = WithdrawalContext(
            WithdrawalTriggeredBySeedJob,
            WithdrawableEntityType.PlacementApplication,
            placementRequest.id,
          ),
        )
      }.hasMessage("Only withdrawals triggered by users are supported")
    }

    @Test
    fun `it creates a domain event if for the applications arrival date`() {
      val withdrawnBy = WithdrawnByFactory().produce()
      every { domainEventTransformer.toWithdrawnBy(user) } returns withdrawnBy
      every { domainEventService.saveMatchRequestWithdrawnEvent(any()) } returns Unit

      service.placementRequestWithdrawn(
        placementRequest,
        withdrawalContext = WithdrawalContext(
          WithdrawalTriggeredByUser(user),
          WithdrawableEntityType.PlacementApplication,
          placementRequest.id,
        ),
      )

      verify(exactly = 1) {
        domainEventService.saveMatchRequestWithdrawnEvent(
          match {
            val data = it.data.eventDetails

            it.applicationId == application.id &&
              it.crn == application.crn &&
              it.nomsNumber == application.nomsNumber &&
              data.applicationId == application.id &&
              data.applicationUrl == "http://frontend/applications/${application.id}" &&
              data.matchRequestId == placementRequest.id &&
              data.personReference == PersonReference(
                crn = application.crn,
                noms = application.nomsNumber!!,
              ) &&
              data.deliusEventNumber == application.eventNumber &&
              data.withdrawnBy == withdrawnBy
            data.withdrawalReason == "DUPLICATE_PLACEMENT_REQUEST" &&
              data.requestIsForApplicationsArrivalDate == true &&
              data.datePeriod ==
              DatePeriod(
                LocalDate.of(2024, 5, 3),
                LocalDate.of(2024, 5, 10),
              )
          },
        )
      }
    }

    @Test
    fun `it doesnt creates a domain event if not for the applications arrival date`() {
      val user = UserEntityFactory()
        .withUnitTestControlProbationRegion()
        .produce()

      val application = ApprovedPremisesApplicationEntityFactory()
        .withCrn(TestConstants.CRN)
        .withCreatedByUser(user)
        .withSubmittedAt(OffsetDateTime.now())
        .produce()

      val assessment = ApprovedPremisesAssessmentEntityFactory()
        .withApplication(application)
        .produce()

      val placementRequirements = PlacementRequirementsEntityFactory()
        .withApplication(application)
        .withAssessment(assessment)
        .produce()

      val placementApplication = PlacementApplicationEntityFactory()
        .withDefaults()
        .withApplication(application)
        .produce()

      val placementRequest = PlacementRequestEntityFactory()
        .withApplication(application)
        .withPlacementRequirements(placementRequirements)
        .withPlacementApplication(placementApplication)
        .withAssessment(assessment)
        .withAllocatedToUser(UserEntityFactory().withDefaultProbationRegion().produce())
        .withWithdrawalReason(PlacementRequestWithdrawalReason.DUPLICATE_PLACEMENT_REQUEST)
        .withExpectedArrival(LocalDate.of(2024, 5, 3))
        .withDuration(7)
        .produce()

      service.placementRequestWithdrawn(
        placementRequest,
        withdrawalContext = WithdrawalContext(
          WithdrawalTriggeredByUser(user),
          WithdrawableEntityType.PlacementApplication,
          placementRequest.id,
        ),
      )

      verify { domainEventService wasNot Called }
    }
  }
}
