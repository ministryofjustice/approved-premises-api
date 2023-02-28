package uk.gov.justice.digital.hmpps.approvedpremisesapi.controller

import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.EventsApiDelegate
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.model.ApplicationAssessedEnvelope
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.model.ApplicationSubmittedEnvelope
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.model.BookingMadeEnvelope
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.model.PersonArrivedEnvelope
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.model.PersonNotArrivedEnvelope
import uk.gov.justice.digital.hmpps.approvedpremisesapi.problem.NotFoundProblem
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.DomainEventService
import java.util.UUID

@Service
class DomainEventsController(
  private val domainEventService: DomainEventService
) : EventsApiDelegate {
  override fun eventsApplicationSubmittedEventIdGet(eventId: UUID): ResponseEntity<ApplicationSubmittedEnvelope> {
    val event = domainEventService.getApplicationSubmittedDomainEvent(eventId)
      ?: throw NotFoundProblem(eventId, "DomainEvent")

    return ResponseEntity.ok(event.data)
  }

  override fun eventsApplicationAssessedEventIdGet(eventId: UUID): ResponseEntity<ApplicationAssessedEnvelope> {
    val event = domainEventService.getApplicationAssessedDomainEvent(eventId)
      ?: throw NotFoundProblem(eventId, "DomainEvent")

    return ResponseEntity.ok(event.data)
  }

  override fun eventsBookingMadeEventIdGet(eventId: UUID): ResponseEntity<BookingMadeEnvelope> {
    val event = domainEventService.getBookingMadeEvent(eventId)
      ?: throw NotFoundProblem(eventId, "DomainEvent")

    return ResponseEntity.ok(event.data)
  }

  override fun eventsPersonArrivedEventIdGet(eventId: UUID): ResponseEntity<PersonArrivedEnvelope> {
    val event = domainEventService.getPersonArrivedEvent(eventId)
      ?: throw NotFoundProblem(eventId, "DomainEvent")

    return ResponseEntity.ok(event.data)
  }

  override fun eventsPersonNotArrivedEventIdGet(eventId: UUID): ResponseEntity<PersonNotArrivedEnvelope> {
    val event = domainEventService.getPersonNotArrivedEvent(eventId)
      ?: throw NotFoundProblem(eventId, "DomainEvent")

    return ResponseEntity.ok(event.data)
  }
}
