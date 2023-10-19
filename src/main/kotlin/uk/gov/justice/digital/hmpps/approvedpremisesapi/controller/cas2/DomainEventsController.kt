package uk.gov.justice.digital.hmpps.approvedpremisesapi.controller.cas2

import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.CAS2ApplyEventsApiDelegate
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas2.model.Cas2ApplicationSubmittedEvent
import uk.gov.justice.digital.hmpps.approvedpremisesapi.problem.NotFoundProblem
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas2.DomainEventService
import java.util.UUID

@Service(
  "uk.gov.justice.digital.hmpps.approvedpremisesapi.controller.cas2.DomainEventsController",
)
class DomainEventsController(private val domainEventService: DomainEventService) : CAS2ApplyEventsApiDelegate {

  override fun eventsCas2ApplicationSubmittedEventIdGet(eventId: UUID): ResponseEntity<Cas2ApplicationSubmittedEvent> {
    val event = domainEventService.getCas2ApplicationSubmittedDomainEvent(eventId)
      ?: throw NotFoundProblem(eventId, "DomainEvent")

    return ResponseEntity.ok(event.data)
  }
}
