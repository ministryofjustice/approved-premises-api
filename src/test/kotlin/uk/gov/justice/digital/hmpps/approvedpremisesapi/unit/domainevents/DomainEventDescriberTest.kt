package uk.gov.justice.digital.hmpps.approvedpremisesapi.unit.domainevents

import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.model.AppealDecision
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.model.ApplicationAssessedEnvelope
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.model.ApplicationWithdrawnEnvelope
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.model.AssessmentAppealedEnvelope
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.model.BookingCancelledEnvelope
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.model.BookingMadeEnvelope
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.model.BookingNotMadeEnvelope
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.model.DatePeriod
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.model.MatchRequestWithdrawnEnvelope
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.model.PersonArrivedEnvelope
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.model.PersonDepartedEnvelope
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.model.PersonNotArrivedEnvelope
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.model.PlacementApplicationWithdrawnEnvelope
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.model.RequestForPlacementCreatedEnvelope
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.model.RequestForPlacementType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.domainevents.DomainEventDescriber
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.events.ApplicationAssessedFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.events.ApplicationWithdrawnFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.events.AssessmentAppealedFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.events.BookingCancelledFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.events.BookingMadeFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.events.BookingNotMadeFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.events.MatchRequestWithdrawnFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.events.PersonArrivedFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.events.PersonDepartedFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.events.PersonNotArrivedFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.events.PlacementApplicationWithdrawnFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.events.RequestForPlacementCreatedFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.DomainEventType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.PlacementApplicationWithdrawalReason
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.PlacementRequestWithdrawalReason
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.DomainEvent
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.DomainEventSummary
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.DomainEventService
import java.time.Instant
import java.time.LocalDate
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.util.UUID

class DomainEventDescriberTest {
  private val mockDomainEventService = mockk<DomainEventService>()

  private val domainEventDescriber = DomainEventDescriber(mockDomainEventService)

  @Test
  fun `Returns expected description for application submitted event`() {
    val domainEventSummary = DomainEventSummaryImpl.ofType(DomainEventType.APPROVED_PREMISES_APPLICATION_SUBMITTED)

    val result = domainEventDescriber.getDescription(domainEventSummary)

    assertThat(result).isEqualTo("The application was submitted")
  }

  @ParameterizedTest
  @CsvSource(value = ["accepted", "rejected"])
  fun `Returns expected description for application assessed event`(decision: String) {
    val domainEventSummary = DomainEventSummaryImpl.ofType(DomainEventType.APPROVED_PREMISES_APPLICATION_ASSESSED)

    every { mockDomainEventService.getApplicationAssessedDomainEvent(any()) } returns buildDomainEvent {
      ApplicationAssessedEnvelope(
        id = it,
        timestamp = Instant.now(),
        eventType = "approved-premises.application.assessed",
        eventDetails = ApplicationAssessedFactory()
          .withDecision(decision)
          .produce(),
      )
    }

    val result = domainEventDescriber.getDescription(domainEventSummary)

    assertThat(result).isEqualTo("The application was assessed and $decision")
  }

  @ParameterizedTest
  @CsvSource(value = ["2024-01-01,2024-04-01", "2024-01-02,2024-04-02"])
  fun `Returns expected description for booking made event`(arrivalDate: LocalDate, departureDate: LocalDate) {
    val domainEventSummary = DomainEventSummaryImpl.ofType(DomainEventType.APPROVED_PREMISES_BOOKING_MADE)

    every { mockDomainEventService.getBookingMadeEvent(any()) } returns buildDomainEvent {
      BookingMadeEnvelope(
        id = it,
        timestamp = Instant.now(),
        eventType = "approved-premises.booking.made",
        eventDetails = BookingMadeFactory()
          .withArrivalOn(arrivalDate)
          .withDepartureOn(departureDate)
          .produce(),
      )
    }

    val result = domainEventDescriber.getDescription(domainEventSummary)

    assertThat(result).isEqualTo("A booking was made for between $arrivalDate and $departureDate")
  }

  @ParameterizedTest
  @CsvSource(value = ["2024-01-01", "2024-01-02"])
  fun `Returns expected description for person arrived event`(arrivalDate: LocalDate) {
    val domainEventSummary = DomainEventSummaryImpl.ofType(DomainEventType.APPROVED_PREMISES_PERSON_ARRIVED)

    every { mockDomainEventService.getPersonArrivedEvent(any()) } returns buildDomainEvent {
      PersonArrivedEnvelope(
        id = it,
        timestamp = Instant.now(),
        eventType = "approved-premises.person.arrived",
        eventDetails = PersonArrivedFactory()
          .withArrivedAt(arrivalDate.atTime(12, 34, 56).toInstant(ZoneOffset.UTC))
          .produce(),
      )
    }

    val result = domainEventDescriber.getDescription(domainEventSummary)

    assertThat(result).isEqualTo("The person moved into the premises on $arrivalDate")
  }

  @ParameterizedTest
  @CsvSource(value = ["2024-01-01", "2024-01-02"])
  fun `Returns expected description for person not arrived event`(expectedArrivalDate: LocalDate) {
    val domainEventSummary = DomainEventSummaryImpl.ofType(DomainEventType.APPROVED_PREMISES_PERSON_NOT_ARRIVED)

    every { mockDomainEventService.getPersonNotArrivedEvent(any()) } returns buildDomainEvent {
      PersonNotArrivedEnvelope(
        id = it,
        timestamp = Instant.now(),
        eventType = "approved-premises.person.not-arrived",
        eventDetails = PersonNotArrivedFactory()
          .withExpectedArrivalOn(expectedArrivalDate)
          .produce(),
      )
    }

    val result = domainEventDescriber.getDescription(domainEventSummary)

    assertThat(result).isEqualTo("The person was due to move into the premises on $expectedArrivalDate but did not arrive")
  }

  @ParameterizedTest
  @CsvSource(value = ["2024-04-01", "2024-04-02"])
  fun `Returns expected description for person departed event`(departureDate: LocalDate) {
    val domainEventSummary = DomainEventSummaryImpl.ofType(DomainEventType.APPROVED_PREMISES_PERSON_DEPARTED)

    every { mockDomainEventService.getPersonDepartedEvent(any()) } returns buildDomainEvent {
      PersonDepartedEnvelope(
        id = it,
        timestamp = Instant.now(),
        eventType = "approved-premises.person.departed",
        eventDetails = PersonDepartedFactory()
          .withDepartedAt(departureDate.atTime(12, 34, 56).toInstant(ZoneOffset.UTC))
          .produce(),
      )
    }

    val result = domainEventDescriber.getDescription(domainEventSummary)

    assertThat(result).isEqualTo("The person moved out of the premises on $departureDate")
  }

  @ParameterizedTest
  @CsvSource(value = ["Reason A", "Reason B"])
  fun `Returns expected description for booking not made event`(reason: String) {
    val domainEventSummary = DomainEventSummaryImpl.ofType(DomainEventType.APPROVED_PREMISES_BOOKING_NOT_MADE)

    every { mockDomainEventService.getBookingNotMadeEvent(any()) } returns buildDomainEvent {
      BookingNotMadeEnvelope(
        id = it,
        timestamp = Instant.now(),
        eventType = "approved-premises.booking.not-made",
        eventDetails = BookingNotMadeFactory()
          .withFailureDescription(reason)
          .produce(),
      )
    }

    val result = domainEventDescriber.getDescription(domainEventSummary)

    assertThat(result).isEqualTo("A booking was not made for the placement request. The reason was: $reason")
  }

  @ParameterizedTest
  @CsvSource(value = ["Reason A", "Reason B"])
  fun `Returns expected description for booking cancelled event`(reason: String) {
    val domainEventSummary = DomainEventSummaryImpl.ofType(DomainEventType.APPROVED_PREMISES_BOOKING_CANCELLED)

    every { mockDomainEventService.getBookingCancelledEvent(any()) } returns buildDomainEvent {
      BookingCancelledEnvelope(
        id = it,
        timestamp = Instant.now(),
        eventType = "approved-premises.booking.cancelled",
        eventDetails = BookingCancelledFactory()
          .withCancellationReason(reason)
          .produce(),
      )
    }

    val result = domainEventDescriber.getDescription(domainEventSummary)

    assertThat(result).isEqualTo("The booking was cancelled. The reason was: '$reason'")
  }

  @Test
  fun `Returns expected description for booking changed event`() {
    val domainEventSummary = DomainEventSummaryImpl.ofType(DomainEventType.APPROVED_PREMISES_BOOKING_CHANGED)

    val result = domainEventDescriber.getDescription(domainEventSummary)

    assertThat(result).isEqualTo("The booking had its arrival or departure date changed")
  }

  @Test
  fun `Returns expected description for application withdrawn event`() {
    val domainEventSummary = DomainEventSummaryImpl.ofType(DomainEventType.APPROVED_PREMISES_APPLICATION_WITHDRAWN)

    every { mockDomainEventService.getApplicationWithdrawnEvent(any()) } returns buildDomainEvent {
      ApplicationWithdrawnEnvelope(
        id = it,
        timestamp = Instant.now(),
        eventType = "approved-premises.application.withdrawn",
        eventDetails = ApplicationWithdrawnFactory()
          .withWithdrawalReason("change_in_circumstances_new_application_to_be_submitted")
          .withOtherWithdrawalReason(null)
          .produce(),
      )
    }

    val result = domainEventDescriber.getDescription(domainEventSummary)

    assertThat(result).isEqualTo("The application was withdrawn. The reason was: 'change in circumstances new application to be submitted'")
  }

  @Test
  fun `Returns expected description for application withdrawn event with additional reason`() {
    val domainEventSummary = DomainEventSummaryImpl.ofType(DomainEventType.APPROVED_PREMISES_APPLICATION_WITHDRAWN)

    every { mockDomainEventService.getApplicationWithdrawnEvent(any()) } returns buildDomainEvent {
      ApplicationWithdrawnEnvelope(
        id = it,
        timestamp = Instant.now(),
        eventType = "approved-premises.application.withdrawn",
        eventDetails = ApplicationWithdrawnFactory()
          .withWithdrawalReason("the main withdrawal reason")
          .withOtherWithdrawalReason("additional reason")
          .produce(),
      )
    }

    val result = domainEventDescriber.getDescription(domainEventSummary)

    assertThat(result).isEqualTo("The application was withdrawn. The reason was: 'the main withdrawal reason' (additional reason)")
  }

  @ParameterizedTest
  @CsvSource(value = ["accepted,Reason A", "rejected,Reason B"])
  fun `Returns expected description for assessment appealed event`(decision: String, reason: String) {
    val domainEventSummary = DomainEventSummaryImpl.ofType(DomainEventType.APPROVED_PREMISES_ASSESSMENT_APPEALED)

    every { mockDomainEventService.getAssessmentAppealedEvent(any()) } returns buildDomainEvent {
      AssessmentAppealedEnvelope(
        id = it,
        timestamp = Instant.now(),
        eventType = "approved-premises.assessment.appealed",
        eventDetails = AssessmentAppealedFactory()
          .withDecision(AppealDecision.valueOf(decision))
          .withDecisionDetail(reason)
          .produce(),
      )
    }

    val result = domainEventDescriber.getDescription(domainEventSummary)

    assertThat(result).isEqualTo("The assessment was appealed and $decision. The reason was: $reason")
  }

  @Test
  fun `Returns expected description for placement application withdrawn event with no dates`() {
    val domainEventSummary = DomainEventSummaryImpl.ofType(DomainEventType.APPROVED_PREMISES_PLACEMENT_APPLICATION_WITHDRAWN)

    every { mockDomainEventService.getPlacementApplicationWithdrawnEvent(UUID.fromString(domainEventSummary.id)) } returns buildDomainEvent {
      PlacementApplicationWithdrawnEnvelope(
        id = it,
        timestamp = Instant.now(),
        eventType = "approved-premises.placement-application.withdrawn",
        eventDetails = PlacementApplicationWithdrawnFactory()
          .withWithdrawalReason(PlacementApplicationWithdrawalReason.RELATED_APPLICATION_WITHDRAWN.toString())
          .produce(),
      )
    }

    val result = domainEventDescriber.getDescription(domainEventSummary)

    assertThat(result).isEqualTo("A request for placement was withdrawn. The reason was: 'Related application withdrawn'")
  }

  @Test
  fun `Returns expected description for placement application withdrawn event with placement dates`() {
    val domainEventSummary = DomainEventSummaryImpl.ofType(DomainEventType.APPROVED_PREMISES_PLACEMENT_APPLICATION_WITHDRAWN)

    every { mockDomainEventService.getPlacementApplicationWithdrawnEvent(UUID.fromString(domainEventSummary.id)) } returns buildDomainEvent {
      PlacementApplicationWithdrawnEnvelope(
        id = it,
        timestamp = Instant.now(),
        eventType = "approved-premises.placement-application.withdrawn",
        eventDetails = PlacementApplicationWithdrawnFactory()
          .withWithdrawalReason(PlacementApplicationWithdrawalReason.DUPLICATE_PLACEMENT_REQUEST.toString())
          .withPlacementDates(
            listOf(
              DatePeriod(LocalDate.of(2024, 1, 2), LocalDate.of(2024, 3, 4)),
              DatePeriod(LocalDate.of(2024, 5, 6), LocalDate.of(2024, 7, 8)),
            ),
          )
          .produce(),
      )
    }

    val result = domainEventDescriber.getDescription(domainEventSummary)

    assertThat(result).isEqualTo(
      "A request for placement was withdrawn for dates Tuesday 2 January 2024 to Monday 4 March 2024, Monday 6 May 2024 to Monday 8 July 2024. " +
        "The reason was: 'Duplicate placement request'",
    )
  }

  @Test
  fun `Returns expected description for match request withdrawn event`() {
    val domainEventSummary = DomainEventSummaryImpl.ofType(DomainEventType.APPROVED_PREMISES_MATCH_REQUEST_WITHDRAWN)

    every { mockDomainEventService.getMatchRequestWithdrawnEvent(UUID.fromString(domainEventSummary.id)) } returns buildDomainEvent {
      MatchRequestWithdrawnEnvelope(
        id = it,
        timestamp = Instant.now(),
        eventType = "approved-premises.match-request.withdrawn",
        eventDetails = MatchRequestWithdrawnFactory()
          .withWithdrawalReason(PlacementRequestWithdrawalReason.NO_CAPACITY_DUE_TO_PLACEMENT_PRIORITISATION.toString())
          .withDatePeriod(DatePeriod(LocalDate.of(2024, 1, 2), LocalDate.of(2024, 3, 4)))
          .produce(),
      )
    }

    val result = domainEventDescriber.getDescription(domainEventSummary)

    assertThat(result).isEqualTo(
      "A request for placement was withdrawn for dates Tuesday 2 January 2024 to Monday 4 March 2024. " +
        "The reason was: 'No capacity due to placement prioritisation'",
    )
  }

  @ParameterizedTest
  @CsvSource(
    "rotl,Release directed following parole board or other hearing/decision",
    "releaseFollowingDecisions,Release on Temporary Licence (ROTL)",
    "additionalPlacement,An additional placement on an existing application",
  )
  fun `Returns expected description for request for placement created event, for additional requests`(type: RequestForPlacementType, expectedTypeDescription: String) {
    val domainEventSummary = DomainEventSummaryImpl.ofType(DomainEventType.APPROVED_PREMISES_REQUEST_FOR_PLACEMENT_CREATED)

    every { mockDomainEventService.getRequestForPlacementCreatedEvent(UUID.fromString(domainEventSummary.id)) } returns buildDomainEvent {
      RequestForPlacementCreatedEnvelope(
        id = it,
        timestamp = Instant.now(),
        eventType = "approved-premises.request-for-placement.created",
        eventDetails = RequestForPlacementCreatedFactory()
          .withRequestForPlacementType(type)
          .withExpectedArrival(LocalDate.of(2025, 3, 12))
          .withDuration(7)
          .produce(),
      )
    }

    val result = domainEventDescriber.getDescription(domainEventSummary)

    assertThat(result).isEqualTo(
      "A request for placement was created with the reason '$expectedTypeDescription' with expected arrival date " +
        "Wednesday 12 March 2025 and length of stay of 7 days (Wednesday 19 March 2025)",
    )
  }

  @Test
  fun `Returns expected description for request for placement created event, for initial request`() {
    val domainEventSummary = DomainEventSummaryImpl.ofType(DomainEventType.APPROVED_PREMISES_REQUEST_FOR_PLACEMENT_CREATED)

    every { mockDomainEventService.getRequestForPlacementCreatedEvent(UUID.fromString(domainEventSummary.id)) } returns buildDomainEvent {
      RequestForPlacementCreatedEnvelope(
        id = it,
        timestamp = Instant.now(),
        eventType = "approved-premises.request-for-placement.created",
        eventDetails = RequestForPlacementCreatedFactory()
          .withRequestForPlacementType(RequestForPlacementType.initial)
          .withExpectedArrival(LocalDate.of(2025, 3, 12))
          .withDuration(7)
          .produce(),
      )
    }

    val result = domainEventDescriber.getDescription(domainEventSummary)

    assertThat(result).isEqualTo(
      "An initial request for placement was created with expected arrival date " +
        "Wednesday 12 March 2025 and length of stay of 7 days (Wednesday 19 March 2025)",
    )
  }

  private fun <T> buildDomainEvent(builder: (UUID) -> T): DomainEvent<T> {
    val id = UUID.randomUUID()
    val applicationId = UUID.randomUUID()

    return DomainEvent(
      id = id,
      applicationId = applicationId,
      crn = "SOME-CRN",
      occurredAt = Instant.now(),
      data = builder(id),
    )
  }
}

data class DomainEventSummaryImpl(
  override val id: String,
  override val type: DomainEventType,
  override val occurredAt: OffsetDateTime,
  override val applicationId: UUID?,
  override val assessmentId: UUID?,
  override val bookingId: UUID?,
  override val premisesId: UUID?,
  override val appealId: UUID?,
  override val triggeredByUser: UserEntity?,
) : DomainEventSummary {
  companion object {
    fun ofType(type: DomainEventType) = DomainEventSummaryImpl(
      id = UUID.randomUUID().toString(),
      type = type,
      occurredAt = OffsetDateTime.now(),
      applicationId = null,
      assessmentId = null,
      bookingId = null,
      premisesId = null,
      appealId = null,
      triggeredByUser = null,
    )
  }
}
