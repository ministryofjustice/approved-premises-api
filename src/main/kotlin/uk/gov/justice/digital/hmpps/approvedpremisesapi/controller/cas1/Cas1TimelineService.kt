package uk.gov.justice.digital.hmpps.approvedpremisesapi.controller.cas1

import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas1TimelineEvent
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.TimelineEvent
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.DomainEventSummary
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.ApplicationTimelineNoteService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1.Cas1DomainEventService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.ApplicationTimelineNoteTransformer
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.ApplicationTimelineTransformer
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.cas1.Cas1ApplicationTimelineTransformer
import java.util.UUID
import kotlin.collections.plusAssign

@Service
class Cas1TimelineService(
  private val applicationTimelineNoteService: ApplicationTimelineNoteService,
  private val applicationTimelineNoteTransformer: ApplicationTimelineNoteTransformer,
  private val domainEventService: Cas1DomainEventService,
  private val applicationTimelineTransformer: ApplicationTimelineTransformer,
  private val cas1applicationTimelineTransformer: Cas1ApplicationTimelineTransformer,
) {
  @Deprecated("To be deleted")
  fun getApplicationTimeline(applicationId: UUID): List<TimelineEvent> {
    val timelineEvents = mutableListOf<TimelineEvent>()
    timelineEvents += getDomainEventsForApplication(applicationId)
    timelineEvents += getApplicationNotesForApplication(applicationId)
    return timelineEvents
  }

  fun getSpaceBookingTimeline(bookingId: UUID): List<TimelineEvent> =
    toTimelineEvent(domainEventService.getAllDomainEventsForSpaceBooking(bookingId))

  private fun getDomainEventsForApplication(applicationId: UUID) =
    toTimelineEvent(domainEventService.getAllDomainEventsForApplication(applicationId))

  private fun getApplicationNotesForApplication(applicationId: UUID): List<TimelineEvent> {
    val noteEntities = applicationTimelineNoteService.getApplicationTimelineNotesByApplicationId(applicationId)
    return noteEntities.map {
      applicationTimelineNoteTransformer.transformToTimelineEvents(it)
    }
  }

  private fun toTimelineEvent(domainEvents: List<DomainEventSummary>) = domainEvents.map {
    applicationTimelineTransformer.transformDomainEventSummaryToTimelineEvent(it)
  }

  fun getApplicationTimelineEvents(applicationId: UUID): List<Cas1TimelineEvent> {
    val cas1TimelineEvents = mutableListOf<Cas1TimelineEvent>()
    cas1TimelineEvents += getTimelineEvents(applicationId)
    cas1TimelineEvents += getNotesTimelineEvents(applicationId)
    return cas1TimelineEvents
  }

  private fun getTimelineEvents(applicationId: UUID) =
    toTimelineEvents(domainEventService.getAllDomainEventsForApplication(applicationId))

  private fun getNotesTimelineEvents(applicationId: UUID): List<Cas1TimelineEvent> {
    val noteEntities = applicationTimelineNoteService.getApplicationTimelineNotesByApplicationId(applicationId)
    return noteEntities.map {
      applicationTimelineNoteTransformer.transformToCas1TimelineEvents(it)
    }
  }

  private fun toTimelineEvents(domainEvents: List<DomainEventSummary>) = domainEvents.map {
    cas1applicationTimelineTransformer.transformDomainEventSummaryToTimelineEvent(it)
  }
}
