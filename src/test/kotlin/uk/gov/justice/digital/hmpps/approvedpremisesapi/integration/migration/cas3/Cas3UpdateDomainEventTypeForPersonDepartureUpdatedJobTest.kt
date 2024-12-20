package uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.migration.cas3

import com.fasterxml.jackson.module.kotlin.readValue
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas1.model.BookingChangedEnvelope
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas1.model.EventType.bookingChanged
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas3.model.CAS3PersonDepartureUpdatedEvent
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas3.model.EventType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.MigrationJobType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.events.BookingChangedFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.events.cas3.CAS3PersonDepartedEventDetailsFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.migration.MigrationJobTestBase
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.DomainEventType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.randomDateTimeBefore
import java.time.Instant
import java.util.UUID

class Cas3UpdateDomainEventTypeForPersonDepartureUpdatedJobTest : MigrationJobTestBase() {

  @Test
  fun `update all person departure updated domain events when event type is person departure`() {
    val personDepartureUpdatedInvalidDomainEvents = generateSequence {
      domainEventFactory.produceAndPersist {
        withType(DomainEventType.CAS3_PERSON_DEPARTURE_UPDATED)
        withData(
          objectMapper.writeValueAsString(
            CAS3PersonDepartureUpdatedEvent(
              id = UUID.randomUUID(),
              timestamp = Instant.now().randomDateTimeBefore(14),
              eventType = EventType.personDeparted,
              eventDetails = CAS3PersonDepartedEventDetailsFactory().produce(),
            ),
          ),
        )
      }
    }.take(5).toList()

    val personDepartureUpdatedValidDomainEvents = generateSequence {
      domainEventFactory.produceAndPersist {
        withType(DomainEventType.CAS3_PERSON_DEPARTURE_UPDATED)
        withData(
          objectMapper.writeValueAsString(
            CAS3PersonDepartureUpdatedEvent(
              id = UUID.randomUUID(),
              timestamp = Instant.now().randomDateTimeBefore(14),
              eventType = EventType.personDepartureUpdated,
              eventDetails = CAS3PersonDepartedEventDetailsFactory().produce(),
            ),
          ),
        )
      }
    }.take(3).toList()

    val approvedPremisesBookingChangedDomainEvents = generateSequence {
      domainEventFactory.produceAndPersist {
        withType(DomainEventType.APPROVED_PREMISES_BOOKING_CHANGED)
        withData(
          objectMapper.writeValueAsString(
            BookingChangedEnvelope(
              id = UUID.randomUUID(),
              timestamp = Instant.now().randomDateTimeBefore(14),
              eventType = bookingChanged,
              eventDetails = BookingChangedFactory().produce(),
            ),
          ),
        )
      }
    }.take(3).toList()

    migrationJobService.runMigrationJob(MigrationJobType.cas3DomainEventTypeForPersonDepartedUpdated)

    personDepartureUpdatedInvalidDomainEvents.forEach {
      val domainEvent = domainEventRepository.findById(it.id)
      Assertions.assertThat(domainEvent).isNotNull()
      val departureUpdatedEvent = objectMapper.readValue<CAS3PersonDepartureUpdatedEvent>(domainEvent.get().data!!)
      Assertions.assertThat(departureUpdatedEvent.id == it.id)
      Assertions.assertThat(departureUpdatedEvent.eventType == EventType.personDepartureUpdated)
    }

    personDepartureUpdatedValidDomainEvents.forEach {
      val domainEvent = domainEventRepository.findById(it.id)
      Assertions.assertThat(domainEvent).isNotNull()
      val departureUpdatedEvent = objectMapper.readValue<CAS3PersonDepartureUpdatedEvent>(domainEvent.get().data!!)
      Assertions.assertThat(departureUpdatedEvent.id == it.id)
      Assertions.assertThat(departureUpdatedEvent.eventType == EventType.personDepartureUpdated)
    }

    approvedPremisesBookingChangedDomainEvents.forEach {
      val domainEvent = domainEventRepository.findById(it.id)
      Assertions.assertThat(domainEvent).isNotNull()
      val bookingChangedEnvelope = objectMapper.readValue<BookingChangedEnvelope>(domainEvent.get().data!!)
      Assertions.assertThat(bookingChangedEnvelope.id == it.id)
      Assertions.assertThat(bookingChangedEnvelope.eventType == bookingChanged)
    }
  }
}
