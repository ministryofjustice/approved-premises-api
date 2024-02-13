package uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.cas2

import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas2TimelineEvent
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.TimelineEventType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.Cas2ApplicationEntity

@Component("Cas2TimelineEventsTransformer")
class TimelineEventsTransformer {
  fun transformApplicationToTimelineEvents(jpa: Cas2ApplicationEntity): List<Cas2TimelineEvent> {
    val timelineEvents: MutableList<Cas2TimelineEvent> = mutableListOf()

    addSubmittedEvent(jpa, timelineEvents)

    addStatusUpdateEvents(jpa, timelineEvents)

    addNoteEvents(jpa, timelineEvents)

    return timelineEvents.sortedByDescending { it.occurredAt }
  }

  private fun addNoteEvents(jpa: Cas2ApplicationEntity, timelineEvents: MutableList<Cas2TimelineEvent>) {
    jpa.notes?.forEach {
      timelineEvents += Cas2TimelineEvent(
        type = TimelineEventType.cas2Note,
        occurredAt = it.createdAt.toInstant(),
        label = "Note",
        createdByName = it.createdByNomisUser.name,
        body = it.body,
      )
    }
  }

  private fun addStatusUpdateEvents(jpa: Cas2ApplicationEntity, timelineEvents: MutableList<Cas2TimelineEvent>) {
    jpa.statusUpdates?.forEach {
      timelineEvents += Cas2TimelineEvent(
        type = TimelineEventType.cas2StatusUpdate,
        occurredAt = it.createdAt?.toInstant()!!,
        label = it.label,
        createdByName = it.assessor.name,
        body = it.statusUpdateDetails?.joinToString { detail -> detail.label },
      )
    }
  }

  private fun addSubmittedEvent(jpa: Cas2ApplicationEntity, timelineEvents: MutableList<Cas2TimelineEvent>) {
    if (jpa.submittedAt !== null) {
      val submittedAtEvent = Cas2TimelineEvent(
        type = TimelineEventType.cas2ApplicationSubmitted,
        occurredAt = jpa.submittedAt?.toInstant()!!,
        label = "Application submitted",
        createdByName = jpa.createdByUser.name,
      )
      timelineEvents += submittedAtEvent
    }
  }
}
